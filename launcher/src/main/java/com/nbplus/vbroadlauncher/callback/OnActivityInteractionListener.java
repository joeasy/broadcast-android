package com.nbplus.vbroadlauncher.callback;

import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Message;

/**
 * Created by basagee on 2015. 5. 19..
 */
public interface OnActivityInteractionListener {
    public void onBackPressed();
    public void onDataChanged();
    public boolean onPushReceived(Message message);
}
