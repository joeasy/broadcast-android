/*
 * Copyright (c) 2016. NB Plus (www.nbplus.co.kr)
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

package com.nbplus.vbroadcreator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nbplus.hybrid.BasicWebViewClient;

import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;

/**
 * Created by basagee on 2015. 5. 19..
 */
public class BroadcastWebViewClient extends BasicWebViewClient {
    private static final String TAG = BroadcastWebViewClient.class.getSimpleName();

    boolean mIsClosingByWebApp = false;

    private BroadcastWebViewClientHandler mHandler;

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
        return "";//LauncherSettings.getInstance(mContext).getDeviceID();
    }

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @Override
    @JavascriptInterface
    public void closeWebApplication() {
        Log.d(TAG, ">> closeWebApplication() called");
        mIsClosingByWebApp = true;
        mContext.finish();
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
    public void onCloseWebApplicationByUser() {
        if (!mIsClosingByWebApp) {
            mWebView.loadUrl("javascript:window.onCloseWebApplicationByUser();");
            mContext.finish();
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
        Log.d(TAG, "onPageFinished() = " + url);
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
        /*
        if (url.indexOf("?") > 0) {
            url += ("&UUID=" + LauncherSettings.getInstance(mContext).getDeviceID());
            url += ("&APPID=" + mContext.getPackageName());
        } else {
            url += ("?UUID=" + LauncherSettings.getInstance(mContext).getDeviceID());
            url += ("&APPID=" + mContext.getPackageName());
        }
        */
        mWebView.loadUrl(url);
    }

    @Override
    public void onUpdateIoTDevices(String iotDevices) {

    }

    @Override
    public boolean registerGcm() {
        return false;
    }

    @Override
    public boolean unRegisterGcm() {
        return false;
    }

    @Override
    public void onRegistered(String gcmRegToken) {

    }

    @Override
    public void onUnRegistered() {

    }
}
