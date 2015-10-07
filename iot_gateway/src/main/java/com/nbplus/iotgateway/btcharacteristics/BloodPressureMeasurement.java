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

package com.nbplus.iotgateway.btcharacteristics;

import android.os.Parcel;

import com.nbplus.iotgateway.data.Constants;
import com.nbplus.iotgateway.data.DataParser;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by basagee on 2015. 9. 14..
 *
 * Service : Blood Pressure	org.bluetooth.service.blood_pressure	0x1810
 * see https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.blood_pressure.xml
 */
public class BloodPressureMeasurement extends BaseCharacteristicValue {

    public enum FlagValueBit {
        BLOOD_PRESSURE_UNIT_FLAG(0x00),
        TIME_STAMP_FLAG(0x01),
        PULSE_RATE_FLAG(0x02),
        USER_ID_FLAG(0x03),
        MEASUREMENT_STATUS_FLAG(0x04)
        ;

        private final int value;

        private FlagValueBit(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
    }

    public static final int BLOOD_PRESSURE_UNIT_MMHG = 0x00;
    public static final int BLOOD_PRESSURE_UNIT_KPA = 0x01;
    public static final String BLOOD_PRESSURE_UNIT_MMHG_STRING = "mmHg";
    public static final String BLOOD_PRESSURE_UNIT_KPA_STRING = "kPa";

    private int mFlags;                     // uint8
    private float mSystollic;               // sfloat
    private float mDiastollic;              // sfloat
    private float mMeanArterialPressure;    // sfloat
    private Calendar mTimeStamp;            // date_time
    private float mPulseRate;               // sfloat
    private int mUserId;                    // uint8
    private byte[] mMeasurementStatus;      // 16bit

    public int getFlagsBitValue(FlagValueBit value) {
        return (mFlags >> value.getValue()) & 0x01;
    }

    public int getBloodPressureMeasurementUnit() {
        return getFlagsBitValue(FlagValueBit.BLOOD_PRESSURE_UNIT_FLAG);
    }

    public String getBloodPressureMeasurementUnitString() {
        return getBloodPressureMeasurementUnit() == BLOOD_PRESSURE_UNIT_MMHG ?
                BLOOD_PRESSURE_UNIT_MMHG_STRING :
                BLOOD_PRESSURE_UNIT_KPA_STRING;
    }

    public byte[] getMeasurementStatus() {
        return mMeasurementStatus;
    }

    public void setMeasurementStatus(byte[] measurementStatus) {
        this.mMeasurementStatus = measurementStatus;
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int userId) {
        this.mUserId = userId;
    }

    public float getPulseRate() {
        return mPulseRate;
    }

    public void setPulseRate(float pulseRate) {
        this.mPulseRate = pulseRate;
    }

    public Calendar getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(Calendar timeStamp) {
        this.mTimeStamp = timeStamp;
    }

    public float getMeanArterialPressure() {
        return mMeanArterialPressure;
    }

    public void setMeanArterialPressure(float meanArterialPressure) {
        this.mMeanArterialPressure = meanArterialPressure;
    }

    public float getDiastollic() {
        return mDiastollic;
    }

    public void setDiastollic(float diastollic) {
        this.mDiastollic = diastollic;
    }

    public float getSystollic() {
        return mSystollic;
    }

    public void setSystollic(float systollic) {
        this.mSystollic = systollic;
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int flags) {
        this.mFlags = flags;
    }




