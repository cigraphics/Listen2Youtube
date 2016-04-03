package com.listen2youtube.service;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Created by khang on 30/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class SongInfo {
    String title;
    String textThumbnail;
    Bitmap bitmapThumbnail;
    Uri uri;

    public SongInfo(String title, String textThumbnail, Bitmap bitmapThumbnail, Uri uri) {
        this.title = title;
        this.textThumbnail = textThumbnail;
        this.bitmapThumbnail = bitmapThumbnail;
        this.uri = uri;
    }
}
