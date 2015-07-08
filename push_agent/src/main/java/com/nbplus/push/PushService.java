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

    // indicates the state our service:
    public enum State {
        Stopped,            // Push agent stopped
        IfRetrieving,       // Push gw server searching. conn to interface server
        Connected           // connected to push gw server
    };

    State mState = State.Stopped;

    // minutes
    final int MILLISECONDS = 1000;
    final int mNextRetryPeriodTerm = 60;

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

    Thread mTcpThread;
    TcpClient mTcpClient;

    String mPushInterfaceServerAddress = null;
    final String PUSH_IF_CONTEXT = "/is/api/appRequest/SessionRequest";
    GetPushInterfaceTask mIfTask = null;

    Context mContext = null;

    private static final int HANDLER_MESSAGE_CONNECTIVITY_CHANGED = 0x01;
    private static final int HANDLER_MESSAGE_RETRY_MESSAGE = 0x02;
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

    // TODO : for test
    private static int mTestMessageIdx = -1;
    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case HANDLER_MESSAGE_RETRY_MESSAGE :
                if (NetworkUtils.isConnected(this)) {
                    Log.d(TAG, "HANDLER_MESSAGE_RETRY_MESSAGE received !!!");
                    getPushGatewayInformationFromServer();
                }

                // TODO : for test
                mTestMessageIdx++;
                if (mTestMessageIdx % 2 == 0) {
                    Message testMsg = new Message();
                    testMsg.what = PushConstants.HANDLER_MESSAGE_RECEIVE_PUSH_DATA;

                    PushMessageData data = new PushMessageData();
                    data.setMessageType(PushConstants.PUSH_MESSAGE_TYPE_PUSH_REQUEST);
                    data.setMessageId(1);
                    data.setPayload("{ \"FROM\":\"김김김\", \"ADDRESS\":\"그냥마을\", " +
                            "\"MESSAGE\":\"" +
                            "누구에게나 결함은 있단다.\n" + "그리고 고치려고 해도\n" + "때로 자기 힘으로는 어쩔 수 없는 결함도 있지.\n" +
                            "집이 가난하다거나 다리가 부자유스러운 것은\n" +
                            "그 아이로서도 어쩔 수 없는 부분이다.\n" +
                            "네가 머리를 감는데도 \n" +
                            "머리 냄새가 나는 것과 똑같이.\n" +
                            "우리 모두는 저마다 모양이 다른 결함들을 \n" +
                            "지니고 산단다.\n" +
                            "하지만 결함이 때로는 고마운 것이 되기도 한단다. \n" +
                            "세상일이란, \n" +
                            "이해하려고 노력해서 이해할 수 있는 것도 있지만\n" +
                            "이해하려고 애쓰지 않아도 \n" +
                            "저절로 이해할 수 있는 것이 있더라.\n" +
                            "그건 자기 결함 때문에 괴로움을 겪어 봤거나\n" +
                            "자기 결함을 숨기지 않고 인정하는 사람이라면\n" +
                            "가질 수 있는 이해심이지.\n" +
                            "너의 머리 냄새가 \n" +
                            "다른 사람을 쉽게 이해할 수 있는\n" +
                            "아주 고마운 것이 되기를 엄마는 진정으로 바란다.\n" +

                            "\", \"SERVICE_TYPE\":\"02\"}");
                    testMsg.obj = data;

                    mHandler.sendMessage(testMsg);
                }
                break;
            case PushConstants.HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA :
                Log.d(TAG, "HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA received !!!");
                if (mIfTask != null) {
                    mIfTask.cancel(true);
                }
                mIfTask = null;

                PushInterfaceData data = (PushInterfaceData)msg.obj;
                if (data != null && PushConstants.RESULT_OK.equals(data.resultCode)) {
                    startPushClientSocket(data);
                } else {
                    if (mState == State.Connected) {
                        releasePushClientSocket();
                    }

                    if (NetworkUtils.isConnected(this)) {
                        mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_RETRY_MESSAGE, MILLISECONDS * mNextRetryPeriodTerm);
                    }
                }
                msg = null;
                break;
            case HANDLER_MESSAGE_CONNECTIVITY_CHANGED :
                Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED received !!!");

                if (NetworkUtils.isConnected(this)) {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is connected !!!");
                    if (!StringUtils.isEmptyString(mPushInterfaceServerAddress)) {
                        getPushGatewayInformationFromServer();
                    }
                } else {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is disconnected !!!");
                    if (mState != State.Stopped) {
                        releasePushClientSocket();
                    }
                    mHandler.removeMessages(HANDLER_MESSAGE_RETRY_MESSAGE);
                }
                break;

            case PushConstants.HANDLER_MESSAGE_RECEIVE_PUSH_DATA :
                Log.d(TAG, "PushConstants.HANDLER_MESSAGE_RECEIVE_PUSH_DATA received !!!");
                PushMessageData pushData = (PushMessageData)msg.obj;
                if (pushData == null) {
                    Log.d(TAG, "empty push message data !!");
                    return;
                }
                String payloadData = pushData.getPayload();
                if (payloadData != null && !StringUtils.isEmptyString(payloadData))  {
                    Log.d(TAG, "Send broadcast payload to app !!!");

                    Intent intent = new Intent();
                    intent.setAction(PushConstants.ACTION_PUSH_MESSAGE_RECEIVED);
                    intent.putExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA, payloadData);
                    sendBroadcast(intent);
                } else {
                    Log.d(TAG, "Empty payload data !!!");
                }

                break;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(HANDLER_MESSAGE_CONNECTIVITY_CHANGED);
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

                        if (mState == State.IfRetrieving) {
                            if (mIfTask != null) {
                                mIfTask.cancel(true);
                            }
                            mIfTask = null;
                        }
                        if (mState == State.Connected) {
                            releasePushClientSocket();
                        }
                        getPushGatewayInformationFromServer();
                    }
                } else {
                    Log.e(TAG, ">> mPushInterfaceServerAddress is empty !!!");
                }
            } else if (action.equals(PushConstants.ACTION_GET_STATUS)) {
                sendSatusChangedBroadcastMessage();
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
        if (mState != State.Stopped) {
            Log.d(TAG, "getPushGatewayInformationFromServer() is not stopped!!");
            return;
        }
        if (!NetworkUtils.isConnected(this)) {
            if (mTcpThread != null) {
                releasePushClientSocket();
            }
            return;
        }

        if (mTcpThread != null) {
            releasePushClientSocket();
        }
        String prefName = getApplicationContext().getPackageName() + "_preferences";
        SharedPreferences prefs = getSharedPreferences(prefName, Context.MODE_PRIVATE);

        mState = State.IfRetrieving;
        mIfTask = new GetPushInterfaceTask(this, mHandler, mPushInterfaceServerAddress + PUSH_IF_CONTEXT);
        mIfTask.execute();
    }

    private void startPushClientSocket(final PushInterfaceData ifaceData) {
        if (ifaceData == null || !PushConstants.RESULT_OK.equals(ifaceData.resultCode)) {
            // send status changed

            // set next retry handler message.

            return;
        }
        mTcpThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mTcpClient = new TcpClient(mContext, ifaceData, new TcpClient.OnMessageReceived() {
                        @Override
                        public void messageReceived(PushBaseData data) {
                            Log.d(TAG, ">> TcpClient received from server = " + data.getMessageType());
                            Message msg = new Message();
                            msg.what = PushConstants.HANDLER_MESSAGE_RECEIVE_PUSH_DATA;
                            msg.obj = data;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void connectionClosed() {
                            Log.e(TAG, ">> TcpClient closed !!!!!");
                            if (mTcpThread != null && mTcpThread.isAlive()) {
                                mTcpThread.interrupt();
                            }
                        }

                        @Override
                        public void onConnectionError() {
                            Log.e(TAG, ">> TcpClient onConnectionError received !!");
                            if (mTcpThread != null && mTcpThread.isAlive()) {
                                mTcpThread.interrupt();
                            }
                        }

                        @Override
                        public void onConnected() {
                            Log.i(TAG, ">> TcpClient onConnected received !!");
                            mState = State.Connected;
                            sendSatusChangedBroadcastMessage();
                        }
                    });
                    mTcpClient.run();
                } finally {
                    Log.d(TAG, ">> TCP client thread is ended..");
                    releasePushClientSocket();
                }
            }
        });
        mTcpThread.start();
    }

    private void releasePushClientSocket() {
        if (mTcpClient != null) {
            mTcpClient.stopClient();
        }
        mTcpClient = null;
        if (mTcpThread != null && mTcpThread.isAlive()) {
            mTcpThread.interrupt();
        }
        mTcpThread = null;
        mState = State.Stopped;
        sendSatusChangedBroadcastMessage();

        if (NetworkUtils.isConnected(mContext)) {
            mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_RETRY_MESSAGE, MILLISECONDS * mNextRetryPeriodTerm);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendSatusChangedBroadcastMessage() {
        Log.d(TAG, "Send Broadcasting message action = " + PushConstants.ACTION_PUSH_STATUS_CHANGED);
        Intent intent = new Intent(PushConstants.ACTION_PUSH_STATUS_CHANGED);
        // You can also include some extra data.
        if (mState == State.Connected) {
            intent.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_CONNECTED);
        } else {
            intent.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED);
        }
        sendBroadcast(intent);
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
