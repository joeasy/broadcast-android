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

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.nbplus.push.data.PushBaseData;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.data.PushInterfaceData;
import com.nbplus.push.data.PushMessageData;

import org.basdroid.common.NetworkUtils;

import java.lang.ref.WeakReference;

/**
 * Created by basagee on 2015. 7. 9..
 */
public class PushRunnable implements Runnable, TcpClient.OnMessageReceived {
    private static final String TAG = PushRunnable.class.getSimpleName();

    // indicates the state our service:
    public enum State {
        Stopped,            // Push agent stopped
        IfRetrieving,       // Push gw server searching. conn to interface server
        Connected           // connected to push gw server
    };

    State mState = State.Stopped;

    // indicates the state our service:
    public enum ThreadCommand {
        StopPushClient,            // Push agent stopped
        StartPushClient           // connected to push gw server
    };

    ThreadCommand mPushThreadCommand = ThreadCommand.StopPushClient;

    //boolean mIsPossibleTcpClientRun = false;
    TcpClient mTcpClient;
    PushInterfaceData mIfaceData;
    Context mContext;
    WifiManager.WifiLock mWifiLock;

    private static final int HANDLER_MESSAGE_PUSH_MESSAGE_TEST = 1111;

    private Handler mServiceHandler = null;
    private PushThreadHandler mHandler = new PushThreadHandler(this);
    // 핸들러 객체 만들기
    private static class PushThreadHandler extends Handler {
        private final WeakReference<PushRunnable> mThread;

        public PushThreadHandler(PushRunnable pushRunnable) {
            mThread = new WeakReference<>(pushRunnable);
        }

