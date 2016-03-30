/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.listen2youtube;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicRetriever {
    final String TAG = "MusicRetriever";


    public interface OnPreparedNewData {
        void onPrepared();
    }

    ContentResolver mContentResolver;

    // the items (songs) we have queried
    List<MusicItem> mItems = new ArrayList<>();
    List<Integer> mItemFiltered = new ArrayList<>();
    List<Integer> playedPosition = new ArrayList<>();
    Random mRandom = new Random();
    OnPreparedNewData onPreparedNewData;
    MusicStoreObserver observer;

    long playlistId = -1;

    public MusicRetriever(ContentResolver mContentResolver, OnPreparedNewData onPreparedNewData) {
        this.mContentResolver = mContentResolver;
        this.onPreparedNewData = onPreparedNewData;
        observer = new MusicStoreObserver(new Handler());
        mContentResolver.registerContentObserver(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer);
    }

    /**
     * Loads music data. This method may take long, so be sure to call it asynchronously without
     * blocking the main thread.
     */
    public void prepareAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (playlistId != -1)
                    findAllSongInPlaylist();
                else findAllLocalMusic();
                mItemFiltered.clear();

                onPreparedNewData.onPrepared();
            }
        }).start();
    }

    private void findAllLocalMusic(){
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.i(TAG, "Querying media...");
        Log.i(TAG, "URI: " + uri.toString());

        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null,
                MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);
        Log.i(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
            mItems.clear();
            onPreparedNewData.onPrepared();
            return;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            Log.e(TAG, "Failed to move cursor to first row (no query results).");
            mItems.clear();
            onPreparedNewData.onPrepared();
            return;
        }

        Log.i(TAG, "Listing...");

        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

        Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
        Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));

        List<MusicItem> tmp = new ArrayList<>();
        do {
            Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
            tmp.add(new MusicItem(
                    cur.getLong(idColumn),
                    cur.getString(artistColumn),
                    cur.getString(titleColumn),
                    cur.getString(albumColumn),
                    cur.getLong(durationColumn)));
        } while (cur.moveToNext());
        cur.close();
        mItems = tmp;
        resetPlayedList();
        onPreparedNewData.onPrepared();
        Log.i(TAG, "Done querying media. MusicRetriever is ready.");
    }

    private void findAllSongInPlaylist(){
        //mItems = PlayListHelper.fetchAllSongInPlaylist(mContentResolver, playlistId);
    }

    public void doFilter(String filterStr){
        mItemFiltered.clear();
        if (filterStr != null && filterStr.length() > 0){
            filterStr = filterStr.toLowerCase();
            for (int i = 0; i < mItems.size(); i++) {
                if (mItems.get(i).getTitle().toLowerCase().contains(filterStr))
                    mItemFiltered.add(i);
            }
        }
    }

    public void resetPlayedList(){
        playedPosition.clear();
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    public void freeMemory(){
        getContentResolver().unregisterContentObserver(observer);
        mItems.clear();
    }

    public int getItemFilteredCount(){
        return mItemFiltered.size();
    }

    public int getCount(){
        return mItems.size();
    }

    public MusicItem getItem(int pos){
        return mItems.get(pos);
    }

    public int getItemFiltered(int pos){
        return mItemFiltered.get(pos);
    }

    /** Returns a random Item. If there are no items available, returns null. */
    public MusicItem getNextItem() {
        if (getCount() <= 0) return null;
        if (playedPosition.size() >= getCount() ) {
            if (Settings.isLoop())
                resetPlayedList();
            else
                return null;
        }
        if (Settings.isRandom()){
            int remainingItems = getCount() - playedPosition.size();
            int r = mRandom.nextInt(remainingItems);
            int n = -1;
            for (int i = 0; i < getCount(); i++) {
                if (!playedPosition.contains(i) && ++n == r) {
                    playedPosition.add(i);
                    return getItem(i);
                }
            }
        } else {
            if (playedPosition.size() == 0 || playedPosition.get(playedPosition.size() - 1) == getCount() - 1) {
                playedPosition.add(0);
                return getItem(0);
            } else {
                int previous = playedPosition.get(playedPosition.size() - 1);
                playedPosition.add(previous + 1);
                return getItem(previous + 1);
            }
        }
        return null;
    }


    public class MusicStoreObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public MusicStoreObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            prepareAsync();
        }
    }
}
