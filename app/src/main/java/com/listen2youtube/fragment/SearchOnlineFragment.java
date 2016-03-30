package com.listen2youtube.fragment;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.google.api.services.youtube.model.SearchResult;
import com.listen2youtube.R;
import com.listen2youtube.Settings;
import com.listen2youtube.Utils;
import com.listen2youtube.YoutubeSearch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by khang on 29/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class SearchOnlineFragment extends BaseFragment implements BaseFragment.OnDataSetChanged {
    private static final String TAG = "SearchOnlineFragment";

    private final LruCache<String, Bitmap> cacheBitmap = new LruCache<String, Bitmap>(15 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private ChannelIconGetter channelIconGetter = new ChannelIconGetter();

    public SearchOnlineDataSet dataSet;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLayout.setEnabled(false);
        tvNoFile.setText("Let's start to search somethings!");
    }

    @Override
    public void initializeAdapter(@Nullable Bundle savedInstanceState) {
        Log.e(TAG, "initializeAdapter - line 67: Online" );
        if (savedInstanceState != null){
            try {
                String data = savedInstanceState.getString("data");
                JSONObject json = new JSONObject(data);
                dataSet = new SearchOnlineDataSet(json, this);
            } catch (JSONException e) {
                e.printStackTrace();
                dataSet = new SearchOnlineDataSet(this);
            }
        } else if (dataSet == null)
            dataSet = new SearchOnlineDataSet(this);
        adapter = new SearchOnlineAdapter(dataSet);
    }

    @Override
    public void onItemClick(View v) {

    }

    @Override
    public void onIcon1Click(View v) {
        final SearchOnlineItem data = (SearchOnlineItem) v.getTag();
        showToast("Start download " + data.getTitle());
        Utils.getLinkAsync(data.id, new Utils.OnCompleteListener() {
            @Override
            public void onComplete(Object response) {
                if (response == null) {
                    showToast("Cannot connect to server. Check your connection");
                    return;
                }
                Log.e(TAG, "onComplete - line 168: " + response);
                DownloadManager downloadManager =
                        (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse((String) response));
                request.allowScanningByMediaScanner();
                if (Settings.isOnlyWifi())
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                request.setDescription(data.getDescription())
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC,
                                Utils.getValidFileName(data.getTitle()) + ".mp3")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setVisibleInDownloadsUi(true)
                        .setMimeType("audio/mp3");
                downloadManager.enqueue(request);
            }
        });
    }

    @Override
    public void onChanged() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("data", dataSet.toJsonObject().toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /************************************** Inner class *******************************************/

    /**********************************
     * SearchOnlineAdapter
     ***************************************/
    class SearchOnlineAdapter extends BaseAdapter<SearchOnlineItem> {
        private final int LOAD_MORE_ITEM = 1;

        public SearchOnlineAdapter(BaseDataSet<SearchOnlineItem> baseDataSet) {
            super(baseDataSet);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == LOAD_MORE_ITEM)
                return new ViewHolder(inflater.inflate(R.layout.loading_more, parent, false), false);
            return super.onCreateViewHolder(parent, viewType);
        }

        @Override
        public int getItemViewType(int position) {
            return position == getItemCount() - 1 ? LOAD_MORE_ITEM : DEFAULT;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position == getItemCount() - 1) {
                ((SearchOnlineDataSet) dataSet).loadMore();
                return;
            }
            super.onBindViewHolder(holder, position);
        }
    }


    /********************************
     * SearchOnlineDataSet
     *****************************************/
    public class SearchOnlineDataSet extends BaseDataSet<SearchOnlineItem> {
        String nextPageToken;
        String query;

        public SearchOnlineDataSet(JSONObject jsonObject, @NonNull OnDataSetChanged onDataSetChanged) throws JSONException {
            this(onDataSetChanged);
            query = jsonObject.getString("query");
            nextPageToken = jsonObject.getString("nextPageToken");
            JSONArray array = jsonObject.getJSONArray("dataSet");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                dataList.add(new SearchOnlineItem(object));
            }
        }

        public SearchOnlineDataSet(@NonNull OnDataSetChanged onDataSetChanged) {
            super(onDataSetChanged);
        }


        String searchingToken = null;

        public void searchQueryAsync(@NonNull String query, final boolean newSearch) {
            if (newSearch)
                setRefreshing(true);
            this.query = query;
            if (newSearch)
                this.nextPageToken = null;
            final String start = query + (nextPageToken == null ? "null" : nextPageToken);
            if (searchingToken != null && searchingToken.equals(start))
                return;
            else
                searchingToken = start;
            YoutubeSearch.search(query, true, nextPageToken, new YoutubeSearch.SearchCallback() {
                @Override
                public void onFinish(List<SearchResult> resultList, String nextPageToken) {
                    String end = SearchOnlineDataSet.this.query +
                            (SearchOnlineDataSet.this.nextPageToken == null ? "null" : SearchOnlineDataSet.this.nextPageToken);
                    if (start.equals(end)) {
                        SearchOnlineDataSet.this.nextPageToken = nextPageToken;
                        final List<SearchOnlineItem> searchResultList = new ArrayList<>();
                        if (resultList != null)
                            for (SearchResult searchResult :
                                    resultList) {
                                searchResultList.add(new SearchOnlineItem(
                                        searchResult.getSnippet().getTitle(),
                                        searchResult.getId().getVideoId(),
                                        searchResult.getSnippet().getChannelId(),
                                        searchResult.getSnippet().getChannelTitle()
                                ));
                            }
                        runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                setDataSet(searchResultList, newSearch);
                                setRefreshing(false);
                            }
                        });
                    }
                }
            });
        }

        public void loadMore() {
            searchQueryAsync(query, false);
        }

        public JSONObject toJsonObject() {
            JSONObject result = new JSONObject();
            try {
                result.put("query", query);
                result.put("nextPageToken", nextPageToken);
                JSONArray array = new JSONArray();
                for (SearchOnlineItem aItem :
                        dataSet.dataList) {
                    array.put(aItem.toJsonObject());
                }
                result.put("dataSet", array);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return result;
        }
    }


    /***********************************
     * SearchOnlineItem
     *****************************************/
    class SearchOnlineItem extends BaseDataItem {
        String title, id, channelId, channelTitle;

        public SearchOnlineItem(JSONObject jsonObject) throws JSONException {
            title = jsonObject.getString("title");
            id = jsonObject.getString("id");
            channelId = jsonObject.getString("channelId");
            channelTitle = jsonObject.getString("channelTitle");
        }

        public SearchOnlineItem(String title, String id, String channelId, String channelTitle) {
            this.title = title;
            this.id = id;
            this.channelId = channelId;
            this.channelTitle = channelTitle;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDescription() {
            return channelTitle;
        }

        @Override
        public void setThumbnailIcon(final AppCompatImageView imageView, int position) {
            ColorGenerator generator = ColorGenerator.MATERIAL;
            String text = channelTitle.substring(0, 1);
            final int p;
            if ((p = channelTitle.indexOf(" ")) != -1) {
                text += channelTitle.substring(p + 1, p + 2);
            }
            if (text.length() == 1)
                text = text.toUpperCase() + channelTitle.substring(1, 2);
            Bitmap bmp = channelIconGetter.get(channelId);
            imageView.setTag(position);
            if (bmp != null)
                imageView.setImageBitmap(bmp);
            else {
                imageView.setImageDrawable(TextDrawable.builder().buildRect(text, generator.getRandomColor()));
                channelIconGetter.loadToCache(channelId, position, new OnFinishLoadCallback() {
                    @Override
                    public void onFinish(int pos) {
                        if ((int) imageView.getTag() == pos) {
                            final Bitmap bmp = channelIconGetter.get(channelId);
                            if (bmp != null)
                                runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        imageView.setImageBitmap(bmp);
                                    }
                                });
                        }
                    }
                });
            }
        }

        @Override
        public int getDrawableResIcon1() {
            return R.drawable.ic_action_download;
        }

        @Override
        public boolean shouldShowHeadphoneIcon(int position) {
            return playingId != null && playingId.equals(id);
        }

        public JSONObject toJsonObject() throws JSONException {
            JSONObject result = new JSONObject();
            result.put("title", title);
            result.put("id", id);
            result.put("channelId", channelId);
            result.put("channelTitle", channelTitle);
            return result;
        }
    }

    /***********************************
     * ChannelIconGetter
     ****************************************/
    public class ChannelIconGetter {
        List<String> downloading = new ArrayList<>();

        public Bitmap get(String channelId) {
            Bitmap bmp;
            synchronized (cacheBitmap) {
                bmp = cacheBitmap.get(channelId);
            }
            return bmp;
        }

        public void loadToCache(final String channelId, final int pos, final OnFinishLoadCallback callback) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File tmpFolder = getContext().getDir("tmp", Context.MODE_PRIVATE);
                    File local = new File(tmpFolder, channelId);
                    if (!local.exists() && !downloading.contains(channelId)) {
                        downloading.add(channelId);
                        String channelUrl = null;
                        try {
                            channelUrl = YoutubeSearch.getChannelIconUrl(channelId);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (channelUrl != null)
                            saveUrl(local, channelUrl);
                        downloading.remove(channelId);
                    }
                    if (local.exists()) {
                        try {
                            Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(local));
                            synchronized (cacheBitmap) {
                                cacheBitmap.put(channelId, bmp);
                            }
                            callback.onFinish(pos);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        private boolean saveUrl(File saveFile, final String urlString) {
            BufferedInputStream in = null;
            FileOutputStream fout = null;
            boolean success = false;
            try {
                in = new BufferedInputStream(new URL(urlString).openStream());
                fout = new FileOutputStream(saveFile);
                long fileSize = in.available();

                final byte data[] = new byte[1024];
                int count = 0, byteRead;
                while ((byteRead = in.read(data, 0, 1024)) != -1) {
                    fout.write(data, 0, byteRead);
                    count += byteRead;
                }
                Log.e(TAG, "saveUrl - line 194: count: " + count + " filesize " + fileSize);
                if (count == fileSize)
                    success = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return success;
        }
    }

    private interface OnFinishLoadCallback {
        void onFinish(int position);
    }
}
