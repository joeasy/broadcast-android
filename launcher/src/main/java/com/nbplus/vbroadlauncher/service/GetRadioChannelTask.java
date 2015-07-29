package com.nbplus.vbroadlauncher.service;

import android.net.Uri;
import android.os.Message;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.nbplus.vbroadlauncher.data.BaseApiResult;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

import org.basdroid.volley.GsonRequest;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class GetRadioChannelTask extends BaseServerApiAsyncTask {

    @Override
    protected BaseApiResult doInBackground(Void... voids) {
        RequestQueue requestQueue = Volley.newRequestQueue(mContext);
        RadioChannelInfo response = null;

        Uri.Builder builder = Uri.parse(mServerPath).buildUpon();
        builder.appendQueryParameter("DEVICE_ID", LauncherSettings.getInstance(mContext).getDeviceID());
        String url = builder.toString();

//        int retryCount = 0;
//        while (retryCount < 3) {        // retry 3 times
            RequestFuture<RadioChannelInfo> future = RequestFuture.newFuture();

            GsonRequest request = new GsonRequest(Request.Method.GET, url, null, RadioChannelInfo.class, future, future);
            request.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 3, 1.0f));
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
        return (BaseApiResult)response;
    }

    @Override
    protected void onPostExecute(BaseApiResult result) {

        // TODO : sample data
//        ArrayList<RadioChannelInfo.RadioChannel> items = new ArrayList<>();
//        RadioChannelInfo.RadioChannel item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "KBS 1FM(클래식FM)";
//        item.channelUrl = "rtsp://kbs-radio.gscdn.com/tunein_1fm/_definst_/tunein_1fm.stream";
//        items.add(item);
//
//        ((RadioChannelInfo)result).setResultCode("0000");
//        ((RadioChannelInfo)result).setRadioChannelList(items);

        // end of TODO : sample data

        // TODO ;; remove later....

        if (mHandler != null) {
            Message message = new Message();
            message.what = Constants.HANDLER_MESSAGE_GET_RADIO_CHANNEL_TASK;
            message.obj = result;
            mHandler.sendMessage(message);
        }
    }

}
