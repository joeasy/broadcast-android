package com.nbplus.vbroadlauncher;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;

import com.nbplus.vbroadlauncher.data.ShowAllLaunchAppsInfo;
import com.nbplus.vbroadlauncher.service.LoadInstalledApplication;

/**
 * Created by basagee on 2015. 6. 1..
 */
public class HomeLauncherApplication extends Application  {
    @Override
    public void onCreate() {
        super.onCreate();
        new LoadInstalledApplication(this).execute();
    }

}
