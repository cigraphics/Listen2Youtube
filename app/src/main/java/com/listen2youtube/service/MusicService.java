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

package com.listen2youtube.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.ext.opus.LibopusAudioTrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.extractor.flv.FlvExtractor;
import com.google.android.exoplayer.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer.extractor.webm.WebmExtractor;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.listen2youtube.R;
import com.listen2youtube.activity.MainActivity;

import java.util.List;


public class MusicService extends Service implements MusicFocusable, ExoPlayer.Listener {

    final static String TAG = "MusicService";


    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.listen2youtube.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.listen2youtube.action.PLAY";
    public static final String ACTION_PAUSE = "com.listen2youtube.action.PAUSE";
    public static final String ACTION_STOP = "com.listen2youtube.action.STOP";

    ExoPlayer exoPlayer = null;

    AudioFocusHelper mAudioFocusHelper = null;
    private long playerPosition = 0;

    // indicates the state our service:
    enum State {
        Stopped,
        Preparing,
        Playing,
        Paused
    }

    ;

    State mState = State.Stopped;


    enum PauseReason {
        UserRequest,
        FocusLoss,
    }

    PauseReason mPauseReason = PauseReason.UserRequest;

    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // title of the song we are currently playing
    String mSongTitle = "";

    // whether the song we are playing is streaming from the network
    boolean mIsStreaming = false;

    WifiLock mWifiLock;

    final int NOTIFICATION_ID = 1;

    final SongList songList = new SongList();
    String playListTag;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    Bitmap mDummyAlbumArt;

    // The component name of MusicIntentReceiver, for use with media button and remote control
    // APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;
    NotificationManager mNotificationManager;

    Notification.Builder mNotificationBuilder = null;

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 160;


