package com.example.cong_jh.stereosync;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.MediaController;

public class MusicController extends MediaController {

    Context myContext;

    public MusicController(Context context) {
        super(context);
        myContext = context;
        Log.i("abcde","Controller start");
    }

    @Override
    public void hide() {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        int keyCode = event.getKeyCode();
        if(keyCode == KeyEvent.KEYCODE_BACK){

            MusicScreen obj = (MusicScreen) myContext;

            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            obj.startActivity(startMain);

            return true;
        }

        return super.dispatchKeyEvent(event);
    }
}
