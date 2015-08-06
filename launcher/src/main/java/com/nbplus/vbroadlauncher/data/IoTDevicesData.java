package com.nbplus.vbroadlauncher.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.nbplus.iotgateway.data.IoTDevice;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 8. 6..
 */
public class IoTDevicesData implements Parcelable {
    @SerializedName("DEVICE_ID")
    private String deviceId;
    @SerializedName("IOT_GW_ID")
    private String iotGatewayId;
    @SerializedName("IOT_DEVICE_INFO")
    private ArrayList<IoTDevice> iotDevices;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIotGatewayId() {
        return iotGatewayId;
    }

    public void setIotGatewayId(String iotGatewayId) {
        this.iotGatewayId = iotGatewayId;
    }

    public ArrayList<IoTDevice> getIotDevices() {
        return iotDevices;
    }

    public void setIotDevices(ArrayList<IoTDevice> iotDevices) {
        this.iotDevices = iotDevices;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.deviceId);
        dest.writeString(this.iotGatewayId);
        dest.writeTypedList(iotDevices);
    }

    public IoTDevicesData() {
    }

    protected IoTDevicesData(Parcel in) {
        this.deviceId = in.readString();
        this.iotGatewayId = in.readString();
        this.iotDevices = in.createTypedArrayList(IoTDevice.CREATOR);
    }

    public static final Creator<IoTDevicesData> CREATOR = new Creator<IoTDevicesData>() {
        public IoTDevicesData createFromParcel(Parcel source) {
            return new IoTDevicesData(source);
        }

        public IoTDevicesData[] newArray(int size) {
            return new IoTDevicesData[size];
        }
    };
}
