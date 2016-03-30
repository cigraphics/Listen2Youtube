package com.listen2youtube.fragment;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.listen2youtube.PlayListHelper;
import com.listen2youtube.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by khang on 30/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class PlaylistFragment extends LocalFileFragment {
    private static final String TAG = "PlaylistFragment";

    int mDisplayingPlaylistId = -1;
    int mPlayingPlaylist = -1;

    PlaylistDataSet dataSet;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLayout.setRefreshing(false);
        tvNoFile.setText("No play list available.");
    }

    @Override
    public void initializeAdapter(@Nullable Bundle savedInstanceState) {
        if (dataSet == null)
            dataSet = new PlaylistDataSet(this);
        dataSet.prepareAsync();
        if (adapter == null)
            adapter = new PlaylistAdapter(dataSet);
    }

    @Override
    public void onItemClick(View v) {
        if (v.getTag() == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Title");
            final AppCompatEditText input = new AppCompatEditText(getContext());
            input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
            input.setMaxLines(1);
            input.setHint("Playlist name");
            builder.setView(input);
            final ContentResolver contentResolver = getContext().getContentResolver();
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String playlistName = input.getText().toString();
                    Uri newPlaylistUri = PlayListHelper.createPlaylist(contentResolver,
                            playlistName);
                    PlaylistItem playlistItem = new PlaylistItem(
                            PlayListHelper.parsePlaylistId(newPlaylistUri),
                            playlistName,
                            new ArrayList<LocalFileItem>()
                    );
                    List<PlaylistItem> a = new ArrayList<PlaylistItem>();
                    a.add(playlistItem);
                    dataSet.setDataSet(a, false);
                    if (newPlaylistUri != null) {
                        Toast.makeText(getContext(), "Create " + playlistName + " playlist success", Toast.LENGTH_SHORT).show();

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
        }
    }

    @Override
    public void onIcon1Click(View v) {

    }

    class PlaylistAdapter extends BaseAdapter<PlaylistItem> {
        private int TYPE_ADD_PLAYLIST = 2, TYPE_PLAYLIST = 3;

        public PlaylistAdapter(BaseDataSet<PlaylistItem> baseDataSet) {
            super(baseDataSet);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (mDisplayingPlaylistId == -1) {
                if (viewType == TYPE_ADD_PLAYLIST) {
                    View v = inflater.inflate(R.layout.add_item, parent, false);
                    v.findViewById(R.id.ripple).setOnClickListener(onItemClick);
                    return new ViewHolder(v, false);
                } else if (viewType == TYPE_PLAYLIST) {
                    return new ViewHolder(inflater.inflate(R.layout.playlist_item, parent, false), true);
                }
            }
            return new ViewHolder(inflater.inflate(R.layout.music_item, parent, false), true);
        }

        @Override
        public int getItemViewType(int position) {
            if (mDisplayingPlaylistId != -1)
                return 0;
            if (position == 0)
                return TYPE_ADD_PLAYLIST;
            return TYPE_PLAYLIST;
        }

        @Override
        public int getItemCount() {
            if (mDisplayingPlaylistId == -1)
                return dataSet.getCount() + 1;
            return dataSet.getItem(mDisplayingPlaylistId).localFileItems.size();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (getItemViewType(position) != TYPE_ADD_PLAYLIST) {
                if (mDisplayingPlaylistId == -1)
                    holder.bindData(dataSet.getItem(position - 1), position - 1);
                else
                    holder.bindData(dataSet.getItem(mDisplayingPlaylistId), position);
            }

        }
    }

    class PlaylistDataSet extends BaseDataSet<PlaylistItem> {
        ContentResolver resolver = getContext().getContentResolver();

        public PlaylistDataSet(@NonNull OnDataSetChanged onDataSetChanged) {
            super(onDataSetChanged);
        }

        public void prepareAsync() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<PlaylistItem> newList = PlayListHelper.fetchAllPlaylist(resolver, PlaylistFragment.this);
                    setDataSet(newList, true);
                }
            }).start();
        }
    }

    public class PlaylistItem extends BaseDataItem {
        public long id;
        public String title;
        public List<LocalFileItem> localFileItems;

        public PlaylistItem(long id, String title, List<LocalFileItem> localFileItems) {
            this.id = id;
            this.title = title;
            this.localFileItems = localFileItems;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDescription() {
            return localFileItems.size() > 0 ? localFileItems.size() + " songs" : "0 song.";
        }

        @Override
        public void setThumbnailIcon(AppCompatImageView imageView, int position) {
            if (mDisplayingPlaylistId != -1) {
                localFileItems.get(position)
                        .setThumbnailIcon(imageView, position);
            } else {
                String text = title.substring(0, 1);
                final int p;
                if ((p = title.indexOf(" ")) != -1) {
                    text += title.substring(p + 1, p + 2);
                }
                if (text.length() < 2)
                    text = text.toUpperCase() + title.substring(1, 2);
                ColorGenerator generator = ColorGenerator.MATERIAL;
                TextDrawable textDrawable = TextDrawable.builder()
                        .buildRound(text, generator.getColor(text));
                imageView.setImageDrawable(textDrawable);
            }
        }

        @Override
        public int getDrawableResIcon1() {
            return R.drawable.ic_action_trash;
        }

        @Override
        public boolean shouldShowHeadphoneIcon(int position) {
            if (mDisplayingPlaylistId != -1)
                return localFileItems.get(position).shouldShowHeadphoneIcon(position);
            return mPlayingPlaylist == id;
        }
    }
}
