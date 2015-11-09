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

package com.nbplus.vbroadlauncher.hybrid;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nbplus.hybrid.BasicWebViewClient;
import com.nbplus.media.MusicService;
import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.push.PushService;
import com.nbplus.push.data.PushConstants;
import com.nbplus.vbroadlauncher.BaseActivity;
import com.nbplus.vbroadlauncher.BroadcastWebViewActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.IoTDevicesData;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RegSettingData;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
import com.nbplus.vbroadlauncher.fragment.LoadIoTDevicesDialogFragment;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;

/**
 * Created by basagee on 2015. 5. 19..
 */
public class BroadcastWebViewClient extends BasicWebViewClient implements TextToSpeechHandler.OnUtteranceProgressListener {
    private static final String TAG = BroadcastWebViewClient.class.getSimpleName();

    enum BroadcastPlayState {
        STOPPED,
        VOICE_PLAYING,
        VOICE_PAUSED,
        TTS_PLAYING
    };
    BroadcastPlayState mBroadcastPlayState = BroadcastPlayState.STOPPED;
    TextToSpeechHandler mText2SpeechHandler = null;
    String mText2SpeechPlayText = null;

    // update iot device dialog
    LoadIoTDevicesDialogFragment mLoadIoTDevicesDialogFragment;

    String mIoTDiscoveringUrl = null;

    public boolean isClosingByWebApp() {
        return mIsClosingByWebApp;
    }

    private boolean mIsClosingByWebApp = false;

    private static final int HANDLER_MESSAGE_START_TTS = 1;
    private static final int HANDLER_MESSAGE_DONE_TTS = 2;
    private static final int HANDLER_MESSAGE_ERROR_TTS = 3;
    private BroadcastWebViewClientHandler mHandler;

    private boolean mIsRadioPauseByWeb = false;

    // 핸들러 객체 만들기
    private static class BroadcastWebViewClientHandler extends Handler {
        private final WeakReference<BroadcastWebViewClient> mActivity;

        public BroadcastWebViewClientHandler(BroadcastWebViewClient client) {
            mActivity = new WeakReference<>(client);
        }

