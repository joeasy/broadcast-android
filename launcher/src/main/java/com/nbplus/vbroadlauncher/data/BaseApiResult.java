package com.nbplus.vbroadlauncher.data;

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
