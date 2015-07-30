package com.nbplus.vbroadlistener;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.vbroadlistener.preference.LauncherSettings;

import java.util.Locale;

/**
 * Created by basagee on 2015. 6. 24..
 */
public abstract class BaseActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, TextToSpeech.OnInitListener,
        LocationListener, ResultCallback<LocationSettingsResult> {
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
     * Constant used in the location settings dialog.
     */
    protected static final int REQUEST_CHECK_SETTINGS = 2;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 1000; // 10 meters
    // boolean flag to toggle periodic location updates
    protected boolean mRequestingLocationUpdates = true;
    private LocationRequest mLocationRequest;
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    protected LocationSettingsRequest mLocationSettingsRequest;


    protected Status mCheckLocationSettingStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkPlayServices()) {
            Log.d(TAG, ">>> checkPlayServices() support");
            // Building the GoogleApi client
            buildGoogleApiClient();
            createLocationRequest();
            buildLocationSettingsRequest();

            //if (LauncherSettings.getInstance(this).getPreferredUserLocation() == null) {
            checkLocationSettings();
            //}
            Log.d(TAG, "HomeLauncherActivity onCreate() call mGoogleApiClient.connect()");
            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Google Play Service is not available !!!!!");
        }
    }

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
        stopLocationUpdates();
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
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported Play Service.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }


    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 10 meters
    }
    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }
    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates()");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates()");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    /**
     * Check if the device's location settings are adequate for the app's needs using the
     * {@link com.google.android.gms.location.SettingsApi#checkLocationSettings(GoogleApiClient,
     * LocationSettingsRequest)} method, with the results provided through a {@code PendingResult}.
     */
    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    /**
     * The callback invoked when
     * {@link com.google.android.gms.location.SettingsApi#checkLocationSettings(GoogleApiClient,
     * LocationSettingsRequest)} is called. Examines the
     * {@link com.google.android.gms.location.LocationSettingsResult} object and determines if
     * location settings are adequate. If they are not, begins the process of presenting a location
     * settings dialog to the user.
     */
    @Override
    public void onResult(LocationSettingsResult locationSettingsResult) {
        mCheckLocationSettingStatus = locationSettingsResult.getStatus();
        switch (mCheckLocationSettingStatus.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i(TAG, "All location settings are satisfied.");
                // Once connected with google api, get the location
//                updateLocaton();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    mCheckLocationSettingStatus.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        startLocationUpdates();
    }

    /**
     * Called when {@code mGoogleApiClient} is disconnected.
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
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

    @Override
    public void onLocationChanged(Location location) {
            Log.d(TAG, ">>> onLocationChanged()");
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
        } else {
            Log.i(TAG, "User chose not to make required location settings changes.");
        }
    }

    /**
     * Getter for the {@code GoogleApiClient}.
     */
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
}
