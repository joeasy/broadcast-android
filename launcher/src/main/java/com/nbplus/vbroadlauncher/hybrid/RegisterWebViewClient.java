package com.nbplus.vbroadlauncher.hybrid;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nbplus.hybrid.BasicWebViewClient;
import com.nbplus.vbroadlauncher.fragment.LauncherFragment;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RegSettingData;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;

import org.basdroid.common.StringUtils;

/**
 * Created by basagee on 2015. 5. 19..
 */
public class RegisterWebViewClient extends BasicWebViewClient {
    private static final String TAG = RegisterWebViewClient.class.getSimpleName();

    public RegisterWebViewClient(Activity activity, WebView view) {
        super(activity, view);
    }

    /**
     *
     * @param data
     */
    @Override
    @JavascriptInterface
    public void setApplicationData(String data) {
        Log.d(TAG, ">> setApplicationData() received = " + data);

        if (StringUtils.isEmptyString(data)) {
            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
        } else {
            try {
                Gson gson = new GsonBuilder().create();
                RegSettingData settings = gson.fromJson(data, RegSettingData.class);
                if (settings != null) {
                    if (StringUtils.isEmptyString(settings.getVillageName())) {
                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
                        return;
                    }
                    if (settings.getServerInformation() == null) {
                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
                        return;
                    } else {
                        VBroadcastServer serverInfo = settings.getServerInformation();
                        if (StringUtils.isEmptyString(serverInfo.getApiServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
                            return;
                        }
                        if (StringUtils.isEmptyString(serverInfo.getDocServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
                            return;
                        }
                        if (StringUtils.isEmptyString(serverInfo.getPushGateway())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
                            return;
                        }
                    }
//                    if (settings.getLauncherShortcuts() == null || settings.getLauncherShortcuts().size() == 0) {
//                        Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
//                        return;
//                    }

                    LauncherSettings.getInstance(mContext).setVillageCode(settings.getVillageCode());
                    LauncherSettings.getInstance(mContext).setVillageName(settings.getVillageName());
                    LauncherSettings.getInstance(mContext).setServerInformation(settings.getServerInformation());
                    //LauncherSettings.getInstance(mContext).setLauncherShortcuts(settings.getLauncherShortcuts());
                    Log.d(TAG, ">> setApplicationData() completed !!!");
                    LauncherSettings.getInstance(mContext).setIsCompletedSetup(true);
                } else {
                    Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.parse_error, Toast.LENGTH_SHORT);
            }
        }
    }

    /**
     *
     * @param appId
     */
    @Override
    @JavascriptInterface
    public void registerPushApplication(String appId) {
        Log.d(TAG, ">> registerPushApplication() called = " + appId);
    }

    /**
     * 어플리케이션 또는 현재 액티비티를 종료한다.
     */
    @Override
    @JavascriptInterface
    public void closeWebApplication() {
        Log.d(TAG, ">> closeWebApplication() called");

        if (LauncherSettings.getInstance(mContext).isCompletedSetup() == false) {
            AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            alert.setPositiveButton(R.string.alert_settings_message, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.setMessage(R.string.alert_phone_message);
            alert.show();

        } else {
            FragmentManager fm = ((AppCompatActivity)mContext).getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();

            LauncherFragment fragment = new LauncherFragment();
            ft.replace(R.id.launcherFragment, fragment);
            ft.commit();
        }

        //mContext.finish();
        return;
    }

}
