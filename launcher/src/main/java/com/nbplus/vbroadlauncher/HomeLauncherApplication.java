/*
 * Copyright (c) 2015. NB Plus (www.nbplus.co.kr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nbplus.vbroadlauncher;

import android.app.Application;
import android.content.Intent;

import com.nbplus.push.data.PushConstants;
import com.nbplus.push.PushService;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
import com.nbplus.vbroadlauncher.service.InstalledApplicationTask;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.basdroid.common.StringUtils;

/**
 * Created by basagee on 2015. 6. 1..
 */
@ReportsCrashes(formUri = "https://collector.tracepot.com/eee1cc67")
public class HomeLauncherApplication extends Application  {
    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);

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
