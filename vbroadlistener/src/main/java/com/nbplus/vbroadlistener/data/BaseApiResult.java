package com.nbplus.vbroadlistener.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 6. 29..
 */
public class BaseApiResult implements Parcelable {
    @SerializedName("RT")
    protected String resultCode;
    @SerializedName("RT_MSG")
    protected String resultMessage;

    public String getResultCode() {
        return resultCode;
    }
    public String getResultMessage() {
        return resultMessage;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.resultCode);
        dest.writeString(this.resultMessage);
    }

    public BaseApiResult() {
    }

    protected BaseApiResult(Parcel in) {
        this.resultCode = in.readString();
        this.resultMessage = in.readString();
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
