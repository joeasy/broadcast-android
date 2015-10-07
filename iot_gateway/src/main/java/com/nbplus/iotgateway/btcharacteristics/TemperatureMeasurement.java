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
 * Service : Health Thermometer	org.bluetooth.service.health_thermometer	0x1809
 * see https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.health_thermometer.xml
 */
public class TemperatureMeasurement extends BaseCharacteristicValue {

    public enum FlagValueBit {
        TEMPERATURE_UNIT_FLAG(0x00),
        TIME_STAMP_FLAG(0x01),
        TEMPERATURE_TYPE_FLAG(0x03)
        ;

        private final int value;

        private FlagValueBit(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
    }

    public static final int TEMPERATURE_MEASUREMENT_UNIT_CELSIUS = 0x00;
    public static final int HEART_RATE_MEASUREMENT_UNIT_FAHRENHEIT = 0x01;
    public static final String TEMPERATURE_MEASUREMENT_UNIT_CELSIUS_STRING = "°C";
    public static final String HEART_RATE_MEASUREMENT_UNIT_FAHRENHEIT_STRING = "°F";

    private int mFlags;                             // uint8
    private float mTemperatureMeasurementValue;       // float
    private Calendar mTimeStamp;                    // date_time
    private int mTemperatureType;                        // 8bit

    public int getFlagsBitValue(FlagValueBit value) {
        return (mFlags >> value.getValue()) & 0x01;
    }

    public int getTemperatureMeasurementUnit() {
        return getFlagsBitValue(FlagValueBit.TEMPERATURE_UNIT_FLAG);
    }

    public String getTemperatureMeasurementUnitString() {
        return getTemperatureMeasurementUnit() == TEMPERATURE_MEASUREMENT_UNIT_CELSIUS ?
                TEMPERATURE_MEASUREMENT_UNIT_CELSIUS_STRING :
                HEART_RATE_MEASUREMENT_UNIT_FAHRENHEIT_STRING;
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int flags) {
        this.mFlags = flags;
    }

    public int getTemperatureType() {
        return mTemperatureType;
    }

    public void setTemperatureType(int temperatureType) {
        this.mTemperatureType = temperatureType;
    }

    public Calendar getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(Calendar timeStamp) {
        this.mTimeStamp = timeStamp;
    }

    public float getTemperatureMeasurementValue() {
        return mTemperatureMeasurementValue;
    }

    public void setTemperatureMeasurementValue(float temperatureMeasurementValue) {
        this.mTemperatureMeasurementValue = temperatureMeasurementValue;
    }

    public static TemperatureMeasurement parseBloodPressureMeasurement(byte[] value) {
        TemperatureMeasurement measurement;
        if (value == null || value.length == 0) {
            return null;
        }

        /**
         * org.bluetooth.characteristic.heart_rate_measurement
         * Assigned Number : 2A37
         *
         * Flags : 8bit
                 Bit Field
                 1	Temperature Units Flag
                    0	Temperature Measurement Value in units of Celsius	C1
                    1	Temperature Measurement Value in units of Fahrenheit	C2
                 1	1	Time Stamp Flag
                    0	Time Stamp field not present
                    1	Time Stamp field present	C3
                 2	1	Temperature Type Flag
                    0	Temperature Type field not present
                    1	Temperature Type field present	C4
                 3	1	Reserved for future use
                 4	1	Reserved for future use
                 5	1	Reserved for future use
                 6	1	Reserved for future use
                 7	1	Reserved for future use

         * Temperature Measurement Value : float
         * Time Stamp : date_time
         * Temperature Type : org.bluetooth.characteristic.temperature_type (8bit)
                 Key	Value
                 1	Armpit
                 2	Body (general)
                 3	Ear (usually ear lobe)
                 4	Finger
                 5	Gastro-intestinal Tract
                 6	Mouth
                 7	Rectum
                 8	Toe
                 9	Tympanum (ear drum)
                 10 - 255	Reserved for future use
                 0 - 0	Reserved for future use
         */

        measurement = new TemperatureMeasurement();
        int flags = value[0] & 0xf;
        measurement.setFlags(flags);

        int pos = 1;

        // set temperature measurement value
        measurement.setTemperatureMeasurementValue(DataParser.getFloat(Arrays.copyOfRange(value, pos, pos + Constants.FLOAT_LEN)));

        if (measurement.getFlagsBitValue(FlagValueBit.TIME_STAMP_FLAG) == Constants.FLAG_VALUE_TRUE) {
            measurement.setTimeStamp(DataParser.getTime(Arrays.copyOfRange(value, pos, pos + Constants.DATE_TIME_LEN)));
            pos += Constants.DATE_TIME_LEN;
        }

        if (measurement.getFlagsBitValue(FlagValueBit.TEMPERATURE_TYPE_FLAG) == Constants.FLAG_VALUE_TRUE) {
            measurement.setTemperatureType(DataParser.getUint8(value[pos]));
            pos += Constants.UINT8_LEN;
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
        dest.writeFloat(this.mTemperatureMeasurementValue);
        dest.writeSerializable(this.mTimeStamp);
        dest.writeInt(this.mTemperatureType);
    }

    public TemperatureMeasurement() {
    }

    protected TemperatureMeasurement(Parcel in) {
        super(in);
        this.mFlags = in.readInt();
        this.mTemperatureMeasurementValue = in.readFloat();
        this.mTimeStamp = (Calendar) in.readSerializable();
        this.mTemperatureType = in.readInt();
    }

    public static final Creator<TemperatureMeasurement> CREATOR = new Creator<TemperatureMeasurement>() {
        public TemperatureMeasurement createFromParcel(Parcel source) {
            return new TemperatureMeasurement(source);
        }

        public TemperatureMeasurement[] newArray(int size) {
            return new TemperatureMeasurement[size];
        }
    };
}
