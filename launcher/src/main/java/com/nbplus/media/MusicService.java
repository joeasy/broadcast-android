package com.nbplus.media;

/**
 * Created by basagee on 2015. 6. 18..
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.RadioActivity;
import com.nbplus.vbroadlauncher.data.Constants;

import org.basdroid.common.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.vov.vitamio.MediaPlayer;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MusicFocusable /*,
        PrepareMusicRetrieverTask.MusicRetrieverPreparedListener*/ {

    // The tag we put on debug messages
    final static String TAG = MusicService.class.getSimpleName();

    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.nbplus.android.musicplayer.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.nbplus.android.musicplayer.action.PLAY";
    public static final String ACTION_PAUSE = "com.nbplus.android.musicplayer.action.PAUSE";
    public static final String ACTION_STOP = "com.nbplus.android.musicplayer.action.STOP";
    public static final String ACTION_SKIP = "com.nbplus.android.musicplayer.action.SKIP";
    public static final String ACTION_REWIND = "com.nbplus.android.musicplayer.action.REWIND";
    public static final String ACTION_URL = "com.nbplus.android.musicplayer.action.URL";
    public static final String ACTION_PLAYING_STATUS = "com.nbplus.android.musicplayer.action.PLAYINGSTATUS";

    // broadcast receiver action
    public static final String ACTION_PLAYED = "com.nbplus.android.musicplayer.action.PLAYED";
    public static final String ACTION_PAUSED = "com.nbplus.android.musicplayer.action.PAUSED";
    public static final String ACTION_STOPPED = "com.nbplus.android.musicplayer.action.STOPPED";
    public static final String ACTION_ERROR = "com.nbplus.android.musicplayer.action.ERROR";
    public static final String ACTION_COMPLETED = "com.nbplus.android.musicplayer.action.COMPLETED";

    public static final String EXTRA_ACTION = "extra_action";
    public static final String EXTRA_MUSIC_ITEM = "extra_url_item";
    public static final String EXTRA_MUSIC_FORCE_STOP = "extra_music_force_stop";
    public static final String EXTRA_PLAYING_STATUS = "extra_playing_status";

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    // our media player
    MediaPlayer mPlayer = null;

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;

    // indicates the state our service:
    public enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    State mState = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;

    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    Uri mWhatToPlayAfterRetrieve = null;

    enum PauseReason {
        UserRequest,  // paused by user request
        FocusLoss,    // paused because of audio focus loss
    };

    // why did we pause? (only relevant if mState == State.Paused)
    PauseReason mPauseReason = PauseReason.UserRequest;

    // do we have audio focus?
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

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiLock mWifiLock;

    // Our instance of our MusicRetriever, which handles scanning for media and
    // providing titles and URIs as we need.
    MusicRetriever mRetriever;

    // play item
    MusicRetriever.Item mPlayingItem = null;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    // Dummy album art we will pass to the remote control (if the APIs are available).
    Bitmap mDummyAlbumArt;

    // The component name of MusicIntentReceiver, for use with media button and remote control
    // APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;
    NotificationManager mNotificationManager;

    Notification mNotification = null;
    RemoteViews mRemoteViews;
    NotificationCompat.Builder mBuilder;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer(getApplicationContext());

            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        }
        else
            mPlayer.reset();
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "debug: Creating service");

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Create the retriever and start an asynchronous task that will prepare it.
        //mRetriever = new MusicRetriever(getContentResolver());
        //(new PrepareMusicRetrieverTask(mRetriever,this)).execute();
        mState = State.Stopped;

        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

        //mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);

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
        if (action != null && action instanceof String) {
            if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
            else if (action.equals(ACTION_PLAY)) processPlayRequest();
            else if (action.equals(ACTION_PAUSE)) processPauseRequest();
            else if (action.equals(ACTION_SKIP)) processSkipRequest();
            else if (action.equals(ACTION_STOP)) processStopRequest(intent);
            else if (action.equals(ACTION_REWIND)) processRewindRequest();
            else if (action.equals(ACTION_URL)) processAddRequest(intent);
            else if (action.equals(ACTION_PLAYING_STATUS)) broadcastPlayingStaus();
        }

        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    void broadcastPlayingStaus() {
        Log.d(TAG, "Send Broadcasting message action = " + ACTION_PLAYING_STATUS);
        Intent intent = new Intent(ACTION_PLAYING_STATUS);
        // You can also include some extra data.
        intent.putExtra(EXTRA_PLAYING_STATUS, mState);
        intent.putExtra(EXTRA_MUSIC_ITEM, mPlayingItem);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    void processTogglePlaybackRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    void processPlayRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready
            mWhatToPlayAfterRetrieve = null; // play a random song
            mStartPlayingAfterRetrieve = true;
            return;
        }

        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            //playNextSong(null);
            mPlayingItem = null;
            //sendBroadcastMessage(ACTION_ERROR, mPlayingItem);
            stopForeground(true);
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            configAndStartMediaPlayer();
            updateNotification(mPlayingItem.getTitle());
        }

        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    void processPauseRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
            sendBroadcastMessage(ACTION_PAUSED, mPlayingItem);
            updateNotification(mPlayingItem.getTitle());
        }

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    void processRewindRequest() {
        if (mState == State.Playing || mState == State.Paused)
            mPlayer.seekTo(0);
    }

    void processSkipRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            playNextSong(null);
        }
    }

    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(Intent intent) {
        boolean forced = intent.getBooleanExtra(EXTRA_MUSIC_FORCE_STOP, false);
        processStopRequest(forced);
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
            sendBroadcastMessage(ACTION_STOPPED, mPlayingItem);
        }
        mPlayingItem = null;
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(Constants.RADIO_NOTIFICATION_ID);
        }
        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
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
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) {
                mState = State.Paused;
                mPlayer.pause();
                sendBroadcastMessage(ACTION_PAUSED, mPlayingItem);
            }
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        else
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mPlayer.isPlaying()) {
            mPlayer.start();
            sendBroadcastMessage(ACTION_PLAYED, mPlayingItem);
        }
    }

    void processAddRequest(Intent intent) {
        // user wants to play a song directly by URL or path. The URL or path comes in the "data"
        // part of the Intent. This Intent is sent by {@link MainActivity} after the user
        // specifies the URL/path via an alert box.
        if (mState == State.Retrieving) {
            // we'll play the requested URL right after we finish retrieving
            mWhatToPlayAfterRetrieve = intent.getData();
            mStartPlayingAfterRetrieve = true;
        }
        else if (mState == State.Playing || mState == State.Paused || mState == State.Stopped) {
            MusicRetriever.Item item = (MusicRetriever.Item)intent.getParcelableExtra(EXTRA_MUSIC_ITEM);
            if (item != null) {
                Log.i(TAG, "Playing from URL/path: " + item.getUrl());
                tryToGetAudioFocus();
                playNextSong(item);
            } else {
                Log.e(TAG, ">> MusicRetriever.Item is not found !!!");
                sendBroadcastMessage(ACTION_ERROR, null);
            }
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
    void playNextSong(MusicRetriever.Item playingItem) {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer

        try {
            if (playingItem != null) {
                if (playingItem.getType() == 0) {
                    // set the source of the media player to a manual URL or path
                    createMediaPlayerIfNeeded();
                    //mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mPlayer.setDataSource(playingItem.getUrl());
                    mIsStreaming = playingItem.getUrl().startsWith("http:") || playingItem.getUrl().startsWith("https:") || playingItem.getUrl().startsWith("mms:");
                } else {
                    mIsStreaming = false; // playing a locally available song

                    // set the source of the media player a a content URI
                    createMediaPlayerIfNeeded();
                    //mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mPlayer.setDataSource(getApplicationContext(), playingItem.getURI());
                }
            }
            else {
                mIsStreaming = false; // playing a locally available song

                playingItem = mRetriever.getRandomItem();
                if (playingItem == null) {
                    Toast.makeText(this,
                            "No available music to play. Place some music on your external storage "
                                    + "device (e.g. your SD card) and try again.",
                            Toast.LENGTH_LONG).show();
                    processStopRequest(true); // stop everything!
                    return;
                }

                // set the source of the media player a a content URI
                createMediaPlayerIfNeeded();
                //mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(getApplicationContext(), playingItem.getURI());
            }

            mSongTitle = playingItem.getTitle();
            mPlayingItem = playingItem;

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
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingItem.getArtist())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingItem.getAlbum())
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.getTitle())
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                            playingItem.getDuration())
                    .putBitmap(
                            RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                            mDummyAlbumArt)
                    .apply();

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            if (mIsStreaming) mWifiLock.acquire();
            else if (mWifiLock.isHeld()) mWifiLock.release();
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
        //playNextSong(null);
        Log.d(TAG, "MusicPlayer onCompletion !!!");
        sendBroadcastMessage(ACTION_COMPLETED, mPlayingItem);
        if (mPlayingItem.getType() == 0) {      // streaming
            mState = State.Stopped;
            relaxResources(true);
            giveUpAudioFocus();
        }
        mPlayingItem = null;
    }

    /** Called when media player is done preparing. */
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        Log.d(TAG, "MusicPlayer onPrepared !!!");
        mState = State.Playing;
        configAndStartMediaPlayer();
        updateNotification(mPlayingItem.getTitle());
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
                Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();

        sendBroadcastMessage(ACTION_ERROR, mPlayingItem);
        mPlayingItem = null;
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
        //Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "gained audio focus.");
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        //Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" : "no duck"), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "lost audio focus. " + (canDuck ? "can duck" : "no duck"));
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mPlayer != null && mPlayer.isPlaying())
            configAndStartMediaPlayer();
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void sendBroadcastMessage(String action, MusicRetriever.Item item) {
        Log.d(TAG, "Send Broadcasting message action = " + action);
        Intent intent = new Intent(action);
        // You can also include some extra data.
        intent.putExtra(EXTRA_PLAYING_STATUS, mState);
        intent.putExtra(EXTRA_MUSIC_ITEM, item);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /** Updates the notification. */
    void updateNotification(String text) {
//        Intent i = new Intent(getApplicationContext(), RadioActivity.class);
//        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
//                i,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        mNotification.setLatestEventInfo(getApplicationContext(), "MusicPlayer", text, pi);

        setRemoteViews();
        mNotificationManager.notify(Constants.RADIO_NOTIFICATION_ID, mNotification);
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        // notification's layout
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.music_notification);
        setRemoteViews();
        mBuilder = new NotificationCompat.Builder(this);

        CharSequence ticker = text;
        int apiVersion = Build.VERSION.SDK_INT;

        if (apiVersion < Build.VERSION_CODES.HONEYCOMB) {
            mNotification = new Notification(R.drawable.ic_notification_radio, ticker, System.currentTimeMillis());
            mNotification.contentView = mRemoteViews;
            //mNotification.contentIntent = pendIntent;

            mNotification.flags |= Notification.FLAG_NO_CLEAR;
            mNotification.defaults |= Notification.DEFAULT_LIGHTS;
            mNotification.priority = Notification.PRIORITY_MAX;

            startForeground(Constants.RADIO_NOTIFICATION_ID, mNotification);

        } else if (apiVersion >= Build.VERSION_CODES.HONEYCOMB) {
            mBuilder.setSmallIcon(R.drawable.ic_notification_radio)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    //.setContentIntent(pendIntent)
                    .setContent(mRemoteViews)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setTicker(ticker);

            mNotification = mBuilder.build();
            mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
            mNotification.defaults |= Notification.DEFAULT_LIGHTS;
            startForeground(Constants.RADIO_NOTIFICATION_ID, mNotification);
        }
    }

    private void setRemoteViews() {
        if (mPlayingItem != null) {
            mRemoteViews.setTextViewText(R.id.play_title, mPlayingItem.getTitle());
        } else {
            mRemoteViews.setTextViewText(R.id.play_title, getString(R.string.activity_radio_default_title));
        }

        // toggle playback
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);
        mRemoteViews.setOnClickPendingIntent(R.id.ic_media_control_play, pi);
        if (mState == State.Paused) {
            mRemoteViews.setImageViewResource(R.id.ic_media_control_play, R.drawable.ic_btn_radio_play_selector);
        } else {
            mRemoteViews.setImageViewResource(R.id.ic_media_control_play, R.drawable.ic_btn_radio_pause_selector);
        }
        // stop button
        intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_STOP);
        pi = PendingIntent.getService(this, 0, intent, 0);
        mRemoteViews.setOnClickPendingIntent(R.id.ic_media_control_stop, pi);
    }
}
