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
public class WeightMeasurement extends BaseCharacteristicValue implements Parcelable {

    // flag value
    public static final int FLAG_VALUE_TRUE = 0x01;

    public static final int MEASUREMENT_INTERNATIONAL_UNIT = 0x00;
    public static final int MEASUREMENT_BRITISH_UNIT = 0x01;

    private int mFlags;
    private int mMeasurementUnit = MEASUREMENT_INTERNATIONAL_UNIT;
    private boolean mIsTimestampPresent = false;
    private Calendar mTimestamp = null;
    private double mWeight = 0.0f;

    private boolean mIsUserIdPresent = false;
    private int mUserId = 255;

    private boolean mIsBmiPresent = false;
    private double mBmi = 0.0f;
    private double mHeight = 0.0f;

    public int getMeasurementUnit() {
        return mMeasurementUnit;
    }

    public String getMeasurementUnitString() {
        return (mMeasurementUnit == MEASUREMENT_INTERNATIONAL_UNIT) ? "Kg" : "Lb";
    }

    public boolean isTimestampPresent() {
        return mIsTimestampPresent;
    }

    public Calendar getTimestamp() {
        return mTimestamp;
    }

    public double getWeight() {
        return mWeight;
    }

    public boolean isUserIdPresent() {
        return mIsUserIdPresent;
    }

    public int getUserId() {
        return mUserId;
    }

    public double getBmi() {
        return mBmi;
    }

    public String getHeightUnitString() {
        return mMeasurementUnit == MEASUREMENT_INTERNATIONAL_UNIT ? "cm" : "inches";
    }

    public double getHeight() {
        return mHeight;
    }

    public boolean isBmiPresent() {
        return mIsBmiPresent;
    }

    public void setHeight(double mHeight) {
        this.mHeight = mHeight;
    }

    public void setFlags(int mFlags) {
        this.mFlags = mFlags;
    }

    public void setMeasurementUnit(int mMeasurementUnit) {
        this.mMeasurementUnit = mMeasurementUnit;
    }

    public void setIsTimestampPresent(boolean mIsTimestampPresent) {
        this.mIsTimestampPresent = mIsTimestampPresent;
    }

    public void setTimestamp(Calendar mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

    public void setWeight(double mWeight) {
        this.mWeight = mWeight;
    }

    public void setIsUserIdPresent(boolean present) {
        mIsUserIdPresent = present;
    }

    public void setUserId(int mUserId) {
        this.mUserId = mUserId;
    }

    public void setIsBmiPresent(boolean mIsBmiPresent) {
        this.mIsBmiPresent = mIsBmiPresent;
    }

    public void setBmi(double mBmi) {
        this.mBmi = mBmi;
    }

    public static ArrayList<WeightMeasurement> parseWeightMeasurement(String address, String uuid, byte[] value) {
        if (value == null || value.length == 0) {
            return null;
        }

        ArrayList<WeightMeasurement> measurements = new ArrayList<WeightMeasurement>();
        /**
         * org.bluetooth.characteristic.weight_measurement
         * Assigned Number : 0x2A9D
         *
         * Flags : 8bit
         * Weight : uint16 (bit 0 flags is set 0 - kg, set 1 - pound)
         * time_stamp : 7bytes = see org.bluetooth.characteristic.date_time
         * User ID : uint8
         * BMI : uint16
         * Height : uint16 (bit 0 flags set 0 - meter, set 1 - inches)
         */
        // 샤오미체중계에서 notification 이올때 최대 2개가 올 수 있다.
        boolean isContinue = true;
        int pos = 0;
        do {
            WeightMeasurement measurement = new WeightMeasurement();
            int flags = value[pos] & 0xf;
            measurement.setFlags(flags);
            // measurement unit
            measurement.setMeasurementUnit(flags & FLAG_VALUE_TRUE);
            // timestamp present
            measurement.setIsTimestampPresent(((flags >> 1) & FLAG_VALUE_TRUE) == FLAG_VALUE_TRUE);
            // userid present
            measurement.setIsUserIdPresent(((flags >> 2) & FLAG_VALUE_TRUE) == FLAG_VALUE_TRUE);
            // bmi present
            measurement.setIsBmiPresent(((flags >> 3) & FLAG_VALUE_TRUE) == FLAG_VALUE_TRUE);

            // set weight
            pos += 1;            // exclude flag
            byte[] temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN);          // uint16
            if (measurement.getMeasurementUnit() == MEASUREMENT_INTERNATIONAL_UNIT) {      // kg
                measurement.setWeight(DataParser.getUint16(temp) * 0.005);
            } else {            // pound
                measurement.setWeight(DataParser.getUint16(temp) * 0.01);
            }
            pos += Constants.UINT16_LEN;

            // set timestamp
            if (measurement.isTimestampPresent()) {
                temp = Arrays.copyOfRange(value, pos, pos + Constants.DATE_TIME_LEN);          // date_time
                measurement.setTimestamp(DataParser.getTime(temp));

                pos += Constants.DATE_TIME_LEN;
            }

            // set user id
            if (measurement.isUserIdPresent()) {
                temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT8_LEN);          // uint8
                measurement.setUserId(DataParser.getUint8(temp[0]));

                pos += Constants.UINT8_LEN;
            }

            // set BMI and Height
            if (measurement.isBmiPresent()) {
                // bmi
                temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN);          // uint16
                measurement.setBmi(DataParser.getUint16(temp) * 0.1);
                pos += Constants.UINT16_LEN;

                // height
                temp = Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN);          // uint16
                if (measurement.getMeasurementUnit() == MEASUREMENT_INTERNATIONAL_UNIT) {      // meter
                    measurement.setWeight(DataParser.getUint16(temp) * 0.001);
                } else {            // inches
                    measurement.setWeight(DataParser.getUint16(temp) * 0.1);
                }

                pos += Constants.UINT16_LEN;
            }

            measurement.setPrimaryAddress(address);
            measurement.setUuidString(uuid);
            measurements.add(measurement);
            if (pos >= value.length) {
                isContinue = false;
            }
        } while (isContinue);


        return measurements;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mFlags);
        dest.writeInt(this.mMeasurementUnit);
        dest.writeByte(mIsTimestampPresent ? (byte) 1 : (byte) 0);
        dest.writeSerializable(this.mTimestamp);
        dest.writeDouble(this.mWeight);
        dest.writeByte(mIsUserIdPresent ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mUserId);
        dest.writeByte(mIsBmiPresent ? (byte) 1 : (byte) 0);
        dest.writeDouble(this.mBmi);
        dest.writeDouble(this.mHeight);
    }

    public WeightMeasurement() {
    }

    protected WeightMeasurement(Parcel in) {
        this.mFlags = in.readInt();
        this.mMeasurementUnit = in.readInt();
        this.mIsTimestampPresent = in.readByte() != 0;
        this.mTimestamp = (Calendar) in.readSerializable();
        this.mWeight = in.readDouble();
        this.mIsUserIdPresent = in.readByte() != 0;
        this.mUserId = in.readInt();
        this.mIsBmiPresent = in.readByte() != 0;
        this.mBmi = in.readDouble();
        this.mHeight = in.readDouble();
    }

    public static final Creator<WeightMeasurement> CREATOR = new Creator<WeightMeasurement>() {
        public WeightMeasurement createFromParcel(Parcel source) {
            return new WeightMeasurement(source);
        }

        public WeightMeasurement[] newArray(int size) {
            return new WeightMeasurement[size];
        }
    };
}
