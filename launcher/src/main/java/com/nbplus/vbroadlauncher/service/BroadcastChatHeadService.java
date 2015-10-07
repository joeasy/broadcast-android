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

package com.nbplus.vbroadlauncher.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.PushPayloadData;
import com.nbplus.vbroadlauncher.hybrid.RealtimeBroadcastWebViewClient;
import com.nbplus.vbroadlauncher.hybrid.TextToSpeechHandler;

import org.basdroid.common.DeviceUtils;

import java.util.Locale;

/**
 * Created by basagee on 2015. 7. 14..
 */
public class BroadcastChatHeadService extends Service implements TextToSpeech.OnInitListener, TextToSpeechHandler.OnUtteranceProgressListener, RealtimeBroadcastWebViewClient.OnRealtimeBroadcastWebViewListener {
    private static final String TAG = BroadcastChatHeadService.class.getSimpleName();

    private WindowManager windowManager;
    private View mChatHead;
    private LayoutInflater inflater;

    private PushPayloadData mBroadcastData;
    private boolean mIsPlaying = false;

    // for audio broadcast
    WebView mWebView;
    RealtimeBroadcastWebViewClient mWebViewClient;

    // for text broadcast
    TextView mTextView;
    TextToSpeech mText2Speech;
    TextToSpeechHandler mText2SpeechHandler;

    private PowerManager.WakeLock mCpuWakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        inflater = LayoutInflater.from(this);
        mChatHead = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createBroadcastChatHead(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void createBroadcastChatHead(Intent intent) {
        if (intent == null) {
            return;
        }

        mBroadcastData = intent.getParcelableExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA);
        if (mBroadcastData == null) {
            Log.d(TAG, "Broadcast data is not found!!!");
            return;
        }
        long mBroadcastIndex = intent.getLongExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, -1);
        String pushType = mBroadcastData.getServiceType();
        if (!Constants.PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST.equals(pushType) &&
                !Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST.equals(pushType) &&
                !Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(pushType)) {
            Log.d(TAG, "This is not broadcast push type !!!");
            return;
        }
        if (mIsPlaying && mChatHead != null) {
            removeChatHead(true);
        }

        mIsPlaying = true;

        int layout = -1;
        if (Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(pushType)) {
            layout = R.layout.fragment_text_broadcast;
        } else {
            layout = R.layout.fragment_audio_broadcast;
        }
        mChatHead = inflater.inflate(layout, null);

        if (Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(mBroadcastData.getServiceType())) {
            // 문자방송
            mTextView = (TextView) mChatHead.findViewById(R.id.broadcast_text);
            mTextView.setText(mBroadcastData.getMessage());
            mTextView.setVerticalScrollBarEnabled(true);
            mTextView.setHorizontalScrollBarEnabled(false);
            mTextView.setMovementMethod(new ScrollingMovementMethod());

            mText2SpeechHandler = new TextToSpeechHandler(this, this);
            mText2Speech = new TextToSpeech(this, this);
        } else {
            // 실시간, 일반음성방송
            mWebView = (WebView)mChatHead.findViewById(R.id.webview);
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
        }
        mChatHead.setTag(mBroadcastIndex);

//        mChatHead.findViewById(R.id.btn_dismiss).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                removeChatHead();
//            }
//        });

        /**
         * To create an overlay view, when setting up the LayoutParams DON'T set the type to TYPE_SYSTEM_OVERLAY.

         Instead set it to TYPE_PHONE.

         Use the following flags:
         FLAG_NOT_TOUCH_MODAL
         FLAG_WATCH_OUTSIDE_TOUCH
         FLAG_NOT_TOUCH_MODAL << I found this one to be quite important. Without it,
         focus is given to the overlay and soft-key (home, menu, etc.) presses are not passed to the activity below.
         */
        int flag = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, flag, 0, PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER;

        /**
         * do not use...
        mChatHead.findViewById(R.id.txt_title).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(mChatHead, params);
                        return true;
                }
                return false;
            }
        });
        */
        addChatHead(mChatHead, params);
    }

    public void addChatHead(View chatHead, WindowManager.LayoutParams params) {
        windowManager.addView(chatHead, params);
        acquireCpuWakeLock();
    }

    public void removeChatHead(boolean isForceByOther) {
        Log.d(TAG, "removeChatHead()");

        if (mText2Speech != null) {
            mText2Speech.shutdown();
        }
        mText2Speech = null;
        mText2SpeechHandler = null;

        if (mWebViewClient != null && isForceByOther) {
            mWebViewClient.onCloseWebApplicationByUser();
        }
        mWebViewClient = null;
        mWebView = null;

        releaseCpuLock();

        if (mChatHead != null) {
            windowManager.removeView(mChatHead);
            long broadcastIdx = (long)mChatHead.getTag();
            if (broadcastIdx > 0) {
                Intent i = new Intent();
                i.setAction(Constants.ACTION_BROADCAST_CHATHEAD_VIEW_DETACHED);
                i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, broadcastIdx);
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            }
        }
        mChatHead = null;
        mIsPlaying = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeChatHead(false);
    }

    // TTS
    // for tts korean check
    @Override
    public void onInit(int status) {
        Log.d(TAG, "> TTS onInit()");

        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, String.format("TextToSpeechManager.onInit(%d) fail!", status));
            Log.d(TAG, "여기서 제대로 설정안했다면 관두자.... 사용자 맘인데...");
            mText2Speech.shutdown();
            mText2Speech = null;

            // TODO : 문자방송실패 팝업????
        } else {
            int result = mText2Speech.setLanguage(Locale.KOREA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, String.format("TextToSpeech.setLanguage(%s) fail!", Locale.KOREA.getDisplayName()));

                // TODO : 문자방송실패 팝업????
                mText2Speech.shutdown();
                mText2Speech = null;
            } else {
                mText2SpeechHandler.setTextToSpeechObject(mText2Speech);
                mText2SpeechHandler.play(mBroadcastData.getMessage());
            }
        }
    }

    @Override
    public void onStart(String s) {
        Log.d(TAG, "TTS onStart()");
//        ((BaseActivity)getActivity()).dismissProgressDialog();
    }

    @Override
    public void onDone(String s) {
        Log.d(TAG, "TTS onDone()");
        removeChatHead(false);
    }

    @Override
    public void onError(String utteranceId, int errorCode) {
        Log.d(TAG, "TTS onError()");
        Toast.makeText(this, R.string.toast_tts_error, Toast.LENGTH_SHORT).show();
        removeChatHead(false);
    }

    public void acquireCpuWakeLock() {
        Log.e(TAG, "Acquiring cpu wake lock");
        if (mCpuWakeLock != null) {
            return;
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mCpuWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "I'm your father");
        mCpuWakeLock.acquire();
    }

    public void releaseCpuLock() {
        Log.e(TAG, "Releasing cpu wake lock");

        if (mCpuWakeLock != null) {
            mCpuWakeLock.release();
            mCpuWakeLock = null;
        }
    }

    @Override
    public void onCloseWebApplication() {
        removeChatHead(false);
    }

    @Override
    public void onPageFinished(boolean success) {

    }

}
