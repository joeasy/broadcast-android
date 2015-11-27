/*
 * Copyright (c) 2015. NB Plus (www.nbplus.co.kr)
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
 *
 */

package com.nbplus.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.data.PushInterfaceData;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;
import org.basdroid.volley.GsonRequest;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class PushService extends Service {
    private static final String TAG = PushService.class.getName();

    // minutes
    public static final int MILLISECONDS = 1000;
    public static final int mNextRetryPeriodTerm = 30;

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
    boolean mLastConnectionStatus = false;
    SharedPreferences mPrefs;

    Thread mPushThread;
    PushRunnable mPushRunnable;

    String mPushInterfaceServerAddress = null;
    final String PUSH_IF_CONTEXT = "/is/api/appRequest/SessionRequest";

    /**
     * start : push interface variables
     */
    RequestFuture<PushInterfaceData> mGwRequestFuture = null;
    String mRequestBody;
    RequestQueue mRequestQueue;
    GetPushInterfaceTask mIfTask = null;
    class GetPushInterfaceRequestBody {
        @SerializedName("DEVICE_ID")
        public String deviceId;
        @SerializedName("DEVICE_TYPE")
        public String deviceType;
        @SerializedName("VERSION")
        public String pushVersion;
        @SerializedName("MAKER")
        public String vendor;
        @SerializedName("MODEL")
        public String model;
        @SerializedName("OS")
        public String os;
    }
    // end

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
                if (mPushRunnable.getState() == PushRunnable.State.Connected) {
                    Log.d(TAG, ">> Already reconnected. ignore retry message !!!");
                    return;
                }
                mHandler.removeMessages(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE);
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
                    mPushRunnable.startPushClientSocket(data);
                } else {
                    if (mPushRunnable.getState() == PushRunnable.State.Connected) {
                        Log.d(TAG, "Close previous connection !!!. and retry.");
                        mPushRunnable.releasePushClientSocket(NetworkUtils.isConnected(this));
                    } else {
                        Log.d(TAG, "retry HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA!!!. after 1 min..");
                        mHandler.removeMessages(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE);
                        mHandler.sendEmptyMessageDelayed(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE, PushService.MILLISECONDS * PushService.mNextRetryPeriodTerm);
                    }
                }
                break;
            case PushConstants.HANDLER_MESSAGE_CONNECTIVITY_CHANGED :
                final boolean isConnected = NetworkUtils.isConnected(this);
                Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED received isConnected = " + isConnected);
                if (mLastConnectionStatus == isConnected) {
                    Log.d(TAG, "mLastConnectionStatus == isConnected do not anything.");
                    return;
                }

                mLastConnectionStatus = isConnected;
                if (mLastConnectionStatus) {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is connected !!!");
                    if (!StringUtils.isEmptyString(mPushInterfaceServerAddress)) {
                        getPushGatewayInformationFromServer();
                    }
                } else {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is disconnected !!!");
                    if (mPushRunnable.getState() != PushRunnable.State.Stopped) {
                        mPushRunnable.releasePushClientSocket(false);
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
        String pushInterfaceServerAddress = null;
        String action = (intent != null) ? intent.getAction() : null;
        Log.d(TAG, "onStartCommand in service.. action = " + action);
        if (action == null) {
            action = PushConstants.ACTION_START_SERVICE;
            pushInterfaceServerAddress = mPrefs.getString(PushConstants.PREF_KEY_PUSH_IF_ADDRESS, "");
        }
        if (action.equals(PushConstants.ACTION_START_SERVICE)) {
            if (intent != null && action != null) {
                pushInterfaceServerAddress = intent.getStringExtra(PushConstants.EXTRA_START_SERVICE_IFADDRESS);
            }
            if (!StringUtils.isEmptyString(pushInterfaceServerAddress)) {
                if (mPushInterfaceServerAddress == null || !pushInterfaceServerAddress.equals(mPushInterfaceServerAddress)) {
                    mPushInterfaceServerAddress = pushInterfaceServerAddress;
                    mPrefs.edit().putString(PushConstants.PREF_KEY_PUSH_IF_ADDRESS, mPushInterfaceServerAddress).apply();

                    if (mPushRunnable.getState() == PushRunnable.State.IfRetrieving) {
                        if (mIfTask != null) {
                            mIfTask.cancel(true);
                        }
                        mIfTask = null;
                    }

                    if (mPushRunnable.getState() == PushRunnable.State.Connected) {
                        mPushRunnable.releasePushClientSocket(false);
                    }
                    Log.d(TAG, "mPushInterfaceServerAddress is (re)setted!!! getPushGatewayInformationFromServer()");
                    getPushGatewayInformationFromServer();
                } else {
                    if (pushInterfaceServerAddress.equals(mPushInterfaceServerAddress)) {
                        Log.d(TAG, ">> Same address... do not anything...");
                        return Service.START_STICKY;
                    }
                }
            } else {
                Log.e(TAG, ">> mPushInterfaceServerAddress is empty... maybe logout???? !!!");
                mPushRunnable.releasePushClientSocket(false);
            }
        } else if (action.equals(PushConstants.ACTION_GET_STATUS)) {
            mPushRunnable.sendSatusChangedBroadcastMessage(PushConstants.PUSH_STATUS_WHAT_NORMAL);
        }

        /**
         * 항상 실행되도록 한다.
         */
        return Service.START_STICKY;//super.onStartCommand(intent, flags, startId);
    }

    /**
     * 1 번만 생성되도록... 최대한메모리를줄이기 위해서.
     * @param url
     */
    private void initPushGatewayTaskSettings(final String url) {
        if (mRequestBody == null) {
            String prefName = mContext.getApplicationContext().getPackageName() + "_preferences";
            SharedPreferences prefs = mContext.getSharedPreferences(prefName, Context.MODE_PRIVATE);

            // load from preferences..
            String deviceId = prefs.getString(PushConstants.KEY_DEVICE_ID, "");
            if (StringUtils.isEmptyString(deviceId)) {
                deviceId = DeviceUtils.getDeviceIdByMacAddress(mContext);
                prefs.edit().putString(PushConstants.KEY_DEVICE_ID, deviceId).apply();
            }

            GetPushInterfaceRequestBody reqBodyObj = new GetPushInterfaceRequestBody();
            reqBodyObj.deviceId = deviceId;
            reqBodyObj.os = Build.VERSION.RELEASE;
            reqBodyObj.pushVersion = Integer.toString(BuildConfig.VERSION_CODE);
            reqBodyObj.vendor = Build.MANUFACTURER;
            reqBodyObj.model = DeviceUtils.getDeviceName();
            reqBodyObj.os = Build.ID + " " + Build.VERSION.RELEASE;
            reqBodyObj.deviceType = "android";

            Gson gson = new GsonBuilder().create();
            mRequestBody = gson.toJson(reqBodyObj, new TypeToken<GetPushInterfaceRequestBody>(){}.getType());
        }

        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext, new HurlStack() {
                @Override
                protected HttpURLConnection createConnection(URL url) throws IOException {
                    HttpURLConnection connection = super.createConnection(url);
                    // Fix for bug in Android runtime(!!!):
                    // https://code.google.com/p/android/issues/detail?id=24672
                    connection.setRequestProperty("Accept-Encoding", "");

                    return connection;
                }
            });

            mGwRequestFuture = RequestFuture.newFuture();
            GsonRequest request = new GsonRequest(Request.Method.POST, url, mRequestBody, PushInterfaceData.class, mGwRequestFuture, mGwRequestFuture);
            request.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 3, 1.0f));
            mRequestQueue.add(request);
        }
    }

    /**
     * volley connection ENETUNREACH 가 유독 여기에서만발생..
     * async로 처리해서 테스트해보자.
     * @param url
     */
    private static int mApiRetryCount = 0;

    private void getFromServer(final String url) {
        if (mRequestBody == null) {
            String prefName = mContext.getApplicationContext().getPackageName() + "_preferences";
            SharedPreferences prefs = mContext.getSharedPreferences(prefName, Context.MODE_PRIVATE);

            // load from preferences..
            String deviceId = prefs.getString(PushConstants.KEY_DEVICE_ID, "");
            if (StringUtils.isEmptyString(deviceId)) {
                deviceId = DeviceUtils.getDeviceIdByMacAddress(mContext);
                prefs.edit().putString(PushConstants.KEY_DEVICE_ID, deviceId).apply();
            }

            GetPushInterfaceRequestBody reqBodyObj = new GetPushInterfaceRequestBody();
            reqBodyObj.deviceId = deviceId;
            reqBodyObj.os = Build.VERSION.RELEASE;
            reqBodyObj.pushVersion = Integer.toString(BuildConfig.VERSION_CODE);
            reqBodyObj.vendor = Build.MANUFACTURER;
            reqBodyObj.model = DeviceUtils.getDeviceName();
            reqBodyObj.os = Build.ID + " " + Build.VERSION.RELEASE;
            reqBodyObj.deviceType = "android";

            Gson gson = new GsonBuilder().create();
            mRequestBody = gson.toJson(reqBodyObj, new TypeToken<GetPushInterfaceRequestBody>(){}.getType());
        }

        RequestQueue requestQueue = Volley.newRequestQueue(mContext, new HurlStack() {
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection connection = super.createConnection(url);
                // Fix for bug in Android runtime(!!!):
                // https://code.google.com/p/android/issues/detail?id=24672
                connection.setRequestProperty("Accept-Encoding", "");

                return connection;
            }
        });
        final GsonRequest gsonRequest = new GsonRequest(Request.Method.POST, url, mRequestBody, PushInterfaceData.class, new Response.Listener<PushInterfaceData>() {

            @Override
            public void onResponse(PushInterfaceData response) {
                Log.d(TAG, ">>> get PushInterfaceData success !!!");
                mApiRetryCount = 0;

                Message message = new Message();
                message.what = PushConstants.HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA;
                message.obj = response;
                mHandler.sendMessage(message);
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                /**
                 * 2015.10.05
                 * 실패시 1분후에 재시도 하도록 한다.
                 */
                Log.d(TAG, ">>> get PushInterfaceData error... retry connection after 1 minutes...");
                if (mPushRunnable.getState() != PushRunnable.State.Stopped) {
                    mPushRunnable.releasePushClientSocket(false);
                }
                mHandler.removeMessages(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE);
                mHandler.sendEmptyMessageDelayed(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE, PushService.MILLISECONDS * PushService.mNextRetryPeriodTerm);
            }
        });
        gsonRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 3, 1.0f));
        requestQueue.add(gsonRequest);
    }

    private void getPushGatewayInformationFromServer() {
        mPushRunnable.releasePushClientSocket(false);
        mHandler.removeMessages(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE);

        mPushRunnable.setState(PushRunnable.State.IfRetrieving);
        //initPushGatewayTaskSettings(mPushInterfaceServerAddress + PUSH_IF_CONTEXT);
        getFromServer(mPushInterfaceServerAddress + PUSH_IF_CONTEXT);
        //mIfTask = new GetPushInterfaceTask(this, mHandler, mGwRequestFuture);
        //mIfTask.execute();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground() {
        // notification's layout
        mBuilder = new NotificationCompat.Builder(this);

        CharSequence ticker = getString(R.string.push_noti_body);
        int apiVersion = Build.VERSION.SDK_INT;

        mBuilder.setSmallIcon(R.drawable.ic_notification_push)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(getString(R.string.push_name))
                .setContentText(ticker)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setTicker(ticker);

        mNotification = mBuilder.build();
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.defaults |= Notification.DEFAULT_LIGHTS;
        startForeground(NOTIFICATION_ID, mNotification);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "PushService onCreate()......");

        String prefName = getApplicationContext().getPackageName() + "_preferences";
        mPrefs = getSharedPreferences(prefName, Context.MODE_PRIVATE);

        // check network status
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);

        mLastConnectionStatus = NetworkUtils.isConnected(this);

        /**
         * 넣지않아도 문제가 없다면...
         * 2015.07.28
         */
        //setUpAsForeground();
        mPushRunnable = new PushRunnable(this, mHandler, null);

        if (mPushThread != null) {
            mPushThread.interrupt();
        }

        mPushThread = new Thread(mPushRunnable);
        mPushThread.start();

        mContext = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PushService onDestroy()......");
        unregisterReceiver(mBroadcastReceiver);
        /**
         * 넣지않아도 문제가 없다면...
         * 2015.07.28
         */
        //stopForeground(true);
        if (mPushRunnable != null) {
            mPushRunnable.releasePushClientSocket(false);
        }
        if (mPushThread != null) {
            mPushThread.interrupt();
        }
        mHandler = null;
    }
}
