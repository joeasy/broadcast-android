package com.nbplus.hybrid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.PhoneState;
import org.basdroid.common.R;
import org.basdroid.common.StringUtils;


/**
 * Created by basagee on 2015. 4. 30..
 */
public class BasicWebViewClient extends WebViewClient {
    private static final String TAG = BasicWebViewClient.class.getSimpleName();

    protected static final String JAVASCRIPT_IF_NAME = "nbplus";

    protected WebView mWebView;
    protected Activity mContext;
    protected BroadcastWebChromeClient mWebChromeClient;

    /**
     * Created by basagee on 2015. 4. 30..
     */
    class BroadcastWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
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
    public BasicWebViewClient(Activity activity, WebView view) {
        mWebView = view;
        mContext = activity;

        mWebChromeClient = new BroadcastWebChromeClient();
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setWebViewClient(this);

        mWebView.addJavascriptInterface(this, JAVASCRIPT_IF_NAME);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // TODO : clear cache ????
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

    }

    public void setBackgroundResource(int resId) {
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setBackgroundResource(resId);
    }

    public void setBackgroundTransparent() {
        mWebView.setBackgroundColor(Color.TRANSPARENT);
    }

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
        if (StringUtils.isEmptyString(url) == false) {
            mWebView.loadUrl(url);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("tel:")) {
            // phone call
            if (!PhoneState.hasPhoneCallAbility(mContext)) {
                Log.d(TAG, ">> This device has not phone call ability !!!");
                return false;
            }

            mContext.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
            return true;
        } else if (url.startsWith("mailto:")) {
            url = url.replaceFirst("mailto:", "");
            url = url.trim();

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("plain/text").putExtra(Intent.EXTRA_EMAIL, new String[]{url});

            mContext.startActivity(i);
            return true;
        } else if (url.startsWith("geo:")) {
            Intent searchAddress = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            mContext.startActivity(searchAddress);
        }
        else {
            // 새로운 URL로 이동시 현재 웹뷰 안에서 로딩되도록 한다.
            view.loadUrl(url);
            return true;
        }
        return true;    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().sync();
        }
    }

    ////////////////////////////////
    /**
     * 아래의 함수들은 자바스크립트에서 Native를 호출할 필요가 있을때 사용한다.
     * window.nbplus.{methodName}  형식으로 사용하면 된다.
     */
    /**
     * 디바이스가 콜 호출이 가능한지 체크한다.
     */
    @JavascriptInterface
    public String getDeviceId() {
        return DeviceUtils.getDeviceIdByMacAddress(mContext);
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

    /**
     *
     * @param data
     */
    @JavascriptInterface
    public void setApplicationData(String data) {
        Log.d(TAG, ">> setApplicationData() received = " + data);
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
    @JavascriptInterface
    public void closeWebApplication() {
        Log.d(TAG, ">> closeWebApplication() called");
        //mContext.finish();
        return;
    }

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @JavascriptInterface
    public boolean isMediaPlaying() {
        return false;
    }

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @JavascriptInterface
    public String getPlayingData() {
        return null;
    }

    /**
     * 미디어 플레이어에 재생 요청.
     * json 이 없는경우, 재생목록에 일시정지된 미디어가 있다면 일시정지된 미디어를 플레이한다.
     * @param json 미디어플레이어에서 재생할 미디어 정보
     */
    @JavascriptInterface
    public boolean playMedia(String json) {
        return false;
    }

    /**
     * 미디어 플레이어에서 재생중인 미디어가 있을 경우, 일시정지
     */
    @JavascriptInterface
    public boolean pauseMedia() {
        return false;
    }

    /**
     * 미디어플레이어가 재생중인 미디어가 있을경우, 미디어 재생을 종료하고 재생목록에서 삭제
     */
    @JavascriptInterface
    public boolean stopMedia() {
        return false;
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
        // Use loadUrl("javascript:...") (API Level 1-18)
        // or evaluateJavascript() (API Level 19+) to evaluate your own JavaScript in the context of the currently-loaded Web page
        mWebView.loadUrl("javascript:window.onBackPressed();");
    }

    /**
     *
     Here's what I came up with today. It's thread-safe, reasonably efficient, and allows for synchronous Javascript execution from Java for an Android WebView.

     Works in Android 2.2 and up. (Requires commons-lang because I need my code snippets passed to eval() as a Javascript string. You could remove this dependency by wrapping the code not in quotation marks, but in function(){})

     First, add this to your Javascript file:

     function evalJsForAndroid(evalJs_index, jsString) {
     var evalJs_result = "";
     try {
     evalJs_result = ""+eval(jsString);
     } catch (e) {
     console.log(e);
     }
     androidInterface.processReturnValue(evalJs_index, evalJs_result);
     }
     Then, add this to your Android activity:

     private Handler handler = new Handler();
     private final AtomicInteger evalJsIndex = new AtomicInteger(0);
     private final Map<Integer, String> jsReturnValues = new HashMap<Integer, String>();
     private final Object jsReturnValueLock = new Object();
     private WebView webView;

     @Override
     public void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
     webView = (WebView) findViewById(R.id.webView);
     webView.addJavascriptInterface(new MyJavascriptInterface(this), "androidInterface");
     }

     public String evalJs(final String js) {
     final int index = evalJsIndex.incrementAndGet();
     handler.post(new Runnable() {
     public void run() {
     webView.loadUrl("javascript:evalJsForAndroid(" + index + ", " +
     "\"" + StringEscapeUtils.escapeEcmaScript(js) + "\")");
     }
     });
     return waitForJsReturnValue(index, 10000);
     }

     private String waitForJsReturnValue(int index, int waitMs) {
     long start = System.currentTimeMillis();

     while (true) {
     long elapsed = System.currentTimeMillis() - start;
     if (elapsed > waitMs)
     break;
     synchronized (jsReturnValueLock) {
     String value = jsReturnValues.remove(index);
     if (value != null)
     return value;

     long toWait = waitMs - (System.currentTimeMillis() - start);
     if (toWait > 0)
     try {
     jsReturnValueLock.wait(toWait);
     } catch (InterruptedException e) {
     break;
     }
     else
     break;
     }
     }
     Log.e("MyActivity", "Giving up; waited " + (waitMs/1000) + "sec for return value " + index);
     return "";
     }

     private void processJsReturnValue(int index, String value) {
     synchronized (jsReturnValueLock) {
     jsReturnValues.put(index, value);
     jsReturnValueLock.notifyAll();
     }
     }

     private static class MyJavascriptInterface {
     private MyActivity activity;

     public MyJavascriptInterface(MyActivity activity) {
     this.activity = activity;
     }

     // this annotation is required in Jelly Bean and later:
     @JavascriptInterface
     public void processReturnValue(int index, String value) {
     activity.processJsReturnValue(index, value);
     }
     }
    */

}
