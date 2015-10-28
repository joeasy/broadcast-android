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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nbplus.iotapp.data.AdRecord;
import com.nbplus.iotapp.data.DataParser;
import com.nbplus.iotapp.perferences.IoTServicePreference;
import com.nbplus.iotapp.service.IoTService;
import com.nbplus.iotlib.callback.IoTServiceResponse;
import com.nbplus.iotlib.data.Constants;
import com.nbplus.iotlib.data.DeviceTypes;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTScenarioDef;
import com.nbplus.iotlib.data.IoTServiceCommand;
import com.nbplus.iotlib.data.IoTServiceStatus;
import com.nbplus.iotlib.exception.InitializeRequiredException;

import org.basdroid.common.PackageUtils;
import org.basdroid.common.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by basagee on 2015. 3. 23..
 */
public class IoTInterface {
    private static final String TAG = IoTInterface.class.getSimpleName();

    // 서비스 상태에 대한 복사본...
    private IoTServiceStatus mServiceStatus = IoTServiceStatus.NONE;
    private IoTResultCodes mErrorCodes = IoTResultCodes.SUCCESS;

    /**
     * IoT 디바이스 데이터 변경내역 검사 주기.
     * 1시간이다.
     */
    private static final int RETRIEVE_IOT_DEVICE_DATA_PERIOD = 60 * 60 * 1000;

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
    private final IoTInterfaceHandler mHandler;

    /**
     * Handler thread to avoid running on the main thread (UI)
     */
    private final HandlerThread handlerThread;

    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    /** Context of the activity from which this connector was launched */
    private Context mCtx;
    private boolean mInitialized = false;

    WeakReference<IoTServiceResponse> mForceRescanCallback = null;
    HashMap<String, WeakReference<IoTServiceResponse>> mRequestedCallbaks = new HashMap<>();
    HashMap<String, WeakReference<IoTServiceResponse>> mWaitingRequestCallbaks = new HashMap<>();

    /**
     * known IoT device scenarios
     */
    HashMap<String, IoTScenarioDef> mIoTScenarioMap = null;

    /**
     * 10초동안 검색된 device list 를 저장해 두는 공간
     */
    private HashMap<String, IoTDevice> mScanedList = new HashMap<>();

    /**
     * singleton instance 로 만들어준다.
     * 굳이 필요는 없지만... 여러개 인스턴스가 생성될 필요는 없다.
     */
    private volatile static IoTInterface mSingletonInstance;
    public static IoTInterface getInstance() {
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
        mHandler = new IoTInterfaceHandler(handlerThread);
        mClientMessenger = new Messenger(mHandler);
    }

