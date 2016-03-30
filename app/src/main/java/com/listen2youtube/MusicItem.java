package com.listen2youtube;

import android.content.ContentUris;
import android.net.Uri;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by khang on 28/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class MusicItem{
    long id;
    String artist;
    String title;
    String album;
    long duration;

    public MusicItem (){}

    public MusicItem(long id, String artist, String title, String album, long duration) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.duration = duration;
    }

    public long getId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    public Uri getURI() {
        return ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
