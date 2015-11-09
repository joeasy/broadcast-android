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

    private boolean digitalInButton4Checked;
    private boolean digitalInButton5Checked;
    private boolean digitalInButton6Checked;
    private boolean digitalInButton7Checked;
    private boolean digitalInButton9Checked;

    public boolean isDigitalInButton4Checked() {
        return digitalInButton4Checked;
    }

    public void setDigitalInButton4Checked(boolean digitalInButton4Checked) {
        this.digitalInButton4Checked = digitalInButton4Checked;
    }

    public boolean isDigitalInButton5Checked() {
        return digitalInButton5Checked;
    }

    public void setDigitalInButton5Checked(boolean digitalInButton5Checked) {
        this.digitalInButton5Checked = digitalInButton5Checked;
    }

    public boolean isDigitalInButton6Checked() {
        return digitalInButton6Checked;
    }

    public void setDigitalInButton6Checked(boolean digitalInButton6Checked) {
        this.digitalInButton6Checked = digitalInButton6Checked;
    }

    public boolean isDigitalInButton7Checked() {
        return digitalInButton7Checked;
    }

    public void setDigitalInButton7Checked(boolean digitalInButton7Checked) {
        this.digitalInButton7Checked = digitalInButton7Checked;
    }

    public boolean isDigitalInButton9Checked() {
        return digitalInButton9Checked;
    }

    public void setDigitalInButton9Checked(boolean digitalInButton9Checked) {
        this.digitalInButton9Checked = digitalInButton9Checked;
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
                measurement.setDigitalInButton7Checked(true);
            }
            else {
                measurement.setDigitalInButton7Checked(false);
            }
            //PIN 6
            if((value[10] & 0x40) == 0x40) {
                measurement.setDigitalInButton6Checked(true);
            }
            else {
                measurement.setDigitalInButton6Checked(false);
            }

            //PIN 5
            if((value[10] & 0x20) == 0x20) {
                measurement.setDigitalInButton5Checked(true);
            }
            else {
                measurement.setDigitalInButton5Checked(false);
            }

            //PIN 9
            if((value[11] & 0x02) == 0x02) {
                measurement.setDigitalInButton9Checked(true);
            }
            else {
                measurement.setDigitalInButton9Checked(false);
            }

            //PIN 4
            if((value[10] & 0x10) == 0x10) {
                measurement.setDigitalInButton4Checked(true);
            }
            else {
                measurement.setDigitalInButton4Checked(false);
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
        dest.writeByte(digitalInButton4Checked ? (byte) 1 : (byte) 0);
        dest.writeByte(digitalInButton5Checked ? (byte) 1 : (byte) 0);
        dest.writeByte(digitalInButton6Checked ? (byte) 1 : (byte) 0);
        dest.writeByte(digitalInButton7Checked ? (byte) 1 : (byte) 0);
        dest.writeByte(digitalInButton9Checked ? (byte) 1 : (byte) 0);
    }

    protected SmartSensor(Parcel in) {
        super(in);
        this.mMeasurementUnit = in.readInt();
        this.mTemperature = in.readDouble();
        this.mHumidity = in.readDouble();
        this.mAIO0 = in.readDouble();
        this.mAIO1 = in.readDouble();
        this.mAIO2 = in.readDouble();
        this.digitalInButton4Checked = in.readByte() != 0;
        this.digitalInButton5Checked = in.readByte() != 0;
        this.digitalInButton6Checked = in.readByte() != 0;
        this.digitalInButton7Checked = in.readByte() != 0;
        this.digitalInButton9Checked = in.readByte() != 0;
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
