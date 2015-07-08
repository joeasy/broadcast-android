package com.nbplus.push.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by basagee on 2015. 6. 30..
 */
public class PushMessageData extends PushBaseData implements Parcelable {
    private int correlator;
    private String appId;
    private String repeatKey;
    private String alert;
    private String payload;

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getCorrelator() {
        return correlator;
    }

    public void setCorrelator(int correlator) {
        this.correlator = correlator;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getRepeatKey() {
        return repeatKey;
    }

    public void setRepeatKey(String repeatKey) {
        this.repeatKey = repeatKey;
    }

    public String getAlert() {
        return alert;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.correlator);
        dest.writeString(this.appId);
        dest.writeString(this.repeatKey);
        dest.writeString(this.alert);
        dest.writeString(this.payload);
    }

    public PushMessageData() {
    }

    protected PushMessageData(Parcel in) {
        this.correlator = in.readInt();
        this.appId = in.readString();
        this.repeatKey = in.readString();
        this.alert = in.readString();
        this.payload = in.readString();
    }

    public static final Parcelable.Creator<PushMessageData> CREATOR = new Parcelable.Creator<PushMessageData>() {
        public PushMessageData createFromParcel(Parcel source) {
            return new PushMessageData(source);
        }

        public PushMessageData[] newArray(int size) {
            return new PushMessageData[size];
        }
    };
}
