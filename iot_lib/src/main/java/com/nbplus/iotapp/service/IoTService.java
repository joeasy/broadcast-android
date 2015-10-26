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

package com.nbplus.iotapp.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.nbplus.iotapp.bluetooth.BluetoothLeService;
import com.nbplus.iotapp.perferences.IoTServicePreference;
import com.nbplus.iotlib.data.Constants;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTServiceCommand;
import com.nbplus.iotlib.data.IoTServiceStatus;

import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by basagee on 2015. 8. 6..
 */
public class IoTService extends Service {
    private static final String TAG = IoTService.class.getSimpleName();

    // internal constants
    private static final int HANDLER_MESSAGE_CONNECTIVITY_CHANGED = IoTServiceCommand.COMMAND_BASE_VALUE - 1;
    private static final int HANDLER_MESSAGE_BT_STATE_CHANGED = HANDLER_MESSAGE_CONNECTIVITY_CHANGED - 1;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiManager.WifiLock mWifiLock;
    boolean mLastConnectionStatus = false;          // network status
    private BluetoothLeService mBluetoothLeService;


    private IoTServiceHandler mHandler = new IoTServiceHandler(this);
    Messenger mServiceMessenger = null;

    private IoTServiceStatus mServiceStatus = IoTServiceStatus.INITIALIZE;
    private IoTResultCodes mErrorCodes = IoTResultCodes.SUCCESS;

    // IoT 서비스를 사용하는 어플리케이션 및 Messenger 맵.
    // REGISTER_SERVICE 시에 등록되며, UNREGISTER_SERVICE 시에 제거된다.
    HashMap<String, WeakReference<Messenger>> mRegisteredApps = new HashMap<>();

    // 핸들러 객체 만들기
    private static class IoTServiceHandler extends Handler {
        private final WeakReference<IoTService> mService;

