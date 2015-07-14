package com.nbplus.vbroadlauncher;

/**
 * Created by basagee on 2015. 5. 15..
 */
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
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
import com.nbplus.push.data.PushConstants;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;

import com.nbplus.vbroadlauncher.fragment.LauncherBroadcastFragment;
import com.nbplus.vbroadlauncher.fragment.LauncherFragment;
import com.nbplus.vbroadlauncher.fragment.RegisterFragment;
import com.nbplus.vbroadlauncher.location.FetchAddressIntentService;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.DisplayUtils;
import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.vov.vitamio.LibsChecker;

public class HomeLauncherActivity extends BaseActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult> {

    // LogCat tag
    private static final String TAG = HomeLauncherActivity.class.getSimpleName();

    protected Handler mActivityHandler = new Handler();

    private ArrayList<OnActivityInteractionListener> mActivityInteractionListener = new ArrayList<>();

    /**
     * Constant used in the location settings dialog.
     */
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    protected Location mLastLocation;
    private Locale mCurrentLocale;
    private FrameLayout mBroadcastFramelayout;
    /**
     * The formatted location address.
     */
    protected Address mAddressOutput;
    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    // Google client to interact with Google API
    protected GoogleApiClient mGoogleApiClient;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 1000; // 10 meters
    // boolean flag to toggle periodic location updates
    protected boolean mRequestingLocationUpdates = true;
    protected boolean mAddressRequested = false;

    private LocationRequest mLocationRequest;
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    protected LocationSettingsRequest mLocationSettingsRequest;


    protected Status mCheckLocationSettingStatus = null;

    //
    protected static final String REQUESTING_LOCATION_UPDATES_KEY = "RequestLocationUpdates";
    protected static final String LOCATION_KEY = "Location";

    // 앱이 설치후 실행하면.. 런처설정시에 기존액티비티가 살아있는 상태로 새로운 액티비티가 실행된다.
    // 실행중인 액티비티를 죽인다.
    private long mActivityRunningTime = -1;
    private static final int HANDLER_MESSAGE_LAUNCHER_ACTIVITY_RUNNING = 1;

    private final HomeLauncherActivityHandler mHandler = new HomeLauncherActivityHandler(this);

    // 핸들러 객체 만들기
    private static class HomeLauncherActivityHandler extends Handler {
        private final WeakReference<HomeLauncherActivity> mActivity;

        public HomeLauncherActivityHandler(HomeLauncherActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            HomeLauncherActivity activity = mActivity.get();
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
            case HANDLER_MESSAGE_LAUNCHER_ACTIVITY_RUNNING :
                Long runningTime = (Long)msg.obj;

                if (runningTime > this.mActivityRunningTime) {
                    Log.d(TAG, "HANDLER_MESSAGE_LAUNCHER_ACTIVITY_RUNNING. new activity is create.. finish");
                    finish();
                }
                break;
//            case Constants.HANDLER_MESSAGE_PUSH_STATUS_CHANGED :
//            case Constants.HANDLER_MESSAGE_PUSH_MESAGE_RECEIVED :
//                setPushServiceStatus(msg.arg1);
//                boolean isProcessed = false;
//                if (mActivityInteractionListener != null) {
//                    for (OnActivityInteractionListener listener : mActivityInteractionListener) {
//                        isProcessed = listener.onPushReceived(msg);
//                    }
//                }
//                if (isProcessed == false) {
//                    Log.e(TAG, ">> Why... 이런경우에는 어떻게 해야 하는거냐????");
//                }
//                break;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.d(TAG, ">> mBroadcastReceiver action received = " + action);
            mHandler.removeMessages(Constants.HANDLER_MESSAGE_PLAY_RADIO_CHANNEL_TIMEOUT);
            // send handler message
            if (Constants.ACTION_LAUNCHER_ACTIVITY_RUNNING.equals(action)) {
                Message msg = new Message();
                msg.what = HANDLER_MESSAGE_LAUNCHER_ACTIVITY_RUNNING;
                msg.obj = intent.getLongExtra(Constants.EXTRA_LAUNCHER_ACTIVITY_RUNNING, 0);
                mHandler.sendMessage(msg);
            }/* else if (PushConstants.ACTION_PUSH_STATUS_CHANGED.equals(action)) {
                Message msg = new Message();
                msg.what = HANDLER_MESSAGE_PUSH_STATUS_CHANGED;
                msg.arg1 = intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED);
                mHandler.sendMessage(msg);
            } else if (PushConstants.ACTION_PUSH_MESSAGE_RECEIVED.equals(action)) {
                Message msg = new Message();
                msg.what = HANDLER_MESSAGE_PUSH_MESAGE_RECEIVED;
                msg.arg1 = intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED);
                msg.obj = intent.getStringExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA);
                mHandler.sendMessage(msg);
            } */
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_home_launcher);

