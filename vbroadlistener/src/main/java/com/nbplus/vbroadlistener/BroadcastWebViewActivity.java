package com.nbplus.vbroadlistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.nbplus.vbroadlistener.data.Constants;
import com.nbplus.vbroadlistener.data.VBroadcastServer;
import com.nbplus.vbroadlistener.gcm.RegistrationIntentService;
import com.nbplus.vbroadlistener.hybrid.BroadcastWebViewClient;
import com.nbplus.vbroadlistener.preference.LauncherSettings;

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

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    BroadcastWebViewClient mWebViewClient;

    private final RadioActivityHandler mHandler = new RadioActivityHandler(this);

    // 핸들러 객체 만들기
    private static class RadioActivityHandler extends Handler {
        private final WeakReference<BroadcastWebViewActivity> mActivity;

        public RadioActivityHandler(BroadcastWebViewActivity activity) {
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
            case Constants.HANDLER_MESSAGE_UPDATE_GCM_DEVICE_TOKEN :
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String deviceToken = sharedPreferences.getString(Constants.GCM_TOKEN_VALUE, "");

                if (mWebViewClient != null) {
                    mWebViewClient.onRegistered(deviceToken);
                }
                break;
            case Constants.HANDLER_MESSAGE_UNREGISTER_GCM:
                if (mWebViewClient != null) {
                    mWebViewClient.onUnRegistered();
                }
                break;
        }
    }

    private BroadcastReceiver mRegistrationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Constants.REGISTRATION_COMPLETE.equals(action)) {
                // web에서 처리한다.
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(Constants.HANDLER_MESSAGE_UPDATE_GCM_DEVICE_TOKEN);
                }

//            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//            boolean sentToken = sharedPreferences.getBoolean(Constants.SENT_TOKEN_TO_SERVER, false);
//            if (sentToken) {
//                mInformationTextView.setText(getString(R.string.gcm_send_message));
//            } else {
//                mInformationTextView.setText(getString(R.string.token_error_message));
//            }
            } else if (Constants.UNREGISTER_GCM.equals(action)) {
                // web에서 처리한다.
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(Constants.HANDLER_MESSAGE_UNREGISTER_GCM);
                }
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

        String url = null;
        VBroadcastServer serverInfo = LauncherSettings.getInstance(this).getServerInformation();
        if (serverInfo != null && serverInfo.getDocServer() != null) {
            url = serverInfo.getDocServer() + LauncherSettings.firstPageContext;
        } else {
            url = LauncherSettings.getInstance(this).getRegisterAddress();
        }
        if (url.indexOf("?") > 0) {
            url += ("&UUID=" + DeviceUtils.getDeviceIdByMacAddress(this));
            url += ("&APPID=" + getApplicationContext().getPackageName());
        } else {
            url += ("?UUID=" + DeviceUtils.getDeviceIdByMacAddress(this));
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

        checkPlayServices();
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

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Constants.REGISTRATION_COMPLETE));
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
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

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWebViewClient != null) {
            //mWebViewClient.stopMediaStream();
        }
    }
}
