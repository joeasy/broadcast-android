package com.nbplus.hybrid;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;

import java.net.HttpCookie;

/**
 * Created by basagee on 2015. 5. 4..
 */
public class CookieSync {
    private static final String TAG = BasicWebViewClient.class.getSimpleName();

    /**
     * This class was deprecated in API level 21.
     * 어플리케이션 시작시에 한번만 해주면 된다.
     */
    public static void createCookieSyncManager(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context);
        }
    }

    /**
     * This class was deprecated in API level 21.
     * onResume 시에 해주면 된다.
     */
    public static void startSync(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().startSync();
        }
    }

    /**
     * This class was deprecated in API level 21.
     * onPause 시에 해주면 된다.
     */
    public static void stopSync(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync();
        }
    }

    /**
     * 웹뷰에 페이지 URL 을 설정하고 로드한다.
     * 이것은 어플리케이션에서 한번만 하면 된다. 액티비티마다 호출할 필요가 없다.
     * @param domain
     * @param cookie
     */
    public static void setCookie(Context context, String domain, HttpCookie cookie) {
        // This class was deprecated in API level 21.
        // The WebView now automatically syncs cookies as necessary.
        // You no longer need to create or use the CookieSyncManager.
        // To manually force a sync you can use the CookieManager method flush() which is a synchronous replacement for sync().
        CookieManager cookieManager = CookieManager.getInstance();
        if (cookie != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // This method was deprecated in API level 21. use removeSessionCookies(ValueCallback) instead.
                cookieManager.removeSessionCookie();
            } else {
                // Removes all session cookies, which are cookies without an expiration date.
                cookieManager.removeSessionCookies(new ValueCallback<Boolean>() {
                    @Override
                    public void onReceiveValue(Boolean value) {
                        Log.d(TAG, "onReceiveValue " + value);
                    }
                });
            }
        }

        String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain();
        cookieManager.setCookie(domain, cookieString);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().sync();
        } else {
            cookieManager.flush();
        }
    }

}
