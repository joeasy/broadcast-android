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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 6. 29..
 */
public class BaseApiResult implements Parcelable {
    public static final String RESULT_SUCCESS = "0000";

    @SerializedName("RT")
    protected String resultCode;
    @SerializedName("RT_MSG")
    protected String resultMessage;

    @SerializedName("DROID_INTERNAL_DATA")
    protected Object object;

    public String getResultCode() {
        return resultCode;
    }
    public String getResultMessage() {
        return resultMessage;
    }
    public Object getObject() {
        return object;
    }


    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
    public void setObject(Object object) {
        this.object = object;
    }

    public BaseApiResult() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.resultCode);
        dest.writeString(this.resultMessage);
        dest.writeParcelable((Parcelable)this.object, flags);
    }

    protected BaseApiResult(Parcel in) {
        this.resultCode = in.readString();
        this.resultMessage = in.readString();
        this.object = in.readParcelable(Object.class.getClassLoader());
    }

    public static final Creator<BaseApiResult> CREATOR = new Creator<BaseApiResult>() {
        public BaseApiResult createFromParcel(Parcel source) {
            return new BaseApiResult(source);
        }

        public BaseApiResult[] newArray(int size) {
            return new BaseApiResult[size];
        }
    };
}
