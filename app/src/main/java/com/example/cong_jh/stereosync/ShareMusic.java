package com.example.cong_jh.stereosync;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;


public class ShareMusic extends Activity {

    public static final int PACKET_SENT = 1;
    public static final int CURRENT = 2;
    public static final int KILL = 3;


    private TextView tvMessage;
    private TextView tvShareStatus;
    private ImageButton bSend;
    private InetAddress serverip = null;

    public static String AllSongs;

    Intent startService;
    int curr = 0;
    String clmsg = "";
    int count = 0;
    int reqno = 0;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PACKET_SENT:
                    reqno ++;
                    clmsg = "Request " + reqno  + " : " + (String) msg.obj;
                    tvMessage.setText(clmsg);
                    break;

                case CURRENT:
                    curr += (int) msg.obj;

                case KILL:
                    count++;
                    if (count == 1)
                        stopService(new Intent(ShareMusic.this, clientservice.class));

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharemusic);

        tvMessage = (TextView) findViewById(R.id.tvMessage);
        bSend = (ImageButton) findViewById(R.id.bSend);
        tvShareStatus = (TextView) findViewById(R.id.tvShareStatus);

        getAllSongs();

        bSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    tvShareStatus.setText("Finding Friend");

                    getServerAddress();

                    startService = new Intent(ShareMusic.this, clientservice.class);
                    startService.putExtra("MESSENGER", new Messenger(mHandler));
                    startService.putExtra("SERVER_IP", serverip);
                    startService.putExtra("PACKET_SIZE", 60);

                    tvShareStatus.setText("Found Friend! Click 'Share' to share music!\n"+serverip.toString());

                    ShareMusic.this.startService(startService);
            }
        });
    }


    public void getServerAddress() {
        final WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        final DhcpInfo dhcp = manager.getDhcpInfo();
        final String address = Formatter.formatIpAddress(dhcp.gateway);

        try {
            serverip = InetAddress.getByName(address);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(ShareMusic.this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }


    public void getAllSongs() {
        AllSongs = "";
        File f = new File(Environment.getExternalStorageDirectory() + "/audio.txt");
        if(f.exists())
            f.delete();
        FileOutputStream fout = null;

        try {
            fout = new FileOutputStream(f);

            DataOutputStream dout = new DataOutputStream(fout);
            ContentResolver musicResolver = getContentResolver();
            Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Cursor musicCursor = musicResolver.query(musicUri, null, MediaStore.Audio.Media.DATA + " LIKE ? ", new String[]{"%/Music%"}, null);

            if (musicCursor != null && musicCursor.moveToFirst()) {
                int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int pathColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

                do {
                    long id = musicCursor.getLong(idColumn);
                    String title = musicCursor.getString(titleColumn);
                    String artist = musicCursor.getString(artistColumn);
                    String path = musicCursor.getString(pathColumn);
                    Log.i("abcde",title);
                    AllSongs = id + "\n" + title + "\n" + artist + "\n" + path + "\n";
                    dout.write(AllSongs.getBytes());
                } while (musicCursor.moveToNext());
            }
            musicCursor.close();
            dout.close();
        } catch (Exception e) {
            Log.i("abcde","익셉션");
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if(startService!=null)
            stopService(startService);
        super.onDestroy();
    }
};