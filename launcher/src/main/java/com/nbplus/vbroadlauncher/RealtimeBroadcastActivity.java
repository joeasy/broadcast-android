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

package com.nbplus.vbroadlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
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
import java.util.Timer;
import java.util.TimerTask;


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

    boolean mIsTTS = false;
    boolean mIsMuted = false;

    private long mBroadcastPayloadIdx = -1;
    private int mStreamMusicVolume = 0;

    private static final int HANDLER_BROADCAST_STARTED = 1000;
    private static final int HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE = 1001;
    private static final int HANDLER_MESSAGE_SETUP_CURRENT_PLAYING = 1002;
    private final RealtimeBroadcastActivityHandler mHandler = new RealtimeBroadcastActivityHandler(this);

    // 핸들러 객체 만들기
    private static class RealtimeBroadcastActivityHandler extends Handler {
        private final WeakReference<RealtimeBroadcastActivity> mActivity;

        public RealtimeBroadcastActivityHandler(RealtimeBroadcastActivity activity) {
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

    private class BroadcastPushEvent {
        public long broadcastPayloadIdx;
        public PushPayloadData payloadData;
    }
    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case HANDLER_MESSAGE_SETUP_CURRENT_PLAYING :
                LauncherSettings.getInstance(this).setCurrentPlayingBroadcastType(mBroadcastData.getServiceType());
                break;
            case HANDLER_BROADCAST_STARTED :
                BroadcastPushEvent evt = (BroadcastPushEvent)msg.obj;

                if (evt == null || evt.payloadData == null) {
                    Log.d(TAG, "not broadcast push.... ignore...");
                    break;
                }
                String serviceType = evt.payloadData.getServiceType();
                if (!Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST.equals(serviceType) &&
                        !Constants.PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST.equals(serviceType) &&
                        !Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(serviceType)) {
                    Log.d(TAG, "not broadcast push.... ignore...");
                    break;
                }

                if (evt.broadcastPayloadIdx > mBroadcastPayloadIdx) {
                    Log.d(TAG, "HANDLER_BROADCAST_STARTED, finish..currIdx = " + mBroadcastPayloadIdx + ", newIdx = " + evt.broadcastPayloadIdx);
                    if (Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(mBroadcastData.getServiceType())) {
                        finishActivity();
                    } else {
                        if (mWebViewClient != null && !mWebViewClient.isClosingByWebApp()) {
                            mWebViewClient.onCloseWebApplicationByUser();
                        }
                    }
                }
            case HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE :
                if (Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(mBroadcastData.getServiceType())) {
                    finishActivity();
                } else {
                    if (mWebViewClient != null && !mWebViewClient.isClosingByWebApp()) {
                        mWebViewClient.onCloseWebApplicationByUser();
                    }
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
                    BroadcastPushEvent evt = new BroadcastPushEvent();

                    evt.broadcastPayloadIdx = intent.getLongExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, -1);
                    evt.payloadData = intent.getParcelableExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA);

                    msg.obj = evt;
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

        Intent intent = getIntent();
        if (intent == null || !PushConstants.ACTION_PUSH_MESSAGE_RECEIVED.equals(intent.getAction())) {
            Log.d(TAG, "empty or none broadcast intent value ...");
            finishActivity();
            return;
        }

        mBroadcastData = intent.getParcelableExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA);
        if (mBroadcastData == null) {
            Log.d(TAG, ">> payload data is null");
            finishActivity();
            return;
        }

        mBroadcastPayloadIdx = intent.getLongExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, -1);
        Log.d(TAG, ">> onCreate() mBroadcastPayloadIdx= " + mBroadcastPayloadIdx);

        // 서버에서 몇십ms  단위로거의 동일 시간에 전달되는 경우 먼저온 푸시의 액티비티가 생성되기도전에
        // broadcast 만전달될 수있다.
        // 액티비티가 생성된 이후에 던지자.
        Intent i = new Intent(this, RealtimeBroadcastActivity.class);
        i.setAction(intent.getAction());
        i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, mBroadcastData);
        i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, mBroadcastPayloadIdx);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PushConstants.ACTION_PUSH_MESSAGE_RECEIVED);
        filter.addAction(Constants.ACTION_BROWSER_ACTIVITY_CLOSE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);

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

            mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_SETUP_CURRENT_PLAYING, 800);
            Log.d(TAG, "text broadcast = " + mBroadcastData.getMessage());

            mText2SpeechHandler = new TextToSpeechHandler(this, this);
            checkText2SpeechAvailable();
            mIsTTS = true;
        } else {
            // 실시간, 일반음성방송
            mWebView = (WebView)findViewById(R.id.webview);
            mWebViewClient = new RealtimeBroadcastWebViewClient(this, mWebView, this);
            mWebViewClient.setBackgroundTransparent();

            String url = mBroadcastData.getMessage();
            if (url.indexOf("?") > 0) {
                url += ("&UUID=" + LauncherSettings.getInstance(this).getDeviceID());
                url += ("&APPID=" + getApplicationContext().getPackageName());
            } else {
                url += ("?UUID=" + LauncherSettings.getInstance(this).getDeviceID());
                url += ("&APPID=" + getApplicationContext().getPackageName());
            }
            mWebViewClient.loadUrl(url);
            mIsTTS = false;
        }
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mStreamMusicVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),  AudioManager.FLAG_PLAY_SOUND);
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (mWebViewClient != null) {
            mWebViewClient.onBackPressed();
        }
    }

    private void microphoneMute(boolean onoff) {
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        if (!audioManager.isMicrophoneMute() && onoff) {
            mIsMuted = true;
            audioManager.setMicrophoneMute(onoff);
        } else if (!onoff) {
            audioManager.setMicrophoneMute(onoff);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onPause()");
        super.onResume();

        if (!mIsTTS) {
            microphoneMute(true);
        }
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        if (!mIsTTS) {
            microphoneMute(false);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    public void getText2SpeechObject(OnText2SpeechListener l) {
        // do not anything.
    }

    public void checkText2SpeechAvailable() {
//        String model = Build.MODEL;
//        if (model.equals("111")) {
            Log.d(TAG, "checkText2SpeechAvailable()");
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, Constants.START_ACTIVITY_REQUEST_CHECK_TTS_DATA);
//        } else {
//            onActivityResult(Constants.START_ACTIVITY_REQUEST_CHECK_TTS_DATA, TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, null);
//        }
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
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
        Log.d(TAG, "onCloseWebApplication()");
        finishActivity();
    }

    @Override
    public void onPageFinished(boolean success) {
        if (success) {
            mHandler.sendEmptyMessage(HANDLER_MESSAGE_SETUP_CURRENT_PLAYING);
        } else {
            LauncherSettings.getInstance(this).setCurrentPlayingBroadcastType(null);
            finishActivity();
        }
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
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE);
    }

    @Override
    public void onError(String utteranceId, int errorCode) {
        Log.d(TAG, "TTS onError()");
        Toast.makeText(this, R.string.toast_tts_error, Toast.LENGTH_SHORT).show();
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_BROWSER_ACTIVITY_CLOSE);
    }

    private void finishActivity() {
        Log.d(TAG, "finishActivity()");
        LauncherSettings.getInstance(this).setCurrentPlayingBroadcastType(null);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        if (mText2Speech != null) {
            mText2Speech.shutdown();
        }
        mText2Speech = null;
        mText2SpeechHandler = null;
        mBroadcastPayloadIdx = -1;

        mWebViewClient = null;
        mWebView = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        releaseCpuLock();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showSystemUI();
            }
        });

        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamMusicVolume, AudioManager.FLAG_PLAY_SOUND);

        finish();
    }

    static boolean userInteraction = false;
    @Override
    public void showText2SpeechAlertDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this).setMessage(R.string.alert_tts_message)
                //.setTitle(R.string.alert_network_title)
                .setCancelable(true)
                .setNegativeButton(R.string.alert_tts_btn_settings,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                userInteraction = true;
                                Intent ttsIntent = new Intent();
                                ttsIntent.setAction(Settings.ACTION_SETTINGS);
                                startActivity(ttsIntent);
                            }
                        })
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                finishActivity();
                                userInteraction = true;
                            }
                        })
                .show();
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                if (userInteraction == false) {
                    dialog.dismiss(); // when the task active then close the dialog
                    finishActivity();
                }
                t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
            }
        }, 10000); // after 2 second (or 2000 miliseconds), the task will be active.
    }

}
