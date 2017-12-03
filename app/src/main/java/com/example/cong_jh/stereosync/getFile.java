package com.example.cong_jh.stereosync;

import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;


class getFile extends Thread {

    long songId;
    String songTitle;
    String songPath;
    Socket clientsocket;
    Handler mHandler;

    public getFile(long songId, String songTitle, String songPath, Socket clientsocket, Handler mHandler) {
        this.songId = songId;
        this.songTitle = songTitle;
        this.songPath = songPath;
        this.clientsocket = clientsocket;
        this.mHandler = mHandler;
    }

    @Override
    public void run() {
        int len;
        boolean once = true,send=true;
        int size = 60*1024;

        File localf = new File(Environment.getExternalStorageDirectory() + "/SharedMusic/" + songId + " " + songTitle + ".mp3");
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long available = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

        try {
            long fileSize;
            byte[] receiveData = null;
            receiveData = new byte[size];

            DataInputStream in;
            DataOutputStream out;
            try{
                out = new DataOutputStream(clientsocket.getOutputStream());
                out.writeUTF("Song");
                out.writeUTF(songPath);                Log.i("abcde", songPath);
                in = new DataInputStream(clientsocket.getInputStream());
                fileSize = in.readLong();
            }catch (Exception e){
                e.printStackTrace();
                mHandler.obtainMessage(MusicService.ERROR,"연결이 종료되었습니다!").sendToTarget();
                return;
            }

            if(fileSize==0)
            {
                mHandler.obtainMessage(MusicService.ERROR,songTitle + "Listen 스마트폰에서 파일을 찾을 수 없습니다").sendToTarget();
                return;
            }

            if(fileSize>available)
            {
                Log.i("abcde", "서버 파일 크기 : "+String.valueOf(fileSize));
                mHandler.obtainMessage(MusicService.ERROR,"저장 공간이 충분하지 않습니다.").sendToTarget();
                return;
            }


            FileOutputStream fout = new FileOutputStream(localf, true);
            DataOutputStream dout = new DataOutputStream(fout);

            while (fileSize > 0 && ((len = in.read(receiveData, 0, (int) Math.min(receiveData.length, fileSize))) != -1)) {
                if (once) {
                    once = false;
                }
                dout.write(receiveData, 0, len);
                fileSize -= len;
            }
            dout.flush();
            dout.close();
            mHandler.obtainMessage(MusicService.PLAYER,localf.getAbsolutePath()).sendToTarget();
        }catch (Exception e){
            e.printStackTrace();
            mHandler.obtainMessage(MusicService.ERROR,"파일전송에러!").sendToTarget();
        }
    }

}