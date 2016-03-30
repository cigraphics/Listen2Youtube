package com.listen2youtube;

/**
 * Created by khang on 28/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class Playlist{
    long id;
    String title;

    public Playlist() {
    }

    public Playlist(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
