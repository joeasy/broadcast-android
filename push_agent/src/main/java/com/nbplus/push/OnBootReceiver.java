package com.nbplus.push;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
    private static final String TAG = OnBootReceiver.class.getName();

    public OnBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         * 참조 : http://stackoverflow.com/questions/7978403/boot-receiver-not-work
         * If you have HTC device you also need to register for "android.intent.action.QUICKBOOT_POWERON".
         */
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            // do your thing here
            Log.i(TAG, "Intent.ACTION_BOOT_COMPLETED or android.intent.action.QUICKBOOT_POWERON received !!");
            ComponentName componentName = new ComponentName(context.getPackageName(), RemoteService.class.getName());
            ComponentName serviceName = context.startService(new Intent().setComponent(componentName));
            if (serviceName == null) {
                Log.e(TAG, "Could not start service " + componentName.toString());
            } else {
                Log.d(TAG, "Start service " + componentName.toString());
            }
        }
    }
}
