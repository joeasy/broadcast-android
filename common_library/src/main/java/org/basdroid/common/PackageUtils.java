package org.basdroid.common;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

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
}
