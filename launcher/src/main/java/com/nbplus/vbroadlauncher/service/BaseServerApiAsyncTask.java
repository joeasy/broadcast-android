package com.nbplus.vbroadlauncher.service;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.nbplus.vbroadlauncher.data.BaseApiResult;

import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class BaseServerApiAsyncTask extends AsyncTask<Void, Void, BaseApiResult> {
    protected Context mContext;
    protected Handler mHandler;
    protected String mServerPath;

    public void setBroadcastApiData(Context context, Handler handler, String path) {
        this.mContext = context;
        this.mHandler = handler;
        this.mServerPath = path;
    }

    @Override
    protected BaseApiResult doInBackground(Void... voids) {
        return null;
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
    protected void onPostExecute(BaseApiResult baseApiResult) {
        super.onPostExecute(baseApiResult);
    }
}
