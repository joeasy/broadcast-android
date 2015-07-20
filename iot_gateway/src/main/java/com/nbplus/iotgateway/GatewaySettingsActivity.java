package com.nbplus.iotgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.basdroid.common.NetworkUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class GatewaySettingsActivity extends BaseActivity {
    private static final String TAG = GatewaySettingsActivity.class.getSimpleName();

    List<ScanResult> results;
    WifiManager mWifiManager = null;

    private static final int HANDLER_SCAN_RESULTS_AVAILABLE_ACTION = 1001;
    private final GatewaySettingsActivityHandler mHandler = new GatewaySettingsActivityHandler(this);

    // 핸들러 객체 만들기
    private static class GatewaySettingsActivityHandler extends Handler {
        private final WeakReference<GatewaySettingsActivity> mActivity;

        public GatewaySettingsActivityHandler(GatewaySettingsActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            GatewaySettingsActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case HANDLER_SCAN_RESULTS_AVAILABLE_ACTION :
                dismissProgressDialog();
                break;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.d(TAG, ">> mBroadcastReceiver action received = " + action);
            // send handler message
            switch (action) {
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION :
                    Message msg = new Message();
                    msg.what = HANDLER_SCAN_RESULTS_AVAILABLE_ACTION;
                    Bundle b = new Bundle();

                    msg.setData(b);
                    mHandler.sendMessage(msg);
                    break;
                default :
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway_settings);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);

        NetworkUtils.enableWifiNetwork(this);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mWifiManager.startScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gateway_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}
