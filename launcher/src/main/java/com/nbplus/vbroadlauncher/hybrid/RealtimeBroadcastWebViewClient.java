package com.nbplus.vbroadlauncher.hybrid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.nbplus.media.MusicService;
import com.nbplus.vbroadlauncher.data.LauncherSettings;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.PhoneState;
import org.basdroid.common.R;
import org.basdroid.common.StringUtils;


/**
 * Created by basagee on 2015. 4. 30..
 */
public class RealtimeBroadcastWebViewClient extends WebViewClient {
    private static final String TAG = RealtimeBroadcastWebViewClient.class.getSimpleName();

    protected static final String JAVASCRIPT_IF_NAME = "nbplus";

    protected WebView mWebView;
    protected Context mContext;
    protected BroadcastWebChromeClient mWebChromeClient;
    protected boolean mPageLoadSuccess = false;
    OnRealtimeBroadcastWebViewListener mOnWebViewListener;
    public boolean isClosingByWebApp() {
        return mIsClosingByWebApp;
    }

    private boolean mIsClosingByWebApp = false;
    private boolean mIsRadioPauseByWeb = false;

    public interface OnRealtimeBroadcastWebViewListener {
        public void onCloseWebApplication();
        public void onPageFinished(boolean success);
    }

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
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    Log.d(TAG, "onPermissionRequest() grant !!!");
                    //if(request.getOrigin().toString().equals("https://apprtc-m.appspot.com/")) {
                    request.grant(request.getResources());
                    //} else {
                    //    request.deny();
                    //}
                }
            });
        }
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(mContext)
                    //.setTitle(MopDef.STR_WEB_ALERT_TITLE)
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
                    //.setTitle(MopDef.STR_WEB_ALERT_TITLE)
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
    public RealtimeBroadcastWebViewClient(Context activity, WebView view, OnRealtimeBroadcastWebViewListener l) {
        mWebView = view;
        mContext = activity;
        mOnWebViewListener = l;

        mWebChromeClient = new BroadcastWebChromeClient();

        // Enable remote debugging via chrome://inspect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.setWebContentsDebuggingEnabled(true);
        }
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setWebViewClient(this);

        mWebView.addJavascriptInterface(this, JAVASCRIPT_IF_NAME);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
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

    /**
     * 웹뷰에 페이지 URL 을 설정하고 로드한다.
     * @param url
     */
    public void loadUrl(String url) {
        if (!StringUtils.isEmptyString(url)) {
            mWebView.loadUrl(url);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
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
            view.loadUrl(url);
        }
        return true;
    }

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
        mPageLoadSuccess = true;
        if (mOnWebViewListener != null) {
            mOnWebViewListener.onPageFinished(true);
        }
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
        }

        // Default behaviour
        super.onReceivedError(view, errorCode, description, failingUrl);
        if (mOnWebViewListener != null) {
            mOnWebViewListener.onPageFinished(false);
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
    public String getDeviceId() {
        return LauncherSettings.getInstance(mContext).getDeviceID();
    }

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

    /**
     * 안드로이드 토스트를 출력한다.
     * @param message : 메시지
     * @param duration : LENGTH_LONG === 1, LENGTH_SHORT === 0
     */
    @JavascriptInterface
    public void toast(String message, int duration) {
        Toast.makeText(mContext, message, duration).show();
    }

    @JavascriptInterface
    public String getLineNumber() {
        Log.d(TAG, "" + PhoneState.getLineNumber1(mContext));
        return PhoneState.getLineNumber1(mContext);
    }

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @JavascriptInterface
    public void closeWebApplication() {
        Log.d(TAG, ">> closeWebApplication() called");
        //mContext.finish();

        if (mIsRadioPauseByWeb) {
            mIsRadioPauseByWeb = false;
            Intent i = new Intent(mContext, MusicService.class);
            i.setAction(MusicService.ACTION_PLAY);
            mContext.startService(i);
        }

        mIsClosingByWebApp = true;
        if (mOnWebViewListener != null) {
            mOnWebViewListener.onCloseWebApplication();
        }
    }

    @JavascriptInterface
    public void onStartBroadcastMediaStream(boolean isTTS, String ttsString) {
        Log.d(TAG, ">> onStartBroadcastMediaStream() called = " + isTTS + ", tts = " + ttsString);
        mIsRadioPauseByWeb = true;
        Intent i = new Intent(mContext, MusicService.class);
        i.setAction(MusicService.ACTION_PAUSE);
        mContext.startService(i);
    }

    @JavascriptInterface
    public void onPauseBroadcastMediaStream() {
        Log.d(TAG, ">> onPauseBroadcastMediaStream() called");
    }

    @JavascriptInterface
    public void onStopBroadcastMediaStream() {
        Log.d(TAG, ">> onStopBroadcastMediaStream() called");
    }
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

    /**
     * 시나리오변경.. 웹앱에 이벤트만 보내고.. 웹앱에서 처리후 closeWebApplication() 호출한다.
     */
    public void onCloseWebApplicationByUser() {
        if (!mPageLoadSuccess) {
            closeWebApplication();
        } else {
            if (!mIsClosingByWebApp) {
                mWebView.loadUrl("javascript:window.onCloseWebApplicationByUser();");
            }
        }
    }

}