        public IoTServiceHandler(IoTService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            IoTService service = mService.get();
            if (service != null) {
                service.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        Log.d(TAG, "handle message msg.what = " + msg.what);
        switch (msg.what) {
            case HANDLER_MESSAGE_CONNECTIVITY_CHANGED :
                Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED received !!!");
                final boolean isConnected = NetworkUtils.isConnected(this);
                if (mLastConnectionStatus == isConnected) {
                    return;
                }

                mLastConnectionStatus = isConnected;
                if (mLastConnectionStatus) {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is connected !!!");
                    // TODO : re-connect
                    if (mServiceStatus == IoTServiceStatus.STOPPED) {

                    }
                } else {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is disconnected !!!");
                }
//
//                mServiceStatus = Status.STOPPED;
                break;
            case HANDLER_MESSAGE_BT_STATE_CHANGED :
                Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED received !!!");
                final int state = msg.arg1;
                if ((mLastConnectionStatus && state == BluetoothAdapter.STATE_ON) ||
                        (!mLastConnectionStatus && state == BluetoothAdapter.STATE_OFF)) {
                    return;
                }

                mLastConnectionStatus = (state == BluetoothAdapter.STATE_ON) ? true : false;
                if (mLastConnectionStatus) {
                    Log.d(TAG, "HANDLER_MESSAGE_BT_STATE_CHANGED bluetooth is enabled !!!");

                    if (mServiceStatus != IoTServiceStatus.RUNNING) {
                        final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                        if (!bindService(gattServiceIntent, mBluetoothServiceConnection, BIND_AUTO_CREATE)) {
                            Log.e(TAG, ">> bind BluetoothServiceConnection failed.... !!!");
                        }
                    }
                } else {
                    Log.d(TAG, "HANDLER_MESSAGE_BT_STATE_CHANGED bluetooth is disabled !!!");
                    mErrorCodes = IoTResultCodes.BLUETOOTH_NOT_ENABLED;

                    mServiceStatus = IoTServiceStatus.STOPPED;
                    sendAllServiceStatusNotification();
                }
//
//                mServiceStatus = Status.STOPPED;
                break;
            case IoTServiceCommand.REGISTER_SERVICE : {
                Bundle b = msg.getData();
                if (msg.replyTo == null || b == null) {
                    Log.e(TAG, "Invalid register args...");
                    break;
                }

                String msgId = b.getString(IoTServiceCommand.KEY_MSGID, "");
                if (!StringUtils.isEmptyString(msgId)) {
                    Log.d(TAG, "msgId == " + msgId);
                    String[] strArrays = msgId.split("_");
                    if (strArrays.length == 2) {
                        mRegisteredApps.put(strArrays[0], new WeakReference<>(msg.replyTo));
                        sendResultToApplication(msg.replyTo, msgId, msg.what, IoTResultCodes.SUCCESS);

                        // 등록이 성공한 어플리케이션에는 현재 서비스의 상태를 보내준다.
                        sendServiceStatusNotification(msg.replyTo);
                    } else {
                        Log.e(TAG, "Invalid register args...");
                        sendResultToApplication(msg.replyTo, msgId, msg.what, IoTResultCodes.INVALID_REQUEST_ARGUMENTS);
                        break;
                    }
                } else {
                    Log.e(TAG, "Invalid register args...");
                    sendResultToApplication(msg.replyTo, "", msg.what, IoTResultCodes.INVALID_REQUEST_ARGUMENTS);
                    break;
                }

                break;
            }
            case IoTServiceCommand.UNREGISTER_SERVICE : {
                Bundle b = msg.getData();
                if (b == null) {
                    Log.e(TAG, "Invalid register args...");
                    break;
                }

                String msgId = b.getString(IoTServiceCommand.KEY_MSGID, "");
                if (!StringUtils.isEmptyString(msgId)) {
                    String[] strArrays = msgId.split("_");
                    if (strArrays.length == 2) {
                        WeakReference<Messenger> clientMessenger = mRegisteredApps.get(strArrays[0]);

                        if (clientMessenger.get() != null) {
                            sendResultToApplication(clientMessenger.get(), msgId, msg.what, IoTResultCodes.SUCCESS);
                            mRegisteredApps.remove(strArrays[0]);
                        } else {
                            //??
                        }
                    } else {
                        Log.e(TAG, "Invalid register args...");
                        break;
                    }
                } else {
                    Log.e(TAG, "Invalid un-register args...");
                }
                break;
            }

            case IoTServiceCommand.GET_DEVICE_LIST: {
                if (IoTServicePreference.isUseIoTGateway(this)) {
                    // TODO
                } else {
                    mBluetoothLeService.scanLeDevicePeriodically(false);
                    mBluetoothLeService.scanLeDevicePeriodically(true);
                }
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(HANDLER_MESSAGE_CONNECTIVITY_CHANGED);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (btState == BluetoothAdapter.STATE_OFF || btState == BluetoothAdapter.STATE_ON) {
                    Message msg = mHandler.obtainMessage(HANDLER_MESSAGE_BT_STATE_CHANGED, btState, 0);
                    mHandler.sendMessage(msg);
                }
            }
            /**
             * Bluetooth control.
             */
            else if (BluetoothLeService.ACTION_DEVICE_LIST.equals(action)) {
                Message notiMessage = new Message();
                notiMessage.what = IoTServiceCommand.DEVICE_LIST_NOTIFICATION;

                Bundle recvBundle = intent.getExtras();
                HashMap<String, IoTDevice> devices = (HashMap<String, IoTDevice>)recvBundle.getSerializable(IoTServiceCommand.KEY_DATA);
                if (recvBundle != null && devices != null) {
                    // 응답에 Bundle 은 request command (int)와 result code (serializable) 로만 이루어진다.
                    Bundle b = new Bundle();
                    b.putString(IoTServiceCommand.KEY_MSGID, getPackageName() + "_" + System.currentTimeMillis());
                    b.putSerializable(IoTServiceCommand.KEY_DATA, devices);
                    notiMessage.setData(b);

                    sendNotificationToApplication(IoTServiceCommand.DEVICE_LIST_NOTIFICATION, notiMessage);
                }
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS.equals(action)) {

            }
        }

    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mBluetoothServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "mBluetoothServiceConnection onServiceConnected()");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            mErrorCodes = mBluetoothLeService.initialize();
            if (!mErrorCodes.equals(IoTResultCodes.SUCCESS)) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                //finish();
                mServiceStatus = IoTServiceStatus.STOPPED;
                return;
            }
            mServiceStatus = IoTServiceStatus.RUNNING;
            mErrorCodes = IoTResultCodes.SUCCESS;

            sendAllServiceStatusNotification();

            // start bluetooth scan.. periodically
            mBluetoothLeService.scanLeDevicePeriodically(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "mBluetoothServiceConnection onServiceDisconnected()");
            mBluetoothLeService = null;
            mServiceStatus = IoTServiceStatus.STOPPED;

            if (mLastConnectionStatus) {
                mErrorCodes = IoTResultCodes.SERVICE_DISCONNECTED;
            } else {
                // ??
            }

