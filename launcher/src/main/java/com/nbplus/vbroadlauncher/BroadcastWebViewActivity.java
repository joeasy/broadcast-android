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

package com.nbplus.vbroadlauncher;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nbplus.iotlib.IoTInterface;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.media.MusicRetriever;
import com.nbplus.media.MusicService;
import com.nbplus.vbroadlauncher.data.BaseApiResult;
import com.nbplus.vbroadlauncher.data.IoTDevicesData;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
import com.nbplus.vbroadlauncher.hybrid.BroadcastWebViewClient;
import com.nbplus.vbroadlauncher.service.SendIoTDeviceListTask;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.DisplayUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


/**
 * Created by basagee on 2015. 5. 28..
 */
public class BroadcastWebViewActivity extends BaseActivity {
    private static final String TAG = BroadcastWebViewActivity.class.getSimpleName();

    BroadcastWebViewClient mWebViewClient;
    ShortcutData mShortcutData;
    Gson mGson;

    private static final int HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE = 1001;
    private static final int HANDLER_MESSAGE_UPDATE_IOT_DEVICE_LIST = 1002;

    private final BroadcastWebViewActivityHandler mHandler = new BroadcastWebViewActivityHandler(this);

    // 핸들러 객체 만들기
    private static class BroadcastWebViewActivityHandler extends Handler {
        private final WeakReference<BroadcastWebViewActivity> mActivity;

        public BroadcastWebViewActivityHandler(BroadcastWebViewActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            BroadcastWebViewActivity activity = mActivity.get();
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
            case HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE :
                if (mWebViewClient != null && !mWebViewClient.isClosingByWebApp()) {
                    mWebViewClient.onCloseWebApplicationByUser();
                }
                break;
            case HANDLER_MESSAGE_UPDATE_IOT_DEVICE_LIST :
                VBroadcastServer serverInfo = LauncherSettings.getInstance(this).getServerInformation();
                if (serverInfo == null || StringUtils.isEmptyString(serverInfo.getApiServer())) {
                    Log.e(TAG, "API server domain is not found !!!");
                    return;
                }
                SendIoTDeviceListTask task = new SendIoTDeviceListTask();
                if (task != null) {
                    Bundle extras = msg.getData();
                    IoTDevicesData data = extras.getParcelable("data");

                    task.setBroadcastApiData(this, mHandler, serverInfo.getApiServer() + Constants.API_IOT_UPDATE_DEVICE_LIST, data);
                    task.execute();
                }
                break;
            case Constants.HANDLER_MESSAGE_SEND_IOT_DEVICE_LIST_COMPLETE_TASK :
                BaseApiResult result = (BaseApiResult)msg.obj;
                Log.d(TAG, "Device list reg result = " + result.getResultCode());
                if (result != null) {
                    if (!StringUtils.isEmptyString(result.getResultCode())) {
                        Bundle b = (Bundle)result.getObject();
                        IoTDevicesData data = b.getParcelable("data");
                        IoTInterface.getInstance().updateBondedWithServerDeviceList(data.getIotDevices());
                        mWebViewClient.onUpdateIoTDevices(result.getResultCode());
                    } else {
                        mWebViewClient.onUpdateIoTDevices("1000");      // Open API 코드참조.
                    }
                }
                break;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = (intent == null) ? "" : intent.getAction();
            Log.d(TAG, ">> mBroadcastReceiver action received = " + action);
            // send handler message
            switch (action) {
                case Constants.ACTION_BROWSER_ACTIVITY_CLOSE :
                    mHandler.sendEmptyMessage(HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE);
                    break;
                case Constants.ACTION_IOT_DEVICE_LIST :
                    if (mWebViewClient == null || !mWebViewClient.isUpdateIoTDevices()) {
                        return;
                    }

                    boolean isCanceled = intent.getBooleanExtra(Constants.EXTRA_IOT_DEVICE_CANCELED, false);
                    if (isCanceled) {           // 사용자가 업데이트를 취소한 경우
                        Log.d(TAG, "User cancel... use last updated..");
//                        Message msg = new Message();
//                        msg.what = Constants.HANDLER_MESSAGE_SEND_IOT_DEVICE_LIST_COMPLETE_TASK;
//                        BaseApiResult result = new BaseApiResult();
//                        result.setResultCode("1000");
//                        msg.obj = result;
//                        mHandler.sendMessage(msg);
                        return;
                    }
                    ArrayList<IoTDevice> iotDevicesList = intent.getParcelableArrayListExtra(Constants.EXTRA_DATA);
                    if (iotDevicesList == null) {
                        iotDevicesList = new ArrayList<>();
                    }

                    if (iotDevicesList.size() == 0) {
                        Log.d(TAG, "No device found.. Do not anything...");
                        return;
                    }

                    IoTDevicesData data = new IoTDevicesData();
                    data.setDeviceId(LauncherSettings.getInstance(context).getDeviceID());
                    data.setIotDevices(iotDevicesList);

                    try {
                        Message msg = new Message();
                        msg.what = HANDLER_MESSAGE_UPDATE_IOT_DEVICE_LIST;
                        Bundle extras = new Bundle();
                        extras.putParcelable("data", data);
                        msg.setData(extras);
                        mHandler.sendMessage(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                default :
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_broadcast_webview);

        Log.d(TAG, "BroadcastWebViewActivity onCreate()");
        WebView webView = (WebView)findViewById(R.id.webview);
        mWebViewClient = new BroadcastWebViewClient(this, webView);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_BROWSER_ACTIVITY_CLOSE);
        filter.addAction(Constants.ACTION_IOT_DEVICE_LIST);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);

        Intent i = getIntent();
        String url = null;
        if (i != null && Constants.ACTION_SHOW_NOTIFICATION_CONTENTS.equals(getIntent().getAction())) {
            url = getIntent().getStringExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS);
        } else {
            mShortcutData = i.getParcelableExtra(Constants.EXTRA_NAME_SHORTCUT_DATA);
            url = mShortcutData.getDomain() + mShortcutData.getPath();
        }

