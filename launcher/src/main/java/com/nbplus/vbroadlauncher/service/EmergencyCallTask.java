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
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class EmergencyCallTask extends AsyncTask<Void, Void, Void> {
    //private ProgressDialog progress = null;
    private Context mContext;
    private Handler mHandler;
    private String mGetServerPath;

    public EmergencyCallTask(Context context) {
        this.mContext = context;
    }

    public EmergencyCallTask(Context context, Handler handler, String url) {
        this.mContext = context;
        this.mHandler = handler;
        this.mGetServerPath = url;
    }

    @Override
    protected Void doInBackground(Void... params) {
        RequestQueue requestQueue = Volley.newRequestQueue(mContext);
        RadioChannelInfo response = null;

        RequestFuture<RadioChannelInfo> future = RequestFuture.newFuture();

        String url = mGetServerPath + "?DEVICE_ID=" + LauncherSettings.getInstance(mContext).getDeviceID();
        GsonRequest request = new GsonRequest(Request.Method.GET, url, RadioChannelInfo.class, future, future);
        requestQueue.add(request);

        try {
            response = future.get(); // this will block (forever)
        } catch (InterruptedException e) {
            // exception handling
            e.printStackTrace();
        } catch (ExecutionException e) {
            // exception handling
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Void result) {
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
