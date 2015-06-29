package com.nbplus.vbroadlauncher.service;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.nbplus.vbroadlauncher.api.GsonRequest;
import com.nbplus.vbroadlauncher.data.BaseApiResult;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class SendEmergencyCallTask extends BaseServerApiAsyncTask {
    @Override
    protected BaseApiResult doInBackground(Void... params) {

        RequestQueue requestQueue = Volley.newRequestQueue(mContext);
        RadioChannelInfo response = null;
        String url = mServerPath + "?DEVICE_ID=" + LauncherSettings.getInstance(mContext).getDeviceID();

        int retryCount = 0;
        while (retryCount < 3) {        // retry 3 times
            RequestFuture<RadioChannelInfo> future = RequestFuture.newFuture();

            GsonRequest request = new GsonRequest(Request.Method.GET, url, BaseApiResult.class, future, future);
            requestQueue.add(request);

            try {
                response = future.get(); // this will block (forever)
                break;
            } catch (InterruptedException e) {
                // exception handling
                e.printStackTrace();
            } catch (ExecutionException e) {
                // exception handling
                e.printStackTrace();
            }
            retryCount++;
            continue;
        }
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
        //progress = ProgressDialog.show(AllAppsActivity.this, null,
        //        "Loading application info...");
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