    /**
     * Binder
     */
    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    final LocalBinder localBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        return localBinder;
    }


    public void setNewSongList(@NonNull List<SongInfo> newSongList, String tag) {
        Log.e(TAG, "setNewSongList - line 210: " + newSongList.size());
        synchronized (songList) {
            songList.songInfoList = newSongList;
            this.playListTag = tag;
        }
    }

    public String getPlayListTag() {
        return playListTag;
    }

    public void playSong(int position) {
        processPlayRequest(position);
    }


    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Factory.newInstance(1);
            exoPlayer.addListener(this);
        } else {
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.stop();
            exoPlayer.seekTo(0);
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "debug: Creating service");

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.music_art);

        mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null)
            if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
            else if (action.equals(ACTION_PLAY)) processPlayRequest(-1);
            else if (action.equals(ACTION_PAUSE)) processPauseRequest();
            else if (action.equals(ACTION_STOP)) processStopRequest();

        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    void processTogglePlaybackRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest(-1);
        } else {
            processPauseRequest();
        }
    }

    void processPlayRequest(int position) {
        tryToGetAudioFocus();

        if (mState == State.Playing || position != -1) {
            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.seekTo(0);
            }
            playNextSong(position);
        } else if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(position);
        } else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSongTitle + " (playing)");
            configAndStartMediaPlayer();
        }

        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    void processPauseRequest() {

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;

            // Stop ExoPlayer and save position where it stopped
            exoPlayer.setSelectedTrack(0, -1);
            playerPosition = exoPlayer.getCurrentPosition();

            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service

        if (releaseMediaPlayer)
            stopForeground(true);
        else {
            stopForeground(false);
            updateNotification(mSongTitle + " (paused)");
        }

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (mAudioFocus != AudioFocus.Focused) {

            //Loss focus, pause player

            mPauseReason = PauseReason.FocusLoss;
            if (exoPlayer.getPlaybackState() == ExoPlayer.STATE_READY
                    && exoPlayer.getPlayWhenReady()) {
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.stop();
            }
        } else {
            exoPlayer.seekTo(playerPosition);
            exoPlayer.setSelectedTrack(0, 0);
        }
    }


    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong(int position) {
        mState = State.Stopped;
        playerPosition = 0;
        relaxResources(false); // release everything except MediaPlayer

        SongInfo playingItem;
        if (position != -1)
            playingItem = songList.songInfoList.get(position);
        else
            playingItem = songList.nextSong();

        if (playingItem == null) {
            processStopRequest(true);
            return;
        }
        mIsStreaming = playingItem.uri.getScheme().startsWith("http");
        createMediaPlayerIfNeeded();
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(
                playingItem.uri,
                new DefaultUriDataSource(this, "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:45.0) Gecko/20100101 Firefox/45.0"),
                new DefaultAllocator(BUFFER_SEGMENT_SIZE), BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT,
                new Mp3Extractor(), new FlvExtractor(), new Mp4Extractor(), new WebmExtractor());
        TrackRenderer audioRenderer = null;
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(this, playingItem.uri, null);
            MediaFormat format = extractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.e(TAG, "playNextSong - line 434: mine " + mime);
            if (mime != null && mime.contains("ffmpeg"))
                audioRenderer = new LibopusAudioTrackRenderer(sampleSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (audioRenderer == null)
            audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT);
        exoPlayer.prepare(audioRenderer);
        exoPlayer.setPlayWhenReady(true);

        mSongTitle = playingItem.titile;

        mState = State.Preparing;
        setUpAsForeground(mSongTitle + " (loading)");

        // Use the media button APIs (if available) to register ourselves for media button
        // events

        MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                mAudioManager, mMediaButtonReceiverComponent);

        // Use the remote control APIs (if available) to set the playback state

        if (mRemoteControlClientCompat == null) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(mMediaButtonReceiverComponent);
            mRemoteControlClientCompat = new RemoteControlClientCompat(
                    PendingIntent.getBroadcast(this /*context*/,
                            0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
            RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                    mRemoteControlClientCompat);
        }

        mRemoteControlClientCompat.setPlaybackState(
                RemoteControlClient.PLAYSTATE_PLAYING);

        mRemoteControlClientCompat.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                        RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                        RemoteControlClient.FLAG_KEY_MEDIA_STOP);

        // Update the remote controls
        mRemoteControlClientCompat.editMetadata(true)
                .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "Music")
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.titile)
                        // TODO: fetch real item artwork
                .putBitmap(
                        RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                        mDummyAlbumArt)
                .apply();

        // starts preparing the media player in the background. When it's done, it will call
        // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
        // the listener to 'this').
        //
        // Until the media player is prepared, we *cannot* call start() on it!
        //mPlayer.prepareAsync();

        // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
        // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
        // we are *not* streaming, we want to release the lock if we were holding it before.
        if (mIsStreaming) mWifiLock.acquire();
        else if (mWifiLock.isHeld()) mWifiLock.release();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.e(TAG, "onPlayerStateChanged - line 512: " + playbackState);
        switch (playbackState) {
            case ExoPlayer.STATE_READY:
                mState = State.Playing;
                updateNotification(mSongTitle + " (playing)");
                configAndStartMediaPlayer();
                break;
            case ExoPlayer.STATE_ENDED:
                if (mState == State.Playing)
                    processPlayRequest(-1);
                else {
                    mState = State.Stopped;
                }
                break;
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "onPlayerError - line 532: " + error);
        if (mState == State.Playing)
            processPlayRequest(-1);
        else
            processStopRequest(true);
    }

    /**
     * Updates the notification.
     */
    void updateNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.setContentText(text)
                .setContentIntent(pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Build the notification object.
        mNotificationBuilder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_action_music_1)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Listen2Youtube")
                .setContentText(text)
                .setContentIntent(pi)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    public void onGainedAudioFocus() {
        Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        if (exoPlayer != null && exoPlayer.getPlayWhenReady())
            configAndStartMediaPlayer();
    }


    @Override
    public void onDestroy() {
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

}
