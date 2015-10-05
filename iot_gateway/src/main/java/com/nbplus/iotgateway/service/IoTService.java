package com.nbplus.iotgateway.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.nbplus.iotgateway.bluetooth.BluetoothLeService;
import com.nbplus.iotgateway.data.Constants;
import com.nbplus.iotgateway.data.IoTDevice;
import com.nbplus.iotgateway.data.IoTGateway;

import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by basagee on 2015. 8. 6..
 */
public class IoTService extends Service {
    private static final String TAG = IoTService.class.getSimpleName();

    // internal constants
    private static final int HANDLER_MESSAGE_CONNECTIVITY_CHANGED = 1001;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiManager.WifiLock mWifiLock;
    boolean mLastConnectionStatus = false;          // network status
    private BluetoothLeService mBluetoothLeService;

    private IoTServiceHandler mHandler = new IoTServiceHandler(this);
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
                } else {
                    Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED network is disconnected !!!");
                }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(HANDLER_MESSAGE_CONNECTIVITY_CHANGED);
            }
        }

    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                //finish();
                return;
            }
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceData.getBluetoothDevice().getAddress());

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    public void onCreate() {
        Log.i(TAG, "debug: Creating service");

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        // check network status
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);

        mLastConnectionStatus = NetworkUtils.isConnected(this);
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */

    private static int num = 0;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }
        if (intent == null || StringUtils.isEmptyString(action)) {
            action = Constants.ACTION_START_IOT_SERVICE;
        }

        switch (action) {
            case Constants.ACTION_START_IOT_SERVICE :
                break;
            case Constants.ACTION_GET_IOT_DEVICE_LIST :
                // TODO : test code
                long ts = System.currentTimeMillis();
                ArrayList<IoTDevice> testDevices = new ArrayList<>();
                IoTDevice device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("IR");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("IR");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("IR");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("ZW");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("ZW");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("ZW");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("ZW");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("ZW");
                testDevices.add(device);


                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("BT");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("BT");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("BT");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("BT");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("BT");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("BT");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("BT");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (++ts));
                device.setDeviceName(String.format("TEST%02d", (++num)));
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                device.setDeviceType("BT");
                testDevices.add(device);


                Intent sendIntent = new Intent();
                sendIntent.setAction(Constants.ACTION_IOT_DEVICE_LIST);
//                IoTGateway iotGateway = new IoTGateway();
//                sendIntent.putExtra(Constants.EXTRA_IOT_GATEWAY_DATA, iotGateway);
                sendIntent.putParcelableArrayListExtra(Constants.EXTRA_IOT_DEVICE_LIST, testDevices);
                LocalBroadcastManager.getInstance(this).sendBroadcast(sendIntent);
                break;
            case Constants.ACTION_SEND_IOT_COMMAND :
                Log.d(TAG, ">> ACTION_SEND_IOT_COMMAND device id = " + intent.getStringExtra(Constants.EXTRA_IOT_SEND_COMM_DEVICE_ID));
                Log.d(TAG, ">> ACTION_SEND_IOT_COMMAND command = " + intent.getStringExtra(Constants.EXTRA_IOT_SEND_COMM_COMMAND_ID));
                break;
        }

        return START_STICKY; // Means we started the service
        // restart in case it's killed.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
