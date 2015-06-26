package com.nbplus.vbroadlauncher;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.hybrid.BroadcastWebViewClient;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.DisplayUtils;
import org.basdroid.common.StringUtils;


/**
 * Created by basagee on 2015. 5. 28..
 */
public class BroadcastWebViewActivity extends BaseActivity {
    private static final String TAG = BroadcastWebViewActivity.class.getSimpleName();

    BroadcastWebViewClient mWebViewClient;
    ShortcutData mShortcutData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_broadcast_webview);

        Log.d(TAG, "BroadcastWebViewActivity onCreate()");
        WebView webView = (WebView)findViewById(R.id.webview);
        mWebViewClient = new BroadcastWebViewClient(this, webView);

        Intent i = getIntent();
        mShortcutData = i.getParcelableExtra(Constants.EXTRA_NAME_SHORTCUT_DATA);
        String url = mShortcutData.getDomain() + mShortcutData.getPath();
        if (url.indexOf("?") > 0) {
            url += ("&UUID=" + DeviceUtils.getDeviceIdByMacAddress(BroadcastWebViewActivity.this));
            url += ("&APPID=" + getApplicationContext().getPackageName());
        } else {
            url += ("?UUID=" + DeviceUtils.getDeviceIdByMacAddress(BroadcastWebViewActivity.this));
            url += ("&APPID=" + getApplicationContext().getPackageName());
        }
        mWebViewClient.loadUrl(url);

        // test view
        final EditText editText = (EditText)findViewById(R.id.et_test_url);
        editText.setText(url);
        Button button = (Button)findViewById(R.id.btn_test_load);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = editText.getText().toString();
                if (StringUtils.isEmptyString(url)) {
                    return;
                }

                if (url.indexOf("?") > 0) {
                    if (!url.contains("UUID=")) {
                        url += ("&UUID=" + DeviceUtils.getDeviceIdByMacAddress(BroadcastWebViewActivity.this));
                    }
                    if (!url.contains("APPID=")) {
                        url += ("&APPID=" + getApplicationContext().getPackageName());
                    }
                } else {
                    if (!url.contains("UUID=")) {
                        url += ("?UUID=" + DeviceUtils.getDeviceIdByMacAddress(BroadcastWebViewActivity.this));
                    }
                    if (!url.contains("APPID=")) {
                        url += ("&APPID=" + getApplicationContext().getPackageName());
                    }
                }
                mWebViewClient.loadUrl(url);
            }
        });
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

        if (mWebViewClient != null) {
            Log.d(TAG, "Prev url is = " + mWebViewClient.getWebView().getUrl());
        }
        mShortcutData = intent.getParcelableExtra(Constants.EXTRA_NAME_SHORTCUT_DATA);
        mWebViewClient.loadUrl(mShortcutData.getDomain() + mShortcutData.getPath());

        // test view
        final EditText editText = (EditText)findViewById(R.id.et_test_url);
        editText.setText(mShortcutData.getDomain() + mShortcutData.getPath());

        Log.d(TAG, ">> reset url = " + mShortcutData.getDomain() + mShortcutData.getPath());
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        mWebViewClient.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        Log.d(TAG, "BroadcastWebViewActivity onDestroy()");
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
}
