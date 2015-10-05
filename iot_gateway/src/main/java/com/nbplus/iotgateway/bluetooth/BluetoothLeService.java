/*
 * Copyright (C) 2013 The Android Open Source Project
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
 */

package com.nbplus.iotgateway.bluetooth;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.nbplus.iotgateway.R;
import com.nbplus.iotgateway.btcharacteristics.GlucoseMeasurement;
import com.nbplus.iotgateway.btcharacteristics.RecordAccessControlPoint;
import com.nbplus.iotgateway.btcharacteristics.WeightMeasurement;
import com.nbplus.iotgateway.data.AdRecord;
import com.nbplus.iotgateway.data.BluetoothDeviceData;
import com.nbplus.iotgateway.data.DataParser;
import com.nbplus.iotgateway.data.GattAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattServer mBluetoothGattServer;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS =
            "com.example.bluetooth.le.ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS";
    public final static String ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS =
            "com.example.bluetooth.le.ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS";

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
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(gatt.getDevice().getAddress(), intentAction);

                close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(gatt.getDevice().getAddress(), ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(gatt.getDevice().getAddress(), ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(gatt.getDevice().getAddress(), ACTION_GATT_CHARACTERISTIC_WRITE_SUCCESS, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(gatt.getDevice().getAddress(), ACTION_DATA_AVAILABLE, characteristic);
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

        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            byte[] descValue = descriptor.getValue();
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

            Log.d(TAG, "onDescriptorWrite() uuid = " + characteristic.getUuid().toString());
            byte byteValue = descValue[0];
            for (int i = 0; i < Byte.SIZE; i++) {
                Log.d(TAG, "byteValue[" + i + "] = " + (byteValue >> i & 0x1));
            }
            broadcastUpdate(gatt.getDevice().getAddress(), ACTION_GATT_DESCRIPTOR_WRITE_SUCCESS, descriptor, status);
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
        sendBroadcast(intent);
    }

    private void broadcastUpdate(String address, final String action, BluetoothGattDescriptor descriptor, int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA_SERVICE_UUID, descriptor.getCharacteristic().getService().getUuid().toString());
        intent.putExtra(EXTRA_DATA_CHARACTERISTIC_UUID, descriptor.getCharacteristic().getUuid().toString());
        intent.putExtra(EXTRA_DATA_STATUS, status);
        sendBroadcast(intent);
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

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            close();
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
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
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG, "Disconnect connection!!");
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.d(TAG, "close ble gatt resources !!");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mBluetoothDeviceAddress = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG, "Read charac uuid = " + characteristic.getUuid().toString() + ", result = " + mBluetoothGatt.readCharacteristic(characteristic));
    }

    public void writeRemoteCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG, "Write charac uuid = " + characteristic.getUuid().toString() + ", result = " + mBluetoothGatt.writeCharacteristic(characteristic));
        byte [] values = characteristic.getValue();
        String str = "";
        // TODO : for log.
        if(values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                str += (String.format("%02x ", values[i]));
            }
        }
        Log.d(TAG, "     value = " + str);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        final int charaProp = characteristic.getProperties();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                Log.d(TAG, ">>>> ENABLE_NOTIFICATION_VALUE : " + characteristic.getUuid().toString());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        } else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                Log.d(TAG, ">>>> ENABLE_INDICATION_VALUE : " + characteristic.getUuid().toString());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
    }

    public void readClientCharacteristicConfig(BluetoothGattCharacteristic characteristic) {
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        boolean readDescriptorResult = mBluetoothGatt.readDescriptor(descriptor);
        Log.d(TAG, "readDescriptorResult = " + readDescriptorResult);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void addGattServerService(BluetoothGattService service)
    {
        mBluetoothGattServer.addService(service);
    }

//    public void addDefinedGattServerServices() {
//        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
//        BluetoothGattService service = new BluetoothGattService(UUID.fromString(GattAttributes.FAN_CONTROL_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);
//        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(GattAttributes.FAN_OPERATING_STATE), BluetoothGattCharacteristic.FORMAT_UINT8, BluetoothGattCharacteristic.PERMISSION_WRITE );
//
//        service.addCharacteristic(characteristic);
//        mBluetoothGattServer.addService(service);
//
//        Log.d(TAG, "Created our own GATT server.\r\n");
//
//    }

    /**
     * BLE scan
     */
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    //private HashMap<BluetoothDevice, BleDevice>

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
            return false;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            //finish();
            return false;
        }

        return true;
    }

    private void scanLeDevice(final boolean enable) {
        /**
         * You have to start a scan for Classic Bluetooth devices with startDiscovery() and a scan for Bluetooth LE devices with startLeScan().
         * Caution: Performing device discovery is a heavy procedure for the Bluetooth adapter and will consume a lot of its resources.

         * Additional : On LG Nexus 4 with Android 4.4.2 startDiscovery() finds Bluetooth LE devices.
         *       On Samsung Galaxy S3 with Android 4.3 startDiscovery() doesn't find Bluetooth LE devices.
         */
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
                        if (leScanner != null) {
                            leScanner.stopScan(mScanCallback);

                        }
                    } else {
                        // deprecated api 21.
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
                if (leScanner != null) {
                    leScanner.startScan(Collections.<ScanFilter>emptyList(),
                            new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), mScanCallback);

                }
            } else {
                // deprecated api 21.
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }

        } else {
            mScanning = false;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
                if (leScanner != null) {
                    leScanner.stopScan(mScanCallback);

                }
            } else {
                // deprecated api 21.
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    // Device scan callback.
    // use API 18 ~ 20
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    final HashMap<Integer, AdRecord> adRecords = AdRecord.parseScanRecord(scanRecord);
                    printScanDevices(device, adRecords);

                    BluetoothDeviceData deviceData = new BluetoothDeviceData();
                    deviceData.setBluetoothDevice(device);
                    deviceData.setAdRecord(adRecords);
                }
            };

    // Device scan callback.
    // use API 21 ~
    @SuppressLint("NewApi")
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //super.onScanResult(callbackType, result);
            Log.d(TAG, "mScanCallback.. onScanResult");
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            //super.onBatchScanResults(results);
            Log.d(TAG, "mScanCallback.. onScanResult");
        }

        @Override
        public void onScanFailed(int errorCode) {
            //super.onScanFailed(errorCode);
            Log.d(TAG, "mScanCallback.. onScanResult");
        }
    };

    private void printScanDevices(BluetoothDevice device, HashMap<Integer, AdRecord> adRecords) {
        /**
         * TODO : for log..
         */
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
                    case AdRecord.TYPE_UUID16 :
                        ArrayList<String> uuids = DataParser.getUint16StringArray(adRecord.getValue());
                        int i = 0;
                        for (String uuid : uuids) {
                            Log.d(TAG, "onLeScan: TYPE_UUID16(_INC)[" + (++i) + "] = " + uuid);
                        }
                        break;

                    case AdRecord.TYPE_UUID32_INC :
                    case AdRecord.TYPE_UUID32 :
                        Log.w(TAG, "not implented.");
                        break;

                    case AdRecord.TYPE_UUID128_INC :
                    case AdRecord.TYPE_UUID128 :
                        str = DataParser.getUuid128String(adRecord.getValue());
                        Log.d(TAG, "onLeScan: TYPE_UUID128(_INC) = " + str);
                        break;

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

}



