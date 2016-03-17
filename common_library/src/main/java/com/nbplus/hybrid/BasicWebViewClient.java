/*
 * Copyright (c) 2015. Basagee Yun. (www.basagee.tk)
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

package com.nbplus.hybrid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.nbplus.progress.ProgressDialogFragment;

import org.apache.http.util.EncodingUtils;
import org.basdroid.common.DeviceUtils;
import org.basdroid.common.NetworkUtils;
import org.basdroid.common.PhoneState;
import org.basdroid.common.R;
import org.basdroid.common.StorageUtils;
import org.basdroid.common.StringUtils;

import java.io.File;
import java.util.Arrays;


/**
 * Created by basagee on 2015. 4. 30..
 */
public abstract class BasicWebViewClient extends WebViewClient {
    private static final String TAG = BasicWebViewClient.class.getSimpleName();

    protected static final String JAVASCRIPT_IF_NAME = "nbplus";

    protected WebView mWebView;
    protected Activity mContext;
    protected BroadcastWebChromeClient mWebChromeClient;
    protected boolean mPageLoadSuccess = false;
    protected DownloadManager mDownloadManager;
    protected ProgressDialogFragment mProgressDialogFragment;
    protected String mAlertTitleString;
    protected String mConfirmTitleString;

    /**
     * Created by basagee on 2015. 4. 30..
     */
    class BroadcastWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            // Show a grant or deny dialog to the user
            mContext.runOnUiThread(new Runnable() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    Log.d(TAG, "onPermissionRequest() grant");
                    //if(request.getOrigin().toString().equals("https://apprtc-m.appspot.com/")) {
                        request.grant(request.getResources());
                    //} else {
                    //    request.deny();
                    //}
                }
            });

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                // On accept or deny call
//                request.grant(request.getResources());
//                // or
//                // request.deny();
//            }
        }
        public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result) {
            new AlertDialog.Builder(mContext)
                    .setTitle(mAlertTitleString)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    result.confirm();
                                }
                            })
                    .setCancelable(false)
                    .create()
                    .show();

            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url,
                                   String message, final JsResult result) {

            new AlertDialog.Builder(mContext)
                    .setTitle(mConfirmTitleString)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    result.confirm();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    result.cancel();
                                }
                            })
                    .create()
                    .show();

            return true;
        }
    }

    public WebView getWebView() {
        return mWebView;
    }

    /**
     * 생성자.
     * @param activity : context
     * @param view : 적용될 웹뷰
     */
    public BasicWebViewClient(Activity activity, WebView view, String alertTitleString, String confirmTitleString) {
        mWebView = view;
        mContext = activity;

        // This will handle downloading. It requires Gingerbread, though
        mDownloadManager = (DownloadManager) mContext.getSystemService(mContext.DOWNLOAD_SERVICE);
        mWebChromeClient = new BroadcastWebChromeClient();

        // Enable remote debugging via chrome://inspect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.setWebContentsDebuggingEnabled(true);
        }
        mWebView.setWebChromeClient(mWebChromeClient);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setGeolocationEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        // Use WideViewport and Zoom out if there is no viewport defined
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.setMediaPlaybackRequiresUserGesture(false);
        }

        // Enable pinch to zoom without the zoom buttons
        webSettings.setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            // Hide the zoom controls for HONEYCOMB+
            webSettings.setDisplayZoomControls(false);
        }

        webSettings.setAppCacheEnabled(true);
        mWebView.clearCache(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Sets whether the WebView should allow third party cookies to be set.
            // Allowing third party cookies is a per WebView policy and can be set differently on different WebView instances.

            // Apps that target KITKAT or below default to allowing third party cookies.
            // Apps targeting LOLLIPOP or later default to disallowing third party cookies.
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mWebView.getSettings().setTextZoom(100);
        }

        if (StringUtils.isEmptyString(alertTitleString)) {
            mAlertTitleString = activity.getString(R.string.default_webview_alert_title);
        } else {
            mAlertTitleString = alertTitleString;
        }

        if (StringUtils.isEmptyString(confirmTitleString)) {
            mConfirmTitleString = activity.getString(R.string.default_webview_confirm_title);
        } else {
            mConfirmTitleString = confirmTitleString;
        }

        mWebView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                mContext.startActivity(intent);

            }
        });
    }

    public void setBackgroundResource(int resId) {
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setBackgroundResource(resId);
    }

    public void setBackgroundTransparent() {
        mWebView.setBackgroundColor(Color.TRANSPARENT);
    }

    public void setPreAuthorizePermission(String url) {
        // mWebView.preauthorizePermission(Uri.parse(url), PermissionRequest.RESOURCE_AUDIO_CAPTURE | PermissionRequest.RESOURCE_VIDEO_CAPTURE);
    }

