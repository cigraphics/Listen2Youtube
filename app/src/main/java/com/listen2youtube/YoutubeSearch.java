package com.listen2youtube;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.List;

/**
 * Created by khang on 23/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class YoutubeSearch {
    private static final String TAG = "YoutubeSearch";
    private static final long NUMBER_OF_VIDEOS_RETURNED = 20;

    public interface SearchCallback {
        void onFinish(List<SearchResult> resultList, String nextPageToken);
    }

    public static void search(String q, boolean onlyMusicVideo, String pageToken, @NonNull final SearchCallback callback) {
        YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {

            }
        })
                .build();
        final YouTube.Search.List search;
        try {
            search = youtube.search().list("id,snippet");
        } catch (IOException e) {
            e.printStackTrace();
            callback.onFinish(null, null);
            return;
        }
        search.setKey(Constants.API_KEY);
        if (onlyMusicVideo)
            search.setVideoCategoryId("10");
        search.setType("video");
        search.setFields("items(id/videoId,snippet/title,snippet/channelId,snippet/channelTitle),nextPageToken");
        search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
        if (pageToken != null && pageToken.length() > 0)
            search.setPageToken(pageToken);
        if (q != null)
            search.setQ(q);
        new Thread(new Runnable() {
            @Override
            public void run() {
                SearchListResponse searchResponse = null;
                try {
                    searchResponse = search.execute();
                    List<SearchResult> searchResultList = searchResponse.getItems();
                    Log.e(TAG, "run - line 64: " + searchResponse.toString());
                    callback.onFinish(searchResultList, searchResponse.getNextPageToken());
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onFinish(null, null);
                }
            }
        }).start();
    }

    public static String getChannelIconUrl(String id) throws IOException {
        YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {

            }
        }) .build();
        ChannelListResponse channelListResponse = youtube.channels().
                list("snippet").
                setFields("items(snippet/thumbnails/default/url)").
                setKey(Constants.API_KEY).
                setId(id).execute();
        if (!channelListResponse.getItems().isEmpty()){
            Channel channel = channelListResponse.getItems().get(0);
            return channel.getSnippet().getThumbnails().getDefault().getUrl();
        }
        return null;
    }
}