    public IoTResultCodes initialize(Context context) {
        mCtx = context;

        mInitialized = true;

        Gson gson = new Gson();
        // load scenarios
        String jsonString = IoTServicePreference.getIoTDeviceScenarioMap(mCtx);
        if (jsonString.equals("{}")) {
            jsonString = loadJSONFromAsset("default_scenario.json");
        }
        if (!StringUtils.isEmptyString(jsonString)) {
            Log.d(TAG, ">> scenario map json string = " + jsonString);
            mIoTScenarioMap = gson.fromJson(jsonString, new TypeToken<HashMap<String, IoTScenarioDef>>(){}.getType());
        }

        // load saved devices list
        String savedJson = IoTServicePreference.getIoTDevicesList(mCtx);
        if (!StringUtils.isEmptyString(savedJson)) {
            Log.d(TAG, ">> scanned list json string = " + jsonString);
            mScanedList = gson.fromJson(savedJson, new TypeToken<HashMap<String, IoTDevice>>(){}.getType());
        }

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
                case IoTServiceCommand.DEVICE_LIST_NOTIFICATION: {
                    Bundle b = msg.getData();
                    if (b != null) {
                        HashMap<String, IoTDevice> devices = (HashMap<String, IoTDevice>)b.getSerializable(IoTServiceCommand.KEY_DATA);
                        boolean changed = false;
                        if (devices != null) {
                            Iterator<String> iter = devices.keySet().iterator();
                            while(iter.hasNext()) {
                                String key = iter.next();
                                IoTDevice device = mScanedList.get(key);
                                if (device != null) {           // already exist
                                    IoTDevice scannedDevice = devices.get(key);
                                    ArrayList<String> scanedUuids = DataParser.getUuids(scannedDevice.getAdRecordHashMap());
                                    if (scanedUuids == null || scanedUuids.size() == 0) {
                                        Log.e(TAG, ">>> xx device name " + device.getDeviceName() + " has no uuid advertisement");
                                        continue;
                                    }
                                    ArrayList<String> deviceUuids = DataParser.getUuids(device.getAdRecordHashMap());
                                    if (deviceUuids == null || deviceUuids.size() == 0) {
                                        device.setAdRecordHashMap(scannedDevice.getAdRecordHashMap());
                                        device.setUuids(scanedUuids);
                                        device.setUuidLen(DataParser.getUuidLength(scannedDevice.getAdRecordHashMap()));
                                        changed = true;
                                    }
                                    continue;
                                }

                                device = devices.get(key);
                                ArrayList<String> uuids = DataParser.getUuids(device.getAdRecordHashMap());
                                if (uuids == null || uuids.size() == 0) {
                                    Log.e(TAG, ">>> device name " + device.getDeviceName() + " has no uuid advertisement");
                                    continue;
                                }
                                changed = true;
                                device.setUuids(uuids);
                                device.setUuidLen(DataParser.getUuidLength(device.getAdRecordHashMap()));
                                mScanedList.put(device.getDeviceId(), device);
                            }

                            Log.d(TAG, "IoTServiceCommand.GET_DEVICE_LIST added size = " + devices.size());
                        }
                        if (changed) {
                            // save to preference.
                            Gson gson = new Gson();
                            String json = gson.toJson(mScanedList);
                            if (json != null) {
                                IoTServicePreference.setIoTDevicesList(mCtx, json);
                            }
                        }
                        Log.d(TAG, "Current device size = " + mScanedList.size());

                        if (mForceRescanCallback != null) {
                            final IoTServiceResponse responseCallback = mForceRescanCallback.get();
                            if (responseCallback != null) {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Bundle b = new Bundle();
                                        ArrayList<IoTDevice> devicesList = null;
                                        if (mScanedList != null && mScanedList.size() > 0) {
                                            devicesList = new ArrayList<>(mScanedList.values());
                                        } else {
                                            devicesList = new ArrayList<>();
                                        }

                                        b.putParcelableArrayList(IoTServiceCommand.KEY_DATA, devicesList);
                                        responseCallback.onResult(IoTServiceCommand.GET_DEVICE_LIST, mServiceStatus, mErrorCodes, b);
                                    }
                                }, 100);
                            }
                            mForceRescanCallback = null;
                        }

                        // TODO : log
                        Iterator<String> iter = mScanedList.keySet().iterator();
                        while(iter.hasNext()) {
                            String key = iter.next();
                            IoTDevice device = mScanedList.get(key);

                            printScanDeviceInformation(device);
                        }
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

            /**
             * IoT 디바이스 상태를 조회한다.
             */
            //mHandler.sendEmptyMessageDelayed();

            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mServiceMessenger = null;
            mServiceStatus = IoTServiceStatus.STOPPED;
            mErrorCodes = IoTResultCodes.SERVICE_DISCONNECTED;
            mBound = false;

