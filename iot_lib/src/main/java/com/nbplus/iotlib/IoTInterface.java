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

package com.nbplus.iotlib;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.nbplus.iotapp.service.IoTService;
import com.nbplus.iotlib.callback.IoTServiceResponse;
import com.nbplus.iotlib.data.Constants;
import com.nbplus.iotlib.data.DeviceTypes;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTServiceCommand;
import com.nbplus.iotlib.data.IoTServiceStatus;
import com.nbplus.iotlib.exception.InitializeRequiredException;
import com.nbplus.iotlib.exception.NotRegisteredException;

import org.basdroid.common.PackageUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Created by basagee on 2015. 3. 23..
 */
public class IoTInterface {
    private static final String TAG = IoTInterface.class.getSimpleName();

    // 서비스 상태에 대한 복사본...
    private IoTServiceStatus mServiceStatus = IoTServiceStatus.NONE;
    private IoTResultCodes mErrorCodes = IoTResultCodes.SUCCESS;

    /**
     * IOT service
     */
    private static final String REMOTE_IOT_SERVICE_PACKAGE = "com.nbplus.iotapp";
    private static final String REMOTE_IOT_SERVICE_CLASS = "com.nbplus.iotapp.service.IoTService";
    private static final String REMOTE_IOT_SERVICE_ACTION = "com.nbplus.iotapp.intent.action.IOT_SERVICE";

    /** Messenger for sending messages to the service. */
    Messenger mServiceMessenger = null;
    /** Messenger for receiving messages from the service. */
    Messenger mClientMessenger = null;

    /**
     * Target we publish for clients to send messages to IncomingHandler. Note
     * that calls to its binder are sequential!
     */
    private final IoTInterfaceHandler handler;

    /**
     * Handler thread to avoid running on the main thread (UI)
     */
    private final HandlerThread handlerThread;

    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    /** Context of the activity from which this connector was launched */
    private Context mCtx;

    HashMap<String, WeakReference<IoTServiceResponse>> mRequestedCallbaks = new HashMap<>();
    HashMap<String, WeakReference<IoTServiceResponse>> mWaitingRequestCallbaks = new HashMap<>();

    /**
     * singleton instance 로 만들어준다.
     * 굳이 필요는 없지만... 여러개 인스턴스가 생성될 필요는 없다.
     */
    private volatile static IoTInterface mSingletonInstance;
    public static IoTInterface getInstance(){
        if (mSingletonInstance == null) {
            synchronized(IoTInterface.class) {
                if (mSingletonInstance == null) {
                    mSingletonInstance = new IoTInterface();
                }
            }
        }
        return mSingletonInstance;
    }
    private IoTInterface() {
        handlerThread = new HandlerThread("IPChandlerThread");
        handlerThread.start();
        handler = new IoTInterfaceHandler(handlerThread);
        mClientMessenger = new Messenger(handler);
    }

    public IoTResultCodes initialize(Context context) {
        mCtx = context;

        // connect to remote service
        if (Constants.USE_ANOTHER_APP) {
            if (PackageUtils.isPackageExisted(mCtx, Constants.NBPLUS_IOT_APP_PACKAGE_NAME)) {
                if (!bindService()) {
                    mErrorCodes = IoTResultCodes.BIND_SERVICE_FAILED;
                    return mErrorCodes;
                }
            } else {
                // show market detail page
                new AlertDialog.Builder(mCtx).setMessage(R.string.app_install_required)
                        .setCancelable(false)
                        .setPositiveButton(R.string.alert_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        PackageUtils.showMarketDetail(mCtx, Constants.NBPLUS_IOT_APP_PACKAGE_NAME);
                                    }
                                })
                        .show();
                mErrorCodes = IoTResultCodes.IOT_APP_NOT_INSTALLED;
                return mErrorCodes;
            }
        } else {
            if (!bindService()) {
                mErrorCodes = IoTResultCodes.BIND_SERVICE_FAILED;
                return mErrorCodes;
            }
        }
        return mErrorCodes;
    }
    // end of singleton

    /**
     * Handler of incoming messages from service.
     */
