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

/**
 * Created by basagee on 2015. 9. 14..
 *
 * Service : Heart Rate	org.bluetooth.service.heart_rate	0x180D
 * see https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.heart_rate.xml
 */
public class HeartRateMeasurement extends BaseCharacteristicValue {

    public enum FlagValueBit {
        HEART_RATE_VALUE_FORMAT_BIT_FLAG(0x00),
        SENSOR_CONTACT_STATUS_FLAG(0x01),               // 2bits value
        ENERGY_EXPENDED_STATUS_FLAG(0x03),
        RR_INTERVAL_FLAG(0x04)
        ;

        private final int value;

        private FlagValueBit(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
    }

    public static final String HEART_RATE_MEASUREMENT_UNIT_STRING = "bpm";

    private int mFlags;                             // uint8
    private int mHeartRateMeasurementValue;         // 8bit or 16bit - see flag 0 bit
    private int mEnertyExpended;                    // uint16
    private int mRrInterval;                        // uint16 - 1/1024 seconds

    public int getFlagsBitValue(FlagValueBit value) {
        return (mFlags >> value.getValue()) & 0x01;
    }
    public int getFlags2BitValue(FlagValueBit value) {
        return (((mFlags >> value.getValue()) & 0x01) << 1) | ((mFlags >> (value.getValue() + 1)) & 0x01);
    }

    public String getHeartRateMeasurementUnitString() {
        return HEART_RATE_MEASUREMENT_UNIT_STRING;
    }

    public int getRrInterval() {
        return mRrInterval;
    }

    public void setRrInterval(int rrInterval) {
        this.mRrInterval = rrInterval;
    }

    public int getEnertyExpended() {
        return mEnertyExpended;
    }

    public void setEnertyExpended(int enertyExpended) {
        this.mEnertyExpended = enertyExpended;
    }

    public int getHeartRateMeasurementValue() {
        return mHeartRateMeasurementValue;
    }

    public void setHeartRateMeasurementValue(int heartRateMeasurementValue) {
        this.mHeartRateMeasurementValue = heartRateMeasurementValue;
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int flags) {
        this.mFlags = flags;
    }





    public static HeartRateMeasurement parseHeartRateMeasurement(byte[] value) {
        HeartRateMeasurement measurement;
        if (value == null || value.length == 0) {
            return null;
        }

        /**
         * org.bluetooth.characteristic.heart_rate_measurement
         * Assigned Number : 2A37
         *
         * Flags : 8bit
                 Bit Field
                 0	1	Heart Rate Value Format bit
                    0	Heart Rate Value Format is set to UINT8. Units: beats per minute (bpm)	C1
                    1	Heart Rate Value Format is set to UINT16. Units: beats per minute (bpm)	C2
                 1	2	Sensor Contact Status bits
                    0	Sensor Contact feature is not supported in the current connection
                    1	Sensor Contact feature is not supported in the current connection
                    2	Sensor Contact feature is supported, but contact is not detected
                    3	Sensor Contact feature is supported and contact is detected
                 3	1	Energy Expended Status bit
                    0	Energy Expended field is not present
                    1	Energy Expended field is present. Units: kilo Joules	C3
                 4	1	RR-Interval bit
                    0	RR-Interval values are not present.
                    1	One or more RR-Interval values are present. Units: 1/1024 seconds	C4
                 5	3	Reserved for future use

         * Heart Rate Measurement Value : uint8 or uint16 - see flag bit 0
         * Energy Expended : uint16
         * RR-Interval : uint16
         */

        measurement = new HeartRateMeasurement();
        int flags = value[0] & 0xf;
        measurement.setFlags(flags);

        int pos = 1;
        if (measurement.getFlagsBitValue(FlagValueBit.HEART_RATE_VALUE_FORMAT_BIT_FLAG) == Constants.FLAG_VALUE_TRUE) {
            // uint16
            measurement.setHeartRateMeasurementValue(DataParser.getUint16(Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN)));
            pos += Constants.UINT16_LEN;
        } else {
            // uint8
            measurement.setHeartRateMeasurementValue(DataParser.getUint8(value[0]));
            pos += Constants.UINT8_LEN;
        }

        if (measurement.getFlagsBitValue(FlagValueBit.ENERGY_EXPENDED_STATUS_FLAG) == Constants.FLAG_VALUE_TRUE) {
            measurement.setEnertyExpended(DataParser.getUint16(Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN)));
            pos += Constants.UINT16_LEN;
        }

        if (measurement.getFlagsBitValue(FlagValueBit.RR_INTERVAL_FLAG) == Constants.FLAG_VALUE_TRUE) {
            measurement.setRrInterval(DataParser.getUint16(Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN)));
            pos += Constants.UINT16_LEN;
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
        dest.writeInt(this.mHeartRateMeasurementValue);
        dest.writeInt(this.mEnertyExpended);
        dest.writeInt(this.mRrInterval);
    }

    public HeartRateMeasurement() {
    }

    protected HeartRateMeasurement(Parcel in) {
        super(in);
        this.mFlags = in.readInt();
        this.mHeartRateMeasurementValue = in.readInt();
        this.mEnertyExpended = in.readInt();
        this.mRrInterval = in.readInt();
    }

    public static final Creator<HeartRateMeasurement> CREATOR = new Creator<HeartRateMeasurement>() {
        public HeartRateMeasurement createFromParcel(Parcel source) {
            return new HeartRateMeasurement(source);
        }

        public HeartRateMeasurement[] newArray(int size) {
            return new HeartRateMeasurement[size];
        }
    };
}
