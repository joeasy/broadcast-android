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

package com.nbplus.iotlib.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 11. 4..
 */
public class IoTCollectedData implements Parcelable {
    @SerializedName("DEVICE_ID")
    private String deviceId;
    @SerializedName("IOT_DATA")
    private ArrayList<IoTData> ioTData;

    public static class IoTData implements Parcelable {
        @SerializedName("IOT_DEVICE_ID")
        private String iotDeviceId;
        @SerializedName("DATE")
        private String date;
        @SerializedName("VALUE")
        private String value;

        public IoTData(String deviceId, String date, String value) {
            this.iotDeviceId = deviceId;
            this.date = date;
            this.value = value;
        }

        public String getIotDeviceId() {
            return iotDeviceId;
        }

        public void setIotDeviceId(String iotDeviceId) {
            this.iotDeviceId = iotDeviceId;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.iotDeviceId);
            dest.writeString(this.date);
            dest.writeString(this.value);
        }

        public IoTData() {
        }

        protected IoTData(Parcel in) {
            this.iotDeviceId = in.readString();
            this.date = in.readString();
            this.value = in.readString();
        }

        public static final Parcelable.Creator<IoTData> CREATOR = new Parcelable.Creator<IoTData>() {
            public IoTData createFromParcel(Parcel source) {
                return new IoTData(source);
            }

            public IoTData[] newArray(int size) {
                return new IoTData[size];
            }
        };
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public ArrayList<IoTData> getIoTData() {
        if (ioTData == null) {
            return new ArrayList<>();
        }
        return ioTData;
    }

    public void setIoTData(ArrayList<IoTData> ioTMeasureData) {
        this.ioTData = ioTMeasureData;
    }

    public int getIoTDataSize() {
        if (this.ioTData == null) {
            return 0;
        }
        return this.ioTData.size();
    }

    public void addAllIoTData(ArrayList<IoTData> ioTDatas) {
        if (this.ioTData == null) {
            this.ioTData = new ArrayList<>();
        }
        this.ioTData.addAll(ioTDatas);
    }

    public void addIoTData(IoTData ioTData) {
        if (this.ioTData == null) {
            this.ioTData = new ArrayList<>();
        }
        this.ioTData.add(ioTData);
    }

    public void clearIoTData() {
        if (this.ioTData == null) {
            this.ioTData = new ArrayList<>();
        }
        this.ioTData.clear();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.deviceId);
        dest.writeTypedList(ioTData);
    }

    public IoTCollectedData() {
    }

    protected IoTCollectedData(Parcel in) {
        this.deviceId = in.readString();
        this.ioTData = in.createTypedArrayList(IoTData.CREATOR);
    }

    public static final Parcelable.Creator<IoTCollectedData> CREATOR = new Parcelable.Creator<IoTCollectedData>() {
        public IoTCollectedData createFromParcel(Parcel source) {
            return new IoTCollectedData(source);
        }

        public IoTCollectedData[] newArray(int size) {
            return new IoTCollectedData[size];
        }
    };
}
