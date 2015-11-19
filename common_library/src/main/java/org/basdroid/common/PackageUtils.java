/*
 * Copyright (c) 2015. Basagee Yun. (www.basagee.tk)
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

package org.basdroid.common;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by basagee on 2015. 5. 15..
 */
public class PackageUtils {

    /**
     * 단말에 특정패키지 이름을 가진 Application이 설치되어있는지 조회한다.
     * @param context
     * @param targetPackage
     * @return
     */
    public static boolean isPackageExisted(Context context, String targetPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            /*PackageInfo info = */pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * 구글플레이 마켓에서 패키지 상세정보를 보여준다.
     * @param context
     * @param targetPackage
     */
    public static void showMarketDetail(Context context, String targetPackage) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + targetPackage));
        context.startActivity(intent);
    }

    /**
     * 구글플레이마켓에서 Publisher 이름으로 검색한다.
     * @param context
     * @param publisherName
     */
    public static void showMarketPublisher(Context context, String publisherName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://search?q=pub:" + publisherName));
        context.startActivity(intent);
    }

    /**
     * 구글플레이마켓에서 query string으로 검색한다.
     * @param context
     * @param query
     */
    public static void searchFromMarket(Context context, String query) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://search?q=" + query));
        context.startActivity(intent);
    }
    public static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    public static boolean isActivePackage(Context context, String packageName) {
        if (context == null || StringUtils.isEmptyString(packageName)) {
            return false;
        }
        String[] activePackages;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            activePackages = getActivePackages(context);
        } else {
            activePackages = getActivePackagesCompat(context);
        }
        if (activePackages != null) {
            for (String activePackage : activePackages) {
                if (activePackage.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] getActivePackagesCompat(Context context) {
        final ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
        final ComponentName componentName = taskInfo.get(0).topActivity;
        final String[] activePackages = new String[1];
        activePackages[0] = componentName.getPackageName();
        return activePackages;
    }


    public static String[] getActivePackages(Context context) {
        final ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        final Set<String> activePackages = new HashSet<String>();

//        final List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
//        for (ActivityManager.AppTask task : tasks) {
//            activePackages.add(task.getTaskInfo().baseIntent.getComponent().getPackageName());
//        }
        final List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                activePackages.addAll(Arrays.asList(processInfo.pkgList));
            }
        }
        return activePackages.toArray(new String[activePackages.size()]);
    }
}