//    private static final int HANDLER_SERVICE_CREATED = IoTServiceCommand.COMMAND_BASE_VALUE - 1;
//    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (Constants.ACTION_SERVICE_CREATE_BROADCAST.equals(action)) {
//                mHandler.sendEmptyMessage(HANDLER_MESSAGE_CONNECTIVITY_CHANGED);
//            }
//
//        }
//
//    };

    class IoTInterfaceHandler extends Handler {

        public IoTInterfaceHandler(HandlerThread thr) {
            super(thr.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IoTServiceCommand.COMMAND_RESPONSE :
                    handleResponse(msg);
                    break;

                case IoTServiceCommand.SERVICE_STATUS_NOTIFICATION: {
                    Bundle b = msg.getData();
                    if (b != null) {
                        mServiceStatus = (IoTServiceStatus)b.getSerializable(IoTServiceCommand.KEY_SERVICE_STATUS);
                        mErrorCodes = (IoTResultCodes)b.getSerializable(IoTServiceCommand.KEY_SERVICE_STATUS_CODE);

                        Log.d(TAG, "IoTServiceCommand.SERVICE_STATUS_NOTIFICATION : status = " + mServiceStatus + ", errCode = " + mErrorCodes);
                    }
                    break;
                }

                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void handleResponse(Message msg) {
        Bundle b = msg.getData();
        if (b == null) {
            Log.d(TAG, "handleResponse : bundle not found..");
            return;
        }

        int cmd = b.getInt(IoTServiceCommand.KEY_CMD, -1);
        switch (cmd) {
            case IoTServiceCommand.REGISTER_SERVICE : {
                IoTResultCodes resultCode = (IoTResultCodes) b.getSerializable(IoTServiceCommand.KEY_RESULT);
                if (resultCode != null && resultCode.equals(IoTResultCodes.SUCCESS)) {
                    // success
                    Log.d(TAG, ">> IoT Register service success...");
                } else {
                    Log.e(TAG, ">> IoT Register service failed code = " + resultCode);
                    mErrorCodes = resultCode;
                }
                break;
            }
            case IoTServiceCommand.UNREGISTER_SERVICE: {
                IoTResultCodes resultCode = (IoTResultCodes) b.getSerializable(IoTServiceCommand.KEY_RESULT);
                if (resultCode != null && resultCode.equals(IoTResultCodes.SUCCESS)) {
                    // success
                    Log.d(TAG, ">> IoT Register service success...");
                } else {
                    Log.e(TAG, ">> IoT Register service failed code = " + resultCode);
                }
                break;
            }
            default:
                Log.d(TAG, "unknown command..");
                break;
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected. send REGISTER_SERVICE..");
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service. We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mServiceMessenger = new Messenger(service);

            // Now that we have the service messenger, lets send our messenger
            Message msg = new Message();
            msg.what = IoTServiceCommand.REGISTER_SERVICE;
            msg.replyTo = mClientMessenger;

            /*
             * In case we would want to send extra data, we could use Bundles:
             * Bundle b = new Bundle(); b.putString("key", "hello world");
             * msg.setData(b);
             */
            Bundle b = new Bundle();
            b.putString(IoTServiceCommand.KEY_MSGID, IoTServiceCommand.generateMessageId(mCtx));
            msg.setData(b);

            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mServiceMessenger = null;
            mServiceStatus = IoTServiceStatus.STOPPED;
            mErrorCodes = IoTResultCodes.SERVICE_DISCONNECTED;
            mBound = false;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Service disconnected... and retry connection!!!");
                    bindService();
                }
            }, 5000);
        }
    };

    /**
     * Method used for binding with the service
     */
    private boolean bindService() {
        /*
         * Note that this is an implicit Intent that must be defined in the
         * Android Manifest.
         */

        Intent i = null;
        if (Constants.USE_ANOTHER_APP) {
            i = new Intent();
            i.setClassName(REMOTE_IOT_SERVICE_PACKAGE, REMOTE_IOT_SERVICE_CLASS);
            i.setAction(REMOTE_IOT_SERVICE_ACTION);
        } else {
            i = new Intent(mCtx, IoTService.class);
            i.setAction(REMOTE_IOT_SERVICE_ACTION);
        }

        try {
            if (mCtx.getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE)) {
                mServiceStatus = IoTServiceStatus.INITIALIZE;
                return true;
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.e(TAG, "Not allowed to bind to service Intent");
        }
        mServiceStatus = IoTServiceStatus.NONE;
        return false;
    }

    private void unbindService() {
        if (mBound) {
            mCtx.getApplicationContext().unbindService(mConnection);
            mBound = false;
        }

//        try {
//            mCtx.unregisterReceiver(mBroadcastReceiver);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /**
     * public methods
     */
    public void getDevicesList(DeviceTypes type, final IoTServiceResponse callback) {
        if (mCtx == null) {
            InitializeRequiredException initException = new InitializeRequiredException("Init required Exception!!");

            try {
                throw initException;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (mServiceStatus.equals(IoTServiceStatus.NONE) && !mErrorCodes.equals(IoTResultCodes.SUCCESS)) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(IoTServiceCommand.GET_DEVICE_LIST, mServiceStatus, mErrorCodes, null);
                }
            }, 100);
        } else if (mServiceStatus.equals(IoTServiceStatus.RUNNING)) {
            // 서비스가 정상동작 중
            Message msg = new Message();

            msg.what = IoTServiceCommand.GET_DEVICE_LIST;
            Bundle b = new Bundle();
            String msgId = IoTServiceCommand.generateMessageId(mCtx);
            b.putString(IoTServiceCommand.KEY_MSGID, msgId);
            b.putSerializable(IoTServiceCommand.KEY_DEVICE_TYPE, type);

            mRequestedCallbaks.put(msgId, new WeakReference<>(callback));

            msg.setData(b);
            handler.sendMessage(msg);

        } else if (mServiceStatus.equals(IoTServiceStatus.INITIALIZE)) {
            // bound 진행중?
            String msgId = IoTServiceCommand.generateMessageId(mCtx);
            mWaitingRequestCallbaks.put(msgId, new WeakReference<>(callback));
            // REGISTER 가 잘못되었다.???
            // 개발시에 처리되어야 한다.
//            try {
//                throw new NotRegisteredException("Not registered !!!");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return;
        } else {
            // stop 상태.. 어플리케이션에 서비스상태와 에러코드를 돌려준다.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(IoTServiceCommand.GET_DEVICE_LIST, mServiceStatus, mErrorCodes, null);
                }
            }, 100);
        }
    }
    public void controlDevice(String deviceId, String command) {

    }

}