//    public void stopMediaStream() {
//        /**
//         * When the application falls into the background we want to stop the media stream
//         * such that the camera is free to use by other apps.
//         */
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            mWebView.evaluateJavascript("if(window.localStream){window.localStream.stop();}", null);
//        }
//    }

    public void setBackground(Drawable drawable) {
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            mWebView.setBackgroundDrawable(drawable);
        } else {
            mWebView.setBackground(drawable);
        }
    }

    private static final String[] DOCUMENT_MIMETYPE = new String[]{
            "application/pdf",
            "application/msword",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            "application/vnd.ms-excel",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
            "application/vnd.ms-powerpoint",
            "application/vnd.ms-powerpoint",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.presentationml.template",
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow"
    };
    // url = file path or whatever suitable URL you want.
    public String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public boolean isDocumentMimeType(String url) {
        String mimeType = getMimeType(url);
        if (!StringUtils.isEmptyString(mimeType) && Arrays.asList(DOCUMENT_MIMETYPE).contains(mimeType)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // for excel download
        if (isDocumentMimeType(url)) {
            Log.d(TAG, "This url is document mimetype = " + url);
            if (StorageUtils.isExternalStorageWritable()) {
                Uri source = Uri.parse(url);

                // Make a new request pointing to the mp3 url
                DownloadManager.Request request = new DownloadManager.Request(source);
                // Use the same file name for the destination
                File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), source.getLastPathSegment());
                request.setDestinationUri(Uri.fromFile(destinationFile));
                // Add it to the manager
                mDownloadManager.enqueue(request);
                Toast.makeText(mContext, R.string.downloads_requested, Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //closeWebApplication();
                    }
                });
                builder.setMessage(R.string.downloads_path_check);
                builder.show();
            }
            return true;
        }

        if (url.startsWith("tel:")) {
            // phone call
            if (!PhoneState.hasPhoneCallAbility(mContext)) {
                Log.d(TAG, ">> This device has not phone call ability !!!");
                return true;
            }

            mContext.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
        } else if (url.startsWith("mailto:")) {
            url = url.replaceFirst("mailto:", "");
            url = url.trim();

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("plain/text").putExtra(Intent.EXTRA_EMAIL, new String[]{url});

            mContext.startActivity(i);
        } else if (url.startsWith("geo:")) {
            Intent searchAddress = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            mContext.startActivity(searchAddress);
        } else {
            // 새로운 URL로 이동시 현재 웹뷰 안에서 로딩되도록 한다.
            dismissProgressDialog();
            loadWebUrl(url);
            showProgressDialog();
        }
        return true;
    }

    @JavascriptInterface
    public void postUrl(String url, String data) {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage("com.android.chrome")           // open only chrome
                .setData(Uri.parse(url));
        mContext.startActivity(intent);
    }

    public abstract void loadWebUrl(String url);
    @JavascriptInterface
    public abstract void updateIoTDevices();
    @JavascriptInterface
    public abstract void onUpdateIoTDevices(String iotDevices);
    @JavascriptInterface
    public abstract boolean registerGcm();
    @JavascriptInterface
    public abstract boolean unRegisterGcm();
    public abstract void onRegistered(String gcmRegToken);
    public abstract void onUnRegistered();

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.d(TAG, "WebView client onPageFinished = " + url);
        super.onPageFinished(view, url);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().sync();
        }
        dismissProgressDialog();
        mPageLoadSuccess = true;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        mPageLoadSuccess = false;
        Log.e(TAG, "WebView client onReceivedError code = " + errorCode);
        switch (errorCode) {
            case ERROR_AUTHENTICATION:               // 서버에서 사용자 인증 실패
            case ERROR_BAD_URL:                            // 잘못된 URL
            case ERROR_CONNECT:                           // 서버로 연결 실패
            case ERROR_FAILED_SSL_HANDSHAKE:     // SSL handshake 수행 실패
            case ERROR_FILE:                                   // 일반 파일 오류
            case ERROR_FILE_NOT_FOUND:                // 파일을 찾을 수 없습니다
            case ERROR_HOST_LOOKUP:            // 서버 또는 프록시 호스트 이름 조회 실패
            case ERROR_IO:                               // 서버에서 읽거나 서버로 쓰기 실패
            case ERROR_PROXY_AUTHENTICATION:    // 프록시에서 사용자 인증 실패
            case ERROR_REDIRECT_LOOP:                // 너무 많은 리디렉션
            case ERROR_TIMEOUT:                          // 연결 시간 초과
            case ERROR_TOO_MANY_REQUESTS:            // 페이지 로드중 너무 많은 요청 발생
            case ERROR_UNKNOWN:                         // 일반 오류
            case ERROR_UNSUPPORTED_AUTH_SCHEME:  // 지원되지 않는 인증 체계
            case ERROR_UNSUPPORTED_SCHEME:
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        closeWebApplication();
                    }
                });
                builder.setMessage(R.string.webview_page_loading_error);
                builder.show();

                dismissProgressDialog();
                // Default behaviour
                super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    ////////////////////////////////
    /**
     * 아래의 함수들은 자바스크립트에서 Native를 호출할 필요가 있을때 사용한다.
     * window.nbplus.{methodName}  형식으로 사용하면 된다.
     */
    /**
     * 디바이스의 UUID 조회. 40bytes SHA-1 value
     */
    @JavascriptInterface
    public abstract String getDeviceId();

    @JavascriptInterface
    public String getApplicationPackageName() {
        return mContext.getApplicationContext().getPackageName();
    }

    /**
     * 디바이스가 콜 호출이 가능한지 체크한다.
     */
    @JavascriptInterface
    public boolean isPhoneCallAbility() {
        return PhoneState.hasPhoneCallAbility(mContext);
    }

    @JavascriptInterface
    public boolean isNetworkAvailable() {
        return NetworkUtils.isConnected(mContext);
    }

    @JavascriptInterface
    public String getIpAddress() {
        return NetworkUtils.getIPAddress(true);
    }

    @JavascriptInterface
    public void onNetworkStatusChanged(boolean connected) {
        String ipAddress = "";
        if (connected) {
            ipAddress = getIpAddress();
        }
        mWebView.loadUrl("javascript:window.onNetworkStatusChanged(" + connected + ", " + ipAddress + ");");
    }

    /**
     * 안드로이드 토스트를 출력한다.
     * @param message : 메시지
     * @param duration : LENGTH_LONG === 1, LENGTH_SHORT === 0
     */
    @JavascriptInterface
    public void toast(String message, int duration) {
        if (duration != Toast.LENGTH_LONG) {
            duration = Toast.LENGTH_SHORT;
        }
        Toast.makeText(mContext, message, duration).show();
    }

    @JavascriptInterface
    public String getLineNumber() {
        Log.d(TAG, "getLineNumber() called");

        String phoneNumberStr = PhoneState.getLineNumber1(mContext);
        Log.d(TAG, ">>> PhoneState.getLineNumber1 = " + phoneNumberStr);
        if (StringUtils.isEmptyString(phoneNumberStr)) {
            return null;
        }

        if (PhoneNumberUtils.isGlobalPhoneNumber(phoneNumberStr)) {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber phoneNumberProto;
            try {
                phoneNumberProto = phoneUtil.parse(phoneNumberStr, "KR");
                return phoneUtil.format(phoneNumberProto, PhoneNumberUtil.PhoneNumberFormat.NATIONAL).replace("-", "").replace(" ", "").replace("(", "").replace(")", "");
            } catch (NumberParseException e) {
                Log.e(TAG, "NumberParseException was thrown: " + e.toString());
                return phoneNumberStr;
            }
        } else {
            return phoneNumberStr;
        }
    }

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @JavascriptInterface
    public abstract void closeWebApplication();

    ////////////////////////////////
    /**
     * 아래의 함수들은 자바스크립트를 Native 에서 호출할 필요가 있을때 사용한다.
     * 아래에서 불리는 자바스크립트 function 들은 웹앱에서 구현이 되어 있어야 한다.
     *
     */
    public void onOrientationChanged(int orientation) {

        // Use loadUrl("javascript:...") (API Level 1-18)
        // or evaluateJavascript() (API Level 19+) to evaluate your own JavaScript in the context of the currently-loaded Web page
        mWebView.loadUrl("javascript:window.onOrientationChanged(" + orientation + ");");
    }

    public void onBackPressed() {
        if (!mPageLoadSuccess) {
            closeWebApplication();
        } else {
            // Use loadUrl("javascript:...") (API Level 1-18)
            // or evaluateJavascript() (API Level 19+) to evaluate your own JavaScript in the context of the currently-loaded Web page
            mWebView.loadUrl("javascript:window.onBackPressed();");
        }
    }

    // progress bar
    protected void showProgressDialog() {
        try {
            dismissProgressDialog();
            mProgressDialogFragment = ProgressDialogFragment.newInstance();
            mProgressDialogFragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), "progress_dialog");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void dismissProgressDialog() {
        try {
            if (mProgressDialogFragment != null) {
                mProgressDialogFragment.dismiss();
            }
            mProgressDialogFragment = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
