package com.example.cong_jh.stereosync;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;


public class DashBoard extends AppCompatActivity {

    private LinearLayout lListen;
    private LinearLayout lShare;

    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        lListen = (LinearLayout)findViewById(R.id.listenLayout);
        lShare = (LinearLayout)findViewById(R.id.shareLayout);

        lListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                i = new Intent(DashBoard.this,Listen.class);
                startActivity(i);
            }
        });
        lShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                i = new Intent(DashBoard.this,ShareMusic.class);
                startActivity(i);
            }
        });


    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
