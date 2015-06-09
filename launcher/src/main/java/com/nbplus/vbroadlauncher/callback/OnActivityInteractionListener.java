package com.nbplus.vbroadlauncher.callback;

import android.content.res.Configuration;
import android.location.Location;

/**
 * Created by basagee on 2015. 5. 19..
 */
public interface OnActivityInteractionListener {
    public void onBackPressed();
    public void onDataChanged();
    // TODO : remove later. for test...
    public void onLocationDataChanged(Location location);
}
