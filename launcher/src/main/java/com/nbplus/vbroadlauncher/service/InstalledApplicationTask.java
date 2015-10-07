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
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
