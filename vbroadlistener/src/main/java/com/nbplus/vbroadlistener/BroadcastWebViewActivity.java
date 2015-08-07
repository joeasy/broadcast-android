package com.nbplus.vbroadlistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

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
    boolean isGoogleMapMode = false;

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
                String deviceToken = LauncherSettings.getInstance(this).getGcmToken();

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

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Constants.REGISTRATION_COMPLETE));

        String fromNotiUrl = null;
        if (getIntent() != null) {
            final String action = getIntent().getAction();
            if (Constants.ACTION_SHOW_NOTIFICATION_CONTENTS.equals(getIntent().getAction())) {
                fromNotiUrl = getIntent().getStringExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS);
                Log.d(TAG, ">> messageId = " + getIntent().getIntExtra("xxx", 0));
            } else if (Constants.ACTION_SHOW_NOTIFICATION_EMERGENCY_CALL.equals(getIntent().getAction())) {
                final String lat = getIntent().getStringExtra(Constants.EXTRA_SHOW_NOTIFICATION_EMERGENCY_LAT);
                final String lon = getIntent().getStringExtra(Constants.EXTRA_SHOW_NOTIFICATION_EMERGENCY_LON);
                fromNotiUrl = "http://maps.google.com/maps?q=" + lat + "," + lon;
                isGoogleMapMode = true;
            }
        }

        loadWebView(fromNotiUrl);
        checkPlayServices();
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (isGoogleMapMode) {
            String url = null;
            VBroadcastServer serverInfo = LauncherSettings.getInstance(this).getServerInformation();
            if (serverInfo != null && serverInfo.getDocServer() != null) {
                url = serverInfo.getDocServer() + LauncherSettings.indexPageContext;
            } else {
                url = LauncherSettings.getInstance(this).getRegisterAddress();
            }
            if (url.indexOf("?") > 0) {
                url += ("&UUID=" + LauncherSettings.getInstance(this).getDeviceID());
                url += ("&APPID=" + getApplicationContext().getPackageName());
            } else {
                url += ("?UUID=" + LauncherSettings.getInstance(this).getDeviceID());
                url += ("&APPID=" + getApplicationContext().getPackageName());
            }
            isGoogleMapMode = false;
            loadWebView(url);
        } else {
            mWebViewClient.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        Log.d(TAG, "BroadcastWebViewActivity onDestroy()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        if (mText2Speech != null) {
            mText2Speech.shutdown();
        }
        mText2Speech = null;
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
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
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

    public void getText2SpeechObject(OnText2SpeechListener l) {
        this.mcheckText2SpeechLister = l;

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, Constants.START_ACTIVITY_REQUEST_CHECK_TTS_DATA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.START_ACTIVITY_REQUEST_CHECK_TTS_DATA :
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    // check korean
                    mText2Speech = new TextToSpeech(this, this);
                } else {
                    Log.d(TAG, "여기서 제대로 설정안했다면 관두자.... 사용자 맘인데...");
                    showText2SpeechAlertDialog();

                    LauncherSettings.getInstance(this).setIsCheckedTTSEngine(true);
                }
                break;
        }
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
        Log.d(TAG, "onNewIntent()... maybe from notifications");
        String fromNotiUrl = null;
        if (intent != null && Constants.ACTION_SHOW_NOTIFICATION_CONTENTS.equals(intent.getAction())) {
            fromNotiUrl = intent.getStringExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS);
            Log.d(TAG, ">> messageId = " + getIntent().getIntExtra("xxx", 0));
        }

        String url = null;
        if (!StringUtils.isEmptyString(fromNotiUrl)) {
            url = fromNotiUrl;
        }

        mWebViewClient.stopPageLoading();
        loadWebView(url);
    }

    private void loadWebView(String url) {
        if (StringUtils.isEmptyString(url)) {
            VBroadcastServer serverInfo = LauncherSettings.getInstance(this).getServerInformation();
            if (serverInfo != null && serverInfo.getDocServer() != null) {
                url = serverInfo.getDocServer() + LauncherSettings.firstPageContext;
            } else {
                url = LauncherSettings.getInstance(this).getRegisterAddress();
            }
        }
        if (StringUtils.isEmptyString(url) || !Patterns.WEB_URL.matcher(url).matches()) {
            Log.e(TAG, "Wrong url ....");
            new AlertDialog.Builder(this).setMessage(R.string.alert_wrong_page_url)
                    //.setTitle(R.string.alert_network_title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.alert_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            })
                    .show();
        } else {
            mWebViewClient.loadWebUrl(url);
        }
        Log.d(TAG, ">> Start url = " + url);

        setContentViewByOrientation();
    }
}
