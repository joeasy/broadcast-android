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
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;

/**
 * Created by basagee on 2015. 7. 9..
 */
public class PushThread implements Runnable, TcpClient.OnMessageReceived {
    private static final String TAG = PushThread.class.getName();

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
        private final WeakReference<PushThread> mThread;

        public PushThreadHandler(PushThread pushThread) {
            mThread = new WeakReference<>(pushThread);
        }

        @Override
        public void handleMessage(Message msg) {
            PushThread service = mThread.get();
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
                data.setAlert("이건 패스워드 찾기입니다.");
                data.setMessageId(1);
                data.setPayload("{ \"FROM\":\"김김김\", \"ADDRESS\":\"그냥마을\", " +
                        "\"MESSAGE\":\"http://www.daum.net" +

                        "\", \"SERVICE_TYPE\":\"08\"}");

//                data.setPayload("{ \"FROM\":\"김김김\", \"ADDRESS\":\"그냥마을\", " +
//                        "\"MESSAGE\":\"" +
//                        "누구에게나 결함은 있단다.\n" + "그리고 고치려고 해도\n" + "때로 자기 힘으로는 어쩔 수 없는 결함도 있지.\n" +
//                        "너의 머리 냄새가 \n" +
//                        "다른 사람을 쉽게 이해할 수 있는\n" +
//                        "아주 고마운 것이 되기를 엄마는 진정으로 바란다.\n" +
//
//                        "\", \"SERVICE_TYPE\":\"02\"}");
//                data.setPayload("{ \"FROM\":\"김김김\", \"ADDRESS\":\"그냥마을\", " +
//                        "\"MESSAGE\":\"http://daum.net\", \"SERVICE_TYPE\":\"01\"}");
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

    public PushThread(Context context, Handler handler, final PushInterfaceData ifaceData) {
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
            mTcpClient = new TcpClient(mContext, mIfaceData, this);
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
        mTcpClient = null;
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

        if (retry && NetworkUtils.isConnected(mContext)) {
            Log.d(TAG, "sendEmptyMessageDelayed PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE");
            mServiceHandler.removeMessages(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE);
            mServiceHandler.sendEmptyMessageDelayed(PushConstants.HANDLER_MESSAGE_RETRY_MESSAGE, PushService.MILLISECONDS * PushService.mNextRetryPeriodTerm);
        }
    }

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
        }
    }

    @Override
    public void run() {
        try {
            mWifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "push_wifi_lock");
            mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_PUSH_MESSAGE_TEST, 20 * 1000);

            while (!Thread.currentThread().isInterrupted()) {
                switch (mPushThreadCommand) {
                    case StartPushClient:
                        Log.i(TAG, "PushThread run() check mPushThreadCommand == StartPushClient");
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
