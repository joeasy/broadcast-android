package com.nbplus.vbroadlauncher.service;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.nbplus.vbroadlauncher.data.BaseApiResult;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

import org.basdroid.volley.GsonRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class SendIoTDeviceListTask extends BaseServerApiAsyncTask {
    private static final String TAG = SendIoTDeviceListTask.class.getSimpleName();

    private String mRequestBody = null;
    public void setBroadcastApiData(Context context, Handler handler, String path, String body) {
        this.mContext = context;
        this.mHandler = handler;
        this.mServerPath = path;
        this.mRequestBody = body;

    }

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
        String url = builder.toString();

        RequestFuture<RadioChannelInfo> future = RequestFuture.newFuture();

        GsonRequest request = new GsonRequest(Request.Method.POST, url, mRequestBody, BaseApiResult.class, future, future);
        request.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 3, 1.0f));
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
        return response;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(BaseApiResult result) {
        if (mHandler != null) {
            if (result == null) {
                result = new BaseApiResult();
            }
            result.setObject(mRequestBody);

            Message message = new Message();
            message.what = Constants.HANDLER_MESSAGE_SEND_IOT_DEVICE_LIST_COMPLETE_TASK;
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
