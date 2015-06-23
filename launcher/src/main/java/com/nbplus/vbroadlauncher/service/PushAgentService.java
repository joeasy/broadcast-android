package com.nbplus.vbroadlauncher.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by basagee on 2015. 6. 22..
 */
public class PushAgentService extends Service {
    private static final String TAG = PushAgentService.class.getName();

    private static String mPushIfDomain;
    private static String mPushIfSessionRequestPath = "/is/api/appRequest/SessionRequest";
    private static String mPushAgentVersion = "1.0";

    public PushAgentService() {
    }

    /**
     * Handler of incoming messages from clients.
     *
     * 아래 사항은 참고하고 있어야 한다. 나중에 서비스 기획이 바뀌는 경우 IPC 수정이 필요하면 아래 내용을 참고해서
     * 적절하게 적용을 해야 한다.
     *
     * In Android there are multiple ways to do IPC, such as Intents (check IntentService),
     * Binders (AIDL or Messenger) and ASHMEM (anonymous shared memory).
     * I selected the Messenger for this example since it transparently queues all requests
     * into a single thread assuring thread-safeness.
     * If you need a multi-threaded service then you should consider the use of AIDL to define
     * your interface (check Android bound services  – implementation example here).
     */
    class ApplicationMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Intent broadcast;
            switch (msg.what) {
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.Note
     * that calls to its binder are sequential!
     */
    final Messenger mServiceMessenger = new Messenger(new ApplicationMessageHandler());

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand in service ...");
        /**
         * 항상 실행되도록 한다.
         */
        return Service.START_STICKY;//super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding to service ...");
        return mServiceMessenger.getBinder();
    }
}
