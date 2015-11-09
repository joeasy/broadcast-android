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
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.DialogInterface;
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
import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.push.PushService;
import com.nbplus.push.data.PushConstants;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.fragment.LauncherFragment;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RegSettingData;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;

import org.basdroid.common.StringUtils;

/**
 * Created by basagee on 2015. 5. 19..
 */
public class RegisterWebViewClient extends BasicWebViewClient {
    private static final String TAG = RegisterWebViewClient.class.getSimpleName();

    public RegisterWebViewClient(Activity activity, WebView view) {
        super(activity, view, activity.getString(R.string.app_name), activity.getString(R.string.app_name));
        mWebView.setWebViewClient(this);
        mWebView.addJavascriptInterface(this, JAVASCRIPT_IF_NAME);
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
        Log.d(TAG, ">> setServerInformation() received = " + data);

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
//                    if (StringUtils.isEmptyString(settings.getVillageName())) {
//                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
//                        return;
//                    }
                    /*if (settings.getServerInformation() == null) {
                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                        return;
                    } else*/ {
                        if (StringUtils.isEmptyString(serverInfo.getApiServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (StringUtils.isEmptyString(serverInfo.getDocServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (StringUtils.isEmptyString(serverInfo.getPushInterfaceServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                            return;
                        }
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

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @Override
    @JavascriptInterface
    public void closeWebApplication() {
        Log.d(TAG, ">> closeWebApplication() called");

        if (!LauncherSettings.getInstance(mContext).isCompletedSetup()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            alert.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.setMessage(R.string.alert_settings_message);
            alert.show();

        } else {
            FragmentManager fm = ((AppCompatActivity)mContext).getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();

            LauncherFragment fragment = new LauncherFragment();
            ft.replace(R.id.launcherFragment, fragment);
            ft.commit();
        }
    }

    // not support
    @JavascriptInterface
    public boolean registerGcm() {
        return false;
    }

    // not support
    @JavascriptInterface
    public boolean unRegisterGcm() {
        return false;
    }

    @JavascriptInterface
    public void updateIoTDevices() {

    }
    ////////////////////////////////
    /**
     * 아래의 함수들은 자바스크립트를 Native 에서 호출할 필요가 있을때 사용한다.
     * 아래에서 불리는 자바스크립트 function 들은 웹앱에서 구현이 되어 있어야 한다.
     *
     */
    public void onRegistered(String gcmRegToken) {
        // do not anything
        //mWebView.loadUrl("javascript:window.onRegistered(" + gcmRegToken + ");");
    }

    public void onUnRegistered() {
        // do not anything
        //mWebView.loadUrl("javascript:window.onUnRegistered();");
    }

    public void onUpdateIoTDevices(String iotDevices) {
        mWebView.loadUrl("javascript:window.onUpdateIoTDevices('" + iotDevices + "');");
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
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    public void stopPageLoading() {
        Log.d(TAG, "stopPageLoading() = ");
        this.dismissProgressDialog();
        if (mWebView != null) {
            mWebView.stopLoading();
        }
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
}
