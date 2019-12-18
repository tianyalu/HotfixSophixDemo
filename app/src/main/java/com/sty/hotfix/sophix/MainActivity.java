package com.sty.hotfix.sophix;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView tvTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTextView = findViewById(R.id.tv_text);

        tvTextView.setText("版本号：" + BuildConfig.VERSION_NAME);

//        tvTextView.setText("Hotfix 版本号：" + BuildConfig.VERSION_NAME + " lalallalall");
    }
}
