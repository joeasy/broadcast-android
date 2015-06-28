package com.nbplus.vbroadlistener.hybrid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nbplus.hybrid.BasicWebViewClient;
import com.nbplus.vbroadlistener.ProgressDialogFragment;
import com.nbplus.vbroadlistener.R;
import com.nbplus.vbroadlistener.data.Constants;
import com.nbplus.vbroadlistener.data.RegSettingData;
import com.nbplus.vbroadlistener.data.VBroadcastServer;
import com.nbplus.vbroadlistener.gcm.RegistrationIntentService;
import com.nbplus.vbroadlistener.preference.LauncherSettings;

import org.basdroid.common.StringUtils;

/**
 * Created by basagee on 2015. 5. 19..
 */
public class BroadcastWebViewClient extends BasicWebViewClient {
    private static final String TAG = BroadcastWebViewClient.class.getSimpleName();

    ProgressDialogFragment mProgressDialogFragment;

    public BroadcastWebViewClient(Activity activity, WebView view) {
        super(activity, view);
    }

    /**
     * 원격수신은 device uuid 를 전달하지 않는다.
     */
    @Override
    @JavascriptInterface
    public String getDeviceId() {
        return null;
    }

    /**
     *
     * @param data
     */
    @JavascriptInterface
    public void setApplicationData(String data) {
        Log.d(TAG, ">> setApplicationData() received = " + data);

        if (StringUtils.isEmptyString(data)) {
            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
        } else {
            try {
                Gson gson = new GsonBuilder().create();
                RegSettingData settings = gson.fromJson(data, RegSettingData.class);
                if (settings != null) {
                    if (StringUtils.isEmptyString(settings.getVillageName())) {
                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (settings.getServerInformation() == null) {
                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        VBroadcastServer serverInfo = settings.getServerInformation();
                        if (StringUtils.isEmptyString(serverInfo.getDocServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    LauncherSettings.getInstance(mContext).setVillageCode(settings.getVillageCode());
                    LauncherSettings.getInstance(mContext).setVillageName(settings.getVillageName());
                    LauncherSettings.getInstance(mContext).setServerInformation(settings.getServerInformation());
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
     * GCM 등록
     * @return boolean
     */
    @JavascriptInterface
    public boolean registerGcm() {
        // Start IntentService to register this application with GCM.
        Intent intent = new Intent(mContext, RegistrationIntentService.class);
        intent.setAction(Constants.REGISTER_GCM);
        mContext.startService(intent);
        return true;
    }

    /**
     * GCM 해제
     * @return boolean
     */
    @JavascriptInterface
    public boolean unRegisterGcm() {
        // Start IntentService to register this application with GCM.
        Intent intent = new Intent(mContext, RegistrationIntentService.class);
        intent.setAction(Constants.UNREGISTER_GCM);
        mContext.startService(intent);
        return true;
    }

    /**
     * IoT GW 로부터 IoT Devices  목록을 갱신한다.
     */
    @JavascriptInterface
    public void updateIoTDevices() {

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
        mWebView.loadUrl("javascript:window.onRegistered('" + gcmRegToken + "');");
    }

    /**
     * GCM 해제 결과
     */
    public void onUnRegistered() {
        mWebView.loadUrl("javascript:window.onUnRegistered();");
    }

    /**
     * 검색된 IoT device 목록 전달
     * @param iotDevices device list
     */
    public void onUpdateIoTDevices(String iotDevices) {
        mWebView.loadUrl("javascript:window.onRegistered('" + iotDevices + "');");
    }

    // progress bar
    private void showProgressDialog() {
        dismissProgressDialog();
        mProgressDialogFragment = ProgressDialogFragment.newInstance();
        mProgressDialogFragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "progress_dialog");
    }
    private void dismissProgressDialog() {
        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        this.showProgressDialog();
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        this.dismissProgressDialog();
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
    }
}
