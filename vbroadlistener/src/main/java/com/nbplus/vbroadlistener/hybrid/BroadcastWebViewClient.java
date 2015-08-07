package com.nbplus.vbroadlistener.hybrid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nbplus.hybrid.BasicWebViewClient;
import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.vbroadlistener.BaseActivity;
import com.nbplus.vbroadlistener.R;
import com.nbplus.vbroadlistener.data.Constants;
import com.nbplus.vbroadlistener.data.RegSettingData;
import com.nbplus.vbroadlistener.data.VBroadcastServer;
import com.nbplus.vbroadlistener.gcm.RegistrationIntentService;
import com.nbplus.vbroadlistener.preference.LauncherSettings;

import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;

/**
 * Created by basagee on 2015. 5. 19..
 */
public class BroadcastWebViewClient extends BasicWebViewClient implements TextToSpeechHandler.OnUtteranceProgressListener {
    private static final String TAG = BroadcastWebViewClient.class.getSimpleName();

    enum BroadcastPlayState {
        STOPPED,
        VOICE_PLAYING,
        VOICE_PAUSED,
        TTS_PLAYING
    };
    BroadcastPlayState mBroadcastPlayState = BroadcastPlayState.STOPPED;
    TextToSpeechHandler mText2SpeechHandler = null;
    String mText2SpeechPlayText = null;
    boolean mIsClosingByWebApp = false;

    private static final int HANDLER_MESSAGE_START_TTS = 1;
    private static final int HANDLER_MESSAGE_DONE_TTS = 2;
    private static final int HANDLER_MESSAGE_ERROR_TTS = 3;
    private BroadcastWebViewClientHandler mHandler;

    // 핸들러 객체 만들기
    private static class BroadcastWebViewClientHandler extends Handler {
        private final WeakReference<BroadcastWebViewClient> mActivity;

        public BroadcastWebViewClientHandler(BroadcastWebViewClient client) {
            mActivity = new WeakReference<>(client);
        }

