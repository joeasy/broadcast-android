package com.nbplus.vbroadlauncher;

/**
 * Created by basagee on 2015. 5. 15..
 */
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.AsyncTask;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
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
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.ShowAllLaunchAppsInfo;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.PreferredLocation;

import com.nbplus.vbroadlauncher.fragment.LauncherFragment;
import com.nbplus.vbroadlauncher.fragment.RegisterFragment;
import com.nbplus.vbroadlauncher.location.FetchAddressIntentService;
import com.nbplus.vbroadlauncher.callback.OnFragmentInteractionListener;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.DisplayUtils;
import org.basdroid.common.StringUtils;

import io.vov.vitamio.LibsChecker;


public class HomeLauncherActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult>, OnFragmentInteractionListener {

    // LogCat tag
    private static final String TAG = HomeLauncherActivity.class.getSimpleName();

    protected Handler mActivityHandler = new Handler();

    private OnActivityInteractionListener mActivityInteractionListener;

    /**
     * Constant used in the location settings dialog.
     */
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    protected Location mLastLocation;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_launcher);

        boolean isTablet = DisplayUtils.isTabletDevice(this);
        if (isTablet) {
            //is tablet
            Log.d("Device", "Tablet");
        } else {
            //is phone
            Log.d("Device", "Phone");
//            AlertDialog.Builder alert = new AlertDialog.Builder(this);
//            alert.setPositiveButton(R.string.alert_phone_finish_ok, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    finish();
//                }
//            });
//            alert.setMessage(R.string.alert_phone_message);
//            alert.show();
//            return;
        }

        Point p = DisplayUtils.getScreenDp(this);
        Log.d(TAG, "Screen dp x = " + p.x + ", y = " + p.y);
        // vitamio library load
        if (!LibsChecker.checkVitamioLibs(this))
            return;

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "HomeLauncherActivity onDestroy() call mGoogleApiClient.disconnect()");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        stopLocationUpdates();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, ">>> mGoogleApiClient connected.. Update location");
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        // Once connected with google api, get the location
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Log.d(TAG, "Connected: Can't get location !!!");
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
            this.mActivityInteractionListener.onBackPressed();
        }
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
        }
    }

    /**
     * Activity에서 일어나는 Interaction lister 이다.
     * 여러개의 프래그먼트에서 동시에 처리하지 않도록 하나만 유지된다.
     * @param listener
     */
    public void setOnActivityInteractionListener(OnActivityInteractionListener listener) {
        this.mActivityInteractionListener = listener;
    }

    // TODO: 필요한가?????
    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(TAG, "onFragmentInteraction()");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentViewByOrientation();
    }

    private void setContentViewByOrientation() {
        int wallpagerResourceId = LauncherSettings.getInstance(this).getWallpagerResourceId();
        int orientation = DisplayUtils.getScreenOrientation(this);
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            getWindow().setBackgroundDrawableResource(LauncherSettings.landWallpaperResource[wallpagerResourceId]);
        } else {
            getWindow().setBackgroundDrawableResource(LauncherSettings.portWallpaperResource[wallpagerResourceId]);
        }
    }
}