        @Override
        public void handleMessage(Message msg) {
            BroadcastWebViewClient client = mActivity.get();
            if (client != null) {
                client.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        Log.d(TAG, "handleMessage tts = " + msg.what);
        switch (msg.what) {
            case HANDLER_MESSAGE_START_TTS :
                dismissProgressDialog();
                mBroadcastPlayState = BroadcastPlayState.TTS_PLAYING;
                break;
            case HANDLER_MESSAGE_DONE_TTS :
                onCompleteTTSBroadcast();
                mBroadcastPlayState = BroadcastPlayState.STOPPED;
                break;
            case HANDLER_MESSAGE_ERROR_TTS :
                dismissProgressDialog();
                mBroadcastPlayState = BroadcastPlayState.STOPPED;
                onCompleteTTSBroadcast();
        }
    }

    public BroadcastWebViewClient(Activity activity, WebView view) {
        super(activity, view, activity.getString(R.string.app_name), activity.getString(R.string.app_name));
        mWebView.setWebViewClient(this);
        mWebView.addJavascriptInterface(this, JAVASCRIPT_IF_NAME);
        mHandler = new BroadcastWebViewClientHandler(this);
    }

    /**
     * 디바이스의 UUID 조회. mac address 기반 40bytes SHA-1 value
     */
    @Override
    @JavascriptInterface
    public String getDeviceId() {
        return LauncherSettings.getInstance(mContext).getDeviceID();
    }

    @JavascriptInterface
    public void setVillageName(String villageName) {
        Log.d(TAG, "setVillageName... = " + villageName);
        if (!StringUtils.isEmptyString(villageName)) {
            LauncherSettings.getInstance(mContext).setVillageName(villageName);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(Constants.ACTION_SET_VILLAGE_NAME));
        }
    }
    /**
     *
     * @param data
     */
    @JavascriptInterface
    public void setServerInformation(String data) {
        Log.d(TAG, ">> setServerInfomation() received = " + data);

        if (StringUtils.isEmptyString(data)) {
            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
        } else {
            try {
                data = new String(data.getBytes("utf-8"));
                Gson gson = new GsonBuilder().create();
                RegSettingData settings = gson.fromJson(data, RegSettingData.class);
                if (settings != null) {
                    VBroadcastServer serverInfo = settings.getServerInformation();
                    if (serverInfo == null) {
                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    serverInfo.setInitialServerPage(Constants.VBROAD_INITIAL_PAGE);
                    LauncherSettings.getInstance(mContext).setServerInformation(serverInfo);
                    LauncherSettings.getInstance(mContext).setIsCompletedSetup(true);

                    Intent intent = new Intent(mContext, PushService.class);
                    intent.setAction(PushConstants.ACTION_START_SERVICE);
                    intent.putExtra(PushConstants.EXTRA_START_SERVICE_IFADDRESS, /*settings.getServerInformation()*/serverInfo.getPushInterfaceServer());
                    mContext.startService(intent);
                } else {
                    Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.parse_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     *
     * @param appId
     */
    @JavascriptInterface
    public void registerPushApplication(String appId) {
        Log.d(TAG, ">> registerPushApplication() called = " + appId);
    }

    @JavascriptInterface
    public void onStartBroadcastMediaStream(boolean isTTS, String ttsString) {
        Log.d(TAG, ">> onStartBroadcastMediaStream() called = " + isTTS + ", tts = " + ttsString);
        Intent i = new Intent(mContext, MusicService.class);
        i.setAction(MusicService.ACTION_PAUSE);
        mIsRadioPauseByWeb = true;
        mContext.startService(i);

        if (mBroadcastPlayState == BroadcastPlayState.TTS_PLAYING) {
            // stop previous playing tts.
            if (mText2SpeechHandler != null) {
                mText2SpeechHandler.stop();
            }
        }

        if (isTTS) {
            // start tts playing
            mText2SpeechPlayText = ttsString;
            if (StringUtils.isEmptyString(mText2SpeechPlayText)) {
                Log.e(TAG, "> do not play tts. is empty string..");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                showProgressDialog();
            }
            if (mText2SpeechHandler == null) {
                mText2SpeechHandler = new TextToSpeechHandler(mContext, this);
                ((BaseActivity)mContext).getText2SpeechObject(new BaseActivity.OnText2SpeechListener() {
                    @Override
                    public void onCheckResult(TextToSpeech tts) {
                        if (tts != null && tts instanceof TextToSpeech) {
                            mText2SpeechHandler.setTextToSpeechObject(tts);
                            mText2SpeechHandler.play(mText2SpeechPlayText);
                            mBroadcastPlayState = BroadcastPlayState.TTS_PLAYING;
                        } else {
                            mText2SpeechPlayText = null;
                            mBroadcastPlayState = BroadcastPlayState.STOPPED;
                            dismissProgressDialog();
                        }
                    }
                });
            } else {
                if (mText2SpeechHandler.getTextToSpeechObject() == null) {
                    ((BaseActivity)mContext).getText2SpeechObject(new BaseActivity.OnText2SpeechListener() {
                        @Override
                        public void onCheckResult(TextToSpeech tts) {
                            if (tts != null && tts instanceof TextToSpeech) {
                                mText2SpeechHandler.setTextToSpeechObject(tts);
                                mText2SpeechHandler.play(mText2SpeechPlayText);
                                mBroadcastPlayState = BroadcastPlayState.TTS_PLAYING;
                            } else {
                                mText2SpeechPlayText = null;
                                mBroadcastPlayState = BroadcastPlayState.STOPPED;
                                dismissProgressDialog();
                            }
                        }
                    });
                } else {
                    mText2SpeechHandler.play(mText2SpeechPlayText);
                    mBroadcastPlayState = BroadcastPlayState.TTS_PLAYING;

                }
            }
        }
    }

    @JavascriptInterface
    public void onPauseBroadcastMediaStream() {
        Log.d(TAG, ">> onPauseBroadcastMediaStream() called");
        if (mBroadcastPlayState == BroadcastPlayState.VOICE_PLAYING) {
            mBroadcastPlayState = BroadcastPlayState.VOICE_PAUSED;
        }
    }

    @JavascriptInterface
    public void onStopBroadcastMediaStream() {
        Log.d(TAG, ">> onStopBroadcastMediaStream() called");

        if (mBroadcastPlayState == BroadcastPlayState.TTS_PLAYING) {
            // stop previous playing tts.
            mText2SpeechPlayText = null;
            mText2SpeechHandler.stop();
        }
        mBroadcastPlayState = BroadcastPlayState.STOPPED;
    }

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @Override
    @JavascriptInterface
    public void closeWebApplication() {
        Log.d(TAG, ">> closeWebApplication() called");

        if (mBroadcastPlayState == BroadcastPlayState.TTS_PLAYING) {
            // stop previous playing tts.
            mText2SpeechPlayText = null;
            mText2SpeechHandler.finalize();
        }
        mBroadcastPlayState = BroadcastPlayState.STOPPED;

        if (mIsRadioPauseByWeb) {
            mIsRadioPauseByWeb = false;
            Intent i = new Intent(mContext, MusicService.class);
            i.setAction(MusicService.ACTION_PLAY);
            mContext.startService(i);
            mIsClosingByWebApp = true;
        }
        dismissProgressDialog();
        dismissUpdateIoTDevicesDialog();
        ((BroadcastWebViewActivity)mContext).finishActivity();
    }


    /**
     * GCM 등록
     * @return boolean
     */
    @JavascriptInterface
    public boolean registerGcm() {
        return false;
    }

    /**
     * GCM 해제
     * @return boolean
     */
    @JavascriptInterface
    public boolean unRegisterGcm() {
        return false;
    }

    @JavascriptInterface
    public void updateIoTDevices() {
        Log.d(TAG, "call updateIoTDevices()");
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIoTDiscoveringUrl = mWebView.getUrl();
//                if (com.nbplus.iotlib.data.Constants.USE_IOT_GATEWAY) {
                    showUpdateIoTDevicesDialog();
//                } else {
//                    ((BroadcastWebViewActivity)mContext).checkBluetoothEnabled();
//                }
            }
        });
    }

    ////////////////////////////////
    /**
     * 아래의 함수들은 자바스크립트를 Native 에서 호출할 필요가 있을때 사용한다.
     * 아래에서 불리는 자바스크립트 function 들은 웹앱에서 구현이 되어 있어야 한다.
     *
     */
    /**
     * GCM 등록 토큰 전달
     * @param gcmRegToken 토큰
     */
    public void onRegistered(String gcmRegToken) {
        // do not anything
        //mWebView.loadUrl("javascript:window.onRegistered(" + gcmRegToken + ");");
    }

    /**
     * GCM 해제 결과
     */
    public void onUnRegistered() {
        // do not anything
        //mWebView.loadUrl("javascript:window.onUnRegistered();");
    }

    /**
     * 검색된 IoT device 목록 전달
     * @param result device list
     */
    public void onUpdateIoTDevices(String result) {
        dismissProgressDialog();
        Log.d(TAG, "call onUpdateIoTDevices() result = " + result);
        mIoTDiscoveringUrl = null;
        mWebView.loadUrl("javascript:window.onUpdateIoTDevices('" + result + "');");
    }

    public void onCompleteTTSBroadcast() {
        mWebView.loadUrl("javascript:window.onCompleteTTSBroadcast();");
    }

    /**
     * 시나리오변경.. 웹앱에 이벤트만 보내고.. 웹앱에서 처리후 closeWebApplication() 호출한다.
     */
    public void onCloseWebApplicationByUser() {
        if (!mIsClosingByWebApp) {
            mWebView.loadUrl("javascript:window.onCloseWebApplicationByUser();");
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void loadWebUrl(String url) {
        if (url.indexOf("?") > 0) {
            url += ("&UUID=" + LauncherSettings.getInstance(mContext).getDeviceID());
            url += ("&APPID=" + mContext.getPackageName());
        } else {
            url += ("?UUID=" + LauncherSettings.getInstance(mContext).getDeviceID());
            url += ("&APPID=" + mContext.getPackageName());
        }
        mWebView.loadUrl(url);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        this.showProgressDialog();
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @Override
    public void onStart(String s) {
        Log.d(TAG, "TTS onStart()");
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_START_TTS);
    }

    @Override
    public void onDone(String s) {
        Log.d(TAG, "TTS onDone()");
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_DONE_TTS);
    }

    @Override
    public void onError(String utteranceId, int errorCode) {
        Log.d(TAG, "TTS onError()");
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_ERROR_TTS);
    }

    public void stopPageLoading() {
        Log.d(TAG, "stopPageLoading() = ");
        this.dismissProgressDialog();
        if (mWebView != null) {
            mWebView.stopLoading();
        }
    }

    public boolean isUpdateIoTDevices() {
        return !StringUtils.isEmptyString(mIoTDiscoveringUrl);
    }
    public void cancelUpdateIoTDevices() {
        mIoTDiscoveringUrl = null;
    }
    public void showNetworkConnectionAlertDialog() {
        new AlertDialog.Builder(mContext).setMessage(R.string.alert_network_message)
                .setTitle(R.string.alert_network_title)
                .setCancelable(true)
                .setNegativeButton(R.string.alert_network_btn_check_wifi,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mContext.startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                            }
                        })
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }
    // progress bar
    public LoadIoTDevicesDialogFragment getUpdateIoTDevicesDialogFragment() {
        return mLoadIoTDevicesDialogFragment;
    }

    public void showUpdateIoTDevicesDialog() {
//        if (com.nbplus.iotlib.data.Constants.USE_IOT_GATEWAY) {
//            final boolean wifiEnabled = NetworkUtils.isWifiEnabled(mContext);
//            if (wifiEnabled) {
                try {
                    dismissProgressDialog();
                    mLoadIoTDevicesDialogFragment = LoadIoTDevicesDialogFragment.newInstance(null);
                    mLoadIoTDevicesDialogFragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "load_iot_devices_dialog");
                } catch (Exception e) {
                    e.printStackTrace();
                }
//            } else {
//                onUpdateIoTDevices("0997");
//                showNetworkConnectionAlertDialog();
//            }
//        } else {
//            try {
//                dismissProgressDialog();
//                mLoadIoTDevicesDialogFragment = LoadIoTDevicesDialogFragment.newInstance(null);
//                mLoadIoTDevicesDialogFragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "load_iot_devices_dialog");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

    }
    public void dismissUpdateIoTDevicesDialog() {
        try {
            if (mLoadIoTDevicesDialogFragment != null) {
                mLoadIoTDevicesDialogFragment.dismiss();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        mLoadIoTDevicesDialogFragment = null;
    }
}
