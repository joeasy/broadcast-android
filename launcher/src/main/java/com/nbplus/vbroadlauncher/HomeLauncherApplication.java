package com.nbplus.vbroadlauncher;

import android.app.Application;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationSettingsResult;
import com.nbplus.vbroadlauncher.callback.OnFragmentInteractionListener;

/**
 * Created by basagee on 2015. 6. 1..
 */
public class HomeLauncherApplication extends Application implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult>, OnFragmentInteractionListener {
    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onResult(LocationSettingsResult locationSettingsResult) {

    }
}
