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

package com.nbplus.iotapp.btcharacteristics;

import android.os.Parcel;
import android.os.Parcelable;

import com.nbplus.iotapp.data.Constants;
import com.nbplus.iotapp.data.DataParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by basagee on 2015. 9. 14..
 *
 * Service : Weight Scale	org.bluetooth.service.weight_scale	0x181D
 * see https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.weight_scale.xml
 */
public class SmartSensor extends BaseCharacteristicValue implements Parcelable {

    // flag value
    public static final int FLAG_VALUE_TRUE = 0x01;

    public static final int MEASUREMENT_CELCIUS_UNIT = 0x01;

    private int mMeasurementUnit = MEASUREMENT_CELCIUS_UNIT;
    private double mTemperature;
    private double mHumidity;
    private double mAIO0;
    private double mAIO1;
    private double mAIO2;

    private boolean mDI4;
    private boolean mDI5;
    private boolean mDI6;
    private boolean mDI7;
    private boolean mDI9;

    public boolean isDI4() {
        return mDI4;
    }

    public void setDI4(boolean DI4) {
        this.mDI4 = DI4;
    }

    public boolean isDI7() {
        return mDI7;
    }

    public void setDI7(boolean DI7) {
        this.mDI7 = DI7;
    }

    public boolean isDI6() {
        return mDI6;
    }

    public void setDI6(boolean DI6) {
        this.mDI6 = DI6;
    }

    public boolean isDI5() {
        return mDI5;
    }

    public void setDI5(boolean DI5) {
        this.mDI5 = DI5;
    }

    public boolean isDI9() {
        return mDI9;
    }

    public void setDI9(boolean DI9) {
        this.mDI9 = DI9;
    }

    public double getTemperature() {
        return mTemperature;
    }

    public void setTemperature(double mTemperature) {
        this.mTemperature = mTemperature;
    }

    public double getHumidity() {
        return mHumidity;
    }

    public void setHumidity(double mHumidity) {
        this.mHumidity = mHumidity;
    }

    public double getAIO0() {
        return mAIO0;
    }

    public void setAIO0(double mAIO0) {
        this.mAIO0 = mAIO0;
    }

    public double getAIO1() {
        return mAIO1;
    }

    public void setAIO1(double mAIO1) {
        this.mAIO1 = mAIO1;
    }

    public double getAIO2() {
        return mAIO2;
    }

    public void setAIO2(double mAIO2) {
        this.mAIO2 = mAIO2;
    }

    public static ArrayList<SmartSensor> parseSmartSensorMeasurement(String address, String uuid, byte[] value) {
        if (value == null || value.length == 0) {
            return null;
        }

        ArrayList<SmartSensor> measurements = new ArrayList<SmartSensor>();
        /**
         * SMART SENSOR BLE Profile Definition.pdf 참조
         */
        // 샤오미체중계에서 notification 이올때 최대 2개가 올 수 있다.
        boolean isContinue = true;
        int pos = 0;
        do {
            SmartSensor measurement = new SmartSensor();
            int tempValue = 0;
            // set temperature
            byte[] temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN);          // uint16
            tempValue = DataParser.getUint16(temp);
            tempValue &= ~0x0003;
            double temperatureC = -46.85 + (175.72 / 65536) * tempValue;
            measurement.setTemperature(temperatureC);
            pos += Constants.UINT16_LEN;

            // set humidity
            temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN);          // uint16
            tempValue = DataParser.getUint16(temp);
            tempValue &= ~0x0003;
            double humidityRH = -6.0 + (125.0 / 65536) * tempValue;
            measurement.setHumidity(humidityRH);
            pos += Constants.UINT16_LEN;

            // set AIO0
            temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN);          // uint16
            tempValue = DataParser.getUint16(temp);
            measurement.setAIO0(tempValue * 0.001);
            pos += Constants.UINT16_LEN;

            // set AIO1
            temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN);          // uint16
            tempValue = DataParser.getUint16(temp);
            measurement.setAIO1(tempValue * 0.001);
            pos += Constants.UINT16_LEN;

            // set AIO2
            temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN);          // uint16
            tempValue = DataParser.getUint16(temp);
            measurement.setAIO2(tempValue * 0.001);
            pos += Constants.UINT16_LEN;

            // Digital Input..
            // PIN7
            if((value[10] & 0x80) == 0x80) {
                measurement.setDI7(true);
            }
            else {
                measurement.setDI7(false);
            }
            //PIN 6
            if((value[10] & 0x40) == 0x40) {
                measurement.setDI6(true);
            }
            else {
                measurement.setDI6(false);
            }

            //PIN 5
            if((value[10] & 0x20) == 0x20) {
                measurement.setDI5(true);
            }
            else {
                measurement.setDI5(false);
            }

            //PIN 9
            if((value[11] & 0x02) == 0x02) {
                measurement.setDI9(true);
            }
            else {
                measurement.setDI9(false);
            }

            //PIN 4
            if((value[10] & 0x10) == 0x10) {
                measurement.setDI4(true);
            }
            else {
                measurement.setDI4(false);
            }
            pos += Constants.UINT16_LEN;

            measurement.setPrimaryAddress(address);
            measurement.setUuidString(uuid);
            measurements.add(measurement);
            if (pos >= value.length) {
                isContinue = false;
            }
        } while (isContinue);


        return measurements;
    }

    public SmartSensor() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.mMeasurementUnit);
        dest.writeDouble(this.mTemperature);
        dest.writeDouble(this.mHumidity);
        dest.writeDouble(this.mAIO0);
        dest.writeDouble(this.mAIO1);
        dest.writeDouble(this.mAIO2);
        dest.writeByte(mDI4 ? (byte) 1 : (byte) 0);
        dest.writeByte(mDI5 ? (byte) 1 : (byte) 0);
        dest.writeByte(mDI6 ? (byte) 1 : (byte) 0);
        dest.writeByte(mDI7 ? (byte) 1 : (byte) 0);
        dest.writeByte(mDI9 ? (byte) 1 : (byte) 0);
    }

    protected SmartSensor(Parcel in) {
        super(in);
        this.mMeasurementUnit = in.readInt();
        this.mTemperature = in.readDouble();
        this.mHumidity = in.readDouble();
        this.mAIO0 = in.readDouble();
        this.mAIO1 = in.readDouble();
        this.mAIO2 = in.readDouble();
        this.mDI4 = in.readByte() != 0;
        this.mDI5 = in.readByte() != 0;
        this.mDI6 = in.readByte() != 0;
        this.mDI7 = in.readByte() != 0;
        this.mDI9 = in.readByte() != 0;
    }

    public static final Creator<SmartSensor> CREATOR = new Creator<SmartSensor>() {
        public SmartSensor createFromParcel(Parcel source) {
            return new SmartSensor(source);
        }

        public SmartSensor[] newArray(int size) {
            return new SmartSensor[size];
        }
    };
}
