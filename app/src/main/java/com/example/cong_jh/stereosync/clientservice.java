package com.example.cong_jh.stereosync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.Socket;


public class clientservice extends Service {

    public static Socket socket;
    clientprocess c;
    InetAddress serverip = null;
    Messenger messenger;
    int pktsize;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        messenger = (Messenger) extras.get("MESSENGER");
        serverip = (InetAddress) extras.get("SERVER_IP");
        pktsize = (int) extras.get("PACKET_SIZE");

        c = new clientprocess(this, serverip, messenger, 15890, pktsize);
        c.start();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        try {
            if(socket!=null)
                socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        super.onDestroy();
    }
}


class clientprocess extends Thread {

    InetAddress serverip = null;
    Context context;
    Socket socket = null;
    MediaPlayer mediaPlayer;
    Messenger messenger;
    int pktsize;
    int port;


    public clientprocess(Context context, InetAddress serverip, Messenger messenger, int port, int pktsize) {
        this.context = context;
        this.serverip = serverip;
        this.messenger = messenger;
        this.port = port;
        this.pktsize = pktsize;
    }



    @Override
    public void run() {
        int size = pktsize * 1024;
        int len = 0;
        int totlen = 0;
        Message msg;

        try {
            Log.i("abcde","왜1 "+ String.valueOf(port));
            socket = new Socket(serverip, port);
            Log.i("abcde","왜 "+ String.valueOf(socket.getPort()));
            clientservice.socket = socket;
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            String path = "";

            File f = null;
            int flag;

            do {
                totlen = 0;
                flag = 0;
                String request = in.readUTF();

                if (request.equals("List")) {
                    f = new File(Environment.getExternalStorageDirectory() + "/audio.txt");
                    if (f.exists()) {
                        DataInputStream din = new DataInputStream(new FileInputStream(f));
                        byte[] sendData = new byte[size];
                        long start = System.currentTimeMillis();
                        out.writeLong(f.length());
                        while ((len = din.read(sendData)) != -1) {
                            out.write(sendData, 0, len);
                            totlen += len;
                        }

                        if(flag==1)
                        {
                            msg = Message.obtain();
                            msg.what = ShareMusic.PACKET_SENT;
                            msg.obj = f.getName() + " File Data Sending Stopped!";
                            messenger.send(msg);
                            continue;
                        }
                        out.flush();


                        long stop = System.currentTimeMillis();
                        double time = (stop - start) / 1000.0;
                        double speed = (totlen / time) / 1048576.0;
                        msg = Message.obtain();
                        msg.what = ShareMusic.PACKET_SENT;
                        msg.obj = f.length() + "Bytes Sent to " + serverip.toString()
                                + " ! in " + time + " secs. Speed : " + speed + " MB/s ";
                        messenger.send(msg);
                        din.close();

                        if(request.equals("List"))
                            f.delete();

                    } else {
                        out.writeLong(0);
                        msg = Message.obtain();
                        msg.what = ShareMusic.PACKET_SENT;
                        msg.obj = path + "File Doesn't Exist!";
                        messenger.send(msg);
                    }
                }
                else if(request.equals("Song")){
                    out.flush();
                    path = in.readUTF();
                    f = new File(path);
                    if (f.exists()) {
                        DataInputStream din = new DataInputStream(new FileInputStream(f));
                        byte[] sendData = new byte[size];
                        long start = System.currentTimeMillis();
                        out.writeLong(f.length());
                        Log.i("abcde","클라 파일 크기"+String.valueOf(f.length()));
                        while ((len = din.read(sendData)) != -1) {
                            out.write(sendData, 0, len);
                            totlen += len;
                        }
                        out.flush();
                        if(mediaPlayer!=null){
                            mediaPlayer.release();
                        }
                        mediaPlayer = new MediaPlayer();
                        try {
                            mediaPlayer.setDataSource(path);
                            mediaPlayer.prepare();
                            //mediaPlayer.start();
                        } catch (Exception e){
                            mediaPlayer.release();
                        }

                        if(flag==1)
                        {
                            msg = Message.obtain();
                            msg.what = ShareMusic.PACKET_SENT;
                            msg.obj = f.getName() + " File Data Sending Stopped!";
                            messenger.send(msg);
                            continue;
                        }

                        long stop = System.currentTimeMillis();
                        double time = (stop - start) / 1000.0;
                        double speed = (totlen / time) / 1048576.0;
                        msg = Message.obtain();
                        msg.what = ShareMusic.PACKET_SENT;
                        msg.obj = f.length() + "Bytes Sent to " + serverip.toString()
                                + " ! in " + time + " secs. Speed : " + speed + " MB/s ";
                        messenger.send(msg);
                        din.close();

                    } else {
                        out.writeLong(0);
                        msg = Message.obtain();
                        msg.what = ShareMusic.PACKET_SENT;
                        msg.obj = path + "File Doesn't Exist!";
                        messenger.send(msg);
                    }
                }
                else if(request.equals("Bye"))
                {
                    msg = Message.obtain();
                    msg.what = ShareMusic.PACKET_SENT;
                    msg.obj = "Friend Disconnected!!";
                    messenger.send(msg);
                    break;
                }
                else if(request.equals("Resume"))
                {
                    mediaPlayer.start();
                }
                else if(request.equals("Pause"))
                {
                    mediaPlayer.pause();
                }
                else if(request.equals("Sync")){
                    try {
                        mediaPlayer.start();
                        Log.i("abcde","클라 : "+ String.valueOf(System.currentTimeMillis()));
                    } catch (Exception e){
                        mediaPlayer.release();
                    }
                }
                else {
                    continue; // CHANGE TO RETURN LATER.
                }
            }while(true);

        } catch (Exception e) {
            msg = Message.obtain();
            msg.what = ShareMusic.PACKET_SENT;
            msg.obj = "Error : " + e.toString();
            try {
                messenger.send(msg);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
