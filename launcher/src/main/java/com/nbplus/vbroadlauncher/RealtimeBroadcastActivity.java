package com.nbplus.vbroadlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbplus.push.data.PushConstants;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.PushPayloadData;
import com.nbplus.vbroadlauncher.hybrid.BroadcastWebViewClient;
import com.nbplus.vbroadlauncher.hybrid.RealtimeBroadcastWebViewClient;
import com.nbplus.vbroadlauncher.hybrid.TextToSpeechHandler;
import com.nbplus.vbroadlauncher.service.BroadcastChatHeadService;

import org.basdroid.common.DeviceUtils;

import java.lang.ref.WeakReference;


/**
 * Created by basagee on 2015. 5. 28..
 */
public class RealtimeBroadcastActivity extends BaseActivity implements BaseActivity.OnText2SpeechListener, TextToSpeechHandler.OnUtteranceProgressListener, RealtimeBroadcastWebViewClient.OnRealtimeBroadcastWebViewListener {
    private static final String TAG = RealtimeBroadcastActivity.class.getSimpleName();

    // for audio broadcast
    WebView mWebView;
    RealtimeBroadcastWebViewClient mWebViewClient;

    // for text broadcast
    TextView mTextView;
    TextToSpeechHandler mText2SpeechHandler;
    private PushPayloadData mBroadcastData;

    private long mBroadcastPayloadIdx = -1;

    private static final int HANDLER_BROADCAST_STARTED = 1000;
    private static final int HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE = 1001;
    private final BroadcastChatHeadProxyActivityHandler mHandler = new BroadcastChatHeadProxyActivityHandler(this);

    // 핸들러 객체 만들기
    private static class BroadcastChatHeadProxyActivityHandler extends Handler {
        private final WeakReference<RealtimeBroadcastActivity> mActivity;

        public BroadcastChatHeadProxyActivityHandler(RealtimeBroadcastActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            RealtimeBroadcastActivity activity = mActivity.get();
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
            case HANDLER_BROADCAST_STARTED :
                long idx = (long)msg.obj;
                if (idx == mBroadcastPayloadIdx) {
                    Log.d(TAG, "HANDLER_BROADCAST_STARTED, finish... ");
                    if (mWebViewClient != null) {
                        mWebViewClient.onCloseWebApplicationByUser();
                    }
                    finish();
                }
            case HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE :
                if (mWebViewClient != null && !mWebViewClient.isClosingByWebApp()) {
                    mWebViewClient.onCloseWebApplicationByUser();
                }
                break;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, ">> mBroadcastReceiver action received = " + action);
            // send handler message
            switch (action) {
                case PushConstants.ACTION_PUSH_MESSAGE_RECEIVED :
                    Message msg = new Message();
                    msg.what = HANDLER_BROADCAST_STARTED;
                    msg.obj = intent.getLongExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, -1);
                    mHandler.sendMessage(msg);
                    break;
                case Constants.ACTION_BROWSER_ACTIVITY_CLOSE :
                    mHandler.sendEmptyMessage(HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE);
                    break;
                default :
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        acquireCpuWakeLock();

        IntentFilter filter = new IntentFilter();
        filter.addAction(PushConstants.ACTION_PUSH_MESSAGE_RECEIVED);
        filter.addAction(Constants.ACTION_BROWSER_ACTIVITY_CLOSE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);

        Intent i;
        Intent intent = getIntent();
        if (intent == null) {
            Log.d(TAG, "empty intent value ...");
            finish();
            return;
        }

        mBroadcastData = intent.getParcelableExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA);
        if (mBroadcastData == null) {
            Log.d(TAG, ">> payload data is null");
            finish();
            return;
        }
        mBroadcastPayloadIdx = intent.getLongExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, -1);

        hideSystemUI();

        if (Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(mBroadcastData.getServiceType())) {
            setContentView(R.layout.fragment_text_broadcast);
        } else {
            setContentView(R.layout.fragment_audio_broadcast);
        }

        if (Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(mBroadcastData.getServiceType())) {
            mcheckText2SpeechLister = this;
            // 문자방송
            mTextView = (TextView) findViewById(R.id.broadcast_text);
            mTextView.setText(mBroadcastData.getMessage());
            mTextView.setVerticalScrollBarEnabled(true);
            mTextView.setHorizontalScrollBarEnabled(false);
            mTextView.setMovementMethod(new ScrollingMovementMethod());

            mText2SpeechHandler = new TextToSpeechHandler(this, this);
            checkText2SpeechAvailable();
        } else {
            // 실시간, 일반음성방송
            mWebView = (WebView)findViewById(R.id.webview);
            mWebViewClient = new RealtimeBroadcastWebViewClient(this, mWebView, this);
            mWebViewClient.setBackgroundTransparent();

            String url = mBroadcastData.getMessage();
            if (url.indexOf("?") > 0) {
                url += ("&UUID=" + DeviceUtils.getDeviceIdByMacAddress(this));
                url += ("&APPID=" + getApplicationContext().getPackageName());
            } else {
                url += ("?UUID=" + DeviceUtils.getDeviceIdByMacAddress(this));
                url += ("&APPID=" + getApplicationContext().getPackageName());
            }
            mWebViewClient.loadUrl(url);
        }
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

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        if (mText2Speech != null) {
            mText2Speech.shutdown();
        }
        mText2Speech = null;
        mText2SpeechHandler = null;

        mWebViewClient = null;
        mWebView = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);


        releaseCpuLock();
        showSystemUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void getText2SpeechObject(OnText2SpeechListener l) {
        // do not anything.
    }

    public void checkText2SpeechAvailable() {
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

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.m
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onCheckResult(TextToSpeech tts) {
        if (tts != null) {
            mText2SpeechHandler.setTextToSpeechObject(mText2Speech);
            mText2SpeechHandler.play(mBroadcastData.getMessage());
        }
    }

    @Override
    public void onCloseWebApplication() {
        finish();
    }

    // tts
    @Override
    public void onStart(String s) {
        Log.d(TAG, "TTS onStart()");
//        ((BaseActivity)getActivity()).dismissProgressDialog();
    }

    @Override
    public void onDone(String s) {
        Log.d(TAG, "TTS onDone()");
        finish();
    }

    @Override
    public void onError(String utteranceId, int errorCode) {
        Log.d(TAG, "TTS onError()");
        Toast.makeText(this, R.string.toast_tts_error, Toast.LENGTH_SHORT).show();
        finish();
    }

}
