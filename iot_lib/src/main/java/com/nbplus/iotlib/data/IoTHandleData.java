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

/**
 * Created by basagee on 2015. 10. 29..
 */
public class IoTHandleData implements Parcelable {
    int requestCommand;
    String msgId;
    String deviceId;
    String serviceUuid;
    String characteristicUuid;
    int deviceTypeId;

    byte[] value;

    public int getRequestCommand() {
        return requestCommand;
    }

    public void setRequestCommand(int requestCommand) {
        this.requestCommand = requestCommand;
    }

    public int getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(int deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getCharacteristicUuid() {
        return characteristicUuid;
    }

    public void setCharacteristicUuid(String characteristicUuid) {
        this.characteristicUuid = characteristicUuid;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public IoTHandleData() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.requestCommand);
        dest.writeString(this.msgId);
        dest.writeString(this.deviceId);
        dest.writeString(this.serviceUuid);
        dest.writeString(this.characteristicUuid);
        dest.writeInt(this.deviceTypeId);
        if (this.value != null) {
            dest.writeInt(this.value.length);
            dest.writeByteArray(this.value);
        } else {
            dest.writeInt(0);
        }
    }

    protected IoTHandleData(Parcel in) {
        this.requestCommand = in.readInt();
        this.msgId = in.readString();
        this.deviceId = in.readString();
        this.serviceUuid = in.readString();
        this.characteristicUuid = in.readString();
        this.deviceTypeId = in.readInt();

        int len = in.readInt();
        if (len > 0) {
            this.value = new byte[len];
            in.readByteArray(this.value);
        }
    }

    public static final Creator<IoTHandleData> CREATOR = new Creator<IoTHandleData>() {
        public IoTHandleData createFromParcel(Parcel source) {
            return new IoTHandleData(source);
        }

        public IoTHandleData[] newArray(int size) {
            return new IoTHandleData[size];
        }
    };
}
