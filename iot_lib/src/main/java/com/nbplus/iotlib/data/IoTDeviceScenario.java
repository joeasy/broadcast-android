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

/**
 * Created by basagee on 2015. 10. 27..
 */
public class IoTDeviceScenario implements Parcelable {
    public static final int CMD_NONE = -1;
    public static final int CMD_READ = 0;
    public static final int CMD_WRITE = 1;
    public static final int CMD_NOTIFY = 2;

    @SerializedName("characteristic")
    String characteristic;
    @SerializedName("service")
    String service;
    @SerializedName("cmd")
    int cmd = -1;
    @SerializedName("data_type")
    String dataType;
    @SerializedName("data")
    String data;

    public String getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(String characteristic) {
        this.characteristic = characteristic;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.characteristic);
        dest.writeString(this.service);
        dest.writeInt(this.cmd);
        dest.writeString(this.dataType);
        dest.writeString(this.data);
    }

    public IoTDeviceScenario() {
    }

    protected IoTDeviceScenario(Parcel in) {
        this.characteristic = in.readString();
        this.service = in.readString();
        this.cmd = in.readInt();
        this.dataType = in.readString();
        this.data = in.readString();
    }

    public static final Parcelable.Creator<IoTDeviceScenario> CREATOR = new Parcelable.Creator<IoTDeviceScenario>() {
        public IoTDeviceScenario createFromParcel(Parcel source) {
            return new IoTDeviceScenario(source);
        }

        public IoTDeviceScenario[] newArray(int size) {
            return new IoTDeviceScenario[size];
        }
    };
}
