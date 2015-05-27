package com.nbplus.vbroadlauncher.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.nbplus.vbroadlauncher.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by basagee on 2015. 5. 20..
 */
public class ShowAllLaunchAppsInfo {

    // when using singleton
    private volatile static ShowAllLaunchAppsInfo uniqueInstance;

    public static ShowAllLaunchAppsInfo getInstance() {
        if (uniqueInstance == null) {
            // 이렇게 하면 처음에만 동기화 된다
            synchronized (ShowAllLaunchAppsInfo.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new ShowAllLaunchAppsInfo();
                }
            }
        }
        return uniqueInstance;
    }

    private Context context;

    public static int getMaxPageItemSize(Context context) {
        return context.getResources().getInteger(R.integer.app_grid_numcolumns) * context.getResources().getInteger(R.integer.app_grid_numrows);
    }

    private ArrayList<ApplicationInfo> mLaunchAppsList = new ArrayList<ApplicationInfo>();
    int mAppListSize = 0;

    private ShowAllLaunchAppsInfo() {
    }

    public void updateApplicationList(Context context) {
        /**
         * asynctask 로 변경하는 도중에 리스트를 변경하는 경우 adapter 에 문제가
         * 생기므로 완료후에 업데이트 한다.
         */
        List<ApplicationInfo> appList = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        //mLaunchAppsList.clear();

        ArrayList<ApplicationInfo> launchAppList = new ArrayList<ApplicationInfo>();
        for (ApplicationInfo info : appList) {
            try {
                if (null != context.getPackageManager().getLaunchIntentForPackage(info.packageName)) {
                    launchAppList.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mLaunchAppsList = launchAppList;
        mAppListSize = mLaunchAppsList.size();
    }

    public int getCount() {
        return mAppListSize/*this.mLaunchAppsList == null ? 0 : this.mLaunchAppsList.size()*/;
    }

    public ArrayList<ApplicationInfo> getSubList(int from, int to) {

        if (from <= 0) from = 0;
        if (to >= mLaunchAppsList.size()) to = mLaunchAppsList.size();

        if (from > to) {
            return new ArrayList<ApplicationInfo>();
        } else {
            return new ArrayList<ApplicationInfo>(mLaunchAppsList.subList(from, to));
        }
    }

}
