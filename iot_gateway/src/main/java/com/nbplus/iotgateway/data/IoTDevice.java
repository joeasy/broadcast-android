package com.nbplus.iotgateway.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 8. 6..
 */
public class IoTDevice implements Parcelable {
    @SerializedName("IOT_DEVICE_ID")
    private String deviceId;
    @SerializedName("IOT_DEVICE_NAME")
    private String deviceName;
    @SerializedName("IOT_DEVICE_MAKER")
    private String deviceVendor;
    @SerializedName("IOT_DEVICE_MODEL")
    private String deviceModel;

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceVendor() {
        return deviceVendor;
    }

    public void setDeviceVendor(String deviceVendor) {
        this.deviceVendor = deviceVendor;
    }

    public IoTDevice() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.deviceId);
        dest.writeString(this.deviceName);
        dest.writeString(this.deviceVendor);
        dest.writeString(this.deviceModel);
    }

    protected IoTDevice(Parcel in) {
        this.deviceId = in.readString();
        this.deviceName = in.readString();
        this.deviceVendor = in.readString();
        this.deviceModel = in.readString();
    }

    public static final Creator<IoTDevice> CREATOR = new Creator<IoTDevice>() {
        public IoTDevice createFromParcel(Parcel source) {
            return new IoTDevice(source);
        }

        public IoTDevice[] newArray(int size) {
            return new IoTDevice[size];
        }
    };
}