        mCurrentLocale = getResources().getConfiguration().locale;
        if (BuildConfig.DEBUG) {
            Point p = DisplayUtils.getScreenSize(this);
            Log.d(TAG, "Screen size px = " + p.x + ", py = " + p.y);
            Point screen = p;
            p = DisplayUtils.getScreenDp(this);
            Log.d(TAG, "Screen dp x = " + p.x + ", y = " + p.y);
            int density = DisplayUtils.getScreenDensity(this);
            Log.d(TAG, "Screen density = " + density);
        }

        boolean isTablet = DisplayUtils.isTabletDevice(this);
        if (isTablet) {
            //is tablet
            Log.d(TAG, "Tablet");
        } else {
            //is phone
            Log.d(TAG, "isTabletDevice() returns Phone.. now check display inches");
            double diagonalInches = DisplayUtils.getDisplayInches(this);
            if (diagonalInches >= 6.4) {
                // 800x400 인경우 portrait 에서 6.43 가량이 나온다.
                // 6.5inch device or bigger
                Log.d(TAG, "DisplayUtils.getDisplayInches() bigger than 6.5");
            } else {
                // smaller device
                Log.d(TAG, "DisplayUtils.getDisplayInches() smaller than 6.5");
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
                alert.setMessage(R.string.alert_phone_message);
                alert.show();

                return;
            }
        }

        if (isMyLauncherDefault()) {
            Log.d(TAG, "isMyLauncherDefault() == true");
            // fake home key event.
            Intent fakeIntent = new Intent();
            fakeIntent.setAction(Intent.ACTION_MAIN);
            fakeIntent.addCategory(Intent.CATEGORY_HOME);
            fakeIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_FORWARD_RESULT
                    | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(fakeIntent);
        } else {
            Log.d(TAG, "isMyLauncherDefault() == false");
            //resetPreferredLauncherAndOpenChooser();
        }

        // 앱이 설치후 실행하면.. 런처설정시에 기존액티비티가 살아있는 상태로 새로운 액티비티가 실행된다.
        // 실행중인 액티비티를 죽인다.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LAUNCHER_ACTIVITY_RUNNING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);

        // 프래그먼트가 직접 처리하자.
//        filter = new IntentFilter();
//        filter.addAction(PushConstants.ACTION_PUSH_STATUS_CHANGED);
//        filter.addAction(PushConstants.ACTION_PUSH_MESSAGE_RECEIVED);
//        registerReceiver(mBroadcastReceiver, filter);

        Intent intent = new Intent(Constants.ACTION_LAUNCHER_ACTIVITY_RUNNING);
        mActivityRunningTime = System.currentTimeMillis();
        intent.putExtra(Constants.EXTRA_LAUNCHER_ACTIVITY_RUNNING, mActivityRunningTime);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if (!NetworkUtils.isConnected(this)) {
            showNetworkConnectionAlertDialog();
        }

        // vitamio library load
        if (!LibsChecker.checkVitamioLibs(this)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            alert.setMessage(R.string.alert_media_message);
            alert.show();
            return;
        }

        // set background image
        setContentViewByOrientation();
        if (LauncherSettings.getInstance(this).getPreferredUserLocation() == null) {
            Location defaultLocation = new Location("stub");

            defaultLocation.setLongitude(126.929810);
            defaultLocation.setLatitude(37.488201);

            LauncherSettings.getInstance(this).setPreferredUserLocation(defaultLocation);
        }

        if (StringUtils.isEmptyString(LauncherSettings.getInstance(this).getDeviceID())) {
            String deviceID = DeviceUtils.getDeviceIdByMacAddress(this);
            LauncherSettings.getInstance(this).setDeviceID(deviceID);
        }

