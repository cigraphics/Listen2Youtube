package com.listen2youtube;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.listen2youtube.fragment.LocalFileFragment;
import com.listen2youtube.fragment.PlaylistFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by khang on 28/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class PlayListHelper {
    private static final String TAG = "PlayListHelper";

    public static Uri createPlaylist(ContentResolver contentResolver, String name) {
        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.NAME, name);
        return contentResolver.insert(uri, values);
    }

    public static List<PlaylistFragment.PlaylistItem> fetchAllPlaylist(ContentResolver resolver, PlaylistFragment playlistFragment) {
        List<PlaylistFragment.PlaylistItem> result = new ArrayList<>();
        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID),
                    nameCol = cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            do {
                long pId = cursor.getLong(idCol);
                result.add(playlistFragment.new PlaylistItem(pId,
                        cursor.getString(nameCol),
                        fetchAllSongInPlaylist(playlistFragment, resolver, pId)));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }

    public static List<Playlist> fetchAllPlaylist(ContentResolver resolver) {
        List<Playlist> result = new ArrayList<>();
        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID),
                    nameCol = cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            do {
                long pId = cursor.getLong(idCol);
                result.add(new Playlist(pId,
                        cursor.getString(nameCol)));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }

    public static void insertSongToPlaylist(ContentResolver resolver, LocalFileFragment.LocalFileItem item, @NonNull Uri playlist) {
        insertSongToPlaylist(resolver, item, parsePlaylistId(playlist));
    }

    public static long parsePlaylistId(Uri uri){
        Pattern pattern = Pattern.compile("(\\d+)(?!.*\\d)");
        Matcher matcher = pattern.matcher(uri.toString());
        if (matcher.find())
            return Long.parseLong(matcher.group(1));
        return 0;
    }

    public static void insertSongToPlaylist(ContentResolver resolver, LocalFileFragment.LocalFileItem item, long playlistId) {
        String[] cols = new String[]{
                "count(*)"
        };
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cur = resolver.query(uri, cols, null, null, null);
        if (cur != null && cur.moveToFirst()) {
            final int base = cur.getInt(0);
            cur.close();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + 1);
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, item.id);
            values.put(MediaStore.Audio.Playlists.Members.ARTIST, item.artist);
            values.put(MediaStore.Audio.Playlists.Members.TITLE, item.title);
            values.put(MediaStore.Audio.Playlists.Members.ALBUM, item.album);
            values.put(MediaStore.Audio.Playlists.Members.DURATION, item.duration);
            resolver.insert(uri, values);
        }
    }

    public static void removeFromPlaylist(ContentResolver resolver, int audioId, int playlistId) {
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        resolver.delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID + " = " + audioId, null);
    }

    public static List<LocalFileFragment.LocalFileItem> fetchAllSongInPlaylist(LocalFileFragment localFileFragment, ContentResolver resolver, long playlistId) {
        List<LocalFileFragment.LocalFileItem> result = new ArrayList<>();
        String[] columns = {
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members.ALBUM,
                MediaStore.Audio.Playlists.Members.DURATION
        };
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cursor = resolver.query(uri, columns, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                result.add( localFileFragment.new LocalFileItem(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getLong(4)
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }
}
