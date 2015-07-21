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
    protected RequestFuture<PushInterfaceData> mRequestFuture;

    public GetPushInterfaceTask(Context context, Handler handler, final RequestFuture<PushInterfaceData> requestFuture) {
        this.mContext = context;
        this.mHandler = handler;
        this.mRequestFuture = requestFuture;
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
        PushInterfaceData response = null;
        try {
            response = mRequestFuture.get(); // this will block (forever)
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
    protected void onPostExecute(PushInterfaceData result) {
        if (mHandler != null) {
            Message message = new Message();
            message.what = PushConstants.HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA;
            message.obj = result;
            mHandler.sendMessage(message);
        }
    }

}
