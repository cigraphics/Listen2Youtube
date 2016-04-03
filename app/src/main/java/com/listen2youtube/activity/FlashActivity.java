package com.listen2youtube.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;

import com.listen2youtube.Constants;
import com.listen2youtube.R;
import com.listen2youtube.Utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by khang on 25/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class FlashActivity extends AppCompatActivity {
    private static final String TAG = "FlashActivity";

    AppCompatTextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flash_activity);
        status = (AppCompatTextView) findViewById(R.id.flash_status);
        status.setVisibility(View.VISIBLE);
        status.setText("Status: Checking permission");
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(FlashActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(FlashActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        99);
            else if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 99);
            } else
                doWakeUpServer();
        } else
            doWakeUpServer();
    }

    void doWakeUpServer() {
        if (Utils.isOnline(this)) {
            status.setText("Status: starting server from sleeping mode...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    URL url;
                    try {
                        url = new URL(Constants.HOST_SERVER);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
                        Log.e(TAG, "run - line 47: " + connection.getResponseCode());
                        Thread.sleep(1000);
                        startMainActivity();
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                status.setText("Error: " + e.getMessage());
                            }
                        });
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        startMainActivity();
                    }
                }
            }).start();
        } else {
            status.setText("Status: you are in offline mode.");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startMainActivity();
                }
            }, 1000);
        }
    }

    void startMainActivity() {
        Intent mainActivity = new Intent(FlashActivity.this, MainActivity.class);
        startActivity(mainActivity);
        finish();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 99) {
            if (ContextCompat.checkSelfPermission(FlashActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(FlashActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        99);
            else if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 99);
            } else
                doWakeUpServer();
        }
    }
}
