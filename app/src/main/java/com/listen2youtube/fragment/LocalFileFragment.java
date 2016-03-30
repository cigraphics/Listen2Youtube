package com.listen2youtube.fragment;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.listen2youtube.PlayListHelper;
import com.listen2youtube.Playlist;
import com.listen2youtube.R;
import com.listen2youtube.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by khang on 29/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class LocalFileFragment extends BaseFragment implements BaseFragment.OnDataSetChanged {
    private static final String TAG = "LocalFileFragment";

    private HashMap<String, Drawable> cacheDrawable = new HashMap<>();
    public LocalFileDataSet dataSet;

    @Override
    public void initializeAdapter(@Nullable Bundle savedInstanceState) {
        Log.e(TAG, "initializeAdapter - line 39: ");
        if (dataSet == null) {
            dataSet = new LocalFileDataSet(this);
            dataSet.prepareAsync();
        }
        if (adapter == null)
            adapter = new LocalFileAdapter(dataSet);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLayout.setEnabled(false);
        tvNoFile.setText("No music file to show.");
    }

    @Override
    public void onItemClick(View v) {

    }

    @Override
    public void onIcon1Click(View v) {
        final LocalFileItem musicItem = (LocalFileItem) v.getTag();
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.getMenu().add(0, 0, 0, "Create new playlist");
        final List<Playlist> playlist = PlayListHelper.fetchAllPlaylist(getContext().getContentResolver());
        for (int i = 1; i <= playlist.size(); i++) {
            popupMenu.getMenu().add(1, i, i, playlist.get(i - 1).getTitle());
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final ContentResolver contentResolver = getContext().getContentResolver();
                switch (item.getItemId()) {
                    case 0:
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Title");
                        final AppCompatEditText input = new AppCompatEditText(getContext());
                        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                        input.setMaxLines(1);
                        input.setHint("Playlist name");
                        builder.setView(input);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String playlistName = input.getText().toString();
                                Uri newPlaylistUri = PlayListHelper.createPlaylist(contentResolver,
                                        playlistName);
                                if (newPlaylistUri != null) {
                                    Toast.makeText(getContext(), "Create " + playlistName + " playlist success", Toast.LENGTH_SHORT).show();
                                    PlayListHelper.insertSongToPlaylist(contentResolver, musicItem, newPlaylistUri);
                                    Toast.makeText(getContext(),
                                            "Insert " + musicItem.getTitle() + " to " + playlistName + " success",
                                            Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(getContext(), "Create " + playlistName + " playlist have an error.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.show();
                        break;
                    default:
                        Playlist mPlaylist = playlist.get(item.getItemId() - 1);
                        PlayListHelper.insertSongToPlaylist(contentResolver, musicItem, mPlaylist.getId());
                        Toast.makeText(getContext(),
                                "Insert " + musicItem.getTitle() + " to " + mPlaylist.getTitle() + " success",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onChanged() {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dataSet != null)
            dataSet.freeObserver();
        cacheDrawable.clear();
    }

    class LocalFileAdapter extends BaseAdapter<LocalFileItem> {
        public LocalFileAdapter(BaseDataSet<LocalFileItem> baseDataSet) {
            super(baseDataSet);
        }

        @Override
        public int getItemCount() {
            return LocalFileFragment.this.dataSet.haveFiltered ?
                    LocalFileFragment.this.dataSet.getItemFilteredCount() :
                    super.getItemCount();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BaseDataItem item;
            if (LocalFileFragment.this.dataSet.haveFiltered)
                item = dataSet.getItem(LocalFileFragment.this.dataSet.getItemFiltered(position));
            else
                item = dataSet.getItem(position);
            holder.bindData(item, position);
        }
    }

    public class LocalFileDataSet extends BaseDataSet<LocalFileItem> {

        boolean haveFiltered = false;
        List<Integer> mItemFiltered = new ArrayList<>();
        List<Integer> playedPosition = new ArrayList<>();
        ContentResolver mContentResolver = getContext().getContentResolver();
        MusicStoreObserver observer;

        Random mRandom = new Random();

        public LocalFileDataSet(@NonNull OnDataSetChanged onDataSetChanged) {
            super(onDataSetChanged);
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
                    findAllLocalMusic();
                    mItemFiltered.clear();
                    haveFiltered = false;
                }
            }).start();
        }

        private void findAllLocalMusic() {
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
                setDataSet(new ArrayList<LocalFileItem>(), true);
                return;
            }
            if (!cur.moveToFirst()) {
                // Nothing to query. There is no music on the device. How boring.
                Log.e(TAG, "Failed to move cursor to first row (no query results).");
                setDataSet(new ArrayList<LocalFileItem>(), true);
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

            List<LocalFileItem> tmp = new ArrayList<>();
            do {
                Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
                tmp.add(new LocalFileItem(
                        cur.getLong(idColumn),
                        cur.getString(artistColumn),
                        cur.getString(titleColumn),
                        cur.getString(albumColumn),
                        cur.getLong(durationColumn)));
            } while (cur.moveToNext());
            cur.close();
            resetPlayedList();
            setDataSet(tmp, true);
            Log.i(TAG, "Done querying media. MusicRetriever is ready.");
        }

        public void doFilter(String filterStr) {
            haveFiltered = false;
            mItemFiltered.clear();
            if (filterStr != null && filterStr.length() > 0) {
                haveFiltered = true;
                filterStr = filterStr.toLowerCase();
                for (int i = 0; i < dataList.size(); i++) {
                    if (dataList.get(i).getTitle().toLowerCase().contains(filterStr))
                        mItemFiltered.add(i);
                }
            }
        }

        public void resetPlayedList() {
            playedPosition.clear();
        }

        public int getItemFilteredCount() {
            return mItemFiltered.size();
        }

        public int getItemFiltered(int pos) {
            return mItemFiltered.get(pos);
        }

        /**
         * Returns a random Item. If there are no items available, returns null.
         */
        public LocalFileItem getNextItem() {
            if (getCount() <= 0) return null;
            if (playedPosition.size() >= getCount()) {
                if (Settings.isLoop())
                    resetPlayedList();
                else
                    return null;
            }
            if (Settings.isRandom()) {
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

        public void freeObserver() {
            mContentResolver.unregisterContentObserver(observer);
        }

        /**
         * Observer music store has changed
         */
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

    public class LocalFileItem extends BaseDataItem {
        public long id;
        public String artist;
        public String title;
        public String album;
        public long duration;

        public LocalFileItem(long id, String artist, String title, String album, long duration) {
            this.id = id;
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.duration = duration;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDescription() {
            int minutes = (int) (duration / 60000);
            String minuteStr = minutes < 10 ? "0" + minutes : "" + minutes;
            int seconds = (int) (duration - minutes * 60000) / 1000;
            String secondStr = seconds < 10 ? "0" + seconds : "" + seconds;
            return minuteStr + ":" + secondStr + " - " + artist + " - " + album;
        }

        @Override
        public void setThumbnailIcon(AppCompatImageView imageView, int position) {
            String text = artist.substring(0, 1);
            final int p;
            if ((p = artist.indexOf(" ")) != -1) {
                text += artist.substring(p + 1, p + 2);
            }
            if (text.length() < 2)
                text = text.toUpperCase() + artist.substring(1, 2);
            if (cacheDrawable.containsKey(text))
                imageView.setImageDrawable(cacheDrawable.get(text));
            else {
                ColorGenerator generator = ColorGenerator.MATERIAL;
                TextDrawable textDrawable = TextDrawable.builder()
                        .buildRect(text, generator.getColor(text));
                imageView.setImageDrawable(textDrawable);
                cacheDrawable.put(text, textDrawable);
            }
        }

        @Override
        public int getDrawableResIcon1() {
            return R.drawable.ic_action_add;
        }

        @Override
        public boolean shouldShowHeadphoneIcon(int position) {
            return playingId != null && playingId.equals(String.valueOf(id));
        }
    }
}
