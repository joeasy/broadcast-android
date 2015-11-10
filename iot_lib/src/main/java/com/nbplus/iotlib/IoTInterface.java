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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nbplus.iotapp.btcharacteristics.GlucoseMeasurement;
import com.nbplus.iotapp.btcharacteristics.RecordAccessControlPoint;
import com.nbplus.iotapp.btcharacteristics.SmartSensor;
import com.nbplus.iotapp.btcharacteristics.WeightMeasurement;
import com.nbplus.iotapp.data.AdRecord;
import com.nbplus.iotapp.data.Constants;
import com.nbplus.iotapp.data.DataGenerator;
import com.nbplus.iotapp.data.DataParser;
import com.nbplus.iotapp.data.GattAttributes;
import com.nbplus.iotapp.perferences.IoTServicePreference;
import com.nbplus.iotapp.service.IoTService;
import com.nbplus.iotlib.api.BaseApiResult;
import com.nbplus.iotlib.api.IoTCollectedData;
import com.nbplus.iotlib.api.SendIoTDeviceDataTask;
import com.nbplus.iotlib.callback.IoTServiceResponse;
import com.nbplus.iotlib.data.IoTConstants;
import com.nbplus.iotlib.data.DeviceTypes;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.iotlib.data.IoTDeviceScenario;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTScenarioDef;
import com.nbplus.iotlib.data.IoTScenarioMap;
import com.nbplus.iotlib.data.IoTServiceCommand;
import com.nbplus.iotlib.data.IoTServiceStatus;
import com.nbplus.iotlib.data.IoTHandleData;
import com.nbplus.iotlib.exception.InitializeRequiredException;