            sendAllServiceStatusNotification();
        }
    };

    @Override
    public void onCreate() {
        Log.i(TAG, "debug: Creating service");
        sendBroadcast(new Intent(com.nbplus.iotlib.data.Constants.ACTION_SERVICE_CREATE_BROADCAST));
        /**
         * Target we publish for clients to send messages to IncomingHandler.Note
         * that calls to its binder are sequential!
         */
        mServiceMessenger = new Messenger(mHandler);

        if (IoTServicePreference.isUseIoTGateway(this)) {
            Log.d(TAG, ">> Use IoT Gateway....");
            // Create the Wifi lock (this does not acquire the lock, this just creates it)
            mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL,  IoTService.class.getSimpleName() + "_lock");

            // check network status
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(mBroadcastReceiver, intentFilter);

            mLastConnectionStatus = NetworkUtils.isConnected(this);

            // TODO : connect to iot gateway
        } else {
            Log.d(TAG, ">> Use internal blutooth....");
            // check bluetooth status
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver, intentFilter);

            // bluetooth local broadcast
            intentFilter = makeGattUpdateIntentFilter();
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);

            // connect to ble service
            mErrorCodes = checkBluetoothEnabled();
            if (mErrorCodes.equals(IoTResultCodes.SUCCESS)) {
                try {
                    if (mBluetoothServiceConnection != null) {
                        unbindService(mBluetoothServiceConnection);
                    }
                } catch (Exception e) {

                }
                final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                bindService(gattServiceIntent, mBluetoothServiceConnection, BIND_AUTO_CREATE);
            } else {
                mServiceStatus = IoTServiceStatus.STOPPED;
                Log.d(TAG, "Internal bluetooth error = " + mErrorCodes);
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_LIST);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS);
        return intentFilter;
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock != null && mWifiLock.isHeld()) mWifiLock.release();
        mWifiLock = null;
        try {
            unregisterReceiver(mBroadcastReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */

    private static int num = 0;
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

    /**
     * 자체적으로 블루투스를 연동하는 경우, 블루투스 enable 여부를 체크하여 사용자가 켜도록 한다.
     */
    public IoTResultCodes checkBluetoothEnabled() {
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return IoTResultCodes.BLE_NOT_SUPPORTED;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            return IoTResultCodes.BLUETOOTH_NOT_SUPPORTED;
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            return IoTResultCodes.BLUETOOTH_NOT_ENABLED;
        }

        mLastConnectionStatus = true;
        return IoTResultCodes.SUCCESS;
    }

    /**
     * 모든 연결된 어플리케이션에 상태변화를 전달한다.
     */
    private void sendAllServiceStatusNotification() {
        Iterator<String> iter = mRegisteredApps.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            WeakReference<Messenger> clientMessengerRef = mRegisteredApps.get(key);
            Messenger clientMessenger = clientMessengerRef.get();
            if (clientMessenger != null) {
                Message response = new Message();
                response.what = IoTServiceCommand.SERVICE_STATUS_NOTIFICATION;

                // 응답에 Bundle 은 request command (int)와 result code (serializable) 로만 이루어진다.
                Bundle b = new Bundle();
                b.putString(IoTServiceCommand.KEY_MSGID, /*this.getApplicationContext().*/getPackageName() + "_" + System.currentTimeMillis());
                b.putSerializable(IoTServiceCommand.KEY_SERVICE_STATUS, mServiceStatus);
                b.putSerializable(IoTServiceCommand.KEY_SERVICE_STATUS_CODE, mErrorCodes);
                response.setData(b);

                try {
                    clientMessenger.send(response);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 특정 어플리케이션에 현재 서비스의 상태를 보내준다.
     * @param clientMessenger
     */
    private void sendServiceStatusNotification(Messenger clientMessenger) {
        if (clientMessenger != null) {
            Message response = new Message();
            response.what = IoTServiceCommand.SERVICE_STATUS_NOTIFICATION;

            // 응답에 Bundle 은 request command (int)와 result code (serializable) 로만 이루어진다.
            Bundle b = new Bundle();
            b.putString(IoTServiceCommand.KEY_MSGID, /*this.getApplicationContext().*/getPackageName() + "_" + System.currentTimeMillis());
            b.putSerializable(IoTServiceCommand.KEY_SERVICE_STATUS, mServiceStatus);
            b.putSerializable(IoTServiceCommand.KEY_SERVICE_STATUS_CODE, mErrorCodes);
            response.setData(b);

            try {
                clientMessenger.send(response);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 어플리케이션에서 요청한 Request에 대한 결과를 전달한다.
     * 대부분이 Async로 이루어지기 때문에 이 메시지에는 요청을 받아들이고 수행을 요청하는 상태까지에 대한 결과만 있다.
     *
     * @param clientMessenger
     * @param msgId
     * @param command
     * @param result
     */
    private void sendResultToApplication(Messenger clientMessenger, String msgId, int command, IoTResultCodes result) {
        if (clientMessenger != null) {
            Message response = new Message();
            response.what = IoTServiceCommand.COMMAND_RESPONSE;

            // 응답에 Bundle 은 request command (int)와 result code (serializable) 로만 이루어진다.
            Bundle b = new Bundle();
            b.putString(IoTServiceCommand.KEY_MSGID, msgId);
            b.putInt(IoTServiceCommand.KEY_CMD, command);
            b.putSerializable(IoTServiceCommand.KEY_RESULT, result);
            response.setData(b);

            try {
                clientMessenger.send(response);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendNotificationToApplication(int cmd, Message message) {
        sendNotificationToApplication(cmd, message, null);
    }
    private void sendNotificationToApplication(int cmd, Message message, String appPackageName) {
        if (StringUtils.isEmptyString(appPackageName)) {            // send to all application
            Iterator<String> iter = mRegisteredApps.keySet().iterator();
            while(iter.hasNext()) {
                String key = iter.next();
                WeakReference<Messenger> clientMessengerRef = mRegisteredApps.get(key);
                Messenger clientMessenger = clientMessengerRef.get();
                if (clientMessenger != null) {

                    try {
                        clientMessenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            WeakReference<Messenger> clientMessengerRef = mRegisteredApps.get(appPackageName);
            Messenger clientMessenger = clientMessengerRef.get();
            if (clientMessenger != null) {
                try {
                    clientMessenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