    public static BloodPressureMeasurement parseBloodPressureMeasurement(byte[] value) {
        BloodPressureMeasurement measurement;
        if (value == null || value.length == 0) {
            return null;
        }

        /**
         * org.bluetooth.characteristic.blood_pressure_measurement
         * Assigned Number : 0x2A35
         *
         * Flags : 8bit
                 Bit Field
                 0	1	Blood Pressure Units Flag
                    0	Blood pressure for Systolic, Diastolic and MAP in units of mmHg	C1
                    1	Blood pressure for Systolic, Diastolic and MAP in units of kPa	C2
                 1	1	Time Stamp Flag
                    0	Time Stamp not present
                    1	Time Stamp present	C3
                 2	1	Pulse Rate Flag
                    0	Pulse Rate not present
                    1	Pulse Rate present	C4
                 3	1	User ID Flag
                    0	User ID not present
                    1	User ID present	C5
                 4	1	Measurement Status Flag
                    0	Measurement Status not present
                    1	Measurement Status present	C6
                 5	1	Reserved for future use
                 6	1	Reserved for future use
                 7	1	Reserved for future use

         * Systolic : SFLOAT
         * Diastolic : SFLOAT
         * Mean Arterial Pressure : SFLOAT

         * Time Stamp : 7bytes = see org.bluetooth.characteristic.date_time
         * Pulse Rate : SFLOAT
         * User ID : uint8
         * Measurement Status : 16bit
                 Key	Value
                 0	1	Body Movement Detection Flag
                     0	No body movement
                     1	Body movement during measurement
                 1	1	Cuff Fit Detection Flag
                     0	Cuff fits properly
                     1	Cuff too loose
                 2	1	Irregular Pulse Detection Flag
                    0	No irregular pulse detected
                    1	Irregular pulse detected
                 3	2	Pulse Rate Range Detection Flags
                    0	Pulse rate is within the range
                    1	Pulse rate exceeds upper limit
                    2	Pulse rate is less than lower limit
                    3	Reserved for future use
                 5	1	Measurement Position Detection Flag
                    0	Proper measurement position
                    1	Improper measurement position
                 6	1	Reserved for future use
                 7	1	Reserved for future use
                 8	1	Reserved for future use
                 9	1	Reserved for future use
                 10	1	Reserved for future use
                 11	1	Reserved for future use
                 12	1	Reserved for future use
                 13	1	Reserved for future use
                 14	1	Reserved for future use
                 15	1	Reserved for future use
         */

        measurement = new BloodPressureMeasurement();
        int flags = value[0] & 0xf;
        measurement.setFlags(flags);

        int pos = 1;
        // set systollic, diastollic, mean arterlial pressure
        measurement.setSystollic(DataParser.getSfloat(Arrays.copyOfRange(value, pos, pos + Constants.SFLOAT_LEN)));
        pos += Constants.SFLOAT_LEN;
        measurement.setDiastollic(DataParser.getSfloat(Arrays.copyOfRange(value, pos, pos + Constants.SFLOAT_LEN)));
        pos += Constants.SFLOAT_LEN;
        measurement.setMeanArterialPressure(DataParser.getSfloat(Arrays.copyOfRange(value, pos, pos + Constants.SFLOAT_LEN)));
        pos += Constants.SFLOAT_LEN;

        // set time stamp
        if (measurement.getFlagsBitValue(FlagValueBit.TIME_STAMP_FLAG) == Constants.FLAG_VALUE_TRUE) {
            measurement.setTimeStamp(DataParser.getTime(Arrays.copyOfRange(value, pos, pos + Constants.DATE_TIME_LEN)));
            pos += Constants.DATE_TIME_LEN;
        }

        // set pulse rate
        if (measurement.getFlagsBitValue(FlagValueBit.PULSE_RATE_FLAG) == Constants.FLAG_VALUE_TRUE) {
            measurement.setPulseRate(DataParser.getSfloat(Arrays.copyOfRange(value, pos, pos + Constants.SFLOAT_LEN)));
            pos += Constants.SFLOAT_LEN;
        }

        // get and set user ID
        if (measurement.getFlagsBitValue(FlagValueBit.USER_ID_FLAG) == Constants.FLAG_VALUE_TRUE) {
            measurement.setUserId(DataParser.getUint8(value[pos]));
            pos += Constants.UINT8_LEN;
        }

        // measurement status
        if (measurement.getFlagsBitValue(FlagValueBit.MEASUREMENT_STATUS_FLAG) == Constants.FLAG_VALUE_TRUE) {
            measurement.setMeasurementStatus(Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN));
        }

        return measurement;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.mFlags);
        dest.writeFloat(this.mSystollic);
        dest.writeFloat(this.mDiastollic);
        dest.writeFloat(this.mMeanArterialPressure);
        dest.writeSerializable(this.mTimeStamp);
        dest.writeFloat(this.mPulseRate);
        dest.writeInt(this.mUserId);
        dest.writeByteArray(this.mMeasurementStatus);
    }

    public BloodPressureMeasurement() {
    }

    protected BloodPressureMeasurement(Parcel in) {
        super(in);
        this.mFlags = in.readInt();
        this.mSystollic = in.readFloat();
        this.mDiastollic = in.readFloat();
        this.mMeanArterialPressure = in.readFloat();
        this.mTimeStamp = (Calendar) in.readSerializable();
        this.mPulseRate = in.readFloat();
        this.mUserId = in.readInt();
        this.mMeasurementStatus = in.createByteArray();
    }

    public static final Creator<BloodPressureMeasurement> CREATOR = new Creator<BloodPressureMeasurement>() {
        public BloodPressureMeasurement createFromParcel(Parcel source) {
            return new BloodPressureMeasurement(source);
        }

        public BloodPressureMeasurement[] newArray(int size) {
            return new BloodPressureMeasurement[size];
        }
    };
}
