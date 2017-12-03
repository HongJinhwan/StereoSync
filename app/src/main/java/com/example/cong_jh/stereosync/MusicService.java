package com.example.cong_jh.stereosync;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static final int PLAYER = 2;
    public static final int ERROR = 3;
    int start = 0;

    private MediaPlayer player;
    private MediaController controller;
    private ArrayList<Song> songs;
    private int songPosn;

    getFile g = null;
    PauseThread pt = null;
    ResumeThread rt = null;
    private boolean down = false;
    private boolean wait = true;

    private String songTitle = "";
    private long songId = -1;
    private static final int NOTIFY_ID = 1;

    ServerSocket serversocket = null;
    Socket clientsocket = null;

    private final IBinder musicBind = new MusicBinder();
    private MusicScreen mainactivity;


    private final Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case PLAYER :
                    down = false;
                    File localf = new File( (String)msg.obj );
                    try {
                        FileInputStream fin = new FileInputStream(localf);
                        player.setDataSource(fin.getFD());
                        player.prepareAsync();
                        Log.i("abcde","sync before");
                    } catch (Exception e) {
                        Log.i("abcde","sync exception");
                        Toast.makeText(mainactivity, "An error Occured while playing the song!", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    break;

                case ERROR :
                    down = false;
                    Toast.makeText(mainactivity, (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };


    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        songPosn = 0;
        player = new MediaPlayer();
        initMusicPlayer();
    }

    public boolean isDown()
    {
        return down;
    }

    public boolean isWait()
    {
        return wait;
    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

    }

    public void setSockets() {
        serversocket = Listen.serversocket;
        clientsocket = Listen.clientsocket;
    }

    public void setContext(Context context) {
        mainactivity = (MusicScreen) context;
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public void setController(MediaController controller) {
        this.controller = controller;
    }

    public void setSong(int songPosn) {
        if(isDown())
            return;
        this.songPosn = songPosn;
    }

    public void playSong() {

        wait = true;
        player.reset();
        Song playSong = songs.get(songPosn);
        //
        removeDir();
        //
        songTitle = playSong.getTitle();
        songId = playSong.getID();

        String songPath = playSong.getPath();
        File localf = new File(Environment.getExternalStorageDirectory() + "/SharedMusic/" + songId + " " + songTitle + ".mp3");

        if(!localf.exists())
        {
            if(isDown())
                down = true;
            g = new getFile(songId,songTitle,songPath,clientsocket,mHandler);
            g.start();
            return;
        }

        Uri u = Uri.parse(localf.getAbsolutePath());
        try {
            player.setDataSource(this, u);
        } catch (Exception e) {
        }

        try {
            player.prepareAsync();
        }catch (Exception e){
            Toast.makeText(mainactivity,"An error Occured!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if(start==1){
            Thread syncThread = new syncThread(clientsocket);
            syncThread.start();
            try {
                Thread.sleep(100 * start);
                start = 1;
            }catch(Exception e){}
            Log.i("abcde","start == 1");
        }

        mp.start();
        wait = false;
        controller.show(0);

        Intent notIntent = new Intent(this, MusicScreen.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        if(start==0){
            Thread syncThread = new syncThread(clientsocket);
            syncThread.start();
            start = 1;
            Log.i("abcde","start == 0");
        }

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);

    }

    public int getPosn() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
        pt = new PauseThread(clientsocket);
        pt.start();
    }

    public void seek(int posn) {
        player.seekTo(posn);
    }

    public void go() {
        player.start();
        rt = new ResumeThread(clientsocket);
        rt.start();
    }

    public void playPrev() {
        songPosn--;
        if (songPosn < 0)
            songPosn = songs.size() - 3;
        playSong();
    }

    public void playNext() {
        songPosn++;
        if (songPosn == songs.size() - 2)
            songPosn = 0;
        playSong();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    public void removeDir() {
        //File localf = new File(Environment.getExternalStorageDirectory() + "/SharedMusic/" + songId + " " + songTitle + ".mp3");
        if (g != null && g.isAlive()) {
            //Log.e("abcde", "destroy: presongid" + presongId + "  presongTitle " + presongTitle);
            onDestroy();
        }
        File fileLoc = new File(Environment.getExternalStorageDirectory() + "/SharedMusic/");
        String[] songList= fileLoc.list();
        Log.e("abcdef", String.valueOf(songList));

        if(songList != null){
            for(int i=0;i<songList.length;i++){
                String filename = songList[i];
                Log.e("abcdef",filename);
                File localf = new File(Environment.getExternalStorageDirectory() + "/SharedMusic/" + filename);
                Log.e("abcdef", String.valueOf(localf));
                if(localf.exists()) {
                    Log.e("abcde", "removeDir" + String.valueOf(localf.exists()));
                    localf.delete();    //root 삭제
                }
            }
        }
        Log.e("abcde","delete end");
    }


    class syncThread extends Thread {
        Socket clientsocket = null;

        public syncThread(Socket clientsocket) {
            this.clientsocket = clientsocket;

            Log.i("abcde","sync const");
        }

        @Override
        public void run() {
            super.run();
            Log.i("abcde","sync run be");
            try {
                Log.i("abcde","sync run try");
                DataOutputStream out;
                out = new DataOutputStream(clientsocket.getOutputStream());
                out.writeUTF("Sync");
                Log.i("abcde", "서버 : " + String.valueOf(System.currentTimeMillis()));
            }
            catch (Exception e){
                Log.i("abcde","sync run ex");
                e.printStackTrace();}
        }
    }
}