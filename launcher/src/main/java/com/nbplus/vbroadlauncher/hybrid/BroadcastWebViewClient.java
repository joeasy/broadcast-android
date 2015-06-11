package com.nbplus.vbroadlauncher.hybrid;

import android.app.Activity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nbplus.hybrid.BasicWebViewClient;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RegSettingData;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
import com.nbplus.vbroadlauncher.fragment.LauncherFragment;

import org.basdroid.common.StringUtils;

/**
 * Created by basagee on 2015. 5. 19..
 */
public class BroadcastWebViewClient extends BasicWebViewClient {
    private static final String TAG = BroadcastWebViewClient.class.getSimpleName();

    public BroadcastWebViewClient(Activity activity, WebView view) {
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
                        if (StringUtils.isEmptyString(serverInfo.getPushInterfaceServer())) {
                            Toast.makeText(mContext, R.string.empty_value, Toast.LENGTH_SHORT);
                            return;
                        }
                    }

                    LauncherSettings.getInstance(mContext).setVillageCode(settings.getVillageCode());
                    LauncherSettings.getInstance(mContext).setVillageName(settings.getVillageName());
                    LauncherSettings.getInstance(mContext).setServerInformation(settings.getServerInformation());
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

        mContext.finish();
        return;
    }

}
