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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.PushPayloadData;
import com.nbplus.vbroadlauncher.service.BroadcastChatHeadService;

import java.lang.ref.WeakReference;


/**
 * Created by basagee on 2015. 5. 28..
 */
public class RealtimeBroadcastProxyActivity extends BaseActivity implements BaseActivity.OnText2SpeechListener {
    private static final String TAG = RealtimeBroadcastProxyActivity.class.getSimpleName();

    private long mBroadcastPayloadIdx = -1;

    private static final int HANDLER_BROADCAST_CHATHEAD_VIEW_DETACHED = 1000;
    private final BroadcastChatHeadProxyActivityHandler mHandler = new BroadcastChatHeadProxyActivityHandler(this);

    // 핸들러 객체 만들기
    private static class BroadcastChatHeadProxyActivityHandler extends Handler {
        private final WeakReference<RealtimeBroadcastProxyActivity> mActivity;

        public BroadcastChatHeadProxyActivityHandler(RealtimeBroadcastProxyActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            RealtimeBroadcastProxyActivity activity = mActivity.get();
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
            case HANDLER_BROADCAST_CHATHEAD_VIEW_DETACHED :
                long idx = (long)msg.obj;
                if (idx == mBroadcastPayloadIdx) {
                    Log.d(TAG, "HANDLER_BROADCAST_CHATHEAD_VIEW_DETACHED, finish... ");
                    finish();
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
                case Constants.ACTION_BROADCAST_CHATHEAD_VIEW_DETACHED :
                    Message msg = new Message();
                    msg.what = HANDLER_BROADCAST_CHATHEAD_VIEW_DETACHED;
                    msg.obj = intent.getLongExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, -1);
                    mHandler.sendMessage(msg);
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
        filter.addAction(Constants.ACTION_BROADCAST_CHATHEAD_VIEW_DETACHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);

        hideSystemUI();

        Intent i;
        Intent intent = getIntent();

        mBroadcastPayloadIdx = System.currentTimeMillis();
        if (intent != null) {
            PushPayloadData data = intent.getParcelableExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA);
            if (data == null) {
                Log.d(TAG, ">>");
                finish();
                return;
            }

            if (Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(data.getServiceType())) {
                mcheckText2SpeechLister = this;
                checkText2SpeechAvailable();
            } else {
                i = new Intent(this, BroadcastChatHeadService.class);
                i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, mBroadcastPayloadIdx);
                i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, data);
                startService(i);
            }
        }
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
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
            Intent intent = getIntent();
            Intent i = new Intent(this, BroadcastChatHeadService.class);
            i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, mBroadcastPayloadIdx);
            i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, intent.getParcelableExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA));
            startService(i);
        }
    }
}
