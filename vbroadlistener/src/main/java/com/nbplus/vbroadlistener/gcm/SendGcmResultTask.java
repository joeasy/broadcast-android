package com.nbplus.vbroadlistener.gcm;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.nbplus.vbroadlistener.data.BaseApiResult;
import com.nbplus.vbroadlistener.preference.LauncherSettings;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.StringUtils;
import org.basdroid.volley.GsonRequest;

import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class SendGcmResultTask  extends AsyncTask<Void, Void, BaseApiResult> {
    private static final String TAG = SendGcmResultTask.class.getSimpleName();
    protected Context mContext;
    protected String mServerPath;
    protected String mMessageId;

    public void setSendGcmResultData(Context context, String path, String messageId) {
        this.mContext = context;
        this.mServerPath = path;
        this.mMessageId = messageId;
    }

    @Override
    protected BaseApiResult doInBackground(Void... params) {

        RequestQueue requestQueue = Volley.newRequestQueue(mContext);
        BaseApiResult response = null;

        Uri.Builder builder = Uri.parse(mServerPath).buildUpon();
        String url = builder.toString();

        if (StringUtils.isEmptyString(LauncherSettings.getInstance(mContext).getDeviceID())) {
            String deviceID = LauncherSettings.getInstance(mContext).getDeviceID();
            LauncherSettings.getInstance(mContext).setDeviceID(deviceID);
        }

        String strRequestBody = String.format("{\"DEVICE_ID\" : \"%s\", \"MESSAGE_ID\" : \"%s\", \"APP_ID\" : \"%s\", \"RT\" : \"0000\"}",
                LauncherSettings.getInstance(mContext).getDeviceID(),
                mMessageId,
                mContext.getApplicationContext().getPackageName());
        RequestFuture<BaseApiResult> future = RequestFuture.newFuture();

        GsonRequest request = new GsonRequest(Request.Method.POST, url, strRequestBody, BaseApiResult.class, future, future);
        requestQueue.add(request);

        try {
            response = future.get(); // this will block (forever)
            Log.d(TAG, "GCM result send success..");
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
        super.onPostExecute(result);
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