            mHandler.postDelayed(new Runnable() {
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
        getDevicesList(type, callback, false);
    }
    public void getDevicesList(DeviceTypes type, final IoTServiceResponse callback, boolean forceRescan) {
        if (!mInitialized) {
            InitializeRequiredException initException = new InitializeRequiredException("Init required Exception!!");

            try {
                throw initException;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (mServiceStatus.equals(IoTServiceStatus.NONE) && !mErrorCodes.equals(IoTResultCodes.SUCCESS)) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(IoTServiceCommand.GET_DEVICE_LIST, mServiceStatus, mErrorCodes, null);
                }
            }, 100);
        } else if (mServiceStatus.equals(IoTServiceStatus.RUNNING)) {
            // 서비스가 정상동작 중
            if (forceRescan) {
                // 전체 다시 검색을 요청할 경우..
                Message msg = new Message();

                msg.what = IoTServiceCommand.GET_DEVICE_LIST;
                Bundle b = new Bundle();
                String msgId = IoTServiceCommand.generateMessageId(mCtx);
                b.putString(IoTServiceCommand.KEY_MSGID, msgId);
                b.putSerializable(IoTServiceCommand.KEY_DEVICE_TYPE, type);

                mForceRescanCallback = new WeakReference<>(callback);
                //mRequestedCallbaks.put(msgId, new WeakReference<>(callback));

                msg.setData(b);
                try {
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Bundle b = new Bundle();
                        ArrayList<IoTDevice> devicesList = null;
                        if (mScanedList != null && mScanedList.size() > 0) {
                            devicesList = new ArrayList<>(mScanedList.values());
                        } else {
                            devicesList = new ArrayList<>();
                        }

                        b.putParcelableArrayList(IoTServiceCommand.KEY_DATA, devicesList);
                        callback.onResult(IoTServiceCommand.GET_DEVICE_LIST, mServiceStatus, mErrorCodes, b);
                    }
                }, 100);
            }
        } else if (mServiceStatus.equals(IoTServiceStatus.INITIALIZE)) {
            // bound 진행중?
            String msgId = IoTServiceCommand.generateMessageId(mCtx);
            mForceRescanCallback = new WeakReference<>(callback);
            //mWaitingRequestCallbaks.put(msgId, new WeakReference<>(callback));
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
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(IoTServiceCommand.GET_DEVICE_LIST, mServiceStatus, mErrorCodes, null);
                }
            }, 100);
        }
    }
    public void controlDevice(String deviceId, String command) {
        if (!mInitialized) {
            InitializeRequiredException initException = new InitializeRequiredException("Init required Exception!!");

            try {
                throw initException;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

    }

    public String loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = mCtx.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }

    private void printScanDeviceInformation(IoTDevice device) {
        if (device.getAdRecordHashMap() == null) {
            Log.d(TAG, "old scanned device...");
            ArrayList<String> uuids = DataParser.getUuids(device.getAdRecordHashMap());
            if (uuids == null) {
                Log.e(TAG, "printScanDeviceInformation has no uuid");
                return;
            }
            String uuidStr = null;
            for (String uuid : uuids) {
                if (uuidStr != null) {
                    uuidStr += ", " + uuid;
                } else {
                    uuidStr = uuid;
                }
            }
            Log.d(TAG, "--OO svcuuids = " + uuidStr + ", name = " + device.getDeviceName() + ", id = " + device.getDeviceId());
        } else {
            AdRecord typeRecord = device.getAdRecordHashMap().get(AdRecord.TYPE_FLAGS);

            String str = "";
            int flags;
            if (typeRecord.getValue() != null && typeRecord.getValue().length > 0) {
                flags = typeRecord.getValue()[0] & 0x0FF;
                str = "";
                if ( (flags & 0x01) > 0 ) { str += "'LE Limited Discoverable Mode' "; }
                if ( (flags & (0x01 << 1)) > 0 ) { str += "'LE General Discoverable Mode' "; }
                if ( (flags & (0x01 << 2)) > 0 ) { str += "'BR/EDR Not Supported' "; }
                if ( (flags & (0x01 << 3)) > 0 ) { str += "'Simultaneous LE and BR/EDR to Same Device Capacble (Controller)' "; }
                if ( (flags & (0x01 << 4)) > 0 ) { str += "'Simultaneous LE and BR/EDR to Same Device Capacble (Host)' "; }
            }

            ArrayList<String> uuids = DataParser.getUuids(device.getAdRecordHashMap());
            if (uuids == null) {
                Log.e(TAG, "printScanDeviceInformation has no uuid");
                return;
            }
            String uuidStr = null;
            for (String uuid : uuids) {
                Log.d(TAG, "uuid = " + uuid);
                if (uuidStr != null) {
                    uuidStr += ", " + uuid;
                } else {
                    uuidStr = uuid;
                }
            }

            AdRecord svcDataRecord = device.getAdRecordHashMap().get(AdRecord.TYPE_SERVICEDATA);
            String svcHexData = null;
            if (svcDataRecord != null) {
                svcHexData = DataParser.getHexString(svcDataRecord.getValue());
            }
            Log.d(TAG, "--NN svcuuids = " + uuidStr + ", name = " + device.getDeviceName() + ", id = " + device.getDeviceId() + ", type = " + str + ", svcData = " + svcHexData);
        }
    }
}