        //url="http://183.98.53.165:8010/web_test/audio_autoplay.html";
        if (StringUtils.isEmptyString(url) || !Patterns.WEB_URL.matcher(url).matches()) {
            Log.e(TAG, "Wrong url ....");
            new AlertDialog.Builder(this).setMessage(R.string.alert_wrong_page_url)
                    //.setTitle(R.string.alert_network_title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.alert_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finishActivity();
                                }
                            })
                    .show();
            return;
        } else {
            mWebViewClient.loadWebUrl(url);
        }
        Log.d(TAG, "start URL = " + url);
        setContentViewByOrientation();
    }

    /**
     * Handle onNewIntent() to inform the fragment manager that the
     * state is not saved.  If you are handling new intents and may be
     * making changes to the fragment state, you want to be sure to call
     * through to the super-class here first.  Otherwise, if your state
     * is saved but the activity is not stopped, you could get an
     * onNewIntent() call which happens before onResume() and trying to
     * perform fragment operations at that point will throw IllegalStateException
     * because the fragment manager thinks the state is still saved.
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "BroadcastWebViewActivity onNewIntent()");
        if (mWebViewClient == null) {
            Log.e(TAG, "mWebViewClient is null..");
            return;
        }

        if (mWebViewClient != null) {
            Log.d(TAG, "Prev url is = " + mWebViewClient.getWebView().getUrl());
        }
        String url = null;
        if (intent != null && Constants.ACTION_SHOW_NOTIFICATION_CONTENTS.equals(getIntent().getAction())) {
            url = intent.getStringExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS);
        } else {
            mShortcutData = intent.getParcelableExtra(Constants.EXTRA_NAME_SHORTCUT_DATA);
            url = mShortcutData.getDomain() + mShortcutData.getPath();
        }

        mWebViewClient.stopPageLoading();
        Log.d(TAG, ">> reset url = " + url);
        if (StringUtils.isEmptyString(url) || !Patterns.WEB_URL.matcher(url).matches()) {
            Log.e(TAG, "Wrong url ....");
            new AlertDialog.Builder(this).setMessage(R.string.alert_wrong_page_url)
                    //.setTitle(R.string.alert_network_title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.alert_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finishActivity();
                                }
                            })
                    .show();
        } else {
            mWebViewClient.loadWebUrl(url);
        }
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (mWebViewClient != null) {
            mWebViewClient.onBackPressed();
        }
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        // 2016.02.23
        // 홈키를눌러서 백그라운드로 갈때... 종료시켜주자.
        if (mWebViewClient != null) {
            mWebViewClient.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "BroadcastWebViewActivity onConfigurationChanged()");
        setContentViewByOrientation();
    }

    private void setContentViewByOrientation() {
        int wallpapereId = LauncherSettings.getInstance(this).getWallpaperId();
        int orientation = DisplayUtils.getScreenOrientation(this);
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            mWebViewClient.setBackgroundResource(LauncherSettings.landWallpaperResource[wallpapereId]);
        } else {
            mWebViewClient.setBackgroundResource(LauncherSettings.portWallpaperResource[wallpapereId]);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "BroadcastWebViewActivity called onStop()");
    }

    public void getText2SpeechObject(OnText2SpeechListener l) {
        this.mcheckText2SpeechLister = l;

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, Constants.START_ACTIVITY_REQUEST_CHECK_TTS_DATA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, ">>> onActivityResult...");
        switch (requestCode) {
            case Constants.START_ACTIVITY_REQUEST_CHECK_TTS_DATA :
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    // check korean
                    mText2Speech = new TextToSpeech(this, this);
                } else {
                    Log.d(TAG, "여기서 제대로 설정안했다면 관두자.... 사용자 맘인데...");
                    showText2SpeechAlertDialog();

                    LauncherSettings.getInstance(this).setIsCheckedTTSEngine(true);
                }
                break;
            case Constants.START_ACTIVITY_REQUEST_ENABLE_BT:
                if (mWebViewClient.getUpdateIoTDevicesDialogFragment() != null) {
                    mWebViewClient.getUpdateIoTDevicesDialogFragment().onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
    }

    /**
     * 자체적으로 블루투스를 연동하는 경우, 블루투스 enable 여부를 체크하여 사용자가 켜도록 한다.
     */