        @Override
        public void handleMessage(Message msg) {
            PushRunnable service = mThread.get();
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
            case HANDLER_MESSAGE_PUSH_MESSAGE_TEST :
                Message testMsg = new Message();
                testMsg.what = PushConstants.HANDLER_MESSAGE_RECEIVE_PUSH_DATA;

                PushMessageData data = new PushMessageData();
                data.setMessageType(PushConstants.PUSH_MESSAGE_TYPE_PUSH_REQUEST);
                data.setAlert("이건 시스템장애 푸시입니다. ");
                data.setMessageId(1);
//                data.setPayload("{ \"FROM\":\"김김김\", \"ADDRESS\":\"그냥마을\", " +
//                        "\"MESSAGE\":\"http://www.daum.net" +
//
//                        "\", \"SERVICE_TYPE\":\"08\"}");
//                data.setPayload("{ \"FROM\":\"김김김\", \"ADDRESS\":\"그냥마을\", " +
//                        "\"MESSAGE\":\"http://daum.net\", \"SERVICE_TYPE\":\"01\"}");

                data.setPayload("{ \"FROM\":\"김김김\", \"ADDRESS\":\"그냥마을\", " +
                        "\"MESSAGE\":\"" +
                        "111222" +

                        "\", \"SERVICE_TYPE\":\"02\"}");
                testMsg.obj = data;

                //mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_PUSH_MESSAGE_TEST, 60 * 1000);
                mHandler.sendMessage(testMsg);
                break;
            case PushConstants.HANDLER_MESSAGE_RECEIVE_PUSH_DATA :
                Log.d(TAG, "PushConstants.HANDLER_MESSAGE_RECEIVE_PUSH_DATA received !!!");
                PushMessageData pushData = (PushMessageData)msg.obj;
                if (pushData == null) {
                    Log.d(TAG, "empty push message data !!");
                    return;
                }
                Log.d(TAG, "Send broadcast payload to app !!!");

                Intent intent = new Intent();
                intent.setAction(PushConstants.ACTION_PUSH_MESSAGE_RECEIVED);
                intent.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE,
                        getState() == State.Connected ? PushConstants.PUSH_STATUS_VALUE_DISCONNECTED : PushConstants.PUSH_STATUS_VALUE_CONNECTED);
                intent.putExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA, pushData);
                mContext.sendBroadcast(intent);
                break;
        }
    }

    public PushRunnable(Context context, Handler handler, final PushInterfaceData ifaceData) {
        this.mContext = context;
        this.mIfaceData = ifaceData;
        this.mServiceHandler = handler;
    }

    public void sendSatusChangedBroadcastMessage() {
        Log.d(TAG, "Send Broadcasting message action = " + PushConstants.ACTION_PUSH_STATUS_CHANGED);
        Intent intent = new Intent(PushConstants.ACTION_PUSH_STATUS_CHANGED);
        if (mState == State.Connected) {
            intent.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_CONNECTED);
        } else {
            intent.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED);
        }
        mContext.sendBroadcast(intent);
    }

    public State getState() {
        return mState;
    }

    public void startPushClientSocket() {
        startPushClientSocket(null);
    }

    public void startPushClientSocket(PushInterfaceData data) {
        //this.mIsPossibleTcpClientRun = true;
        this.mIfaceData = data;
        if (this.mIfaceData != null) {
            releasePushClientSocket(false);
            mServiceHandler.removeMessages(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE);
            mPushThreadCommand = ThreadCommand.StartPushClient;
        }
    }

    public void setState(State state) {
        mState = state;
    }

    public void runTcpClient() {
        try {
            if (mIfaceData == null) {
                Log.i(TAG, "Invalid interface server data !!!");
            }

            if (mTcpClient == null) {
                mTcpClient = new TcpClient(/*mContext, */mIfaceData, this);
            } else {
                mTcpClient.setInterfaceServer(mIfaceData);
            }
            mTcpClient.run();
        } finally {
            releasePushClientSocket();
        }
    }

    private void releasePushClientSocket() {
        releasePushClientSocket(true);
    }
    public void releasePushClientSocket(boolean retry) {
        if (mTcpClient != null) {
            mTcpClient.stopClient();
        }
//        mTcpClient = null;
//                    if (mTcpThread != null && mTcpThread.isAlive()) {
//                        mTcpThread.interrupt();
//                    }
//                    mTcpThread = null;

        State prevState = mState;
        mState = State.Stopped;

        if (prevState != mState) {
            sendSatusChangedBroadcastMessage();
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock != null && mWifiLock.isHeld()) mWifiLock.release();
        mWifiLock = null;

        if (retry && NetworkUtils.isConnected(mContext)) {
            Log.d(TAG, "sendEmptyMessageDelayed PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE");
            mServiceHandler.removeMessages(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE);
            mServiceHandler.sendEmptyMessageDelayed(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE, PushService.MILLISECONDS * PushService.mNextRetryPeriodTerm);
        }
    }

    @Override
    public void messageReceived(PushBaseData data) {
        Log.d(TAG, ">> TcpClient received from server(messageId) = " + data.getMessageId());
        Message msg = new Message();
        msg.what = PushConstants.HANDLER_MESSAGE_RECEIVE_PUSH_DATA;
        msg.obj = data;
        mHandler.sendMessage(msg);
    }

    @Override
    public void connectionClosed() {
        Log.e(TAG, ">> TcpClient closed !!!!!");
//                                if (mTcpThread != null && mTcpThread.isAlive()) {
//                                    mTcpThread.interrupt();
//                                }
        //mIsPossibleTcpClientRun = false;
        releasePushClientSocket();
    }

    @Override
    public void onConnectionError() {
        Log.e(TAG, ">> TcpClient onConnectionError received !!");
//                                if (mTcpThread != null && mTcpThread.isAlive()) {
//                                    mTcpThread.interrupt();
//                                }
        //mIsPossibleTcpClientRun = false;
        releasePushClientSocket();
    }

    @Override
    public void onConnected() {
        Log.i(TAG, ">> TcpClient onConnected received !!");
        mState = State.Connected;
        sendSatusChangedBroadcastMessage();

        if (mWifiLock != null) {
            mWifiLock.acquire();
            Log.i(TAG, ">> mWifiLock.acquire() called !!");
        }
    }

    @Override
    public void run() {
        try {
            mWifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, PushRunnable.class.getSimpleName() + "_lock");
            //mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_PUSH_MESSAGE_TEST, 20 * 1000);

            while (!Thread.currentThread().isInterrupted()) {
                switch (mPushThreadCommand) {
                    case StartPushClient:
                        Log.i(TAG, "PushRunnable run() check mPushThreadCommand == StartPushClient");
                        mPushThreadCommand = ThreadCommand.StopPushClient;
                        runTcpClient();
                        break;
                }

                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            releasePushClientSocket();
        }
    }
}
