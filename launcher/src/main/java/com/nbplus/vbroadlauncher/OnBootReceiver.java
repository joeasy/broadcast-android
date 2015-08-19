package com.nbplus.vbroadlauncher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nbplus.push.PushService;
import com.nbplus.push.data.PushConstants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;

import org.basdroid.common.StringUtils;

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
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            // do your thing here
            Log.i(TAG, "Intent.ACTION_BOOT_COMPLETED or android.intent.action.QUICKBOOT_POWERON received !!");
//            VBroadcastServer serverInfo = LauncherSettings.getInstance(context).getServerInformation();
//            if (serverInfo != null && StringUtils.isEmptyString(serverInfo.getPushInterfaceServer()) == false) {
//                ComponentName componentName = new ComponentName(context.getPackageName(), PushService.class.getName());
//                Intent i = new Intent();
//                i.setComponent(componentName);
//                i.setAction(PushConstants.ACTION_START_SERVICE);
//                i.putExtra(PushConstants.EXTRA_START_SERVICE_IFADDRESS, serverInfo.getPushInterfaceServer());
//                ComponentName serviceName = context.startService(i);
//                if (serviceName == null) {
//                    Log.e(TAG, "Could not start service " + componentName.toString());
//                } else {
//                    Log.d(TAG, "Start service " + componentName.toString());
//                }
//            }
        }
    }
}
