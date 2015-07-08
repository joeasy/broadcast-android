package com.nbplus.vbroadlauncher;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.nbplus.vbroadlauncher.data.LauncherSettings;

import java.util.Locale;

/**
 * Created by basagee on 2015. 6. 24..
 */
public abstract class BaseActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String TAG = BaseActivity.class.getSimpleName();

    protected TextToSpeech mText2Speech;
    protected OnText2SpeechListener mcheckText2SpeechLister = null;

    private PowerManager.WakeLock mCpuWakeLock;
    private int mDefaultWindowFlags = -1;

    public void showNetworkConnectionAlertDialog() {
        new AlertDialog.Builder(this).setMessage(R.string.alert_network_message)
                //.setTitle(R.string.alert_network_title)
                .setCancelable(true)
                .setNegativeButton(R.string.alert_network_btn_check_wifi,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                            }
                        })
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    public void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    public abstract void getText2SpeechObject(OnText2SpeechListener l);

    public interface OnText2SpeechListener {
        public void onCheckResult(TextToSpeech tts);
    }

    // for tts korean check
    @Override
    public void onInit(int status) {
        Log.d(TAG, "> TTS onInit()");

        LauncherSettings.getInstance(this).setIsCheckedTTSEngine(true);
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, String.format("TextToSpeechManager.onInit(%d) fail!", status));
            Log.d(TAG, "여기서 제대로 설정안했다면 관두자.... 사용자 맘인데...");
            if (mcheckText2SpeechLister != null) {
                mcheckText2SpeechLister.onCheckResult(null);
                mText2Speech.shutdown();
                mText2Speech = null;
            }
            showText2SpeechAlertDialog();
        } else {
            int result = mText2Speech.setLanguage(Locale.KOREA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, String.format("TextToSpeech.setLanguage(%d) fail!", Locale.KOREA));
                Log.d(TAG, "여기서 제대로 설정안했다면 관두자.... 사용자 맘인데...");
                if (mcheckText2SpeechLister != null) {
                    mcheckText2SpeechLister.onCheckResult(null);
                    mText2Speech.shutdown();
                    mText2Speech = null;
                }
                showText2SpeechAlertDialog();
            } else {
                if (mcheckText2SpeechLister != null) {
                    mcheckText2SpeechLister.onCheckResult(mText2Speech);
                } else {
                    if (mcheckText2SpeechLister != null) {
                        mcheckText2SpeechLister.onCheckResult(null);
                        mText2Speech.shutdown();
                        mText2Speech = null;
                    }
                }
            }
        }
    }

    public void showText2SpeechAlertDialog() {
        new AlertDialog.Builder(this).setMessage(R.string.alert_tts_message)
                //.setTitle(R.string.alert_network_title)
                .setCancelable(true)
                .setNegativeButton(R.string.alert_tts_btn_settings,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent ttsIntent = new Intent();
                                ttsIntent.setAction(Settings.ACTION_SETTINGS);
                                startActivity(ttsIntent);
                            }
                        })
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                .show();
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

        Window window = this.getWindow();
        mDefaultWindowFlags = window.getAttributes().flags;
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    public void releaseCpuLock() {
        Log.e(TAG, "Releasing cpu wake lock");

        if (mCpuWakeLock != null) {
            mCpuWakeLock.release();
            mCpuWakeLock = null;
        }
        Window wind = this.getWindow();
        if ((mDefaultWindowFlags & WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD) != 0) {
            wind.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        if ((mDefaultWindowFlags & WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED) != 0) {
            wind.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        if ((mDefaultWindowFlags & WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) != 0) {
            wind.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }
}
