package com.example.cong_jh.stereosync;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicScreen extends AppCompatActivity implements MediaController.MediaPlayerControl {

    private MusicController controller;

    private ArrayList<Song> songList;
    private ListView songView;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    private boolean playbackPaused = false;
    private boolean mDebug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musicscreen);

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });
        songList.add(new Song(0, "", "", ""));
        songList.add(new Song(0, "", "", ""));
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        setController();
        Log.i("abcde","screen setcontroller");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("abcde","screen start");
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("abcde","screen serviceconnected");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicSrv = binder.getService();
            musicSrv.setList(songList);
            musicSrv.setSockets();
            musicSrv.setContext(MusicScreen.this);
            musicSrv.setController(controller);

            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void songPicked(View view) {
        if(musicSrv.isDown())
        {
            Toast.makeText(this, "Already Downloading a song, Please Wait", Toast.LENGTH_LONG).show();
            return;
        }

        int a = Integer.parseInt(view.getTag().toString());
        if (a < songList.size() - 2) {
            musicSrv.setSong(a);
            musicSrv.playSong();

            Log.i("abcde","screen playsong");
        }
    }


    private void setController() {
        Log.i("abcde","screen setc");
        if (controller == null)
            controller = new MusicController(this);

        controller.setPrevNextListeners(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playNext();
                    }
                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPrev();
                    }
                });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }


    private void playNext() {

        if(musicSrv.isWait()&&musicSrv.isDown())
        {
            Toast.makeText(this, "Already Downloading a song, Please Wait", Toast.LENGTH_LONG).show();
            return;
        }
        musicSrv.playNext();
    }

    private void playPrev() {

        if(musicSrv.isWait()&&musicSrv.isDown())
        {
            Toast.makeText(this, "Already Downloading a song, Please Wait", Toast.LENGTH_LONG).show();
            return;
        }
        musicSrv.playPrev();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_musicscreen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_shuffle:
                break;

            case R.id.action_end:
                if(playIntent!=null)
                    stopService(playIntent);
                musicSrv = null;
                Intent intent = new Intent(this,Listen.class);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void getSongList() {

        File f = new File(Environment.getExternalStorageDirectory() + "/audio.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String s;
            while ((s = br.readLine()) != null) {
                long id = Long.parseLong(s);
                s = br.readLine();
                String title = s;
                s = br.readLine();
                String artist = s;
                s = br.readLine();
                String path = s;
                songList.add(new Song(id, title, artist, path));
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        f.delete();
    }

    @Override
    public void start() {
        if(!musicSrv.isWait()) {
            musicSrv.go();
        }
    }

    @Override
    public void pause() {
        playbackPaused = true;
        if(musicSrv.isPng()){
            musicSrv.pausePlayer();
        }
    }


    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && !musicSrv.isWait()) {
            return musicSrv.getDur();

        } else return 0;
    }


    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && !musicSrv.isWait()) {;
            return musicSrv.getPosn();
        } else return 0;
    }


    @Override
    public void seekTo(int pos) {
        if(musicSrv!=null&&!musicSrv.isWait()){
            musicSrv.seek(pos);
        }

    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound){
            return musicSrv.isPng();
        }
        else return false;
    }


    @Override
    public int getBufferPercentage() {
        return 0;
    }


    @Override
    public boolean canPause() {
        if(musicSrv!=null){
            return musicSrv.isPng();
        }
        return true;
    }


    @Override
    public boolean canSeekBackward() {
        if(musicSrv!=null)
        {
            return musicSrv.isPng();
        }
        return true;
    }

    @Override
    public boolean canSeekForward() {
        if(musicSrv!=null)
        {
            return musicSrv.isPng();
        }

        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    protected void onDestroy() {
        if(playIntent!=null)
        stopService(playIntent);
        Thread byeThread = new byeThread(Listen.clientsocket);
        byeThread.start();
        musicSrv = null;
        super.onDestroy();
    }

    class byeThread extends Thread {
        Socket clientsocket = null;

        public byeThread(Socket clientsocket) {
            this.clientsocket = clientsocket;
        }

        @Override
        public void run() {
            super.run();
            Log.i("abcde","sync run be");
            try {
                if(Listen.clientsocket!=null) {
                    DataOutputStream out = new DataOutputStream(Listen.clientsocket.getOutputStream());
                    out.writeUTF("Bye");
                    out.flush();
                    Listen.clientsocket.close();
                    if (Listen.serversocket != null)
                        Listen.serversocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}