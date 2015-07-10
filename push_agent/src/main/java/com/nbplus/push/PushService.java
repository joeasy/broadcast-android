package com.nbplus.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.nbplus.push.data.PushBaseData;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.data.PushInterfaceData;
import com.nbplus.push.data.PushMessageData;

import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;

public class PushService extends Service {
    private static final String TAG = PushService.class.getName();

    // minutes
    public static final int MILLISECONDS = 1000;
    public static final int mNextRetryPeriodTerm = 60;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiManager.WifiLock mWifiLock;

    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;
    NotificationManager mNotificationManager;
    Notification mNotification = null;
    NotificationCompat.Builder mBuilder;

    PushThread mPushThread;

    String mPushInterfaceServerAddress = null;
    final String PUSH_IF_CONTEXT = "/is/api/appRequest/SessionRequest";
    GetPushInterfaceTask mIfTask = null;

    Context mContext = null;

    private PushServiceHandler mHandler = new PushServiceHandler(this);
    // 핸들러 객체 만들기
    private static class PushServiceHandler extends Handler {
        private final WeakReference<PushService> mService;

        public PushServiceHandler(PushService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            PushService service = mService.get();
            if (service != null) {
                service.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE :
                Log.d(TAG, "HANDLER_MESSAGE_RETRY_MESSAGE received !!!");
                if (NetworkUtils.isConnected(this)) {
                    getPushGatewayInformationFromServer();
                }
                break;
            case PushConstants.HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA :
                Log.d(TAG, "HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA received !!!");
                if (mIfTask != null) {
                    mIfTask.cancel(true);
                }
                mIfTask = null;

                PushInterfaceData data = (PushInterfaceData)msg.obj;
                Log.d(TAG, "result = " + ((data != null) ? data.resultCode : null) + ", data = " + data);
                if (data != null && PushConstants.RESULT_OK.equals(data.resultCode)) {
                    mPushThread.startPushClientSocket(data);
                } else {
                    if (mPushThread.getState() == PushThread.State.Connected) {
                        mPushThread.releasePushClientSocket(NetworkUtils.isConnected(this));
                    }
                }
                break;
            case PushConstants.HANDLER_MESSAGE_CONNECTIVITY_CHANGED :
                Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED received !!!");

                if (NetworkUtils.isConnected(this)) {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is connected !!!");
                    if (!StringUtils.isEmptyString(mPushInterfaceServerAddress)) {
                        getPushGatewayInformationFromServer();
                    }
                } else {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is disconnected !!!");
                    if (mPushThread.getState() != PushThread.State.Stopped) {
                        mPushThread.releasePushClientSocket(false);
                    }
                    mHandler.removeMessages(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE);
                }
                break;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(PushConstants.HANDLER_MESSAGE_CONNECTIVITY_CHANGED);
            }
        }

    };

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * {@link android.content.Context#startService}, providing the arguments it supplied and a
     * unique integer token representing the start request.  Do not call this method directly.
     * <p/>
     * <p>For backwards compatibility, the default implementation calls
     * {@link #onStart} and returns either {@link #START_STICKY}
     * or {@link #START_STICKY_COMPATIBILITY}.
     * <p/>
     * <p>If you need your application to run on platform versions prior to API
     * level 5, you can use the following model to handle the older {@link #onStart}
     * callback in that case.  The <code>handleCommand</code> method is implemented by
     * you as appropriate:
     * <p/>
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.java
     * start_compatibility}
     * <p/>
     * <p class="caution">Note that the system calls this on your
     * service's main thread.  A service's main thread is the same
     * thread where UI operations take place for Activities running in the
     * same process.  You should always avoid stalling the main
     * thread's event loop.  When doing long-running operations,
     * network calls, or heavy disk I/O, you should kick off a new
     * thread, or use {@link android.os.AsyncTask}.</p>
     *
     * @param intent  The Intent supplied to {@link android.content.Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.  Currently either
     *                0, {@link #START_FLAG_REDELIVERY}, or {@link #START_FLAG_RETRY}.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     * @return The return value indicates what semantics the system should
     * use for the service's current started state.  It may be one of the
     * constants associated with the {@link #START_CONTINUATION_MASK} bits.
     * @see #stopSelfResult(int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;
        Log.d(TAG, "onStartCommand in service.. action = " + action);
        if (action != null && action instanceof String) {
            if (action.equals(PushConstants.ACTION_START_SERVICE)) {
                String pushInterfaceServerAddress = intent.getStringExtra(PushConstants.EXTRA_START_SERVICE_IFADDRESS);
                if (!StringUtils.isEmptyString(pushInterfaceServerAddress)) {
                    if (mPushInterfaceServerAddress == null || !pushInterfaceServerAddress.equals(mPushInterfaceServerAddress)) {
                        mPushInterfaceServerAddress = pushInterfaceServerAddress;

                        if (mPushThread.getState() == PushThread.State.IfRetrieving) {
                            if (mIfTask != null) {
                                mIfTask.cancel(true);
                            }
                            mIfTask = null;
                        }

                        if (mPushThread.getState() == PushThread.State.Connected) {
                            mPushThread.releasePushClientSocket(false);
                        }
                        getPushGatewayInformationFromServer();
                    }
                } else {
                    Log.e(TAG, ">> mPushInterfaceServerAddress is empty !!!");
                }
            } else if (action.equals(PushConstants.ACTION_GET_STATUS)) {
                mPushThread.sendSatusChangedBroadcastMessage();
            }
//            else if (action.equals(ACTION_PAUSE)) processPauseRequest();
//            else if (action.equals(ACTION_SKIP)) processSkipRequest();
//            else if (action.equals(ACTION_STOP)) processStopRequest(intent);
//            else if (action.equals(ACTION_REWIND)) processRewindRequest();
//            else if (action.equals(ACTION_URL)) processAddRequest(intent);
//            else if (action.equals(ACTION_PLAYING_STATUS)) broadcastPlayingStaus();
        } else {

        }

        /**
         * 항상 실행되도록 한다.
         */
        return Service.START_REDELIVER_INTENT;//super.onStartCommand(intent, flags, startId);
    }

    private void getPushGatewayInformationFromServer() {
        mPushThread.releasePushClientSocket(false);

        String prefName = getApplicationContext().getPackageName() + "_preferences";
        SharedPreferences prefs = getSharedPreferences(prefName, Context.MODE_PRIVATE);

        mPushThread.setState(PushThread.State.IfRetrieving);
        mIfTask = new GetPushInterfaceTask(this, mHandler, mPushInterfaceServerAddress + PUSH_IF_CONTEXT);
        mIfTask.execute();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground() {
        // notification's layout
//        mRemoteViews = new RemoteViews(getPackageName(), R.layout.music_notification);
//        setRemoteViews();
//        mBuilder = new NotificationCompat.Builder(this);
//
//        CharSequence ticker = text;
//        int apiVersion = Build.VERSION.SDK_INT;
//
//        if (apiVersion < Build.VERSION_CODES.HONEYCOMB) {
//            mNotification = new Notification(R.drawable.ic_launcher, ticker, System.currentTimeMillis());
//            mNotification.contentView = mRemoteViews;
//            //mNotification.contentIntent = pendIntent;
//
//            mNotification.flags |= Notification.FLAG_NO_CLEAR;
//            mNotification.defaults |= Notification.DEFAULT_LIGHTS;
//
//            startForeground(NOTIFICATION_ID, mNotification);
//
//        } else if (apiVersion >= Build.VERSION_CODES.HONEYCOMB) {
//            mBuilder.setSmallIcon(R.drawable.ic_launcher)
//                    .setAutoCancel(false)
//                    .setOngoing(true)
//                            //.setContentIntent(pendIntent)
//                    .setContent(mRemoteViews)
//                    .setTicker(ticker);
//
//            mNotification = mBuilder.build();
//            mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
//            mNotification.defaults |= Notification.DEFAULT_LIGHTS;
//            startForeground(NOTIFICATION_ID, mNotification);
//        }
    }

    private void setRemoteViews() {
//        if (mPlayingItem != null) {
//            mRemoteViews.setTextViewText(R.id.play_title, mPlayingItem.getTitle());
//        } else {
//            mRemoteViews.setTextViewText(R.id.play_title, getString(R.string.activity_radio_default_title));
////            mRemoteViews.setViewVisibility(R.id.play_time, View.GONE);
//        }
//
//        // toggle playback
//        Intent intent = new Intent(this, PushAgentService.class);
//        intent.setAction(PushAgentService.ACTION_TOGGLE_PLAYBACK);
//        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);
//        mRemoteViews.setOnClickPendingIntent(R.id.ic_media_control_play, pi);
//        if (mState == State.Paused) {
//            mRemoteViews.setImageViewResource(R.id.ic_media_control_play, R.drawable.ic_btn_radio_play_selector);
//        } else {
//            mRemoteViews.setImageViewResource(R.id.ic_media_control_play, R.drawable.ic_btn_radio_pause_selector);
//        }
//        // stop button
//        intent = new Intent(this, com.nbplus.media.MusicService.class);
//        intent.setAction(com.nbplus.media.MusicService.ACTION_STOP);
//        pi = PendingIntent.getService(this, 0, intent, 0);
//        mRemoteViews.setOnClickPendingIntent(R.id.ic_media_control_stop, pi);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PushService onCreate()......");

        // check network status
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);

        mPushThread = new PushThread(this, mHandler, null);
        new Thread(mPushThread).start();

        mContext = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PushService onDestroy()......");
        unregisterReceiver(mBroadcastReceiver);
        mHandler = null;
    }
}
