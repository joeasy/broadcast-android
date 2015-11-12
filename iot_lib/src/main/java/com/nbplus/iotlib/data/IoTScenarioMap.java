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

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by basagee on 2015. 10. 27..
 */
public class IoTScenarioMap implements Parcelable {
    @SerializedName("version")
    private int version;
    @SerializedName("map")
    private HashMap<String, IoTScenarioDef> scenarioMap;
    @SerializedName("emergency_device_list")
    private ArrayList<String> emergencyCallDeviceList;
    @SerializedName("emergency_devices_scenario")
    private HashMap<String, ArrayList<IoTDeviceScenario>> emergencyCallDevicesScenarioMap;

    public ArrayList<String> getEmergencyCallDeviceList() {
        return emergencyCallDeviceList;
    }

    public void setEmergencyCallDeviceList(ArrayList<String> emergencyCallDeviceList) {
        this.emergencyCallDeviceList = emergencyCallDeviceList;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public HashMap<String, IoTScenarioDef> getScenarioMap() {
        return scenarioMap;
    }

    public void setScenarioMap(HashMap<String, IoTScenarioDef> scenarioMap) {
        this.scenarioMap = scenarioMap;
    }

    public HashMap<String, ArrayList<IoTDeviceScenario>> getEmergencyCallDevicesScenarioMap() {
        return emergencyCallDevicesScenarioMap;
    }

    public void setEmergencyCallDevicesScenarioMap(HashMap<String, ArrayList<IoTDeviceScenario>> emergencyCallDevicesScenarioMap) {
        this.emergencyCallDevicesScenarioMap = emergencyCallDevicesScenarioMap;
    }

    public IoTScenarioMap() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.version);
        dest.writeSerializable(this.scenarioMap);
        dest.writeStringList(this.emergencyCallDeviceList);
        dest.writeSerializable(this.emergencyCallDevicesScenarioMap);
    }

    protected IoTScenarioMap(Parcel in) {
        this.version = in.readInt();
        this.scenarioMap = (HashMap<String, IoTScenarioDef>) in.readSerializable();
        this.emergencyCallDeviceList = in.createStringArrayList();
        this.emergencyCallDevicesScenarioMap = (HashMap<String, ArrayList<IoTDeviceScenario>>) in.readSerializable();
    }

    public static final Creator<IoTScenarioMap> CREATOR = new Creator<IoTScenarioMap>() {
        public IoTScenarioMap createFromParcel(Parcel source) {
            return new IoTScenarioMap(source);
        }

        public IoTScenarioMap[] newArray(int size) {
            return new IoTScenarioMap[size];
        }
    };
}
