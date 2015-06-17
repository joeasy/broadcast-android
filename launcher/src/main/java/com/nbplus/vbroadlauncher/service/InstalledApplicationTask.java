package com.nbplus.vbroadlauncher.service;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.nbplus.vbroadlauncher.data.ShowAllLaunchAppsInfo;
import com.nbplus.vbroadlauncher.data.Constants;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class InstalledApplicationTask extends AsyncTask<Void, Void, Void> {
    //private ProgressDialog progress = null;
    private Context mContext;
    private Handler mHandler;

    public InstalledApplicationTask(Context context) {
        this.mContext = context;
    }

    public InstalledApplicationTask(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    @Override
    protected Void doInBackground(Void... params) {
        ShowAllLaunchAppsInfo.getInstance().updateApplicationList(mContext);

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
            message.what = Constants.HANDLER_MESSAGE_INSTALLED_APPLIST_TASK;
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
