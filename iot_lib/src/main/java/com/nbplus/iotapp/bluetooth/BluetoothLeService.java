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

package com.nbplus.iotapp.bluetooth;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.nbplus.iotlib.R;
import com.nbplus.iotapp.btcharacteristics.GlucoseMeasurement;
import com.nbplus.iotapp.btcharacteristics.RecordAccessControlPoint;
import com.nbplus.iotapp.btcharacteristics.WeightMeasurement;
import com.nbplus.iotapp.data.AdRecord;
import com.nbplus.iotapp.data.DataParser;
import com.nbplus.iotapp.data.GattAttributes;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.iotlib.data.IoTHandleData;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTServiceCommand;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private HashMap<String, BluetoothGatt> mConnectedBluetoothGattMap = new HashMap<>();
    private BluetoothGattServer mBluetoothGattServer;

    /**
     * 10초동안 검색된 device list 를 저장해 두는 공간
     */
    private HashMap<String, IoTDevice> mScanedList = new HashMap<>();
    private HashMap<String, IoTDevice> mTempScanedList = new HashMap<>();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private int mConnectionState = STATE_DISCONNECTED;

    public final static String ACTION_DEVICE_LIST =
            "com.nbplus.bluetooth.le.ACTION_DEVICE_LIST";
    public final static String ACTION_GATT_CONNECTED =
            "com.nbplus.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.nbplus.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.nbplus.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.nbplus.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS =
            "com.nbplus.bluetooth.le.ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS";
    public final static String ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS =
            "com.nbplus.bluetooth.le.ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS";
    public final static String ACTION_GATT_CHARACTERISTIC_READ_SUCCESS =
            "com.nbplus.bluetooth.le.ACTION_GATT_CHARACTERISTIC_READ_SUCCESS";

    // intent extra data
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_DATA_SERVICE_UUID = "com.example.bluetooth.le.EXTRA_DATA_SERVICE_UUID";
    public final static String EXTRA_DATA_CHARACTERISTIC_UUID = "com.example.bluetooth.le.EXTRA_DATA_CHARACTERISTIC_UUID";
    public final static String EXTRA_DATA_STATUS = "com.example.bluetooth.le.EXTRA_DATA_STATUS";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(GattAttributes.HEART_RATE_MEASUREMENT);

    // for weight scale measurement
    public final static UUID UUID_WEIGHT_MEASUREMENT =
            UUID.fromString(GattAttributes.WEIGHT_MEASUREMENT);

    // for glucose measurement
    public final static UUID UUID_GLUCOSE_MEASUREMENT =
            UUID.fromString(GattAttributes.GLUCOSE_MEASUREMENT);
    public final static UUID UUID_GLUCOSE_MEASUREMENT_CONTEXT =
            UUID.fromString(GattAttributes.GLUCOSE_MEASUREMENT_CONTEXT);
    public final static UUID UUID_RECORD_ACCESS_CONTROL_POINT =
            UUID.fromString(GattAttributes.RECORD_ACCESS_CONTROL_POINT);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(gatt.getDevice().getAddress(), intentAction);
                Log.i(TAG, "Connected to GATT server." + gatt.getDevice().getAddress());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server = " + gatt.getDevice().getAddress());
                broadcastUpdate(gatt.getDevice().getAddress(), intentAction);

                close(gatt.getDevice().getAddress());
            } else {
                Log.d(TAG, "onConnectionStateChange : Unknown status = " + status + ", newState = " + newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Show all the supported services and characteristics on the user interface.
                HashMap<String, ArrayList<String>> discoveredServices = new HashMap<>();

                Bundle extras = new Bundle();
                List<BluetoothGattService> gattServices = getSupportedGattServices(gatt.getDevice().getAddress());
                if (gattServices != null && gattServices.size() > 0) {
                    for (BluetoothGattService service : gattServices) {

                        ArrayList<String> characteristicsList = new ArrayList<>();
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            characteristicsList.add(characteristic.getUuid().toString());
                        }
                        discoveredServices.put(service.getUuid().toString(), characteristicsList);
                    }

                    extras.putSerializable(IoTServiceCommand.KEY_DATA, discoveredServices);
                }
                broadcastUpdate(gatt.getDevice().getAddress(), ACTION_GATT_SERVICES_DISCOVERED, extras);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            byte[] value = characteristic.getValue();

            IoTHandleData data = new IoTHandleData();
            data.setDeviceId(gatt.getDevice().getAddress());
            data.setServiceUuid(characteristic.getService().getUuid().toString());
            data.setCharacteristicUuid(characteristic.getUuid().toString());
            data.setValue(value);
            data.setStatus(status);

            broadcastUpdate(ACTION_GATT_CHARACTERISTIC_READ_SUCCESS, data);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            IoTHandleData data = new IoTHandleData();
            data.setDeviceId(gatt.getDevice().getAddress());
            data.setServiceUuid(characteristic.getService().getUuid().toString());
            data.setCharacteristicUuid(characteristic.getUuid().toString());
            data.setValue(characteristic.getValue());
            data.setStatus(status);

            broadcastUpdate(ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS, data);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();

            IoTHandleData data = new IoTHandleData();
            data.setDeviceId(gatt.getDevice().getAddress());
            data.setServiceUuid(characteristic.getService().getUuid().toString());
            data.setCharacteristicUuid(characteristic.getUuid().toString());
            data.setValue(value);
            data.setStatus(BluetoothGatt.GATT_SUCCESS);

            broadcastUpdate(ACTION_DATA_AVAILABLE, data);
        }

        @Override
        public void onDescriptorRead (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            byte[] descValue = descriptor.getValue();
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

            Log.d(TAG, "onDescriptorRead() uuid = " + characteristic.getUuid().toString());
            byte byteValue = descValue[0];
            for (int i = 0; i < Byte.SIZE; i++) {
                Log.d(TAG, "byteValue[" + i + "] = " + (byteValue >> i & 0x1));
            }
        }

        /**
         * modified 2015.11.03
         * @param gatt
         * @param descriptor
         * @param status
         */
        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            byte[] descValue = descriptor.getValue();
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

            IoTHandleData data = new IoTHandleData();
            data.setDeviceId(gatt.getDevice().getAddress());
            data.setServiceUuid(characteristic.getService().getUuid().toString());
            data.setCharacteristicUuid(characteristic.getUuid().toString());
            data.setValue(descValue);
            data.setStatus(status);

            broadcastUpdate(ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS, data);
        }

    };

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d(TAG, "Our gatt server connection state changed, new state ");
            Log.d(TAG, Integer.toString(newState));
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d(TAG, "Our gatt server service was added.");
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Our gatt characteristic was read.");
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "We have received a write request for one of our hosted characteristics");

            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "Our gatt server descriptor was read.");
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("HELLO", "Our gatt server descriptor was written.");
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.d(TAG, "Our gatt server on execute write.");
            super.onExecuteWrite(device, requestId, execute);
        }
    };

    private void broadcastUpdate(String address, final String action) {
        final Intent intent = new Intent(action);
        Bundle extras = new Bundle();
        extras.putString(IoTServiceCommand.KEY_DEVICE_UUID, address);
        intent.putExtras(extras);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(String address, final String action, Bundle extras) {
        final Intent intent = new Intent(action);
        extras.putString(IoTServiceCommand.KEY_DEVICE_UUID, address);
        intent.putExtras(extras);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastDeviceListUpdate() {
        final Intent intent = new Intent(ACTION_DEVICE_LIST);
        Bundle extras = new Bundle();
        extras.putSerializable(IoTServiceCommand.KEY_DATA, mScanedList);
        intent.putExtras(extras);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * modified 2015.11.03
     * @param action
     * @param data
     */
    private void broadcastUpdate(final String action, IoTHandleData data) {
        final Intent intent = new Intent(action);
        intent.putExtra(IoTServiceCommand.KEY_DATA, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String address,
                                 final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_SERVICE_UUID, characteristic.getService().getUuid().toString());
        intent.putExtra(EXTRA_DATA_CHARACTERISTIC_UUID, characteristic.getUuid().toString());

        String str = "";
        byte [] values = characteristic.getValue();

        Log.d(TAG, "onCharacteristicChanged: address : " + address + ", uuid:" + characteristic.getUuid().toString());

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, values);
        } else if (UUID_WEIGHT_MEASUREMENT.equals(characteristic.getUuid())) {      // for weight scale
            int flag = values[0] & 0xff;
            Log.w(TAG, String.format("Measurement data received flag = %02x", flag));
            /**
             * 샤오미체중계는 플래그의 reserved field 값이 2인 경우에만 확정된값이다.
             */
            if (address != null && address.startsWith(GattAttributes.XIAOMI_MAC_ADDRESS_FILTER)) {
                if (values == null || values.length <= 0 || (values[0] & 0xf0) != 0x20) {
                    Log.d(TAG, "ignore ... flag 4nibble 0x20 is not ... ");
                    return;
                }
            }

            ArrayList<WeightMeasurement> measurements = WeightMeasurement.parseWeightMeasurement(address, characteristic.getUuid().toString(), values);

            intent.putParcelableArrayListExtra(EXTRA_DATA, measurements);
        } else if (UUID_GLUCOSE_MEASUREMENT.equals(characteristic.getUuid())) {
            GlucoseMeasurement measurement = GlucoseMeasurement.parseGlucoseMeasurement(values);

            intent.putExtra(EXTRA_DATA, measurement);
        } else if (UUID_GLUCOSE_MEASUREMENT_CONTEXT.equals(characteristic.getUuid())) {

        } else if (UUID_RECORD_ACCESS_CONTROL_POINT.equals(characteristic.getUuid())) {
            RecordAccessControlPoint recordAccessControlPoint = RecordAccessControlPoint.parseRecordAccessControlPoint(values);
            intent.putExtra(EXTRA_DATA, recordAccessControlPoint);
        } else if (UUID.fromString(GattAttributes.CURRENT_TIME).equals(characteristic.getUuid())) {
            if (values != null && values.length > 0) {
                intent.putExtra(EXTRA_DATA, values);
            }
            //intent.putExtra(EXTRA_DATA, characteristic.getValue());
        } else {
            // For all other profiles, writes the data formatted in HEX.
            if (values != null && values.length > 0) {
                intent.putExtra(EXTRA_DATA, values);
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

//        if (mScanedList.get(address) == null) {
//            Iterator<String> iter = mScanedList.keySet().iterator();
//            while (iter.hasNext()) {
//                Log.d(TAG, "scanned address = " + iter.next());
//            }
//            Log.w(TAG, "Received address = " + address);
//            Log.w(TAG, "This device is not activated...... check device status");
//            return false;
//        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);

        // Previously connected device.  Try to reconnect.
        if (bluetoothGatt != null) {
            close(address);
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection address = " + address);
            if (bluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (bluetoothGatt == null) {
            Log.w(TAG, "device.connectGatt failed");
            return false;
        }
        mConnectedBluetoothGattMap.put(address, bluetoothGatt);

        Log.d(TAG, "Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(String address) {
        if (StringUtils.isEmptyString(address)) {
            Log.w(TAG, "Unknown address");
        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);
        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG, "Disconnect connection!!");
        bluetoothGatt.disconnect();
    }

    public void discoveryServices(String address) {
        if (StringUtils.isEmptyString(address)) {
            Log.w(TAG, "Unknown address");
        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Iterator<String> iter = mConnectedBluetoothGattMap.keySet().iterator();

        while(iter.hasNext()) {
            String key = iter.next();
            close(key);
        }
    }

    public void close(String address) {
        if (StringUtils.isEmptyString(address)) {
            Log.w(TAG, "Unknown address");
        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);
        if (bluetoothGatt == null) {
            return;
        }
        Log.d(TAG, "close ble gatt resources !!");
        bluetoothGatt.close();
        mConnectedBluetoothGattMap.remove(address);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     */
    public boolean readCharacteristic(String address, String serviceUuid, String characteristicUuid) {
        Log.d(TAG, "readCharacteristic add = " + address + ", svc = " + serviceUuid + ", char = " + characteristicUuid);
        if (StringUtils.isEmptyString(address) ||
                StringUtils.isEmptyString(serviceUuid) || StringUtils.isEmptyString(characteristicUuid)) {
            Log.w(TAG, "Unknown parameter");
            return false;
        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);
        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (service == null) {
            Log.w(TAG, "Service not found.");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
        if (characteristic == null) {
            Log.w(TAG, "characteristic not found.");
            return false;
        }
        boolean result = bluetoothGatt.readCharacteristic(characteristic);
        Log.d(TAG, "Read charac uuid = " + characteristic.getUuid().toString() + ", result = " + result);

        return result;
    }

    public boolean writeRemoteCharacteristic(String address, String serviceUuid, String characteristicUuid, byte[] value) {
        Log.d(TAG, "writeRemoteCharacteristic add = " + address + ", svc = " + serviceUuid + ", char = " + characteristicUuid);
        if (StringUtils.isEmptyString(address) ||
                StringUtils.isEmptyString(serviceUuid) || StringUtils.isEmptyString(characteristicUuid)) {
            Log.w(TAG, "Unknown parameter");
            return false;
        }
        if (value == null) {
            Log.w(TAG, "value is empty");
            return false;
        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);

        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (service == null) {
            Log.w(TAG, "Service not found.");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
        if (characteristic == null) {
            Log.w(TAG, "characteristic not found.");
            return false;
        }

        characteristic.setValue(value);
        boolean result = bluetoothGatt.writeCharacteristic(characteristic);
        Log.d(TAG, "Write charac uuid = " + characteristic.getUuid().toString() + ", result = " + result);

        return result;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param enabled If true, enable notification.  False otherwise.
     */
    public boolean setCharacteristicNotification(String address,
                                              String serviceUuid,
                                              String characteristicUuid,
                                              boolean enabled) {
        Log.d(TAG, "writeRemoteCharacteristic add = " + address + ", svc = " + serviceUuid + ", char = " + characteristicUuid);
        if (StringUtils.isEmptyString(address) ||
                StringUtils.isEmptyString(serviceUuid) || StringUtils.isEmptyString(characteristicUuid)) {
            Log.w(TAG, "Unknown parameter");
            return false;
        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);
        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUuid));
        if (service == null) {
            Log.w(TAG, "Service not found.");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
        if (characteristic == null) {
            Log.w(TAG, "characteristic not found.");
            return false;
        }

        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        final int charaProp = characteristic.getProperties();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                Log.d(TAG, ">>>> ENABLE_NOTIFICATION_VALUE : " + characteristic.getUuid().toString());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);

                return true;
            } else {
                return false;
            }
        } else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                Log.d(TAG, ">>>> ENABLE_INDICATION_VALUE : " + characteristic.getUuid().toString());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void readClientCharacteristicConfig(String address,
                                               BluetoothGattCharacteristic characteristic) {
        if (StringUtils.isEmptyString(address)) {
            Log.w(TAG, "Unknown address");
        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        boolean readDescriptorResult = bluetoothGatt.readDescriptor(descriptor);
        Log.d(TAG, "readDescriptorResult = " + readDescriptorResult);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {
        if (StringUtils.isEmptyString(address)) {
            Log.w(TAG, "Unknown address");
        }
        BluetoothGatt bluetoothGatt = mConnectedBluetoothGattMap.get(address);
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }

    public void addGattServerService(BluetoothGattService service) {
        mBluetoothGattServer.addService(service);
    }

    /**
     * BLE scan
     */
    private BluetoothAdapter mBluetoothAdapter;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    // 스마트밴드 연동을 위해서.. 스마트밴드는 이벤트가 발생했을 경우에 2-3분간만 broadcast 를 보낸다.
    private static long SCAN_PERIOD = 5000;
    private static long SCAN_WAIT_EMPTY_RETRY_PERIOD = 10000;
    private static long SCAN_WAIT_PERIOD = 10000;
    private static long SCAN_WAIT_UNPLUGGED_PERIOD = 60000;

    private static final int HANDLER_MSG_EXPIRED_SCAN_PERIOD = 1000;
    private static final int HANDLER_MSG_EXPIRED_SCAN_WAIT_PERIOD = HANDLER_MSG_EXPIRED_SCAN_PERIOD + 1;

    private BleServiceHandler mBleServiceHandler = new BleServiceHandler(this);
    // 핸들러 객체 만들기
    private static class BleServiceHandler extends Handler {
        private final WeakReference<BluetoothLeService> mService;

        public BleServiceHandler(BluetoothLeService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            BluetoothLeService service = mService.get();
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
            case HANDLER_MSG_EXPIRED_SCAN_PERIOD:
                mScanedList = new HashMap<>(mTempScanedList);
                mTempScanedList.clear();
                scanLeDevicePeriodically(false);
                broadcastDeviceListUpdate();
                break;
            case HANDLER_MSG_EXPIRED_SCAN_WAIT_PERIOD:
                scanLeDevicePeriodically(true);
                break;
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    @SuppressLint("NewApi")
    public IoTResultCodes initialize() {

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
            return IoTResultCodes.BLE_NOT_SUPPORTED;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return IoTResultCodes.BLUETOOTH_NOT_SUPPORTED;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            //finish();
            return IoTResultCodes.BLUETOOTH_NOT_SUPPORTED;
        }
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            return IoTResultCodes.BLUETOOTH_NOT_ENABLED;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLeScanLollipopCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    //super.onScanResult(callbackType, result);

                    try {
                        BluetoothDevice btDevice = result.getDevice();
                        byte[] scanRecord = result.getScanRecord().getBytes();
                        final HashMap<Integer, AdRecord> adRecords = AdRecord.parseScanRecord(scanRecord);

                        IoTDevice iotDevice = new IoTDevice();
                        iotDevice.setDeviceId(btDevice.getAddress());
                        iotDevice.setDeviceName(btDevice.getName());
                        iotDevice.setDeviceType(IoTDevice.DEVICE_TYPE_STRING_BT);
                        iotDevice.setAdRecordHashMap(adRecords);

                        /**
                         * UUID 가 없는것은 무시한다.
                         */
                        ArrayList<String> scanedUuids = DataParser.getUuids(iotDevice.getAdRecordHashMap());
                        if (scanedUuids == null || scanedUuids.size() == 0) {
                            Log.e(TAG, ">>> xx device name " + iotDevice.getDeviceName() + " has no uuid advertisement");
                        } else {
                            mTempScanedList.put(iotDevice.getDeviceId(), iotDevice);
                            //printScanDevices(btDevice, adRecords);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    //super.onBatchScanResults(results);
                    Log.d(TAG, "mScanCallback.. onBatchScanResults");
                }

                @Override
                public void onScanFailed(int errorCode) {
                    //super.onScanFailed(errorCode);
                    Log.d(TAG, "mScanCallback.. onScanFailed");
                }
            };
        } else {
            mLeScanKitkatCallback = new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d(TAG, ">> mLeScanKitkatCallback..");

                    try {
                        final HashMap<Integer, AdRecord> adRecords = AdRecord.parseScanRecord(scanRecord);
                        IoTDevice iotDevice = new IoTDevice();
                        iotDevice.setDeviceId(device.getAddress());
                        iotDevice.setDeviceName(device.getName());
                        iotDevice.setDeviceType(IoTDevice.DEVICE_TYPE_STRING_BT);
                        iotDevice.setAdRecordHashMap(adRecords);

                        /**
                         * UUID 가 없는것은 무시한다.
                         */
                        ArrayList<String> scanedUuids = DataParser.getUuids(iotDevice.getAdRecordHashMap());
                        if (scanedUuids == null || scanedUuids.size() == 0) {
                            Log.e(TAG, ">>> xx device name " + iotDevice.getDeviceName() + " has no uuid advertisement");
                        } else {
                            mTempScanedList.put(iotDevice.getDeviceId(), iotDevice);
                            //printScanDevices(device, adRecords);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        return IoTResultCodes.SUCCESS;
    }

    public void scanLeDevicePeriodically(final boolean enable) {
        scanLeDevicePeriodically(enable, true);
    }
    public void scanLeDevicePeriodically(final boolean enable, final boolean setPeriod) {
        Log.d(TAG, "scanLeDevicePeriodically enabled = " + enable);
        /**
         * You have to start a scan for Classic Bluetooth devices with startDiscovery() and a scan for Bluetooth LE devices with startLeScan().
         * Caution: Performing device discovery is a heavy procedure for the Bluetooth adapter and will consume a lot of its resources.

         * Additional : On LG Nexus 4 with Android 4.4.2 startDiscovery() finds Bluetooth LE devices.
         *       On Samsung Galaxy S3 with Android 4.3 startDiscovery() doesn't find Bluetooth LE devices.
         */
        if (enable) {
            mBleServiceHandler.removeMessages(HANDLER_MSG_EXPIRED_SCAN_PERIOD);
            mBleServiceHandler.removeMessages(HANDLER_MSG_EXPIRED_SCAN_WAIT_PERIOD);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
                if (leScanner != null) {
                    leScanner.startScan(Collections.<ScanFilter>emptyList(),
                            new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), mLeScanLollipopCallback);

                }
            } else {
                // deprecated api 21.
                mBluetoothAdapter.startLeScan(mLeScanKitkatCallback);
            }
            // Stops scanning after a pre-defined scan period.

            long scanTime = SCAN_PERIOD;
            if (!mIsBatteryPlugged) {
                scanTime *= 2;         // default = 10 sec, unplugged = 20sec
            }
            Log.d(TAG, "Set.. scan time ms = " + scanTime);
            mBleServiceHandler.sendEmptyMessageDelayed(HANDLER_MSG_EXPIRED_SCAN_PERIOD, scanTime);
            mIsLeScanning = true;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
                if (leScanner != null) {
                    leScanner.stopScan(mLeScanLollipopCallback);

                }
            } else {
                // deprecated api 21.
                mBluetoothAdapter.stopLeScan(mLeScanKitkatCallback);
            }
            mBleServiceHandler.removeMessages(HANDLER_MSG_EXPIRED_SCAN_PERIOD);
            mBleServiceHandler.removeMessages(HANDLER_MSG_EXPIRED_SCAN_WAIT_PERIOD);

            long waitTime = SCAN_WAIT_PERIOD;
            if (!mIsBatteryPlugged) {
                waitTime = SCAN_WAIT_UNPLUGGED_PERIOD;         // default = 290 sec, unplugged = 약 30분.
            }

            if (setPeriod) {
                Log.d(TAG, "Set.. scan wait time ms = " + waitTime);
                mBleServiceHandler.sendEmptyMessageDelayed(HANDLER_MSG_EXPIRED_SCAN_WAIT_PERIOD, waitTime);
            }
            mIsLeScanning = false;
        }
    }

    // Device scan callback.
    // use API 18 ~ 20
    private BluetoothAdapter.LeScanCallback mLeScanKitkatCallback = null;

    // Device scan callback.
    // use API 21 ~
    @SuppressLint("NewApi")
    private ScanCallback mLeScanLollipopCallback = null;

    /**
     * for log.
     * @param device
     * @param adRecords
     */
    private void printScanDevices(BluetoothDevice device, HashMap<Integer, AdRecord> adRecords) {
        Log.d(TAG, "onLeScan() =============================================");
        Log.d(TAG, "onLeScan: uuid:" + (device.getUuids() != null ? device.getUuids().toString() : "null") + ", name = " + device.getName());
        Log.d(TAG, "onLeScan: address:" + device.getAddress());
        Log.d(TAG, "onLeScan: bluetooth class:" + device.getBluetoothClass());
        Log.d(TAG, "onLeScan: type:" + device.getType());

        String str = "";
        byte[] values;

        for (Map.Entry<Integer, AdRecord> entry : adRecords.entrySet()) {
            Integer type = entry.getKey();
            AdRecord adRecord = entry.getValue();

            if (adRecord != null) {
                switch (type) {
                    case AdRecord.TYPE_FLAGS :
                        int flags = adRecord.getValue()[0] & 0x0FF;
                        str = "";
                        if ( (flags & 0x01) > 0 ) { str += "'LE Limited Discoverable Mode' "; }
                        if ( (flags & (0x01 << 1)) > 0 ) { str += "'LE General Discoverable Mode' "; }
                        if ( (flags & (0x01 << 2)) > 0 ) { str += "'BR/EDR Not Supported' "; }
                        if ( (flags & (0x01 << 3)) > 0 ) { str += "'Simultaneous LE and BR/EDR to Same Device Capacble (Controller)' "; }
                        if ( (flags & (0x01 << 4)) > 0 ) { str += "'Simultaneous LE and BR/EDR to Same Device Capacble (Host)' "; }

                        Log.d(TAG, "onLeScan: TYPE_FLAGS = " + str);
                        break;

                    case AdRecord.TYPE_UUID16_INC :
                    case AdRecord.TYPE_UUID16 : {
                        ArrayList<String> uuids = DataParser.getUint16StringArray(adRecord.getValue());
                        int i = 0;
                        for (String uuid : uuids) {
                            Log.d(TAG, "onLeScan: TYPE_UUID16(_INC)[" + (++i) + "] = " + uuid);
                        }
                        break;
                    }
                    case AdRecord.TYPE_UUID32_INC :
                    case AdRecord.TYPE_UUID32 : {
                        ArrayList<String> uuids = DataParser.getUint32StringArray(adRecord.getValue());
                        int i = 0;
                        for (String uuid : uuids) {
                            Log.d(TAG, "onLeScan: TYPE_UUID32(_INC)[" + (++i) + "] = " + uuid);
                        }
                        break;
                    }

                    case AdRecord.TYPE_UUID128_INC :
                    case AdRecord.TYPE_UUID128 : {
                        ArrayList<String> uuids = DataParser.getUint128StringArray(adRecord.getValue());
                        int i = 0;
                        for (String uuid : uuids) {
                            Log.d(TAG, "onLeScan: TYPE_UUID128(_INC)[" + (++i) + "] = " + uuid);
                        }
                        break;
                    }

                    case AdRecord.TYPE_NAME_SHORT :
                        str = DataParser.getString(adRecord.getValue());
                        Log.d(TAG, "onLeScan: TYPE_NAME_SHORT = " + str);
                        break;

                    case AdRecord.TYPE_NAME :
                        str = DataParser.getString(adRecord.getValue());
                        Log.d(TAG, "onLeScan: TYPE_NAME = " + str);
                        break;

                    case AdRecord.TYPE_TRANSMITPOWER :
                        Log.d(TAG, "onLeScan: TYPE_TRANSMITPOWER = " + DataParser.getInt8(adRecord.getValue()[0]));
                        break;

                    case AdRecord.TYPE_SERVICEDATA :
                        values = adRecord.getValue();
                        String uuid = DataParser.getUint16String(Arrays.copyOfRange(values, 0, 2));
                        Log.d(TAG, "onLeScan: TYPE_SERVICEDATA uuid = " + uuid);
                        str = DataParser.getHexString(Arrays.copyOfRange(values, 2, values.length));
                        Log.d(TAG, "onLeScan: TYPE_SERVICEDATA hexstringdata = " + str);
                        break;

                    case AdRecord.TYPE_APPEARANCE :
                        str = DataParser.getUint16String(adRecord.getValue());
                        Log.d(TAG, "onLeScan: TYPE_APPEARANCE = " + str);
                        break;

                    case AdRecord.TYPE_VENDOR_SPECIFIC :
                        values = adRecord.getValue();
                        // https://www.bluetooth.org/en-us/specification/assigned-numbers/company-identifiers
                        str = DataParser.getUint16String(Arrays.copyOfRange(values, 0, 2));
                        Log.d(TAG, "onLeScan: TYPE_VENDOR_SPECIFIC company = " + str);
                        if ("004C".equals(str)) { // Apple Inc
                            int offset = 2;
                            int data_type = values[offset++];
                            int data_length = values[offset++];
                            if (data_type == 0x02) { // iBeacon
                                // https://www.uncinc.nl/nl/blog/finding-out-the-ibeacons-specifications
                                // http://www.warski.org/blog/2014/01/how-ibeacons-work/
                                // http://developer.iotdesignshop.com/tutorials/bluetooth-le-and-ibeacon-primer/

//                                            String uuid = parseUUID(this.parseHex(Arrays.copyOfRange(value, offset, offset + 16), true));
//                                            offset += 16;
//                                            ad.apple.ibeacon.major = parseHex(Arrays.copyOfRange(value, offset, offset + 2), true);
//                                            offset += 2;
//                                            ad.apple.ibeacon.minor = parseHex(Arrays.copyOfRange(value, offset, offset + 2), true);
//                                            offset += 2;
//                                            ad.tx_power = this.parseSignedNumber(value[offset]);
                            } else {
//                                            ad.apple.vendor = this.parseHex(Arrays.copyOfRange(value, offset - 2, offset + data_length), true);
                            }
                        }
                        else {
//                                        ad.vendor = this.parseHex(Arrays.copyOfRange(value, i, i + len - 1), true);
                        }
                        break;

                }
            }
        }

        Log.d(TAG, "=============================================");
    }

    /**
     * battery plugged broadcast receiver
     */
    private static boolean mIsBatteryPlugged = false;
    private static boolean mIsLeScanning = false;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mIsBatteryPlugged = DeviceUtils.isPlugged(this);
        Log.d(TAG, "onCreate() battery plugged = " + mIsBatteryPlugged);

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);
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
        unregisterReceiver(mBroadcastReceiver);
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean isPlugged= false;
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    isPlugged = isPlugged || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
                }

                if (mIsBatteryPlugged != isPlugged) {
                    mIsBatteryPlugged = isPlugged;
                    Log.d(TAG, "ACTION_BATTERY_CHANGED = " + mIsBatteryPlugged);

                    // 배터리 충전중인 상태가 되면. scanning이 아니라면 scanning을 한번 해준다.
                    // 최대 40분이상 waiting 타임이므로.. 충전상태일때의 시간텀을 가지도록.
                    // 배터리 충전중인 상태가 아니라면 그대로 둔다.
                    if (mIsBatteryPlugged && !mIsLeScanning) {
                        scanLeDevicePeriodically(false);
                        scanLeDevicePeriodically(true);
                    }
                }
            }
        }
    };
}



