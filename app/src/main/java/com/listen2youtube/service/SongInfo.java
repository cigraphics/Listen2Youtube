package com.listen2youtube.service;

import android.net.Uri;

/**
 * Created by khang on 30/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class SongInfo {
    String titile;
    Uri uri;

    public SongInfo(String titile, Uri uri) {
        this.titile = titile;
        this.uri = uri;
    }
}