        Log.d(TAG, "SET Device ID = " + LauncherSettings.getInstance(this).getDeviceID());

        // First we need to check availability of play services
        mResultReceiver = new AddressResultReceiver(mActivityHandler);
        // Set defaults, then update using values stored in the Bundle.
        mAddressRequested = false;
        mAddressOutput = null;

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

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

        /**
         * 서버정보가 없거나 숏컷정보가 없다면 회원등록이나 설정에 문제가 있다.
         * 회원등록 페이지를 호출한다.
         */
        mBroadcastFramelayout = (FrameLayout) findViewById(R.id.realtimeBroadcastFragment);
        if (mBroadcastFramelayout != null) {
            mBroadcastFramelayout.setVisibility(View.GONE);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Bundle bundle;

        if (LauncherSettings.getInstance(this).isCompletedSetup() == false) {

            // open user register fragment
            RegisterFragment registerFragment = new RegisterFragment();
            bundle = new Bundle();
            registerFragment.setArguments(bundle);

            fragmentTransaction.replace(R.id.launcherFragment, registerFragment);
        } else {
            // open main launcher fragment
            LauncherFragment launcherFragment = new LauncherFragment();
            bundle = new Bundle();
            launcherFragment.setArguments(bundle);

            fragmentTransaction.replace(R.id.launcherFragment, launcherFragment);
        }
        fragmentTransaction.commit();

    }

