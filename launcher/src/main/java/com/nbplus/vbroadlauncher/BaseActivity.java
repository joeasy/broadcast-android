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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.push.data.PushConstants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;

import org.basdroid.common.NetworkUtils;

import java.util.Locale;

/**
 * Created by basagee on 2015. 6. 24..
 */
public abstract class BaseActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String TAG = BaseActivity.class.getSimpleName();

    protected TextToSpeech mText2Speech;
    protected OnText2SpeechListener mcheckText2SpeechLister = null;

    ProgressDialogFragment mProgressDialogFragment;
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

    public void showWifiEnableAlertDialog() {
        new AlertDialog.Builder(this).setMessage(R.string.alert_wifi_enable_message)
                .setTitle(R.string.alert_wifi_enable_title)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                NetworkUtils.enableWifiNetwork(getApplicationContext());
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
                Log.e(TAG, String.format("TextToSpeech.setLanguage(%s) fail!", Locale.KOREA.getDisplayName()));
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

    /**
     * Push agent status...
     */
    protected int mPushServiceStatus = PushConstants.PUSH_STATUS_VALUE_DISCONNECTED;

    public boolean isPushServiceConnected() {
        return (mPushServiceStatus == PushConstants.PUSH_STATUS_VALUE_CONNECTED);
    }

    public void setPushServiceStatus(int pushServiceStatus) {
        this.mPushServiceStatus = pushServiceStatus;
    }

    public void showProgressDialog() {
        dismissProgressDialog();
        mProgressDialogFragment = ProgressDialogFragment.newInstance();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(mProgressDialogFragment, "progress_dialog");
        transaction.commitAllowingStateLoss();
        // for java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        //mProgressDialogFragment.show(getSupportFragmentManager(), "progress_dialog");
    }
    public void dismissProgressDialog() {
        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        dismissProgressDialog();
    }
}
