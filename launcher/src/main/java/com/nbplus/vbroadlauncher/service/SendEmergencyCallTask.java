package com.nbplus.vbroadlauncher.service;

import android.location.Location;
import android.net.Uri;
import android.os.Message;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nbplus.vbroadlauncher.data.BaseApiResult;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

import org.basdroid.volley.GsonRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class SendEmergencyCallTask extends BaseServerApiAsyncTask {
    private static final String TAG = SendEmergencyCallTask.class.getSimpleName();

    @Override
    protected BaseApiResult doInBackground(Void... params) {

        RequestQueue requestQueue = Volley.newRequestQueue(mContext, new HurlStack() {
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection connection = super.createConnection(url);
                // Fix for bug in Android runtime(!!!):
                // https://code.google.com/p/android/issues/detail?id=24672
                connection.setRequestProperty("Accept-Encoding", "");

                return connection;
            }
        });
        BaseApiResult response = null;

        Uri.Builder builder = Uri.parse(mServerPath).buildUpon();
        //builder.appendQueryParameter("DEVICE_ID", LauncherSettings.getInstance(mContext).getDeviceID());
        String url = builder.toString();

        Location location = LauncherSettings.getInstance(mContext).getPreferredUserLocation();
        if (location == null) {
            Log.d(TAG, ">> set default location");
            location = new Location("stub");
            location.setLongitude(126.929810);
            location.setLatitude(37.488201);

            LauncherSettings.getInstance(mContext).setPreferredUserLocation(location);
        }
        String strRequestBody = String.format("{\"DEVICE_ID\" : \"%s\", \"LAT\":\"%s\", \"LON\":\"%s\"}",
                LauncherSettings.getInstance(mContext).getDeviceID(),
                "" + location.getLatitude(),
                "" + location.getLongitude());
//        int retryCount = 0;
//        while (retryCount < 3) {        // retry 3 times
            RequestFuture<RadioChannelInfo> future = RequestFuture.newFuture();

            GsonRequest request = new GsonRequest(Request.Method.POST, url, strRequestBody, BaseApiResult.class, future, future);
            request.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 3, 1.0f));
            requestQueue.add(request);

            try {
                response = future.get(); // this will block (forever)
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
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(BaseApiResult result) {

        if (mHandler != null) {
            Message message = new Message();
            message.what = Constants.HANDLER_MESSAGE_SEND_EMERGENCY_CALL_COMPLETE_TASK;
            message.obj = result;
            mHandler.sendMessage(message);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
