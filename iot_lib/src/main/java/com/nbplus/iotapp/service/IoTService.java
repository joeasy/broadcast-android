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
import com.nbplus.iotlib.data.IoTConstants;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTServiceCommand;
import com.nbplus.iotlib.data.IoTServiceStatus;
import com.nbplus.iotlib.data.IoTHandleData;

import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
    private static final int HANDLER_MESSAGE_CONNECTION_NOT_RESPOND = HANDLER_MESSAGE_BT_STATE_CHANGED - 1;

    private static final int CONNECTION_NOT_RESPOND_WAIT_TIME = 5 * 1000;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiManager.WifiLock mWifiLock;
    boolean mLastConnectionStatus = false;          // network status
    private BluetoothLeService mBluetoothLeService;
    boolean mUseIoTGateway = false;

    private IoTServiceHandler mHandler = new IoTServiceHandler(this);
    Messenger mServiceMessenger = null;

    private IoTServiceStatus mServiceStatus = IoTServiceStatus.INITIALIZE;
    private IoTResultCodes mErrorCodes = IoTResultCodes.SUCCESS;

    // IoT 서비스를 사용하는 어플리케이션 및 Messenger 맵.
    // REGISTER_SERVICE 시에 등록되며, UNREGISTER_SERVICE 시에 제거된다.
    HashMap<String, WeakReference<Messenger>> mRegisteredApps = new HashMap<>();

    IoTHandleData mRequestHandleData = null;
    ArrayList<String> mConnectedDeviceList = new ArrayList<>();
    boolean mIsDisconnectAllState = false;

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
            case IoTServiceCommand.SCANNING_START: {
                if (mUseIoTGateway) {

                } else {
                    if (mBluetoothLeService != null && mServiceStatus == IoTServiceStatus.RUNNING) {
                        mBluetoothLeService.scanLeDevicePeriodically(true);
                    }
                }
                break;
            }
            case IoTServiceCommand.SCANNING_STOP: {
                if (mUseIoTGateway) {

                } else {
                    if (mBluetoothLeService != null && mServiceStatus == IoTServiceStatus.RUNNING) {
                        mBluetoothLeService.scanLeDevicePeriodically(false, false);
                    }
                }
                break;
            }
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
                    unbindService(mBluetoothServiceConnection);

                    mServiceStatus = IoTServiceStatus.STOPPED;
                    mHandler.removeMessages(HANDLER_MESSAGE_CONNECTION_NOT_RESPOND);
                    mConnectedDeviceList.clear();
                    mRequestHandleData = null;

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
                if (mUseIoTGateway) {
                    // TODO
                } else {
                    mBluetoothLeService.scanLeDevicePeriodically(false);
                    mBluetoothLeService.scanLeDevicePeriodically(true);
                }
                break;
            }

            case IoTServiceCommand.DEVICE_DISCONNECT_ALL: {
                Bundle b = msg.getData();
                String msgId = b.getString(IoTServiceCommand.KEY_MSGID, "");

                if (mConnectedDeviceList.size() > 0) {
                    mIsDisconnectAllState = true;
                    IoTHandleData ioTHandleData = b.getParcelable(IoTServiceCommand.KEY_DATA);
                    ioTHandleData.setMsgId(msgId);
                    ioTHandleData.setRequestCommand(msg.what);
                    mRequestHandleData = ioTHandleData;

                    String connectedDeviceId = mConnectedDeviceList.get(0);
                    if (mUseIoTGateway) {
                        // TODO

                    } else {
                        if (mBluetoothLeService != null) {
                            mBluetoothLeService.disconnect(connectedDeviceId);
                        } else {
                            Messenger clientMessenger = getClientMessengerFromMessageId(msgId);
                            sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        }
                    }
                } else {
                    Messenger clientMessenger = getClientMessengerFromMessageId(msgId);
                    sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.SUCCESS);
                }
                break;
            }

            case IoTServiceCommand.DEVICE_CONNECT: {
                Bundle b = msg.getData();
                IoTHandleData ioTHandleData = b.getParcelable(IoTServiceCommand.KEY_DATA);
                String msgId = b.getString(IoTServiceCommand.KEY_MSGID, "");
                Messenger clientMessenger = getClientMessengerFromMessageId(msgId);

                if (mUseIoTGateway) {
                    if (ioTHandleData == null) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case IoTServiceCommand.DEVICE_CONNECT : ioTHandleData is null");
                        return;
                    }
                } else {
                    if (mBluetoothLeService == null || ioTHandleData == null) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case IoTServiceCommand.DEVICE_CONNECT : mBluetoothLeService or ioTHandleData is null");
                        return;
                    }
                    if (ioTHandleData.getDeviceTypeId() != IoTDevice.DEVICE_TYPE_ID_BT) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case IoTServiceCommand.DEVICE_DISCONNECT : is not bt device... mUseIoTGateway == false");
                        return;
                    }

                    if (mBluetoothLeService.connect(ioTHandleData.getDeviceId())) {
                        ioTHandleData.setMsgId(msgId);
                        ioTHandleData.setRequestCommand(msg.what);

                        mRequestHandleData = ioTHandleData;
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.SUCCESS);
                        // 스마트센서라는놈... connect 가 안되는 경우가 있다.
                        mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_CONNECTION_NOT_RESPOND, CONNECTION_NOT_RESPOND_WAIT_TIME);
                    } else {
                        Log.w(TAG, "mBluetoothLeService.connect() return false");
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                    }
                }
                break;
            }

            case HANDLER_MESSAGE_CONNECTION_NOT_RESPOND : {
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.disconnect(mRequestHandleData.getDeviceId());
                }
                Messenger clientMessenger = getClientMessengerFromMessageId(mRequestHandleData.getMsgId());
                sendResultToApplication(clientMessenger, mRequestHandleData.getMsgId(), IoTServiceCommand.DEVICE_CONNECT, IoTResultCodes.DEVICE_CONNECTION_NOT_RESPOND);
                break;
            }

            case IoTServiceCommand.DEVICE_DISCONNECT: {
                Bundle b = msg.getData();
                IoTHandleData ioTHandleData = b.getParcelable(IoTServiceCommand.KEY_DATA);
                String msgId = b.getString(IoTServiceCommand.KEY_MSGID, "");
                Messenger clientMessenger = getClientMessengerFromMessageId(msgId);

                if (mUseIoTGateway) {
                    if (ioTHandleData == null) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case IoTServiceCommand.DEVICE_DISCONNECT : ioTHandleData is null");
                        return;
                    }
                } else {
                    if (mBluetoothLeService == null || ioTHandleData == null) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case IoTServiceCommand.DEVICE_DISCONNECT : mBluetoothLeService or ioTHandleData is null");
                        return;
                    }
                    if (ioTHandleData.getDeviceTypeId() != IoTDevice.DEVICE_TYPE_ID_BT) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case IoTServiceCommand.DEVICE_DISCONNECT : is not bt device... mUseIoTGateway == false");
                        return;
                    }

                    if (mConnectedDeviceList.contains(ioTHandleData.getDeviceId())) {
                        ioTHandleData.setMsgId(msgId);
                        ioTHandleData.setRequestCommand(msg.what);

                        mRequestHandleData = ioTHandleData;
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.SUCCESS);
                        mBluetoothLeService.disconnect(ioTHandleData.getDeviceId());
                    } else {
                        Log.w(TAG, "mConnectedDeviceList.contains(ioTHandleData.getDeviceId()) is false");
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                    }
                }
                break;
            }

            case IoTServiceCommand.DEVICE_READ_DATA:
            case IoTServiceCommand.DEVICE_WRITE_DATA:
            case IoTServiceCommand.DEVICE_SET_NOTIFICATION: {
                Bundle b = msg.getData();
                IoTHandleData ioTHandleData = b.getParcelable(IoTServiceCommand.KEY_DATA);
                String msgId = b.getString(IoTServiceCommand.KEY_MSGID, "");
                Messenger clientMessenger = getClientMessengerFromMessageId(msgId);

                if (mUseIoTGateway) {
                    if (ioTHandleData == null) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case READ_WRITE_SETNOTI : ioTHandleData is null");
                        return;
                    }
                } else {
                    if (mBluetoothLeService == null || ioTHandleData == null) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case READ_WRITE_SETNOTI : ioTHandleData is null");
                        return;
                    }

                    if (ioTHandleData.getDeviceTypeId() != IoTDevice.DEVICE_TYPE_ID_BT) {
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        Log.w(TAG, "case READ_WRITE_SETNOTI : is not bt device... mUseIoTGateway == false");
                        return;
                    }

                    if (mConnectedDeviceList.contains(ioTHandleData.getDeviceId())) {
                        ioTHandleData.setMsgId(msgId);
                        ioTHandleData.setRequestCommand(msg.what);

                        boolean result = true;
                        switch (msg.what) {
                            case IoTServiceCommand.DEVICE_READ_DATA:
                                result = mBluetoothLeService.readCharacteristic(ioTHandleData.getDeviceId(),
                                        ioTHandleData.getServiceUuid(),
                                        ioTHandleData.getCharacteristicUuid());
                                break;
                            case IoTServiceCommand.DEVICE_WRITE_DATA:
                                result = mBluetoothLeService.writeRemoteCharacteristic(ioTHandleData.getDeviceId(),
                                        ioTHandleData.getServiceUuid(),
                                        ioTHandleData.getCharacteristicUuid(),
                                        ioTHandleData.getValue());
                                break;
                            case IoTServiceCommand.DEVICE_SET_NOTIFICATION:
                                result = mBluetoothLeService.setCharacteristicNotification(ioTHandleData.getDeviceId(),
                                        ioTHandleData.getServiceUuid(),
                                        ioTHandleData.getCharacteristicUuid(),
                                        true);
                                break;
                        }

                        if (result) {
                            mRequestHandleData = ioTHandleData;
                            sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.SUCCESS);
                        } else {
                            sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                        }
                    } else {
                        Log.w(TAG, "mConnectedDeviceList.contains(ioTHandleData.getDeviceId()) is false");
                        sendResultToApplication(clientMessenger, msgId, msg.what, IoTResultCodes.FAILED);
                    }
                }
                break;
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

                    sendNotificationToApplication(notiMessage);
                }
            }
            /**
             * when using bluetooth
             */
            else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_CONNECTED received. Attempt to serviceDiscovery()");
                mHandler.removeMessages(HANDLER_MESSAGE_CONNECTION_NOT_RESPOND);
                if (mBluetoothLeService != null) {
                    Bundle b = intent.getExtras();
                    if (b == null) {
                        Log.w(TAG, "Bundle is not found!!");
                        return;
                    }

                    String address = b.getString(IoTServiceCommand.KEY_DEVICE_UUID, "");
                    if (StringUtils.isEmptyString(address)) {
                        Log.w(TAG, "Unknown address information... close it. ");
                        mBluetoothLeService.disconnect(address);
                        return;
                    }

                    for (int i = 0; i < mConnectedDeviceList.size(); i++) {
                        Log.d(TAG, ">> mConnectedDeviceList[" + i + "] = " + mConnectedDeviceList.get(i));
                    }
                    if (!mConnectedDeviceList.contains(address)) {
                        mConnectedDeviceList.add(address);
                        Log.d(TAG, address + " address is added to mConnectedDeviceList");
                    }
                    mBluetoothLeService.discoveryServices(address);
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_DISCONNECTED received.");
                mHandler.removeMessages(HANDLER_MESSAGE_CONNECTION_NOT_RESPOND);
                Bundle b = intent.getExtras();
                if (b == null) {
                    Log.w(TAG, "Bundle is not found!!");
                    return;
                }

                String address = b.getString(IoTServiceCommand.KEY_DEVICE_UUID, "");
                if (StringUtils.isEmptyString(address)) {
                    Log.w(TAG, "Unknown address information.... ");
                    return;
                }

                // send to IoT interface module
                Message msg = new Message();
                msg.what = IoTServiceCommand.DEVICE_DISCONNECTED;
                Bundle extras = new Bundle();
                extras.putString(IoTServiceCommand.KEY_DEVICE_UUID, address);
                msg.setData(extras);

                // 리소스 해제..
                mBluetoothLeService.close(address);

                if (mIsDisconnectAllState) {
                    mConnectedDeviceList.remove(address);
                    if (mConnectedDeviceList.size() > 0) {
                        mBluetoothLeService.disconnect(mConnectedDeviceList.get(0));
                    } else {
                        Messenger clientMessenger = getClientMessengerFromMessageId(mRequestHandleData.getMsgId());
                        sendResultToApplication(clientMessenger, mRequestHandleData.getMsgId(), mRequestHandleData.getRequestCommand(), IoTResultCodes.SUCCESS);
                        mIsDisconnectAllState = false;
                    }
                    return;
                }

                if (mRequestHandleData == null) {
                    // send to all
                    sendNotificationToApplication(null, msg);
                } else {
                    for (int i = 0; i < mConnectedDeviceList.size(); i++) {
                        Log.d(TAG, ">> mConnectedDeviceList[" + i + "] = " + mConnectedDeviceList.get(i));
                    }
                    Log.d(TAG, ">> mConnectedDeviceList.contains(" + address + ") = " + mConnectedDeviceList.contains(address));
                    // 요청은 순차적으로 이루어져야 한다.
                    if (mRequestHandleData.getRequestCommand() != IoTServiceCommand.DEVICE_DISCONNECT) {
                        // 연결 시도시에 연결이되지 않고.. disconnect 가 오는 경우가 있음.(슬립상태여서.. connect 안되는경우등등...)
                        // 연결중에 명령을 시도하더라도 중간에 비정상적으로 종료되는 케이스가 있을 수 있음.
                        Log.w(TAG, address + " device connection is closed...");
                    }
                    sendNotificationToApplication(mRequestHandleData.getMsgId(), msg);
                    mRequestHandleData = null;
                }
                mConnectedDeviceList.remove(address);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED received.");
                Bundle b = intent.getExtras();
                if (b == null) {
                    Log.w(TAG, "Bundle is not found!!");
                    return;
                }

                String address = b.getString(IoTServiceCommand.KEY_DEVICE_UUID, "");
                if (StringUtils.isEmptyString(address) || !mConnectedDeviceList.contains(address)) {
                    Log.w(TAG, "Unknown address information... close it. ");
                    mBluetoothLeService.disconnect(address);
                    return;
                }

                // 요청은 순차적으로 이루어져야 한다.
                if (mRequestHandleData.getRequestCommand() != IoTServiceCommand.DEVICE_CONNECT) {
                    if (StringUtils.isEmptyString(address) || !mConnectedDeviceList.contains(address)) {
                        Log.w(TAG, "Unknown address information... close it. ");
                        mBluetoothLeService.disconnect(address);
                        mRequestHandleData = null;
                        return;
                    }
                }

                HashMap<String, ArrayList<String>> discoveredServices = (HashMap<String, ArrayList<String>>)b.getSerializable(IoTServiceCommand.KEY_DATA);
                //device.setDiscoveredServices(discoveredServices);

                // send to IoT interface module
                Message msg = new Message();
                msg.what = IoTServiceCommand.DEVICE_CONNECTED;
                Bundle extras = new Bundle();
                extras.putString(IoTServiceCommand.KEY_DEVICE_UUID, mRequestHandleData.getDeviceId());
                extras.putSerializable(IoTServiceCommand.KEY_DATA, discoveredServices);
                msg.setData(extras);

                sendNotificationToApplication(mRequestHandleData.getMsgId(), msg);
                mRequestHandleData = null;
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "ACTION_DATA_AVAILABLE received.");
                IoTHandleData resultData = intent.getParcelableExtra(IoTServiceCommand.KEY_DATA);

                Message msg = new Message();
                msg.what = IoTServiceCommand.DEVICE_NOTIFICATION_DATA;
                Bundle extras = new Bundle();
                extras.putString(IoTServiceCommand.KEY_DEVICE_UUID, resultData.getDeviceId());
                if (resultData == null) {
                    Log.w(TAG, "result data not found");
                    extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.FAILED);
                } else {
                    if (resultData.getStatus() == IoTHandleData.STATUS_SUCCESS) {
                        extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.SUCCESS);
                        extras.putParcelable(IoTServiceCommand.KEY_DATA, resultData);
                    } else {
                        extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.FAILED);
                    }
                }
                msg.setData(extras);
                sendNotificationToApplication(null, msg);
            } else if (BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS.equals(action)) {
                Log.d(TAG, "ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS received.");
                IoTHandleData resultData = intent.getParcelableExtra(IoTServiceCommand.KEY_DATA);

                Message msg = new Message();
                msg.what = IoTServiceCommand.DEVICE_SET_NOTIFICATION_RESULT;
                Bundle extras = new Bundle();
                extras.putString(IoTServiceCommand.KEY_DEVICE_UUID, mRequestHandleData.getDeviceId());
                if (resultData == null) {
                    Log.w(TAG, "result data not found");
                    extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.FAILED);
                } else {
                    if (resultData.getStatus() == IoTHandleData.STATUS_SUCCESS) {
                        extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.SUCCESS);
                        extras.putParcelable(IoTServiceCommand.KEY_DATA, resultData);
                    } else {
                        extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.FAILED);
                    }
                }
                msg.setData(extras);
                sendNotificationToApplication(mRequestHandleData.getMsgId(), msg);
                mRequestHandleData = null;
            } else if (BluetoothLeService.ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS.equals(action) ||
                    BluetoothLeService.ACTION_GATT_CHARACTERISTIC_READ_SUCCESS.equals(action)) {
                Log.d(TAG, "ACTION_GATT_CHARACTERISTIC_WRITE_or_READ_SUCCESS received.");
                IoTHandleData resultData = intent.getParcelableExtra(IoTServiceCommand.KEY_DATA);

                Message msg = new Message();
                if (BluetoothLeService.ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS.equals(action)) {
                    msg.what = IoTServiceCommand.DEVICE_WRITE_DATA_RESULT;
                } else {
                    msg.what = IoTServiceCommand.DEVICE_READ_DATA_RESULT;
                }
                Bundle extras = new Bundle();
                extras.putString(IoTServiceCommand.KEY_DEVICE_UUID, mRequestHandleData.getDeviceId());
                if (resultData == null) {
                    Log.w(TAG, "result data not found");
                    extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.FAILED);
                } else {
                    if (resultData.getStatus() == IoTHandleData.STATUS_SUCCESS) {
                        extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.SUCCESS);
                        extras.putParcelable(IoTServiceCommand.KEY_DATA, resultData);
                    } else {
                        extras.putSerializable(IoTServiceCommand.KEY_RESULT, IoTResultCodes.FAILED);
                    }
                }
                msg.setData(extras);
                sendNotificationToApplication(mRequestHandleData.getMsgId(), msg);
                mRequestHandleData = null;
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
        sendBroadcast(new Intent(IoTConstants.ACTION_SERVICE_CREATE_BROADCAST));
        /**
         * Target we publish for clients to send messages to IncomingHandler.Note
         * that calls to its binder are sequential!
         */
        mServiceMessenger = new Messenger(mHandler);

        mUseIoTGateway = IoTServicePreference.isUseIoTGateway(this);
        if (mUseIoTGateway) {
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

    private void sendNotificationToApplication(Message message) {
        sendNotificationToApplication(null, message);
    }
    private void sendNotificationToApplication(String msgId, Message message) {
        if (StringUtils.isEmptyString(msgId)) {            // send to all application
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
            Messenger clientMessenger = getClientMessengerFromMessageId(msgId);
            if (clientMessenger != null) {
                try {
                    clientMessenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Messenger getClientMessengerFromMessageId(String msgId) {
        if (!StringUtils.isEmptyString(msgId)) {
            String[] strArrays = msgId.split("_");
            if (strArrays.length == 2) {
                WeakReference<Messenger> clientMessenger = mRegisteredApps.get(strArrays[0]);

                return clientMessenger.get();
            } else {
                Log.e(TAG, "Invalid register args...");
                return null;
            }
        }
        return null;
    }
}
