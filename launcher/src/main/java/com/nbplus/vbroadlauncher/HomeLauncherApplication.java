package com.nbplus.vbroadlauncher;

import android.app.Application;

import com.nbplus.vbroadlauncher.service.InstalledApplicationTask;

/**
 * Created by basagee on 2015. 6. 1..
 */
public class HomeLauncherApplication extends Application  {
    @Override
    public void onCreate() {
        super.onCreate();
        new InstalledApplicationTask(this).execute();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