//    public void checkBluetoothEnabled() {
//        // Use this check to determine whether BLE is supported on the device.  Then you can
//        // selectively disable BLE-related features.
//        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            showBluetoothAlertDialog(R.string.ble_not_supported);
//            return;
//        }
//
//        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
//        // BluetoothAdapter through BluetoothManager.
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
//
//        // Checks if Bluetooth is supported on the device.
//        if (bluetoothAdapter == null) {
//            showBluetoothAlertDialog(R.string.error_bluetooth_not_supported);
//            return;
//        }
//
//        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
//        // fire an intent to display a dialog asking the user to grant permission to enable it.
//        if (!bluetoothAdapter.isEnabled()) {
//            if (!bluetoothAdapter.isEnabled()) {
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, Constants.START_ACTIVITY_REQUEST_ENABLE_BT);
//            }
//        } else {
//            mWebViewClient.showUpdateIoTDevicesDialog();
//        }
//    }

    private void showBluetoothAlertDialog(int msgId) {
        new AlertDialog.Builder(this).setMessage(R.string.alert_network_message)
                //.setTitle(R.string.alert_network_title)
                .setCancelable(true)
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mWebViewClient.onUpdateIoTDevices("0997");
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    public void finishActivity() {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        Log.d(TAG, "BroadcastWebViewActivity onDestroy()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        if (mText2Speech != null) {
            mText2Speech.shutdown();
        }

        if (mWebViewClient != null) {
            mWebViewClient.cancelUpdateIoTDevices();
            mWebViewClient = null;
        }
        mText2Speech = null;

        finish();
    }

    public void dismissUpdateIoTDevicesDialog() {
        mWebViewClient.dismissUpdateIoTDevicesDialog();
    }

}
