package com.nbplus.iotgateway.data;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;

/**
 * Created by basagee on 2015. 9. 18..
 *
 * // hashmap parcelable
 @Override
 public void writeToParcel(Parcel dest, int flags) {
 dest.writeByte(isBleHealthService ? (byte) 1 : (byte) 0);
 dest.writeString(this.bleHealthUuid);
 dest.writeParcelable(this.bluetoothDevice, 0);

 Bundle b = new Bundle();
 b.putSerializable("AdRecords", this.adRecords);
 dest.writeBundle(b);
 }

 protected BluetoothDeviceData(Parcel in) {
 this.isBleHealthService = in.readByte() != 0;
 this.bleHealthUuid = in.readString();
 this.bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());

 Bundle b = in.readBundle();
 b.setClassLoader(AdRecord.class.getClassLoader());
 this.adRecords = (HashMap<Integer, AdRecord>) in.readParcelable();
 }
 */
public class BluetoothDeviceData implements Parcelable {

    private boolean isBleHealthService;
    private String bleHealthUuid;

    private BluetoothDevice bluetoothDevice;
    private HashMap<Integer, AdRecord> adRecords;

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public HashMap<Integer, AdRecord> getAdRecord() {
        return adRecords;
    }

    public void setAdRecord(HashMap<Integer, AdRecord> adRecords) {
        this.adRecords = adRecords;
    }

    public boolean isBleHealthService() {
        return isBleHealthService;
    }

    public void setIsBleHealthService(boolean isBleHealthService) {
        this.isBleHealthService = isBleHealthService;
    }

    public String getBleHealthUuid() {
        return bleHealthUuid;
    }

    public void setBleHealthUuid(String bleHealthUuid) {
        this.bleHealthUuid = bleHealthUuid;
    }

    public BluetoothDeviceData() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(isBleHealthService ? (byte) 1 : (byte) 0);
        dest.writeString(this.bleHealthUuid);
        dest.writeParcelable(this.bluetoothDevice, 0);

        Bundle b = new Bundle();
        b.putSerializable("AdRecords", this.adRecords);
        dest.writeBundle(b);
    }

    protected BluetoothDeviceData(Parcel in) {
        this.isBleHealthService = in.readByte() != 0;
        this.bleHealthUuid = in.readString();
        this.bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());

        Bundle b = in.readBundle();
        b.setClassLoader(AdRecord.class.getClassLoader());
        this.adRecords = (HashMap<Integer, AdRecord>) b.getSerializable("AdRecords");
    }

    public static final Creator<BluetoothDeviceData> CREATOR = new Creator<BluetoothDeviceData>() {
        public BluetoothDeviceData createFromParcel(Parcel source) {
            return new BluetoothDeviceData(source);
        }

        public BluetoothDeviceData[] newArray(int size) {
            return new BluetoothDeviceData[size];
        }
    };
}