    // when screen orientation changed.
    protected void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
        }
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
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(Constants.ACTION_BROWSER_ACTIVITY_CLOSE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "HomeLauncherActivity onDestroy() call mGoogleApiClient.disconnect()");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        if (mText2Speech != null) {
            mText2Speech.shutdown();
        }
        mText2Speech = null;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        //unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        // Once connected with google api, get the location
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Log.d(TAG, "Connected: But can't get location !!!");
        } else {
            onLocationChanged(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.d(TAG, ">>> onLocationChanged().. Update location");
            // Assign the new location
            mLastLocation = location;

            Log.d(TAG, ">>> latitude = " + location.getLatitude());
            Log.d(TAG, ">>> Longitude = " + location.getLongitude());

            boolean isChanged = false;
            Location prevLocation = LauncherSettings.getInstance(this).getPreferredUserLocation();
            LauncherSettings.getInstance(this).setPreferredUserLocation(location);

            if (prevLocation != null) {
                double lonDiff = prevLocation.getLongitude() - location.getLongitude();
                double latDiff = prevLocation.getLatitude() - location.getLatitude();

                if (lonDiff > 0.5 || lonDiff < -0.5 || latDiff > 0.5 || latDiff < -0.5) {
                    isChanged = true;
                }
            }

            // sned to weatherview
            if (prevLocation == null || isChanged) {
                Intent intent=new Intent(Constants.LOCATION_CHANGED_ACTION);
                sendBroadcast(intent);
            }
        } else {
            Log.d(TAG, ">>> onLocationChanged().. but location is null");
        }
    }

    public void onAddressResultReceived(boolean isSucceeded, Address address, String errorMessage) {
        // Display the address string or an error message sent from the intent service.
        mAddressOutput = address;

        // Reset. Enable the Fetch Address button and stop showing the progress bar.
        mAddressRequested = false;

    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected boolean startFetchAddressIntentService() {
        if (mLastLocation != null) {
            // If GoogleApiClient isn't connected, we process the user's request by setting
            // mAddressRequested to true. Later, when GoogleApiClient connects, we launch the service to
            // fetch the address. As far as the user is concerned, pressing the Fetch Address button
            // immediately kicks off the process of getting the address.
            mAddressRequested = true;
            // Create an intent for passing to the intent service responsible for fetching the address.
            Intent intent = new Intent(this, FetchAddressIntentService.class);

            // Pass the result receiver as an extra to the service.
            intent.putExtra(Constants.RECEIVER, mResultReceiver);

            // Pass the location data as an extra to the service.
            intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

            // Start the service. If the service isn't already running, it is instantiated and started
            // (creating a process for it if needed); if it is running then it remains running. The
            // service kills itself automatically once all intents are processed.
            startService(intent);
            return true;
        }
        return false;
    }

    protected void updateLocaton() {
        if (mGoogleApiClient!= null && mGoogleApiClient.isConnected()) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            onLocationChanged(location);
            if (location == null) {
                Log.d(TAG, "Connected: Can't get location !!!");
            }
        }
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == Constants.SUCCESS_RESULT) {
                onAddressResultReceived(true, (Address) resultData.getParcelable(Constants.RESULT_DATA_KEY), null);
            } else {
                onAddressResultReceived(false, null, resultData.getString(Constants.RESULT_MESSAGE));
            }
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
                updateLocaton();
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

    protected int getLocationSettingStatusCode() {
        return mCheckLocationSettingStatus != null ? mCheckLocationSettingStatus.getStatusCode() : -1;
    }

    protected void startResolutionForResult() {
        try {
            // Show the dialog by calling startResolutionForResult(), and check the result
            // in onActivityResult().
            mCheckLocationSettingStatus.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
        } catch (IntentSender.SendIntentException e) {
            Log.i(TAG, "PendingIntent unable to execute request.");
        }
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (this.mActivityInteractionListener != null) {
            for (OnActivityInteractionListener listener : mActivityInteractionListener) {
                listener.onBackPressed();
            }
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
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        //startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;

            case Constants.START_ACTIVITY_REQUEST_CHECK_TTS_DATA :
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    // check korean
                    mText2Speech = new TextToSpeech(this, this);
                } else {
                    Log.d(TAG, "여기서 제대로 설정안했다면 관두자.... 사용자 맘인데...");
                    if (mcheckText2SpeechLister != null) {
                        mcheckText2SpeechLister.onCheckResult(null);
                        mText2Speech = null;
                    }
                    showText2SpeechAlertDialog();

                    LauncherSettings.getInstance(this).setIsCheckedTTSEngine(true);
                }
                break;
        }
    }

    /**
     * Activity에서 일어나는 Interaction lister 이다.
     * 여러개의 프래그먼트에서 동시에 처리하지 않도록 하나만 유지된다.
     * @param listener
     */
    public void registerActivityInteractionListener(OnActivityInteractionListener listener) {
        if (this.mActivityInteractionListener.indexOf(listener) < 0) {
            this.mActivityInteractionListener.add(listener);
        }
    }

    public void unRegisterActivityInteractionListener(OnActivityInteractionListener listener) {
        this.mActivityInteractionListener.remove(listener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged !!!");
        setContentViewByOrientation();
    }

    private void setContentViewByOrientation() {
        int wallpaperId = LauncherSettings.getInstance(this).getWallpaperId();
        int orientation = DisplayUtils.getScreenOrientation(this);
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            getWindow().setBackgroundDrawableResource(LauncherSettings.landWallpaperResource[wallpaperId]);
        } else {
            getWindow().setBackgroundDrawableResource(LauncherSettings.portWallpaperResource[wallpaperId]);
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
    /**
    @Override
    protected void onNewIntent(Intent intent) {
        //super.onNewIntent(intent);
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "onNewIntent.. action = " + action);
//        if (PushConstants.ACTION_PUSH_STATUS_CHANGED.equals(action) || PushConstants.ACTION_PUSH_MESSAGE_RECEIVED.equals(action)) {
    //            setPushServiceStatus(intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED));
//
//            if (mActivityInteractionListener != null) {
//                for (OnActivityInteractionListener listener : mActivityInteractionListener) {
//                    listener.onPushReceived(intent);
//                }
//            }
//        }
    }
    */

    protected boolean isMyLauncherDefault() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        final String myPackageName = getPackageName();
        List<ComponentName> activities = new ArrayList<ComponentName>();
        final PackageManager packageManager = (PackageManager) getPackageManager();

        // You can use name of your package here as third argument
        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            if (myPackageName.equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }
    protected void resetPreferredLauncherAndOpenChooser() {
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, HomeLauncherActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

//        Intent selector = new Intent(Intent.ACTION_MAIN);
//        selector.addCategory(Intent.CATEGORY_HOME);
//        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(selector);


//        Intent fakeIntent = new Intent();
//        fakeIntent.setAction(Intent.ACTION_MAIN);
//        fakeIntent.addCategory(Intent.CATEGORY_HOME);
//        fakeIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
//                | Intent.FLAG_ACTIVITY_FORWARD_RESULT
//                | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
//                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//        startActivity(fakeIntent);

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }
}
