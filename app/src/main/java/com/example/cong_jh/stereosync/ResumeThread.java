package com.example.cong_jh.stereosync;

import android.util.Log;

import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Created by qowhd on 2017-11-18.
 */

public class ResumeThread extends Thread {

    Socket clientsocket;
    int stop = 0;

    private boolean mDebug = false;

    public ResumeThread(Socket clientsocket) {

        this.clientsocket = clientsocket;
    }

    @Override
    public void run() {

        int len;
        boolean once = true,send=true;
        int totlen = 0;
        int size = 60*1024;

        try {
            DataOutputStream out;

            try{
                out = new DataOutputStream(clientsocket.getOutputStream());
                out.writeUTF("Resume");

                if(mDebug)
                    Log.i("MUSIC SERVICE", "RESUME");


            }catch (Exception e){

                if(mDebug)
                    Log.i("MUSIC SERVICE", e.toString());

                e.printStackTrace();
                return;
            }

        }catch (Exception e){

            if(mDebug)
                Log.i("MUSIC SERVICE", e.toString());

            e.printStackTrace();
        }
    }
}