        @Override
        public void handleMessage(Message msg) {
            BroadcastWebViewClient client = mActivity.get();
            if (client != null) {
                client.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        Log.d(TAG, "handleMessage tts = " + msg.what);
        switch (msg.what) {
            case HANDLER_MESSAGE_START_TTS :
                dismissProgressDialog();
                mBroadcastPlayState = BroadcastPlayState.TTS_PLAYING;
                break;
            case HANDLER_MESSAGE_DONE_TTS :
                onCompleteTTSBroadcast();
                mBroadcastPlayState = BroadcastPlayState.STOPPED;
                break;
            case HANDLER_MESSAGE_ERROR_TTS :
                dismissProgressDialog();
                mBroadcastPlayState = BroadcastPlayState.STOPPED;
                onCompleteTTSBroadcast();
        }
    }

    public BroadcastWebViewClient(Activity activity, WebView view) {
        super(activity, view, activity.getString(R.string.app_name), activity.getString(R.string.app_name));
        mWebView.setWebViewClient(this);
        mWebView.addJavascriptInterface(this, JAVASCRIPT_IF_NAME);
        mHandler = new BroadcastWebViewClientHandler(this);
    }

    /**
     * 디바이스의 UUID 조회. mac address 기반 40bytes SHA-1 value
     */
    @Override
    @JavascriptInterface
    public String getDeviceId() {
        return LauncherSettings.getInstance(mContext).getDeviceID();
    }

    @JavascriptInterface
    public void setVillageName(String villageName) {
        if (!StringUtils.isEmptyString(villageName)) {
            LauncherSettings.getInstance(mContext).setVillageName(villageName);
        }
    }
    /**
     *
     * @param data
     */
    @JavascriptInterface
    public void setServerInformation(String data) {
        Log.d(TAG, ">> setServerInformation() received = " + data);

        if (StringUtils.isEmptyString(data)) {
            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
        } else {
            try {
                data = new String(data.getBytes("utf-8"));
                Gson gson = new GsonBuilder().create();
                RegSettingData settings = gson.fromJson(data, RegSettingData.class);
                if (settings != null) {
                    VBroadcastServer serverInfo = settings.getServerInformation();
                    if (serverInfo == null) {
                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                        return;
                    }
//                    if (StringUtils.isEmptyString(settings.getVillageName())) {
//                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
//                        return;
//                    }
                    /*if (settings.getServerInformation() == null) {
                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                        return;
                    } else*/ {
                        //VBroadcastServer serverInfo = settings.getServerInformation();
                        if (StringUtils.isEmptyString(serverInfo.getApiServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (StringUtils.isEmptyString(serverInfo.getDocServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (StringUtils.isEmptyString(serverInfo.getPushInterfaceServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

//                    LauncherSettings.getInstance(mContext).setVillageCode(settings.getVillageCode());
//                    LauncherSettings.getInstance(mContext).setVillageName(settings.getVillageName());
                    LauncherSettings.getInstance(mContext).setServerInformation(serverInfo);
                    LauncherSettings.getInstance(mContext).setIsCompletedSetup(true);

                } else {
                    Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.parse_error, Toast.LENGTH_SHORT).show();
            }
        }
    }


    @JavascriptInterface
    public void onStartBroadcastMediaStream(boolean isTTS, String ttsString) {
        Log.d(TAG, ">> onStartBroadcastMediaStream() called = " + isTTS + ", tts = " + ttsString);
        if (mBroadcastPlayState == BroadcastPlayState.TTS_PLAYING) {
            // stop previous playing tts.
            if (mText2SpeechHandler != null) {
                mText2SpeechHandler.stop();
            }
        }

        if (isTTS) {
            // start tts playing
            mText2SpeechPlayText = ttsString;
            if (StringUtils.isEmptyString(mText2SpeechPlayText)) {
                Log.e(TAG, "> do not play tts. is empty string..");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                showProgressDialog();
            }
            if (mText2SpeechHandler == null) {
                mText2SpeechHandler = new TextToSpeechHandler(mContext, this);
                ((BaseActivity)mContext).getText2SpeechObject(new BaseActivity.OnText2SpeechListener() {
                    @Override
                    public void onCheckResult(TextToSpeech tts) {
                        if (tts != null && tts instanceof TextToSpeech) {
                            mText2SpeechHandler.setTextToSpeechObject(tts);
                            mText2SpeechHandler.play(mText2SpeechPlayText);
                            mBroadcastPlayState = BroadcastPlayState.TTS_PLAYING;
                        } else {
                            mText2SpeechPlayText = null;
                            mBroadcastPlayState = BroadcastPlayState.STOPPED;
                            dismissProgressDialog();
                        }
                    }
                });
            } else {
                if (mText2SpeechHandler.getTextToSpeechObject() == null) {
                    ((BaseActivity)mContext).getText2SpeechObject(new BaseActivity.OnText2SpeechListener() {
                        @Override
                        public void onCheckResult(TextToSpeech tts) {
                            if (tts != null && tts instanceof TextToSpeech) {
                                mText2SpeechHandler.setTextToSpeechObject(tts);
                                mText2SpeechHandler.play(mText2SpeechPlayText);
                                mBroadcastPlayState = BroadcastPlayState.TTS_PLAYING;
                            } else {
                                mText2SpeechPlayText = null;
                                mBroadcastPlayState = BroadcastPlayState.STOPPED;
                                dismissProgressDialog();
                            }
                        }
                    });
                } else {
                    mText2SpeechHandler.play(mText2SpeechPlayText);
                    mBroadcastPlayState = BroadcastPlayState.TTS_PLAYING;

                }
            }
        }
    }

    @JavascriptInterface
    public void onPauseBroadcastMediaStream() {
        Log.d(TAG, ">> onPauseBroadcastMediaStream() called");
        if (mBroadcastPlayState == BroadcastPlayState.VOICE_PLAYING) {
            mBroadcastPlayState = BroadcastPlayState.VOICE_PAUSED;
        }
    }

    @JavascriptInterface
    public void onStopBroadcastMediaStream() {
        Log.d(TAG, ">> onStopBroadcastMediaStream() called");

        if (mBroadcastPlayState == BroadcastPlayState.TTS_PLAYING) {
            // stop previous playing tts.
            mText2SpeechPlayText = null;
            mText2SpeechHandler.stop();
        }
        mBroadcastPlayState = BroadcastPlayState.STOPPED;
    }

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @Override
    @JavascriptInterface
    public void closeWebApplication() {
        Log.d(TAG, ">> closeWebApplication() called");

        if (mBroadcastPlayState == BroadcastPlayState.TTS_PLAYING) {
            // stop previous playing tts.
            mText2SpeechPlayText = null;
            mText2SpeechHandler.finalize();
        }
        mIsClosingByWebApp = true;

        mBroadcastPlayState = BroadcastPlayState.STOPPED;
        mContext.finish();
    }


    /**
     * GCM 등록
     * @return boolean
     */
    @JavascriptInterface
    public boolean registerGcm() {
        Log.d(TAG, ">> call registerGcm()");
        // Start IntentService to register this application with GCM.
        Intent intent = new Intent(mContext, RegistrationIntentService.class);
        intent.setAction(Constants.REGISTER_GCM);
        mContext.startService(intent);
        return true;
    }

    /**
     * GCM 해제
     * @return boolean
     */
    @JavascriptInterface
    public boolean unRegisterGcm() {
        Log.d(TAG, ">> call unRegisterGcm()");
        // Start IntentService to register this application with GCM.
        Intent intent = new Intent(mContext, RegistrationIntentService.class);
        intent.setAction(Constants.UNREGISTER_GCM);
        mContext.startService(intent);
        return true;
    }

    /**
     * IoT GW 로부터 IoT Devices  목록을 갱신한다.
     */
    @JavascriptInterface
    public void updateIoTDevices() {

    }
    ////////////////////////////////
    /**
     * 아래의 함수들은 자바스크립트를 Native 에서 호출할 필요가 있을때 사용한다.
     * 아래에서 불리는 자바스크립트 function 들은 웹앱에서 구현이 되어 있어야 한다.
     *
     */
    /**
     * GCM 등록 토큰 전달
     * @param gcmRegToken 토큰
     */
    public void onRegistered(String gcmRegToken) {
        mWebView.loadUrl("javascript:window.onRegistered('" + gcmRegToken + "');");
    }

    /**
     * GCM 해제 결과
     */
    public void onUnRegistered() {
        mWebView.loadUrl("javascript:window.onUnRegistered();");
    }

    /**
     * 검색된 IoT device 목록 전달
     * @param iotDevices device list
     */
    public void onUpdateIoTDevices(String iotDevices) {
        mWebView.loadUrl("javascript:window.onUpdateIoTDevices('" + iotDevices + "');");
    }

    public void onCompleteTTSBroadcast() {
        mWebView.loadUrl("javascript:window.onCompleteTTSBroadcast();");
    }

    public void onCloseWebApplicationByUser() {
        if (!mIsClosingByWebApp) {
            mWebView.loadUrl("javascript:window.onCloseWebApplicationByUser();");

            if (mBroadcastPlayState == BroadcastPlayState.TTS_PLAYING) {
                // stop previous playing tts.
                mText2SpeechPlayText = null;
                mText2SpeechHandler.finalize();
            }
            mBroadcastPlayState = BroadcastPlayState.STOPPED;
            mContext.finish();
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        this.showProgressDialog();
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.d(TAG, "onPageFinished() = " + url);
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @Override
    public void onStart(String s) {
        Log.d(TAG, "TTS onStart()");
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_START_TTS);
    }

    @Override
    public void onDone(String s) {
        Log.d(TAG, "TTS onDone()");
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_DONE_TTS);
    }

    @Override
    public void onError(String utteranceId, int errorCode) {
        Log.d(TAG, "TTS onError()");
        mHandler.sendEmptyMessage(HANDLER_MESSAGE_ERROR_TTS);
    }

    public void stopPageLoading() {
        Log.d(TAG, "stopPageLoading() = ");
        this.dismissProgressDialog();
        if (mWebView != null) {
            mWebView.stopLoading();
        }
    }

    @Override
    public void loadWebUrl(String url) {
        if (url.indexOf("?") > 0) {
            url += ("&UUID=" + LauncherSettings.getInstance(mContext).getDeviceID());
            url += ("&APPID=" + mContext.getPackageName());
        } else {
            url += ("?UUID=" + LauncherSettings.getInstance(mContext).getDeviceID());
            url += ("&APPID=" + mContext.getPackageName());
        }
        mWebView.loadUrl(url);
    }
}
