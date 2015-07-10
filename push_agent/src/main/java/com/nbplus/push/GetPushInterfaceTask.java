package com.nbplus.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.data.PushInterfaceData;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.StringUtils;
import org.basdroid.volley.GsonRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class GetPushInterfaceTask extends AsyncTask<Void, Void, PushInterfaceData> {

    protected Context mContext;
    protected Handler mHandler;
    protected String mServerPath;

    public GetPushInterfaceTask(Context context, Handler handler, String path) {
        this.mContext = context;
        this.mHandler = handler;
        this.mServerPath = path;
    }

    class GetPushInterfaceRequestBody {
        @SerializedName("DEVICE_ID")
        public String deviceId;
        @SerializedName("DEVICE_TYPE")
        public String deviceType;
        @SerializedName("VERSION")
        public String pushVersion;
        @SerializedName("MAKER")
        public String vendor;
        @SerializedName("MODEL")
        public String model;
        @SerializedName("OS")
        public String os;
    }

    @Override
    protected void onPreExecute() {
                super.onPreExecute();
        }

    @Override
    protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
        }
    @Override
    protected void onCancelled() {
                super.onCancelled();
        }

    @Override
    protected PushInterfaceData doInBackground(Void... voids) {
        RequestQueue requestQueue = Volley.newRequestQueue(mContext);
        PushInterfaceData response = null;

        String prefName = mContext.getApplicationContext().getPackageName() + "_preferences";
        SharedPreferences prefs = mContext.getSharedPreferences(prefName, Context.MODE_PRIVATE);

        // load from preferences..
        String deviceId = prefs.getString(PushConstants.KEY_DEVICE_ID, "");
        if (StringUtils.isEmptyString(deviceId)) {
            deviceId = DeviceUtils.getDeviceIdByMacAddress(mContext);
            prefs.edit().putString(PushConstants.KEY_DEVICE_ID, deviceId).apply();
        }
        String url = mServerPath;

        GetPushInterfaceRequestBody requestBody = new GetPushInterfaceRequestBody();
        requestBody.deviceId = deviceId;
        requestBody.os = Build.VERSION.RELEASE;
        requestBody.pushVersion = Integer.toString(BuildConfig.VERSION_CODE);
        requestBody.vendor = Build.MANUFACTURER;
        requestBody.model = DeviceUtils.getDeviceName();
        requestBody.os = Build.ID + " " + Build.VERSION.RELEASE;
        requestBody.deviceType = "android";

        Gson gson = new GsonBuilder().create();
        String strRequestBody = gson.toJson(requestBody, new TypeToken<GetPushInterfaceRequestBody>(){}.getType());

//        int retryCount = 0;
//        while (retryCount < 3) {        // retry 3 times
            RequestFuture<PushInterfaceData> future = RequestFuture.newFuture();

            GsonRequest request = new GsonRequest(Request.Method.POST, url, strRequestBody, PushInterfaceData.class, future, future);
            requestQueue.add(request);

            try {
                response = future.get(); // this will block (forever)
                Thread.sleep(1000);
//                break;
            } catch (InterruptedException e) {
                // exception handling
                e.printStackTrace();
            } catch (ExecutionException e) {
                // exception handling
                e.printStackTrace();
            }
//            retryCount++;
//        }
        return response;
    }

    @Override
    protected void onPostExecute(PushInterfaceData result) {
        // sample data
//        if (result == null || !PushConstants.RESULT_OK.equals(result.resultCode)) {
//            result = new PushInterfaceData();
//            result.resultCode = PushConstants.RESULT_OK;
//            result.sessionKey = "P7V80283M";
//            result.deviceAuthKey = "1OWE2RTYU";
//            result.interfaceServerAddress = "175.207.46.132";
//            result.interfaceServerPort = "7002";
//            result.keepAliveSeconds = "30";
//        }
        /*else {
            result.interfaceServerAddress = "192.168.77.111";
            result.interfaceServerPort = "7005";
        }*/
        // end of sample data

        if (mHandler != null) {
            Message message = new Message();
            message.what = PushConstants.HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA;
            message.obj = result;
            mHandler.sendMessage(message);
        }
    }

}
