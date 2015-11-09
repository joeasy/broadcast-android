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
import android.os.AsyncTask;
import android.os.Handler;

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
