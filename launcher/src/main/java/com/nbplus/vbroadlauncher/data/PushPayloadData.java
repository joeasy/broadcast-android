package com.nbplus.vbroadlauncher.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 6. 29..
 */
public class PushPayloadData implements Parcelable {
    @SerializedName("FROM")
    protected String from;
    @SerializedName("ADDRESS")
    protected String address;
    @SerializedName("MESSAGE")
    protected String message;
    @SerializedName("SERVICE_TYPE")
    protected String serviceType;
    @SerializedName("IOT_DEVICE_ID")
    protected String iotControlDeviceId;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getIotControlDeviceId() {
        return iotControlDeviceId;
    }

    public void setIotControlDeviceId(String iotControlDeviceId) {
        this.iotControlDeviceId = iotControlDeviceId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.from);
        dest.writeString(this.address);
        dest.writeString(this.message);
        dest.writeString(this.serviceType);
        dest.writeString(this.iotControlDeviceId);
    }

    public PushPayloadData() {
    }

    protected PushPayloadData(Parcel in) {
        this.from = in.readString();
        this.address = in.readString();
        this.message = in.readString();
        this.serviceType = in.readString();
        this.iotControlDeviceId = in.readString();
    }

    public static final Creator<PushPayloadData> CREATOR = new Creator<PushPayloadData>() {
        public PushPayloadData createFromParcel(Parcel source) {
            return new PushPayloadData(source);
        }

        public PushPayloadData[] newArray(int size) {
            return new PushPayloadData[size];
        }
    };
}
