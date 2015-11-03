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
import java.util.LinkedHashMap;

/**
 * Created by basagee on 2015. 10. 27..
 */
public class IoTScenarioDef implements Parcelable {
    @SerializedName("type")
    private String type;
    @SerializedName("check")
    private ArrayList<String> scenarioFilter;
    @SerializedName("scenarios")
    private LinkedHashMap<String, ArrayList<IoTDeviceScenario>> scenarios;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getScenarioFilter() {
        return scenarioFilter;
    }

    public void setScenarioFilter(ArrayList<String> scenarioFilter) {
        this.scenarioFilter = scenarioFilter;
    }

    public ArrayList<IoTDeviceScenario> getScenarios(String id) {
        return scenarios.get(id);
    }

    public void setScenarios(LinkedHashMap<String, ArrayList<IoTDeviceScenario>> scenarios) {
        this.scenarios = scenarios;
    }

    public IoTScenarioDef() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.type);
        dest.writeStringList(this.scenarioFilter);
        dest.writeSerializable(this.scenarios);
    }

    protected IoTScenarioDef(Parcel in) {
        this.type = in.readString();
        this.scenarioFilter = in.createStringArrayList();
        this.scenarios = (LinkedHashMap<String, ArrayList<IoTDeviceScenario>>) in.readSerializable();
    }

    public static final Creator<IoTScenarioDef> CREATOR = new Creator<IoTScenarioDef>() {
        public IoTScenarioDef createFromParcel(Parcel source) {
            return new IoTScenarioDef(source);
        }

        public IoTScenarioDef[] newArray(int size) {
            return new IoTScenarioDef[size];
        }
    };
}
