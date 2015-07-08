package com.nbplus.vbroadlauncher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.nbplus.push.PushService;
import com.nbplus.push.data.PushConstants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;

import org.basdroid.common.StringUtils;

public class BroadcastPushReceiver extends BroadcastReceiver {
    private static final String TAG = BroadcastPushReceiver.class.getName();

    public BroadcastPushReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (PushConstants.ACTION_PUSH_STATUS_CHANGED.equals(action)) {
            Log.d(TAG, "Receive.. broadcast ACTION_PUSH_STATUS_CHANGED from push service. re-direct to activity!!!");
            ComponentName componentName = new ComponentName(context.getPackageName(), HomeLauncherActivity.class.getName());
            Intent i = new Intent();
            i.setComponent(componentName);
            i.setAction(action);
            i.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else if (PushConstants.ACTION_PUSH_MESSAGE_RECEIVED.equals(action)) {
            Log.d(TAG, "Receive.. broadcast ACTION_PUSH_MESSAGE_RECEIVED from push service. re-direct to activity!!!");
            ComponentName componentName = new ComponentName(context.getPackageName(), HomeLauncherActivity.class.getName());
            Intent i = new Intent();
            i.setComponent(componentName);
            i.setAction(action);
            i.putExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA, intent.getStringExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
