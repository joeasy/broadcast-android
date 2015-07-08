package com.nbplus.vbroadlauncher;

import android.app.Application;
import android.content.Intent;

import com.nbplus.push.data.PushConstants;
import com.nbplus.push.PushService;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
import com.nbplus.vbroadlauncher.service.InstalledApplicationTask;

import org.basdroid.common.StringUtils;

/**
 * Created by basagee on 2015. 6. 1..
 */
public class HomeLauncherApplication extends Application  {
    @Override
    public void onCreate() {
        super.onCreate();

        // pre-load installed application list
        new InstalledApplicationTask(this).execute();

        // start push agent service
        VBroadcastServer serverInfo = LauncherSettings.getInstance(this).getServerInformation();
        if (serverInfo != null && StringUtils.isEmptyString(serverInfo.getPushInterfaceServer()) == false) {
            Intent intent = new Intent(this, PushService.class);
            intent.setAction(PushConstants.ACTION_START_SERVICE);
            intent.putExtra(PushConstants.EXTRA_START_SERVICE_IFADDRESS, serverInfo.getPushInterfaceServer());
            startService(intent);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
