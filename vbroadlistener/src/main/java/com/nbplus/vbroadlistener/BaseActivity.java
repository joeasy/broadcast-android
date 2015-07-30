package com.nbplus.vbroadlistener;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.LocationServices;
import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.vbroadlistener.preference.LauncherSettings;

import java.util.Locale;

/**
 * Created by basagee on 2015. 6. 24..
 */
public abstract class BaseActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, TextToSpeech.OnInitListener {
    private static final String TAG = BaseActivity.class.getSimpleName();

    protected TextToSpeech mText2Speech;
    protected OnText2SpeechListener mcheckText2SpeechLister = null;

    ProgressDialogFragment mProgressDialogFragment;
    private PowerManager.WakeLock mCpuWakeLock;
    private int mDefaultWindowFlags = -1;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Next available request code.
     */
    protected static final int NEXT_AVAILABLE_REQUEST_CODE = 2;

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

    /**
     * Called when activity gets visible. A connection to Drive services need to
     * be initiated as soon as the activity is visible. Registers
     * {@code ConnectionCallbacks} and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
        }
        mGoogleApiClient.connect();
    }


    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Called when activity gets invisible. Connection to Drive service needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        dismissProgressDialog();
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
    }

    /**
     * Called when {@code mGoogleApiClient} is disconnected.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution is
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }
    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Getter for the {@code GoogleApiClient}.
     */
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
}
