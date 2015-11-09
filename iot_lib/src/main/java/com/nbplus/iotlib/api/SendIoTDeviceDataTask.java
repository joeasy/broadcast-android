/*
 * Copyright (c) 2015. NB Plus (www.nbplus.co.kr)
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

package com.nbplus.iotlib.api;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.nbplus.iotlib.IoTInterface;

import org.basdroid.volley.GsonRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class SendIoTDeviceDataTask extends BaseServerApiAsyncTask {
    private static final String TAG = SendIoTDeviceDataTask.class.getSimpleName();

    private IoTCollectedData mRequestBody = null;
    Gson mGson;

    public void setSendColledApiData(Context context, Handler handler, String path, IoTCollectedData data) {
        this.mContext = context;
        this.mHandler = handler;
        this.mServerPath = path;
        this.mRequestBody = data;

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

        RequestFuture<BaseApiResult> future = RequestFuture.newFuture();
        if (mGson == null) {
            mGson = new GsonBuilder().create();
        }
        String json = mGson.toJson(mRequestBody);
        Log.d(TAG, "Send device data = " + json);

        GsonRequest request = new GsonRequest(Request.Method.POST, url, json, BaseApiResult.class, future, future);
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
            message.what = IoTInterface.HANDLER_SEND_IOT_DEVICE_DATA_TASK_COMPLETED;
            Bundle b = new Bundle();
            b.putParcelable("data", mRequestBody);
            result.setObject(b);
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
