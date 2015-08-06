package com.nbplus.iotgateway.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
                device.setDeviceId("10000000000-100-" + ts);
                device.setDeviceName("TEST01");
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (ts + 1));
                device.setDeviceName("TEST02");
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                testDevices.add(device);

                device = new IoTDevice();
                device.setDeviceId("10000000000-100-" + (ts + 2));
                device.setDeviceName("TEST03");
                device.setDeviceVendor("11");
                device.setDeviceModel("22");
                testDevices.add(device);

                Intent sendIntent = new Intent();
                sendIntent.setAction(Constants.ACTION_IOT_DEVICE_LIST);
                IoTGateway iotGateway = new IoTGateway();
                sendIntent.putExtra(Constants.EXTRA_IOT_GATEWAY_DATA, iotGateway);
                intent.putParcelableArrayListExtra(Constants.EXTRA_IOT_DEVICE_LIST, testDevices);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case Constants.ACTION_SEND_IOT_COMMAND :
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
