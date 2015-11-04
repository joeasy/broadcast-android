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

package com.nbplus.iotlib.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.nbplus.iotapp.data.AdRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by basagee on 2015. 8. 6..
 */
public class IoTDevice implements Parcelable {
    public static final String DEVICE_TYPE_STRING_IR = "IR";
    public static final String DEVICE_TYPE_STRING_BT = "BT";
    public static final String DEVICE_TYPE_STRING_ZW = "ZW";

    // for grid view
    public static final int DEVICE_TYPE_ID_NONE = 0x0000;
    public static final int DEVICE_TYPE_ID_IR = 0x0001;
    public static final int DEVICE_TYPE_ID_BT = 0x0002;
    public static final int DEVICE_TYPE_ID_ZW = 0x0003;


    public static final int DEVICE_BT_UUID_LEN_16 = 0x0001;
    public static final int DEVICE_BT_UUID_LEN_32 = 0x0002;
    public static final int DEVICE_BT_UUID_LEN_128 = 0x0003;

    @SerializedName("IOT_DEVICE_ID")
    private String deviceId;
    @SerializedName("IOT_DEVICE_NAME")
    private String deviceName;
    @SerializedName("IOT_DEVICE_MAKER")
    private String deviceVendor;
    @SerializedName("IOT_DEVICE_MODEL")
    private String deviceModel;
    @SerializedName("IOT_DEVICE_TYPE")
    private String deviceType;          // "IR", "BT", "ZW", etc.....
    @SerializedName("IOT_DEVICE_BT_UUIDLEN")
    private int uuidLen;                // 0: 16, 1: 32, 2: 128
    @SerializedName("IOT_DEVICE_UUIDS")            // for bluetooth
    private ArrayList<String> uuids;

    @SerializedName("BONDED")
    private boolean isBondedWithServer;

    HashMap<Integer, AdRecord> adRecordHashMap;
    transient HashMap<String, ArrayList<String>> discoveredServices;
    transient ArrayList<IoTDeviceScenario> deviceScenario;
    transient int scenarioPosition = 0;
    transient boolean isKnownDevice;
    transient int savedRecordCount = -1;
    transient int recvedRecordCount = 0;

    public HashMap<String, ArrayList<String>> getDiscoveredServices() {
        return discoveredServices;
    }

    public int getSavedRecordCount() {
        return savedRecordCount;
    }

    public void setSavedRecordCount(int savedRecordCount) {
        this.savedRecordCount = savedRecordCount;
    }

    public int getRecvedRecordCount() {
        return recvedRecordCount;
    }

    public void setRecvedRecordCount(int recvedRecordCount) {
        this.recvedRecordCount = recvedRecordCount;
    }

    public int getScenarioPosition() {
        return scenarioPosition;
    }

    public void setScenarioPosition(int scenarioPosition) {
        this.scenarioPosition = scenarioPosition;
    }

    public boolean isKnownDevice() {
        return isKnownDevice;
    }

    public void setIsKnownDevice(boolean isKnownDevice) {
        this.isKnownDevice = isKnownDevice;
    }

    public boolean isBondedWithServer() {
        return isBondedWithServer;
    }

    public void setIsBondedWithServer(boolean isBonded) {
        this.isBondedWithServer = isBonded;
    }

    public void setDiscoveredServices(HashMap<String, ArrayList<String>> discoveredServices) {
        this.discoveredServices = discoveredServices;
    }

    public ArrayList<IoTDeviceScenario> getDeviceScenario() {
        return deviceScenario;
    }

    public void setDeviceScenario(ArrayList<IoTDeviceScenario> deviceScenario) {
        this.deviceScenario = deviceScenario;
    }

    public IoTDeviceScenario getNextScenario() {
        if (this.deviceScenario == null || this.scenarioPosition == this.deviceScenario.size()) {
            return null;
        }
        return this.deviceScenario.get(this.scenarioPosition++);
    }

    public IoTDeviceScenario getCurrentScenario() {
        if (this.deviceScenario == null || this.scenarioPosition == this.deviceScenario.size()) {
            return null;
        }
        return this.deviceScenario.get(this.scenarioPosition);
    }

    public int getUuidLen() {
        return uuidLen;
    }

    public void setUuidLen(int uuidLen) {
        this.uuidLen = uuidLen;
    }

    public ArrayList<String> getUuids() {
        return uuids;
    }

    public void setUuids(ArrayList<String> uuids) {
        this.uuids = uuids;
    }

    public HashMap<Integer, AdRecord> getAdRecordHashMap() {
        return adRecordHashMap;
    }

    public void setAdRecordHashMap(HashMap<Integer, AdRecord> adRecordHashMap) {
        this.adRecordHashMap = adRecordHashMap;
    }

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

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public int getDeviceTypeId() {
        if (DEVICE_TYPE_STRING_IR.equals(deviceType)) {
            return DEVICE_TYPE_ID_IR;
        } else if (DEVICE_TYPE_STRING_BT.equals(deviceType)) {
            return DEVICE_TYPE_ID_BT;
        } else if (DEVICE_TYPE_STRING_ZW.equals(deviceType)) {
            return DEVICE_TYPE_ID_ZW;
        } else {
            return DEVICE_TYPE_ID_NONE;
        }
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
        dest.writeString(this.deviceType);
        dest.writeInt(this.uuidLen);
        dest.writeStringList(this.uuids);
        dest.writeByte(isBondedWithServer ? (byte) 1 : (byte) 0);
        dest.writeSerializable(this.adRecordHashMap);
        dest.writeSerializable(this.discoveredServices);
        dest.writeTypedList(deviceScenario);
        dest.writeInt(this.scenarioPosition);
        dest.writeByte(isKnownDevice ? (byte) 1 : (byte) 0);
        dest.writeInt(this.savedRecordCount);
        dest.writeInt(this.recvedRecordCount);
    }

    protected IoTDevice(Parcel in) {
        this.deviceId = in.readString();
        this.deviceName = in.readString();
        this.deviceVendor = in.readString();
        this.deviceModel = in.readString();
        this.deviceType = in.readString();
        this.uuidLen = in.readInt();
        this.uuids = in.createStringArrayList();
        this.isBondedWithServer = in.readByte() != 0;
        this.adRecordHashMap = (HashMap<Integer, AdRecord>) in.readSerializable();
        this.discoveredServices = (HashMap<String, ArrayList<String>>) in.readSerializable();
        this.deviceScenario = in.createTypedArrayList(IoTDeviceScenario.CREATOR);
        this.scenarioPosition = in.readInt();
        this.isKnownDevice = in.readByte() != 0;
        this.savedRecordCount = in.readInt();
        this.recvedRecordCount = in.readInt();
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