import org.basdroid.common.NetworkUtils;
import org.basdroid.common.PackageUtils;
import org.basdroid.common.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    private static final int RETRIEVE_IOT_DEVICE_DATA_PERIOD = 30 * 60 * 1000;

    // haneler
    private static final int HANDLER_RETRIEVE_IOT_DEVICES = IoTServiceCommand.COMMAND_BASE_VALUE - 1;
    private static final int HANDLER_WAIT_FOR_NOTIFY_DATA = HANDLER_RETRIEVE_IOT_DEVICES - 1;
    public static final int HANDLER_SEND_IOT_DEVICE_DATA_TASK_COMPLETED = HANDLER_WAIT_FOR_NOTIFY_DATA - 1;
    private static final int HANDLER_COMMAND_NOT_RESPOND = HANDLER_SEND_IOT_DEVICE_DATA_TASK_COMPLETED - 1;

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

    private Gson mGson;

    /** Context of the activity from which this connector was launched */
    private Context mCtx;
    private boolean mInitialized = false;

    WeakReference<IoTServiceResponse> mForceRescanCallback = null;

    /**
     * known IoT device scenarios
     */
    IoTScenarioMap mIoTScenarioMap = null;

    /**
     * 10초동안 검색된 device list 를 저장해 두는 공간
     */
    private HashMap<String, IoTDevice> mScanedList = new HashMap<>();
    private int mBondedWithServerCount = 0;
    private int mCurrentRetrieveIndex = -1;
    private IoTDevice mCurrentRetrieveDevice;
    private String mDeviceId;
    private String mCollectServerAddress;
    private IoTCollectedData mCollectedData;
    private static final int MAX_CONNECTION_RETRY = 3;
    private int mConnectionRetryCount = 0;
    private int mCommandRetryCount = 0;

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

    public void setCollectServerAddress(String address) {
        this.mCollectServerAddress = address;
    }
    public IoTResultCodes initialize(Context context, String deviceId, String collectAddress) {
        mCtx = context;
        this.mCollectServerAddress = collectAddress;
        this.mDeviceId = deviceId;

        if (mInitialized) {
            Log.d(TAG, "Already initialized....");
            return mErrorCodes;
        }
        mInitialized = true;

        mGson = new Gson();
        // load scenarios

        try {
            String jsonString = IoTServicePreference.getIoTDeviceScenarioMap(mCtx);
            if (!StringUtils.isEmptyString(jsonString)) {
                Log.d(TAG, ">> scenario map json string = " + jsonString);
                IoTScenarioMap jsonMap = mGson.fromJson(jsonString, IoTScenarioMap.class);
                // default json map.
                IoTScenarioMap defaultJsonMap = null;
                String assetJson = loadJSONFromAsset("default_scenario.json");
                if (!StringUtils.isEmptyString(assetJson)) {
                    defaultJsonMap = mGson.fromJson(assetJson, IoTScenarioMap.class);
                }

                if (jsonMap != null && jsonMap.getVersion() <= 0) {
                    Log.d(TAG, "saved json scenario map is older version. use default json map..");
                    mIoTScenarioMap = defaultJsonMap;
                    IoTServicePreference.setIoTDeviceScenarioMap(mCtx, assetJson);
                } else {
                    if (jsonMap == null) {
                        Log.d(TAG, "saved json scenario map is null. use default json map..");
                        mIoTScenarioMap = defaultJsonMap;
                        IoTServicePreference.setIoTDeviceScenarioMap(mCtx, assetJson);
                    } else if (defaultJsonMap != null && (defaultJsonMap.getVersion() > jsonMap.getVersion())) {
                        Log.d(TAG, "saved json scenario map is older version. use default json map..");
                        mIoTScenarioMap = defaultJsonMap;
                        IoTServicePreference.setIoTDeviceScenarioMap(mCtx, assetJson);
                    } else {
                        Log.d(TAG, "saved json scenario map is latest version. use this map..");
                        mIoTScenarioMap = jsonMap;
                    }
                }
            } else {
                Log.d(TAG, "saved json scenario map is not found. use default json map..");
                // default json map.
                IoTScenarioMap defaultJsonMap = null;
                String assetJson = loadJSONFromAsset("default_scenario.json");
                if (!StringUtils.isEmptyString(assetJson)) {
                    defaultJsonMap = mGson.fromJson(assetJson, IoTScenarioMap.class);
                }

                mIoTScenarioMap = defaultJsonMap;
                IoTServicePreference.setIoTDeviceScenarioMap(mCtx, assetJson);
            }
        } catch (Exception e) {

        }
        if (mIoTScenarioMap.getScenarioMap() == null) {
            mIoTScenarioMap.setScenarioMap(new HashMap<String, IoTScenarioDef>());
        }

        try {
            // load saved devices list
            String savedJson = IoTServicePreference.getIoTDevicesList(mCtx);
            if (!StringUtils.isEmptyString(savedJson)) {
                Log.d(TAG, ">> scanned list json string = " + savedJson);
                mScanedList = mGson.fromJson(savedJson, new TypeToken<HashMap<String, IoTDevice>>() {
                }.getType());
                Iterator<String> iter = mScanedList.keySet().iterator();

                while (iter.hasNext()) {
                    String key = iter.next();
                    if (mScanedList.get(key).isBondedWithServer()) {
                        mBondedWithServerCount++;
                    }
                    mScanedList.get(key).setIsKnownDevice(isKnownScenarioDevice(mScanedList.get(key).getDeviceTypeId(),
                            mScanedList.get(key).getUuids(), mScanedList.get(key).getUuidLen()));
                }
            }
        } catch (Exception e) {

        }

        // load unsent collected data.
        try {
            String unsentJosn = IoTServicePreference.getUnSentCollectedData(mCtx);
            if (!StringUtils.isEmptyString(unsentJosn)) {
                Log.d(TAG, ">> load unsent iot data string = " + unsentJosn);
                mCollectedData = mGson.fromJson(unsentJosn, IoTCollectedData.class);
            } else {
                mCollectedData = new IoTCollectedData();
                mCollectedData.setDeviceId(mDeviceId);
            }
        } catch (Exception e) {

        }

        // connect to remote service
        if (IoTConstants.USE_ANOTHER_APP) {
            if (PackageUtils.isPackageExisted(mCtx, IoTConstants.NBPLUS_IOT_APP_PACKAGE_NAME)) {
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
                                        PackageUtils.showMarketDetail(mCtx, IoTConstants.NBPLUS_IOT_APP_PACKAGE_NAME);
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
            handleServiceMessage(msg);
        }
    }

    private void handleServiceMessage(Message msg) {
        switch (msg.what) {
            case IoTServiceCommand.COMMAND_RESPONSE : {
                Bundle b = msg.getData();
                if (b == null) {
                    Log.w(TAG, "bundle data is null");
                    return;
                }
                handleResponse(b);
                break;
            }

            // bt command 응답이 없는 경우가 있음.
            // 3번 재시도후 안되면 ...
            case HANDLER_COMMAND_NOT_RESPOND : {
                Log.d(TAG, "HANDLER_COMMAND_NOT_RESPOND retry or next device..");
                if (mCommandRetryCount < MAX_CONNECTION_RETRY) {
                    retryDeviceCommand();
                } else {
                    mCurrentRetrieveIndex++;
                    sendConnectToDeviceMessage(mCurrentRetrieveIndex);
                }
                break;
            }
            case IoTServiceCommand.SERVICE_STATUS_NOTIFICATION: {
                Log.d(TAG, "IoTServiceCommand.SERVICE_STATUS_NOTIFICATION");
                Bundle b = msg.getData();
                if (b == null) {
                    Log.w(TAG, "bundle data is null");
                    return;
                }
                mServiceStatus = (IoTServiceStatus)b.getSerializable(IoTServiceCommand.KEY_SERVICE_STATUS);
                if (mServiceStatus == IoTServiceStatus.RUNNING) {
                    /**
                     * IoT 디바이스 상태를 조회한다.
                     */
                    mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
                    // 서비스 시작 후 1분이 지난시점부터.
                    mHandler.sendEmptyMessageDelayed(HANDLER_RETRIEVE_IOT_DEVICES, 60 * 1000);
                    sendCollectedDataToServer();
                } else {
                    mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
                    mHandler.removeMessages(HANDLER_COMMAND_NOT_RESPOND);
                    mCurrentRetrieveIndex = -1;
                    mCurrentRetrieveDevice = null;

                    Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
                }

                Intent intent = new Intent(IoTConstants.ACTION_IOT_SERVICE_STATUS_CHANGED);
                intent.putExtra(IoTConstants.EXTRA_SERVICE_STATUS, (mServiceStatus == IoTServiceStatus.RUNNING));
                LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);

                mErrorCodes = (IoTResultCodes)b.getSerializable(IoTServiceCommand.KEY_SERVICE_STATUS_CODE);
                Log.d(TAG, "IoTServiceCommand.SERVICE_STATUS_NOTIFICATION : status = " + mServiceStatus + ", errCode = " + mErrorCodes);
                break;
            }
            case IoTServiceCommand.DEVICE_LIST_NOTIFICATION: {
                Bundle b = msg.getData();
                handleDeviceListNotification(b);
                break;
            }

            case IoTServiceCommand.DEVICE_CONNECTED: {
                Bundle b = msg.getData();
                handleDeviceConnectedNotification(b);

                break;
            }

            case IoTServiceCommand.DEVICE_DISCONNECTED: {
                mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                Bundle b = msg.getData();
                handleDeviceDisconnectedNotification(b);

                break;
            }

            case IoTServiceCommand.DEVICE_WRITE_DATA_RESULT:
            case IoTServiceCommand.DEVICE_SET_NOTIFICATION_RESULT: {
                Log.d(TAG, "IoTServiceCommand.DEVICE_SET_NOTIFICATION_RESULT received");

                Bundle b = msg.getData();
                IoTResultCodes resultCode = (IoTResultCodes) b.getSerializable(IoTServiceCommand.KEY_RESULT);
                if (IoTResultCodes.SUCCESS.equals(resultCode)) {
                    // success set notifications.
                    Log.d(TAG, "success set notification.. proceed next command");

                    IoTHandleData resultData = (IoTHandleData)b.getParcelable(IoTServiceCommand.KEY_DATA);
                    if (resultData != null) {
                        // xiaomi
                        if (resultData.getCharacteristicUuid().equals(GattAttributes.MISCALE_CHARACTERISTIC_2A2F)) {
                            handleXiaomiScale(msg.what, resultData);
                        } else if (resultData.getCharacteristicUuid().equals(GattAttributes.SMART_SENSOR)) {
                            handleSmartSensor(msg.what, resultData);
                        } else if (resultData.getServiceUuid().equals(GattAttributes.GLUCOSE_SERVICE_UUID)) {
                            handleGlucoseMeasurement(msg.what, resultData);
                        }
                        else {
                            proceedDeviceCommand();
                        }
                    }
                } else {
                    // fail set notifications.
                    Log.d(TAG, "fail set notification.. disconnect device");
                    mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                    mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                }
                break;
            }

            case IoTServiceCommand.DEVICE_READ_DATA_RESULT:
            case IoTServiceCommand.DEVICE_NOTIFICATION_DATA: {
                // read 인지 notification 인지 구분해야 한다.
                Log.d(TAG, "IoTServiceCommand.DEVICE_NOTIFICATION_DATA received");
                Bundle b = msg.getData();
                IoTHandleData resultData = (IoTHandleData)b.getParcelable(IoTServiceCommand.KEY_DATA);
                if (resultData != null) {
                    // xiaomi
                    if (resultData.getCharacteristicUuid().equals(GattAttributes.MISCALE_CHARACTERISTIC_2A2F)) {
                        handleXiaomiScale(msg.what, resultData);
                    } else if (resultData.getCharacteristicUuid().equals(GattAttributes.SMART_SENSOR)) {
                        handleSmartSensor(msg.what, resultData);
                    }else if (resultData.getServiceUuid().equals(GattAttributes.GLUCOSE_SERVICE_UUID)) {
                        handleGlucoseMeasurement(msg.what, resultData);
                    }
                    else {
                        proceedDeviceCommand();
                    }
                }
                break;
            }

            case HANDLER_RETRIEVE_IOT_DEVICES : {
                Log.d(TAG, "HANDLER_RETRIEVE_IOT_DEVICES received.. Clear all previous connection first..");
                if (mServiceStatus != IoTServiceStatus.RUNNING) {
                    Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
                } else {
                    sendMessageToService(IoTServiceCommand.SCANNING_STOP, null);
                    mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
                    sendMessageToService(IoTServiceCommand.DEVICE_DISCONNECT_ALL, null);
                }
                break;
            }

            case HANDLER_WAIT_FOR_NOTIFY_DATA : {
                mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                if (mCurrentRetrieveDevice == null) {
                    return;
                }
                Log.w(TAG, "I have no more scenario for this device = " + mCurrentRetrieveDevice.getDeviceName());
                Bundle extras = new Bundle();
                IoTHandleData data = new IoTHandleData();
                data.setDeviceId(mCurrentRetrieveDevice.getDeviceId());
                data.setDeviceTypeId(mCurrentRetrieveDevice.getDeviceTypeId());

                extras.putParcelable(IoTServiceCommand.KEY_DATA, data);
                sendMessageToService(IoTServiceCommand.DEVICE_DISCONNECT, extras);

                break;
            }

            case HANDLER_SEND_IOT_DEVICE_DATA_TASK_COMPLETED: {
                Log.d(TAG, "HANDLER_SEND_IOT_DEVICE_DATA_TASK_COMPLETED received... ");

                Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
                LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);

                BaseApiResult result = (BaseApiResult)msg.obj;
                if (BaseApiResult.RESULT_SUCCESS.equals(result.getResultCode())) {
                    Log.d(TAG, "Send collected data to server success..");
                } else {
                    Log.w(TAG, "Send collected data to server failed.. set unsent data");
                    Bundle extras = msg.getData();
                    if (extras != null) {
                        IoTCollectedData data = extras.getParcelable("data");
                        if (data == null) {
                            break;
                        }

                        mCollectedData.addAllIoTData(data.getIoTData());
                        IoTServicePreference.setUnSentCollectedData(mCtx, mGson.toJson(mCollectedData));
                    }
                }
                break;
            }

            default:
                break;
        }
    }

    private void handleResponse(Bundle b) {
        int cmd = b.getInt(IoTServiceCommand.KEY_CMD, -1);
        switch (cmd) {
            case IoTServiceCommand.DEVICE_DISCONNECT_ALL: {
                Log.d(TAG, "DEVICE_DISCONNECT_ALL received.. Start retrieve... ");
                mCurrentRetrieveIndex = 0;

                if (mServiceStatus == IoTServiceStatus.RUNNING) {
                    sendConnectToDeviceMessage(mCurrentRetrieveIndex);
                } else {
                    Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
                }
                break;
            }
            case IoTServiceCommand.REGISTER_SERVICE : {
                IoTResultCodes resultCode = (IoTResultCodes) b.getSerializable(IoTServiceCommand.KEY_RESULT);
                if (resultCode != null && resultCode.equals(IoTResultCodes.SUCCESS)) {
                    // success
                    Log.d(TAG, ">> IoT Register service success...");
                } else {
                    Log.w(TAG, ">> IoT Register service failed code = " + resultCode);
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
                    Log.w(TAG, ">> IoT Register service failed code = " + resultCode);
                }
                break;
            }

            case IoTServiceCommand.DEVICE_CONNECT: {
                IoTResultCodes resultCode = (IoTResultCodes) b.getSerializable(IoTServiceCommand.KEY_RESULT);
                if (resultCode != null && resultCode.equals(IoTResultCodes.SUCCESS)) {
                    // success
                    Log.d(TAG, ">> IoT DEVICE_CONNECT success...");
                } else {
                    Log.w(TAG, ">> IoT DEVICE_CONNECT failed code = " + resultCode);
                    if (IoTResultCodes.DEVICE_CONNECTION_NOT_RESPOND.equals(resultCode) &&
                            mConnectionRetryCount < MAX_CONNECTION_RETRY) {
                        if (mServiceStatus != IoTServiceStatus.RUNNING) {
                            mCurrentRetrieveIndex = -1;
                            mCurrentRetrieveDevice = null;

                            Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
                            LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
                            break;
                        }
                        Log.d(TAG, "mConnectionRetryCount = " + mConnectionRetryCount);
                        mConnectionRetryCount++;
                        Bundle extras = new Bundle();
                        IoTHandleData data = new IoTHandleData();
                        data.setDeviceId(mCurrentRetrieveDevice.getDeviceId());
                        data.setDeviceTypeId(mCurrentRetrieveDevice.getDeviceTypeId());

                        extras.putParcelable(IoTServiceCommand.KEY_DATA, data);
                        sendMessageToService(IoTServiceCommand.DEVICE_CONNECT, extras);
                        break;
                    }
                    if (mCurrentRetrieveIndex + 1 < mScanedList.size()) {
                        mCurrentRetrieveIndex++;
                        sendConnectToDeviceMessage(mCurrentRetrieveIndex);
                    } else {
                        Log.d(TAG, "Retrieving all devices.. completed");
                        mCurrentRetrieveIndex = -1;
                        mCurrentRetrieveDevice = null;

                        mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
                        mHandler.sendEmptyMessageDelayed(HANDLER_RETRIEVE_IOT_DEVICES, RETRIEVE_IOT_DEVICE_DATA_PERIOD);
                        sendCollectedDataToServer();
                    }
                }
                break;
            }

            case IoTServiceCommand.DEVICE_DISCONNECT: {
                IoTResultCodes resultCode = (IoTResultCodes) b.getSerializable(IoTServiceCommand.KEY_RESULT);
                if (resultCode != null && resultCode.equals(IoTResultCodes.SUCCESS)) {
                    // success
                    Log.d(TAG, ">> IoT DEVICE_DISCONNECT success...");
                } else {
                    Log.w(TAG, ">> IoT DEVICE_DISCONNECT failed code = " + resultCode);
                    if (mCurrentRetrieveIndex + 1 < mScanedList.size()) {
                        mCurrentRetrieveIndex++;
                        sendConnectToDeviceMessage(mCurrentRetrieveIndex);
                    } else {
                        mCurrentRetrieveIndex = -1;
                        mCurrentRetrieveDevice = null;

                        mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
                        mHandler.sendEmptyMessageDelayed(HANDLER_RETRIEVE_IOT_DEVICES, RETRIEVE_IOT_DEVICE_DATA_PERIOD);
                        sendCollectedDataToServer();
                    }
                }
                break;
            }

            case IoTServiceCommand.DEVICE_READ_DATA:
            case IoTServiceCommand.DEVICE_WRITE_DATA:
            case IoTServiceCommand.DEVICE_SET_NOTIFICATION: {
                IoTResultCodes resultCode = (IoTResultCodes) b.getSerializable(IoTServiceCommand.KEY_RESULT);
                if (resultCode != null && resultCode.equals(IoTResultCodes.SUCCESS)) {
                    // success
                    Log.d(TAG, ">> IoT READ_WRITE_SET_NOTIFICATION success...");
                } else {
                    Log.e(TAG, ">> IoT READ_WRITE_SET_NOTIFICATION failed code = " + resultCode);
                    mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                    mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
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

            mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
            mCurrentRetrieveIndex = -1;
            mCurrentRetrieveDevice = null;

            Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
            LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
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
        if (IoTConstants.USE_ANOTHER_APP) {
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
//        if (!BuildConfig.DEBUG) {           // skip release mode..
//            return;
//        }
        if (device.getAdRecordHashMap() == null) {
            Log.d(TAG, "old scanned device...");
            ArrayList<String> uuids = device.getUuids();
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

            ArrayList<String> uuids = device.getUuids();
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

            AdRecord svcDataRecord = device.getAdRecordHashMap().get(AdRecord.TYPE_SERVICEDATA);
            String svcHexData = null;
            if (svcDataRecord != null) {
                svcHexData = DataParser.getHexString(svcDataRecord.getValue());
            }
            Log.d(TAG, "--NN svcuuids = " + uuidStr + ", name = " + device.getDeviceName() + ", id = " + device.getDeviceId() + ", type = " + str + ", svcData = " + svcHexData);
        }
    }

    private void sendMessageToService(int what, Bundle b) {
        // Now that we have the service messenger, lets send our messenger
        Message msg = new Message();
        msg.what = what;

            /*
             * In case we would want to send extra data, we could use Bundles:
             * Bundle b = new Bundle(); b.putString("key", "hello world");
             * msg.setData(b);
             */
        if (b == null) {
            b = new Bundle();

            IoTHandleData data = new IoTHandleData();
            b.putParcelable(IoTServiceCommand.KEY_DATA, data);
        }
        b.putString(IoTServiceCommand.KEY_MSGID, IoTServiceCommand.generateMessageId(mCtx));
        msg.setData(b);

        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendConnectToDeviceMessage(int idx) {
        Log.d(TAG, "Retrieving next devices.. idx = " + idx);
        Iterator<String> iter = mScanedList.keySet().iterator();
        int i = 0;

        if (idx >= mScanedList.size()) {
            // 마지막 아이템이다.
            mCurrentRetrieveIndex = -1;
            mCurrentRetrieveDevice = null;

            mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
            mHandler.sendEmptyMessageDelayed(HANDLER_RETRIEVE_IOT_DEVICES, RETRIEVE_IOT_DEVICE_DATA_PERIOD);
            sendCollectedDataToServer();
        }

        boolean sendSuccess = false;
        while (iter.hasNext()) {
            if (i < idx) {
                i++;
                iter.next();
                continue;
            }

            i++;
            String key = iter.next();
            IoTDevice device = mScanedList.get(key);
            if (!device.isBondedWithServer()) {
                Log.d(TAG, key + " device is not bonded with server. skip this device.");
                mCurrentRetrieveIndex++;
                continue;
            }
            if (!device.isKnownDevice()) {
                Log.d(TAG, key + " device is unknown scenario. skip this device.");
                mCurrentRetrieveIndex++;
                continue;
            }

            mCurrentRetrieveDevice = device;
            Log.d(TAG, "Request connect device id = " + mCurrentRetrieveDevice.getDeviceId());

            Bundle b = new Bundle();
            IoTHandleData data = new IoTHandleData();
            data.setDeviceId(device.getDeviceId());
            data.setDeviceTypeId(device.getDeviceTypeId());

            b.putParcelable(IoTServiceCommand.KEY_DATA, data);
            sendMessageToService(IoTServiceCommand.DEVICE_CONNECT, b);
            mConnectionRetryCount = 0;
            sendSuccess = true;

            break;
        }

        if (!sendSuccess) {
            mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
            mHandler.sendEmptyMessageDelayed(HANDLER_RETRIEVE_IOT_DEVICES, RETRIEVE_IOT_DEVICE_DATA_PERIOD);
            sendCollectedDataToServer();
        }
    }

    private void handleDeviceListNotification(Bundle b) {
        if (b == null) {
            Log.w(TAG, "bundle data is null");
            return;
        }
        HashMap<String, IoTDevice> scannedDevices = (HashMap<String, IoTDevice>)b.getSerializable(IoTServiceCommand.KEY_DATA);
        if (scannedDevices == null || scannedDevices.size() == 0) {
            Log.w(TAG, "empty device list");
            //return;
        }

        if (scannedDevices == null) {
            scannedDevices = new HashMap<>();
        }
        boolean changed = false;
        Iterator<String> iter = scannedDevices.keySet().iterator();
        Log.d(TAG, "IoTServiceCommand.GET_DEVICE_LIST added size = " + scannedDevices.size());
        while(iter.hasNext()) {
            String key = iter.next();
            IoTDevice device = mScanedList.get(key);

            IoTDevice scannedDevice = scannedDevices.get(key);
            ArrayList<String> scannedUuids = DataParser.getUuids(scannedDevice.getAdRecordHashMap());
            Log.d(TAG, "check " + key + " is exist or new...");
            if (device != null) {           // already exist
                if (scannedUuids == null || scannedUuids.size() == 0) {
                    Log.e(TAG, ">>> xx device name " + device.getDeviceName() + " has no uuid advertisement");
                    continue;
                }
                ArrayList<String> deviceUuids = device.getUuids();
                if (deviceUuids == null || deviceUuids.size() == 0) {
                    if (device.getDeviceTypeId() == IoTDevice.DEVICE_TYPE_ID_BT) {
                        device.setAdRecordHashMap(scannedDevice.getAdRecordHashMap());
                        device.setUuids(scannedUuids);
                        device.setUuidLen(DataParser.getUuidLength(scannedDevice.getAdRecordHashMap()));
                    }
                    device.setIsKnownDevice(isKnownScenarioDevice(device.getDeviceTypeId(), device.getUuids(), device.getUuidLen()));
                    mScanedList.put(key, device);
                    changed = true;
                } else {
                    if (!scannedUuids.equals(deviceUuids)) {
                        if (device.getDeviceTypeId() == IoTDevice.DEVICE_TYPE_ID_BT) {
                            device.setAdRecordHashMap(scannedDevice.getAdRecordHashMap());
                            device.setUuids(scannedUuids);
                            device.setUuidLen(DataParser.getUuidLength(scannedDevice.getAdRecordHashMap()));
                        }
                        device.setIsKnownDevice(isKnownScenarioDevice(device.getDeviceTypeId(), device.getUuids(), device.getUuidLen()));
                        mScanedList.put(key, device);
                        changed = true;
                    }
                }

                // 등록된 긴급호출 디바이스인 경우.
                if (device.isBondedWithServer() && isEmergencyCallDevice(scannedDevice.getDeviceTypeId(), scannedUuids,
                        DataParser.getUuidLength(scannedDevice.getAdRecordHashMap()))) {
                    Log.d(TAG, "Emergency call device broadcast received : " + scannedDevice.getDeviceId());
                    final Intent intent = new Intent(IoTConstants.ACTION_RECEIVE_EMERGENCY_CALL_DEVICE_BROADCAST);
                    LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
                }

                continue;
            }

            ArrayList<String> uuids = DataParser.getUuids(scannedDevice.getAdRecordHashMap());
            if (uuids == null || uuids.size() == 0) {
                Log.e(TAG, ">>> device name " + scannedDevice.getDeviceName() + " has no uuid advertisement");
                continue;
            }
            changed = true;
            scannedDevice.setUuids(uuids);
            scannedDevice.setUuidLen(DataParser.getUuidLength(scannedDevice.getAdRecordHashMap()));
            scannedDevice.setIsKnownDevice(isKnownScenarioDevice(scannedDevice.getDeviceTypeId(), scannedDevice.getUuids(), scannedDevice.getUuidLen()));
            mScanedList.put(scannedDevice.getDeviceId(), scannedDevice);
        }

        if (changed) {
            // save to preference.
            String json = mGson.toJson(mScanedList);
            if (json != null) {
                IoTServicePreference.setIoTDevicesList(mCtx, json);
            }
        }
        Log.d(TAG, "Current device size = " + mScanedList.size());
        // TODO : log
//        if (BuildConfig.DEBUG) {
            iter = mScanedList.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                IoTDevice device = mScanedList.get(key);

                printScanDeviceInformation(device);
            }
//        }

        if (mForceRescanCallback == null) {
            return;
        }

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

    private void handleDeviceConnectedNotification(Bundle b) {
        if (b == null) {
            Log.w(TAG, "bundle data is null");
            return;
        }

        String deviceUuid = b.getString(IoTServiceCommand.KEY_DEVICE_UUID);
        if (StringUtils.isEmptyString(deviceUuid)) {
            Log.w(TAG, "deviceUuid is null");
            return;
        }

        if (mCurrentRetrieveDevice == null || !deviceUuid.equals(mCurrentRetrieveDevice.getDeviceId())) {
            Log.w(TAG, "current retrieve device is empty or not matched..");
            Log.w(TAG, "mCurrentRetrieveDevice.getDeviceId() = " + mCurrentRetrieveDevice.getDeviceId());
            Log.w(TAG, "connected device getDeviceId() = " + deviceUuid);

            Bundle extras = new Bundle();
            IoTHandleData data = new IoTHandleData();
            data.setDeviceId(deviceUuid);
            data.setDeviceTypeId(mCurrentRetrieveDevice.getDeviceTypeId());

            extras.putParcelable(IoTServiceCommand.KEY_DATA, data);
            sendMessageToService(IoTServiceCommand.DEVICE_DISCONNECT, extras);
            return;
        }

        mConnectionRetryCount = 0;
        HashMap<String, ArrayList<String>> disCoveredServices =
                (HashMap<String, ArrayList<String>>)b.getSerializable(IoTServiceCommand.KEY_DATA);
        if (disCoveredServices == null || disCoveredServices.size() == 0) {
            Log.w(TAG, "empty disCoveredServices list");
            Bundle extras = new Bundle();
            IoTHandleData data = new IoTHandleData();
            data.setDeviceId(deviceUuid);
            data.setDeviceTypeId(mCurrentRetrieveDevice.getDeviceTypeId());

            extras.putParcelable(IoTServiceCommand.KEY_DATA, data);
            sendMessageToService(IoTServiceCommand.DEVICE_DISCONNECT, extras);
            return;
        }

        // check.. has known scenario.
        mCurrentRetrieveDevice.setDiscoveredServices(disCoveredServices);
        ArrayList<IoTDeviceScenario> deviceScenario
                = getKnownIoTDeviceScenarios(mCurrentRetrieveDevice);
        if (deviceScenario == null) {
            Log.w(TAG, "I have no scenario for this device = " + mCurrentRetrieveDevice.getDeviceName());
            Bundle extras = new Bundle();
            IoTHandleData data = new IoTHandleData();
            data.setDeviceId(deviceUuid);

            extras.putParcelable(IoTServiceCommand.KEY_DATA, data);
            sendMessageToService(IoTServiceCommand.DEVICE_DISCONNECT, extras);
            return;
        }

        mCurrentRetrieveDevice.setDeviceScenario(deviceScenario);
        mCurrentRetrieveDevice.setScenarioPosition(0);

        proceedDeviceCommand();
    }

    private boolean isKnownScenarioDevice(int deviceType, ArrayList<String> uuids, int uuidLen) {
        if (uuids == null) {
            return false;
        }
        boolean isKnown = false;
        for (int i = 0; i < uuids.size(); i++) {
            String uuid = uuids.get(i);
            if (uuid == null) {
                continue;
            }

            if (deviceType == IoTDevice.DEVICE_TYPE_ID_BT) {
                switch (uuidLen) {
                    case IoTDevice.DEVICE_BT_UUID_LEN_16 :
                        uuid = String.format(GattAttributes.GATT_BASE_UUID128_FROM_UUID16_FORMATTING, uuids.get(i));
                        break;
                    case IoTDevice.DEVICE_BT_UUID_LEN_32 :
                        uuid = String.format(GattAttributes.GATT_BASE_UUID128_FROM_UUID32_FORMATTING, uuids.get(i));
                        break;
                    case IoTDevice.DEVICE_BT_UUID_LEN_128 :
                        // do nothing
                        break;
                }
            }
            if (mIoTScenarioMap.getScenarioMap().get(uuid) != null) {
                Log.d(TAG, "isKnownScenarioDevice() uuid = " + uuid);
                isKnown = true;
                break;
            }
        }

        return isKnown;
    }

    /*
     * smart band
     */
    private boolean isEmergencyCallDevice(int deviceType, ArrayList<String> uuids, int uuidLen) {
        if (uuids == null) {
            return false;
        }
        boolean isEmergencyCallDevice = false;
        for (int i = 0; i < uuids.size(); i++) {
            String uuid = uuids.get(i);
            if (uuid == null) {
                continue;
            }

            if (deviceType == IoTDevice.DEVICE_TYPE_ID_BT) {
                switch (uuidLen) {
                    case IoTDevice.DEVICE_BT_UUID_LEN_16 :
                        uuid = String.format(GattAttributes.GATT_BASE_UUID128_FROM_UUID16_FORMATTING, uuids.get(i));
                        break;
                    case IoTDevice.DEVICE_BT_UUID_LEN_32 :
                        uuid = String.format(GattAttributes.GATT_BASE_UUID128_FROM_UUID32_FORMATTING, uuids.get(i));
                        break;
                    case IoTDevice.DEVICE_BT_UUID_LEN_128 :
                        // do nothing
                        break;
                }
            }
            if (mIoTScenarioMap.getEmergencyCallDeviceList().contains(uuid)) {
                Log.d(TAG, "isEmergencyCallDevice() uuid = " + uuid);
                isEmergencyCallDevice = true;
                break;
            }
        }

        return isEmergencyCallDevice;
    }

    private ArrayList<IoTDeviceScenario> getKnownIoTDeviceScenarios(IoTDevice device) {
        if (device == null) {
            return null;
        }

        ArrayList<String> uuids = device.getUuids();
        IoTScenarioDef ioTScenarioDef = null;
        String uuid = null;

        for (int i = 0; i < uuids.size(); i++) {
            if (device.getDeviceTypeId() == IoTDevice.DEVICE_TYPE_ID_BT) {
                int uuidLen = device.getUuidLen();
                switch (uuidLen) {
                    case IoTDevice.DEVICE_BT_UUID_LEN_16 :
                        uuid = String.format(GattAttributes.GATT_BASE_UUID128_FROM_UUID16_FORMATTING, uuids.get(i));
                        break;
                    case IoTDevice.DEVICE_BT_UUID_LEN_32 :
                        uuid = String.format(GattAttributes.GATT_BASE_UUID128_FROM_UUID32_FORMATTING, uuids.get(i));
                        break;
                    case IoTDevice.DEVICE_BT_UUID_LEN_128 :
                        uuid = uuids.get(i);
                        break;
                }
            } else {
                uuid = uuids.get(i);
            }

            ioTScenarioDef = mIoTScenarioMap.getScenarioMap().get(uuid);
            if (ioTScenarioDef != null) {
                break;
            }
        }

        if (ioTScenarioDef == null) {
            Log.w(TAG, "getKnownIoTDeviceScenarios() ioTScenarioDef is null");
            return null;
        }

        // check scenario filter
        ArrayList<String> serviceFilter = ioTScenarioDef.getScenarioFilter();
        if (serviceFilter != null && serviceFilter.size() > 0) {
            Iterator<String> iter = device.getDiscoveredServices().keySet().iterator();
            String foundUuid = null;

            while(iter.hasNext()) {
                foundUuid  = iter.next();
                if (serviceFilter.contains(foundUuid)) {
                    break;
                }
            }

            if (foundUuid == null) {
                Log.w(TAG, "getKnownIoTDeviceScenarios() foundUuid is null");
                return null;
            }
            return ioTScenarioDef.getScenarios(foundUuid);
        } else {
            return ioTScenarioDef.getScenarios(uuid);
        }
    }

    private void handleDeviceDisconnectedNotification(Bundle b) {
        if (mCurrentRetrieveIndex == -1 && mCurrentRetrieveDevice == null) {
            return;
        }

        String deviceUuid = b.getString(IoTServiceCommand.KEY_DEVICE_UUID);
        if (StringUtils.isEmptyString(deviceUuid)) {
            Log.w(TAG, "deviceUuid is null");
            return;
        }

        if (!deviceUuid.equals(mCurrentRetrieveDevice.getDeviceId())) {
            Log.d(TAG, "This device is not mCurrentRetrieveDevice... ignore.. ");
            return;
        }
        mConnectionRetryCount = 0;
        if (mCurrentRetrieveIndex + 1 < mScanedList.size()) {
            mCurrentRetrieveIndex++;
            sendConnectToDeviceMessage(mCurrentRetrieveIndex);
        } else {
            Log.d(TAG, "Retrieving all devices.. completed");
            mCurrentRetrieveIndex = -1;
            mCurrentRetrieveDevice = null;

            mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
            mHandler.sendEmptyMessageDelayed(HANDLER_RETRIEVE_IOT_DEVICES, RETRIEVE_IOT_DEVICE_DATA_PERIOD);
            sendCollectedDataToServer();
        }
    }

    /*
    private IoTDeviceScenario findDeviceScenario(IoTDevice device) {
        if (mIoTScenarioMap == null || mIoTScenarioMap.size() <= 0) {
            return null;
        }

        if (device == null || device.getUuids() == null || device.getUuids().size() == 0) {
            return null;
        }

        IoTScenarioDef def = null;
        IoTDeviceScenario scenario = null;

        if (device.getDeviceTypeId() == IoTDevice.DEVICE_TYPE_ID_BT) {
            ArrayList<String> uuids = device.getUuids();
            int uuidLen = device.getUuidLen();

            for (String uuid : uuids) {
                String fullUuid = "";
                if (uuidLen == IoTDevice.DEVICE_BT_UUID_LEN_16) {
                    fullUuid = String.format(GattAttributes.GATT_BASE_UUID128_FROM_UUID16_FORMATTING, uuid);

                    def = mIoTScenarioMap.get(fullUuid);
                    if (def == null) {
                        continue;
                    } else {
                        def.getScenarioFilter();
                    }
                }
            }
        }

    }
    */

    public void updateBondedWithServerDeviceList(ArrayList<IoTDevice> bondedList) {
        ArrayList<String> bondedDeviceIds = new ArrayList<>();
        for (int i = 0; i < bondedList.size(); i++) {
            bondedDeviceIds.add(bondedList.get(i).getDeviceId());
        }

        mBondedWithServerCount = 0;
        Iterator<String> iter = mScanedList.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();

            if (bondedDeviceIds.contains(key)) {
                mBondedWithServerCount++;
                mScanedList.get(key).setIsBondedWithServer(true);
            } else {
                mScanedList.get(key).setIsBondedWithServer(false);
            }
        }
        Gson gson = new Gson();
        String json = gson.toJson(mScanedList);
        if (json != null) {
            Log.d(TAG, "updateBondedWithServerDeviceList json = " + json);
            IoTServicePreference.setIoTDevicesList(mCtx, json);
        }

        /**
         * 전체 리스트가 갱신되는 시점에 전체데이터 조회를 한번 한다.
         */
        mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
        mHandler.sendEmptyMessageDelayed(HANDLER_RETRIEVE_IOT_DEVICES, 10 * 1000);
        sendCollectedDataToServer();
    }

    private boolean proceedDeviceCommand() {
        Log.d(TAG, ">> proceedDeviceCommand()");
        // try first scenario command
        IoTDeviceScenario scenario = mCurrentRetrieveDevice.getNextScenario();
        if (scenario == null) {
            mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
            mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
            return true;
        }

        Bundle extras = new Bundle();
        IoTHandleData data = new IoTHandleData();
        data.setDeviceId(mCurrentRetrieveDevice.getDeviceId());
        data.setDeviceTypeId(mCurrentRetrieveDevice.getDeviceTypeId());
        data.setCharacteristicUuid(scenario.getCharacteristic());
        data.setServiceUuid(scenario.getService());

        int cmd = -1;
        switch (scenario.getCmd()) {
            case IoTDeviceScenario.CMD_READ :
                cmd = IoTServiceCommand.DEVICE_READ_DATA;
                break;
            case IoTDeviceScenario.CMD_WRITE :
                cmd = IoTServiceCommand.DEVICE_WRITE_DATA;
                if (StringUtils.isEmptyString(scenario.getDataType())) {
                    // data type 이 없으면 data 값이 있는지 확인한다.
                    if (StringUtils.isEmptyString(scenario.getData())) {
                        // data 값이없으면 진행을 중지한다.
                        Log.w(TAG, "Can't find data type or data");
                        mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                        mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                        return false;
                    }

                    byte[] setData = DataParser.getHextoBytes(scenario.getData());
                    data.setValue(setData);
                } else {
                    if (scenario.getDataType().equals("current_time")) {
                        byte[] dataByte = DataGenerator.getCurrentTimeData();
                        data.setValue(dataByte);
                    } else if (scenario.getDataType().startsWith("miscale_")) {
                        String dataString = scenario.getData();
                        Log.d(TAG, "miscale write data hex string = " + dataString);
                        byte[] dataByte = DataParser.getHextoBytes(dataString);
                        data.setValue(dataByte);
                    } else if (scenario.getDataType().equals("glucose_report_number")) {
                        String dataString = scenario.getData();

                        byte[] reportNumber = new byte[] { (byte)0x04, (byte)0x01 };
                        // op, operator, filter type, next seq(2bytes)...
                        byte[] reportRecords = new byte[] { (byte)0x01, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x01 };

//                        if (mCurrentRetrieveDevice.getLastSequenceNumber() == 0) {
                            Log.d(TAG, "glucose_report_number write data hex string = " + DataParser.getHexString(reportNumber));
                            data.setValue(reportNumber);
//                        } else {
//                            try {
//                                int lastRecordNum = mCurrentRetrieveDevice.getLastSequenceNumber();
//                                //lastRecordNum++;
//                                reportRecords[2] = (byte)((lastRecordNum & 0xff));
//                                reportRecords[3] = (byte)((lastRecordNum & 0xff00) >> 8);
//
//                                Log.d(TAG, "glucose_get_records last seq = " + mCurrentRetrieveDevice.getLastSequenceNumber() + ", hex string = " + DataParser.getHexString(reportRecords));
//                                data.setValue(reportRecords);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                                mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
//                                mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
//                                return false;
//                            }
//                        }
                    }
                    else {
                        Log.w(TAG, "Unknown data type..");
                        mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                        mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                        return false;
                    }
                }
                break;
            case IoTDeviceScenario.CMD_NOTIFY :
                cmd = IoTServiceCommand.DEVICE_SET_NOTIFICATION;
                break;
            default: {
                Log.w(TAG, "Unknown device command... skip this device");
                mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                return false;
            }
        }

        if (cmd == -1) {
            Log.w(TAG, "Unknown device command... skip this device");
            mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
            mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
            return false;
        }
        extras.putParcelable(IoTServiceCommand.KEY_DATA, data);
        sendMessageToService(cmd, extras);
        return true;
    }

    private boolean retryDeviceCommand() {
        Log.d(TAG, ">> proceedDeviceCommand()");
        // try first scenario command
        IoTDeviceScenario scenario = mCurrentRetrieveDevice.getCurrentScenario();
        if (scenario == null) {
            mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
            mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
            return true;
        }

        Bundle extras = new Bundle();
        IoTHandleData data = new IoTHandleData();
        data.setDeviceId(mCurrentRetrieveDevice.getDeviceId());
        data.setDeviceTypeId(mCurrentRetrieveDevice.getDeviceTypeId());
        data.setCharacteristicUuid(scenario.getCharacteristic());
        data.setServiceUuid(scenario.getService());

        int cmd = -1;
        switch (scenario.getCmd()) {
            case IoTDeviceScenario.CMD_READ :
                cmd = IoTServiceCommand.DEVICE_READ_DATA;
                break;
            case IoTDeviceScenario.CMD_WRITE :
                cmd = IoTServiceCommand.DEVICE_WRITE_DATA;
                if (StringUtils.isEmptyString(scenario.getDataType())) {
                    // data type 이 없으면 data 값이 있는지 확인한다.
                    if (StringUtils.isEmptyString(scenario.getData())) {
                        // data 값이없으면 진행을 중지한다.
                        Log.w(TAG, "Can't find data type or data");
                        mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                        mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                        return false;
                    }

                    byte[] setData = DataParser.getHextoBytes(scenario.getData());
                    data.setValue(setData);
                } else {
                    if (scenario.getDataType().equals("current_time")) {
                        byte[] dataByte = DataGenerator.getCurrentTimeData();
                        data.setValue(dataByte);
                    } else if (scenario.getDataType().startsWith("miscale_")) {
                        String dataString = scenario.getData();
                        Log.d(TAG, "miscale write data hex string = " + dataString);
                        byte[] dataByte = DataParser.getHextoBytes(dataString);
                        data.setValue(dataByte);
                    } else if (scenario.getDataType().equals("glucose_report_number")) {
                        String dataString = scenario.getData();

                        byte[] reportNumber = new byte[] { (byte)0x04, (byte)0x01 };
                        // op, operator, filter type, next seq(2bytes)...
                        byte[] reportRecords = new byte[] { (byte)0x01, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x01 };

                        Log.d(TAG, "glucose_report_number write data hex string = " + DataParser.getHexString(reportNumber));
                        data.setValue(reportNumber);
                    }
                    else {
                        Log.w(TAG, "Unknown data type..");
                        mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                        mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                        return false;
                    }
                }
                break;
            case IoTDeviceScenario.CMD_NOTIFY :
                cmd = IoTServiceCommand.DEVICE_SET_NOTIFICATION;
                break;
            default: {
                Log.w(TAG, "Unknown device command... skip this device");
                mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                return false;
            }
        }

        if (cmd == -1) {
            Log.w(TAG, "Unknown device command... skip this device");
            mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
            mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
            return false;
        }
        extras.putParcelable(IoTServiceCommand.KEY_DATA, data);
        sendMessageToService(cmd, extras);
        return true;
    }
    private void handleXiaomiScale(int msgWhat, IoTHandleData data) {
        if (data == null) {
            return;
        }
        // 샤오미는 아래와 같은 시나리오로 진행된다.
        // 0. set notify 2a2f
        // 1. write 2a2f - for notify saved record number
        // 2. write result
        // 3. notify 2a2f - saved recoreds number count (value의 1byte 값이 0x01)
        // 4. (if (number > 0)) write 2a2f - for notify saved records
        // 5.                   write result
        // 6.                   notify saved record (value의 1byte 값이 0x62)
        // 7.                   notify end of record (value의 1byte 값이 0x03)
        // 8.                   write 2a2f - delete saved records
        byte[] values = data.getValue();
        if (values == null) {
            Log.w(TAG, "handleXiaomiScale() value is not found..");
        }
        switch (msgWhat) {
            case IoTServiceCommand.DEVICE_SET_NOTIFICATION_RESULT : {
                proceedDeviceCommand();
                break;
            }
            case IoTServiceCommand.DEVICE_WRITE_DATA_RESULT: {
                switch (values[0] & 0xff) {
                    case 0x01:          // get number success.. data will be notify
                        Log.d(TAG, "xiaomi sclae : get number success.. data will be notified..");
                        break;
                    case 0x02:
                        Log.d(TAG, "xiaomi sclae : get data success.. data will be notified..");
                        break;
                    case 0x03:
                        Log.d(TAG, "xiaomi sclae : stop saved notification data .. success");
                        proceedDeviceCommand();     // delete saved record.
                        break;
                    case 0x04:
                        Log.d(TAG, "xiaomi sclae : remove all saved data success... ");
                        Log.d(TAG, "xiaomi scale : disconnect.... ");
                        mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                        mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                        break;
                }
                break;
            }
            case IoTServiceCommand.DEVICE_NOTIFICATION_DATA: {
                switch (values[0] & 0xFF) {
                    case 0x01 :     // get saved number
                        int savedNumber = DataParser.getUint16(Arrays.copyOfRange(values, 1, 3));
                        Log.d(TAG, ">> xiaomi sclae : Saved Data Num is " + savedNumber);
                        if (savedNumber > 0) {
                            proceedDeviceCommand();     // next command - notify record.
                        } else {
                            // disconnect ...
                            Log.d(TAG, "xiaomi scale : disconnect.... ");
                            mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                            mHandler.sendEmptyMessageDelayed(HANDLER_WAIT_FOR_NOTIFY_DATA, 100);
                        }
                        break;
//                        case 0x02 :     // do not reached
//                            break;
                    case 0x03 :     // end of saved data ??
                        Log.d(TAG, ">> xiaomi sclae : End of saved data flag received...");
                        proceedDeviceCommand();         // stop notifications.
                        break;
                    case 0x62 :     // saved data
                        // send to server
                        String address = data.getDeviceId();
                        String characUuid = data.getCharacteristicUuid();
                        ArrayList<WeightMeasurement> measurements = WeightMeasurement.parseWeightMeasurement(address, characUuid, values);

                        for (WeightMeasurement measurement : measurements) {
                            if (measurement != null) {
                                IoTCollectedData.IoTData iotData = new IoTCollectedData.IoTData();
                                iotData.setIotDeviceId(address);
                                SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
                                Calendar calendar = measurement.getTimestamp();
                                iotData.setDate(df.format(calendar.getTime()));
                                iotData.setValue(Double.toString(measurement.getWeight()));

                                Log.d(TAG, "xiaomi sclae : Weight = " + iotData.getValue() + "" + measurement.getMeasurementUnitString() + ", timestamp = " + iotData.getDate());
                                mCollectedData.addIoTData(iotData);
                            }
                        }
                        break;
                }
                break;
            }
        }

    }

    private void handleSmartSensor(int msgWhat, IoTHandleData data) {
        if (data == null) {
            return;
        }
        // 스마트센서는 아래와 같은 시나리오로 진행된다.
        // 0. set notify 11e1
        byte[] values = data.getValue();
        if (values == null) {
            Log.w(TAG, "handleSmartSensor() value is not found..");
        }
        switch (msgWhat) {
            case IoTServiceCommand.DEVICE_SET_NOTIFICATION_RESULT : {
                Log.d(TAG, "handleSmartSensor() : IoTServiceCommand.DEVICE_SET_NOTIFICATION_RESULT received.");
                break;
            }
            case IoTServiceCommand.DEVICE_NOTIFICATION_DATA: {
                // 한시간에 한번만 받는다. 데이터를 받으면 연결종료하고 다음 시간에 받는다.
                Log.d(TAG, "handleSmartSensor() : IoTServiceCommand.DEVICE_NOTIFICATION_DATA received.");
                String address = data.getDeviceId();
                String characUuid = data.getCharacteristicUuid();
                ArrayList<SmartSensor> measurements = SmartSensor.parseSmartSensorMeasurement(address, characUuid, values);

                for (SmartSensor measurement : measurements) {
                    if (measurement != null) {
                        IoTCollectedData.IoTData iotData = new IoTCollectedData.IoTData();
                        iotData.setIotDeviceId(address);
                        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
                        Calendar calendar = Calendar.getInstance();
                        iotData.setDate(df.format(calendar.getTime()));

                        String value = String.format("%.2f,%.2f", measurement.getTemperature(), measurement.getHumidity());
                        iotData.setValue(value);

                        Log.d(TAG, "SmartSensor : Temp and Humidity = " + iotData.getValue() + ", timestamp = " + iotData.getDate());
                        mCollectedData.addIoTData(iotData);

                        mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                        mHandler.sendEmptyMessage(HANDLER_WAIT_FOR_NOTIFY_DATA);
                    }
                }
                break;
            }
        }

    }

    /**
     *
     9 Appendix A – Example of Record Access Control Point Usage

     Below is an informative example showing the use of the RACP in the context of the Glucose Profile:
     1. At 04 October 2011 12:40:00 pm (user-facing time internal to the Server i.e., Base Time + Time Offset), the Client requests records for the first time and requests the number of all records stored in the device.
     a. The Client writes Op Code 0x04 to request number of records with an Operator of 0x01 meaning “all records” and no Operand.
     b. The Server indicates back Op Code 0x05, an Operator of 0x00 (meaning “Null”) and Operand containing the number of all records (0x00F7 in this example)
     2. Immediately after that, the Client requests a report of stored records.
     a. The Client writes Op Code 0x01 to request all records with an Operator of 0x01 meaning
     “all records” and no Operand.
     b. The Server notifies all records (Series of Glucose Measurement characteristics followed
     sometimes by Glucose Measurement Context characteristics) where the total number of
     Glucose Measurement characteristics totals 0x00F7.
     c. The Server indicates Op Code 0x06 with an Operator of 0x00 (meaning “Null”) and
     Operand of 0x01, 0x01 meaning “successful response to Op Code 0x01”.
     d. The Client stores the Sequence Number of the last received record for future use
     (0x00F7 since this was the first use and with the assumption in this example that the
     sequence number of the first record is 0x0001).
     e. Since this is a critical application, the Client performs some post-processing checks to
     make sure no major inconsistencies to the Base Time or Time Offset occurred. The
     Client also checks to see if any numbers in the sequence are missing.
     3. Several days later, the Client requests a report of records since the last update.
     a. The Client writes Op Code 0x01 to request records with an Operator of 0x03 meaning “greater than or equal to” and an Operand set to Filter Type 0x01, 0x00F8) that is one number in the sequence more than the Sequence Number from the last record it received.
     b. The Server notifies all records that have accrued since the last request.
     c. The Server indicates Op Code 0x06 with an Operator of 0x00 (meaning “Null”) and
     Operand of 0x05, 0x01 meaning “successful response to Op Code 0x05”.
     */
    private void handleGlucoseMeasurement(int msgWhat, IoTHandleData data) {
        if (data == null) {
            return;
        }
        // 스마트센서는 아래와 같은 시나리오로 진행된다.
        // 0. set notify 11e1
        byte[] values = data.getValue();
        if (values == null) {
            Log.w(TAG, "handleGlucoseMeasurement() value is not found..");
        }
        switch (msgWhat) {
            case IoTServiceCommand.DEVICE_SET_NOTIFICATION_RESULT : {
                Log.d(TAG, "handleGlucoseMeasurement() : IoTServiceCommand.DEVICE_SET_NOTIFICATION_RESULT received.");
                proceedDeviceCommand();
                break;
            }
            case IoTServiceCommand.DEVICE_WRITE_DATA_RESULT: {
                Log.d(TAG, "handleGlucoseMeasurement() : IoTServiceCommand.DEVICE_WRITE_DATA_RESULT");
                Log.d(TAG, "values = " + DataParser.getHexString(values));
                mHandler.sendEmptyMessageDelayed(HANDLER_COMMAND_NOT_RESPOND, 5 * 1000);
                break;
            }
            case IoTServiceCommand.DEVICE_NOTIFICATION_DATA: {
                mHandler.removeMessages(HANDLER_COMMAND_NOT_RESPOND);
                Log.d(TAG, "handleGlucoseMeasurement() : IoTServiceCommand.DEVICE_NOTIFICATION_DATA received.");
                String address = data.getDeviceId();
                String characUuid = data.getCharacteristicUuid();
                String serviceUuid = data.getServiceUuid();

                if (GattAttributes.RECORD_ACCESS_CONTROL_POINT.equals(characUuid)) {
                    RecordAccessControlPoint recordAccessControlPoint = RecordAccessControlPoint.parseRecordAccessControlPoint(values);
                    if (recordAccessControlPoint == null) {
                        mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                        mHandler.sendEmptyMessage(HANDLER_WAIT_FOR_NOTIFY_DATA);
                        break;
                    }

                    switch (recordAccessControlPoint.getOpCode()) {
                        case Constants.RACP_RES_OP_CODE_NUMBER_OF_STORED_RECORDS :
                            int numOfRecords = DataParser.getUint16(recordAccessControlPoint.getOperand());
                            Log.d(TAG, ">> RACP_RES_OP_CODE_NUMBER_OF_STORED_RECORDS received : num = " + numOfRecords);
                            if (numOfRecords > 0) {
                                proceedDeviceCommand();
                            } else {
                                mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                                mHandler.sendEmptyMessage(HANDLER_WAIT_FOR_NOTIFY_DATA);
                                break;
                            }
                            break;
                        case Constants.RACP_RES_OP_CODE_RESPONSE_CODE :
                            byte[] operand = recordAccessControlPoint.getOperand();
                            Log.d(TAG, "Constants.RACP_RES_OP_CODE_RESPONSE_CODE = " + DataParser.getHexString(recordAccessControlPoint.getOperand()));
                            int cmd = operand[0] & 0xff;
                            if (cmd >= Constants.RACP_REQ_OP_CODE_REPORT_STORED_RECORDS && cmd <= Constants.RACP_REQ_OP_CODE_REPORT_NUMBER_OF_STORED_RECORDS) {
                                int respCode = operand[1] & 0xff;

                                if (cmd == Constants.RACP_REQ_OP_CODE_REPORT_STORED_RECORDS) {
                                    if (respCode != Constants.RACP_OPERAND_RES_VALUE_SUCCESS) {
                                        Log.w(TAG, "RACP req operation failed... ");

                                        mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                                        mHandler.sendEmptyMessage(HANDLER_WAIT_FOR_NOTIFY_DATA);
                                        break;
                                    } else {
                                        // end of RACP_REQ_OP_CODE_REPORT_STORED_RECORDS
                                        Log.d(TAG, "Send RACP_REQ_OP_CODE_REPORT_STORED_RECORDS");
                                        // TODO : 지금은 지운다. Gatt 규격으로는 안지워도 되는데... 잘안됨.
                                        proceedDeviceCommand();
                                        break;
                                    }
                                } else if (cmd == Constants.RACP_REQ_OP_CODE_DELETE_STORED_RECORDS) {
                                    mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                                    mHandler.sendEmptyMessage(HANDLER_WAIT_FOR_NOTIFY_DATA);
                                    break;
                                }
                            } else {
                                mHandler.removeMessages(HANDLER_WAIT_FOR_NOTIFY_DATA);
                                mHandler.sendEmptyMessage(HANDLER_WAIT_FOR_NOTIFY_DATA);
                                break;
                            }
                            break;
                    }
                } else if (GattAttributes.GLUCOSE_MEASUREMENT.equals(characUuid)) {
                    Log.d(TAG, "Gluecose measurement received....");
                    GlucoseMeasurement measurement = GlucoseMeasurement.parseGlucoseMeasurement(values);
                    if (measurement != null) {
                        // convert to mg/dl...
                        long mgdl = 0;
                        if (measurement.getFlagsBitValue(GlucoseMeasurement.FlagValueBit.GLUCOSE_CONCENTRATION_UNIT) == Constants.FLAG_VALUE_TRUE) {
                            // mol/L how to...
                            // blood sugar mmol/l -> mg/dl == mgdl * 18
                            //             mol/l -> mmol/l == mol * 1000;
                            float molPerLitre = measurement.getGlucoseConcentration();
                            mgdl = Math.round(molPerLitre * 1000 * 18);
                        } else {
                            // kg/m3↔kg/L   1 kg/L = 1000 kg/m3
                            // kg/m3↔mg/dL   1 kg/m3 = 100 mg/dL
                            // ==> mg/dL↔kg/L   1 kg/L = 100000 mg/dL
                            float kgPerLitre = measurement.getGlucoseConcentration();
                            mgdl = Math.round(kgPerLitre * 1.0E05);
                        }

                        IoTCollectedData.IoTData iotData = new IoTCollectedData.IoTData();
                        iotData.setIotDeviceId(address);
                        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
                        Calendar calendar = measurement.getBaseTime();
                        iotData.setDate(df.format(calendar.getTime()));
                        iotData.setValue(Long.toString(mgdl));

                        try {
                            mScanedList.get(mCurrentRetrieveDevice.getDeviceId()).setLastSequenceNumber(measurement.getSequenceNumber());
                            // save to preference.
                            String json = mGson.toJson(mScanedList);
                            if (json != null) {
                                IoTServicePreference.setIoTDevicesList(mCtx, json);
                            }
                        } catch (Exception e) {

                        }
                        mCollectedData.addIoTData(iotData);
                        Log.d(TAG, "Gluecose measurement : seq = " + measurement.getSequenceNumber() + "value = " + mgdl + "" + " mg/dl, timestamp = " + iotData.getDate());

                        mCollectedData.addIoTData(iotData);
                    }
                }
                break;
            }
        }

    }

    private void sendCollectedDataToServer() {
        if (!NetworkUtils.isConnected(mCtx)) {
            Log.w(TAG, "sendCollectedDataToServer() : Network is unavailable..");
            IoTServicePreference.setUnSentCollectedData(mCtx, mGson.toJson(mCollectedData));

            Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
            LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
            return;
        }

        sendMessageToService(IoTServiceCommand.SCANNING_START, null);
        if (StringUtils.isEmptyString(mCollectServerAddress)) {
            Log.w(TAG, "sendCollectedDataToServer() : mCollectServerAddress is empty..");
            IoTServicePreference.setUnSentCollectedData(mCtx, mGson.toJson(mCollectedData));

            Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
            LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
            return;
        }

        IoTCollectedData data = new IoTCollectedData();
        data.setDeviceId(mCollectedData.getDeviceId());
        data.setIoTData((ArrayList<IoTCollectedData.IoTData>) mCollectedData.getIoTData().clone());

        mCollectedData.clearIoTData();
        Log.d(TAG, ">> post clear : IoTCollectedData size = " + data.getIoTDataSize());

        if (data.getIoTDataSize() == 0) {
            Log.d(TAG, "sendCollectedDataToServer() : IoTCollectedData size 0. do not send.. ");

            Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
            LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
            return;
        }

        SendIoTDeviceDataTask task = new SendIoTDeviceDataTask();
        if (task != null) {
            task.setSendColledApiData(mCtx, mHandler, mCollectServerAddress, data);
            task.execute();
        }
    }

    public void forceDataSync() {
        if (mServiceStatus != IoTServiceStatus.RUNNING) {
            Intent intent = new Intent(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
            LocalBroadcastManager.getInstance(mCtx).sendBroadcast(intent);
            return;
        }
        if (mCurrentRetrieveIndex < 0 && mCurrentRetrieveDevice == null) {
            mHandler.removeMessages(HANDLER_RETRIEVE_IOT_DEVICES);
            mHandler.sendEmptyMessage(HANDLER_RETRIEVE_IOT_DEVICES);
        }
    }

    public boolean isIoTServiceAvailable() {
        if (mBondedWithServerCount > 0 && mServiceStatus == IoTServiceStatus.RUNNING) {
            return true;
        }
        return false;
    }
}
