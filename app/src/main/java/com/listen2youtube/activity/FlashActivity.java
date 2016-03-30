package com.listen2youtube.activity;

import android.content.Intent;
import android.os.Bundle;
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            status.setVisibility(View.VISIBLE);
                        }
                    });
                    if (Utils.isOnline(FlashActivity.this)) {
                        while (true) {

                            URL url = new URL(Constants.HOST_SERVER);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.connect();
                            Log.e(TAG, "run - line 47: " + connection.getResponseCode());
                            if (connection.getResponseCode() / 100 == 2) {
                                connection.disconnect();
                                Thread.sleep(1000);
                                startMainActivity();
                                break;
                            }
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                status.setText("Status: you are in offline mode.");
                            }
                        });
                        Thread.sleep(1000);
                        startMainActivity();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            status.setText("Status: unknown error, check your connection!");
                        }
                    });
                    startMainActivity();
                }
            }
        }).start();
    }

    void startMainActivity() {
        Intent mainActivity = new Intent(FlashActivity.this, MainActivity.class);
        startActivity(mainActivity);
        finish();
    }
}
