package com.listen2youtube;

import android.app.Application;

/**
 * Created by khang on 03/04/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class MainApplication extends Application {
    private static final String TAG = "MainApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Settings.init(this);
    }
}
