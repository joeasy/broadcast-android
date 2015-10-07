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
 * Service : Glucose	org.bluetooth.service.glucose	0x1808
 * see https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.glucose.xml
 */
public class GlucoseMeasurement extends BaseCharacteristicValue {

    public enum FlagValueBit {
        TIME_OFFSET_PRESENT(0x00),
        GLUCOSE_CONCENTRATION_PRESENT(0x01),
        GLUCOSE_CONCENTRATION_UNIT(0x02),
        SENSOR_STATUS_ANNUNCIATION_PRESENT(0x03),
        CONTEXT_INFORMATION_FOLLOWS(0x04)
        ;

        private final int value;

        private FlagValueBit(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
    }

    public static final int GLUCOSE_CONCENTRATION_UNIT_KG_PER_LITRE = 0x00;
    public static final int GLUCOSE_CONCENTRATION_UNIT_MOL_PER_LITRE = 0x01;
    public static final String GLUCOSE_CONCENTRATION_UNIT_KG_PER_LITRE_STRING = "kg/L";
    public static final String GLUCOSE_CONCENTRATION_UNIT_MOL_PER_LITRE_STRING = "mol/L";

    private int mFlags;                     // uint8
    private int mSequenceNumber;            // uint16
    private Calendar mBaseTime;             // date_time
    private int mTimeOffset;                // sint16
    private float mGlucoseConcentration;    // sfloat
    private int mType;                      // nibble
    private int mSampleLocation;            // nibble
    private byte[] mSensorStatusAnnunciation;  // 16bit

    public byte[] getSensorStatusAnnunciation() {
        return mSensorStatusAnnunciation;
    }

    public void setSensorStatusAnnunciation(byte[] sensorStatusAnnunciation) {
        this.mSensorStatusAnnunciation = sensorStatusAnnunciation;
    }

    public int getGlucoseConcentrationUnit() {
        return getFlagsBitValue(FlagValueBit.GLUCOSE_CONCENTRATION_UNIT);
    }

    public String getGlucoseConcentrationUnitString() {
        return getGlucoseConcentrationUnit() == GLUCOSE_CONCENTRATION_UNIT_KG_PER_LITRE ?
                GLUCOSE_CONCENTRATION_UNIT_KG_PER_LITRE_STRING :
                GLUCOSE_CONCENTRATION_UNIT_MOL_PER_LITRE_STRING;
    }

    public int getFlagsBitValue(FlagValueBit value) {
        return (mFlags >> value.getValue()) & 0x01;
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int flags) {
        this.mFlags = flags;
    }

    public int getSequenceNumber() {
        return mSequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.mSequenceNumber = sequenceNumber;
    }

    public Calendar getBaseTime() {
        return mBaseTime;
    }

    public void setBaseTime(Calendar baseTime) {
        this.mBaseTime = baseTime;
    }

    public int getTimeOffset() {
        return mTimeOffset;
    }

    public void setTimeOffset(int timeOffset) {
        this.mTimeOffset = timeOffset;
    }

    public float getGlucoseConcentration() {
        return mGlucoseConcentration;
    }

    public void setGlucoseConcentration(float glucoseConcentration) {
        this.mGlucoseConcentration = glucoseConcentration;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        this.mType = type;
    }

    public int getSampleLocation() {
        return mSampleLocation;
    }

    public void setSampleLocation(int sampleLocation) {
        this.mSampleLocation = sampleLocation;
    }


    public static GlucoseMeasurement parseGlucoseMeasurement(byte[] value) {
        GlucoseMeasurement measurement;
        if (value == null || value.length == 0) {
            return null;
        }

        /**
         * org.bluetooth.characteristic.glucose_measurement
         * Assigned Number : 0x2A18
         *
         * Flags : 8bit
                 Bit Field
                 0	Time Offset Present
                    0	False
                    1	True	C1
                 1	Glucose Concentration, Type and Sample Location Present
                    0	False
                    1	True	C2
                 2	Glucose Concentration Units
                    0	kg/L	C3
                    1	mol/L	C4
                 3	Sensor Status Annunciation Present
                    0	False
                    1	True	C5
                 4	Context Information Follows
                    0	False
                    1	True
                 5~7	Reserved for future use
         * Sequence Number : uint16
         * Base Time : 7bytes = see org.bluetooth.characteristic.date_time
         * Time Offset : sint16
         * Glucose Concentration : sfloat (bit 0 flags is set 0 - kg/L, set 1 - mol/L)
         * Type : nibble
                 Key	Value
                 0	Reserved for future use
                 1	Capillary Whole blood
                 2	Capillary Plasma
                 3	Venous Whole blood
                 4	Venous Plasma
                 5	Arterial Whole blood
                 6	Arterial Plasma
                 7	Undetermined Whole blood
                 8	Undetermined Plasma
                 9	Interstitial Fluid (ISF)
                 10	Control Solution
                 11 - 15	Reserved for future use

         * Sample Location : nibble
                 Key	Value
                 0	Reserved for future use
                 1	Finger
                 2	Alternate Site Test (AST)
                 3	Earlobe
                 4	Control solution
                 15	Sample Location value not available
                 5 - 14	Reserved for future use

         * Sensor Status Annunciation : 16bit
                 Bit	Size	Name
                 0	1	Device battery low at time of measurement
                     0	False
                     1	True
                 1	1	Sensor malfunction or faulting at time of measurement
                     0	False
                     1	True
                 2	1	Sample size for blood or control solution insufficient at time of measurement
                     0	False
                     1	True
                 3	1	Strip insertion error
                     0	False
                     1	True
                 4	1	Strip type incorrect for device
                     0	False
                     1	True
                 5	1	Sensor result higher than the device can process
                     0	False
                     1	True
                 6	1	Sensor result lower than the device can process
                     0	False
                     1	True
                 7	1	Sensor temperature too high for valid test/result at time of measurement
                     0	False
                     1	True
                 8	1	Sensor temperature too low for valid test/result at time of measurement
                     0	False
                     1	True
                 9	1	Sensor read interrupted because strip was pulled too soon at time of measurement
                     0	False
                     1	True
                 10	1	General device fault has occurred in the sensor
                     0	False
                     1	True
                 11	1	Time fault has occurred in the sensor and time may be inaccurate
                     0	False
                     1	True
                 12	4	Reserved for future use
         */

        measurement = new GlucoseMeasurement();
        int flags = value[0] & 0xf;
        measurement.setFlags(flags);

        int pos = 1;
        // set sequence number
        measurement.setSequenceNumber(DataParser.getUint16(Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN)));

        // set base time
        pos += Constants.UINT16_LEN;
        measurement.setBaseTime(DataParser.getTime(Arrays.copyOfRange(value, pos, pos + Constants.DATE_TIME_LEN)));

        pos += Constants.DATE_TIME_LEN;
        if (measurement.getFlagsBitValue(FlagValueBit.TIME_OFFSET_PRESENT) == Constants.FLAG_VALUE_TRUE) {
            // get and set time offset
            measurement.setTimeOffset(DataParser.getUint16(Arrays.copyOfRange(value, pos, pos + Constants.UINT16_LEN)));

            pos += Constants.UINT16_LEN;
        }

        if (measurement.getFlagsBitValue(FlagValueBit.GLUCOSE_CONCENTRATION_PRESENT) == Constants.FLAG_VALUE_TRUE) {
            // get and set glucose concentration
            measurement.setGlucoseConcentration(DataParser.getSfloat(Arrays.copyOfRange(value, pos, pos + Constants.SFLOAT_LEN)));

            // get and set type
            pos += Constants.SFLOAT_LEN;
            int data = value[pos];
            measurement.setType((data & 0xf0) >> 4);
            measurement.setSampleLocation((data & 0x0f));

            pos += Constants.UINT8_LEN;
        }

        if (measurement.getFlagsBitValue(FlagValueBit.SENSOR_STATUS_ANNUNCIATION_PRESENT) == Constants.FLAG_VALUE_TRUE) {
            measurement.setSensorStatusAnnunciation(Arrays.copyOfRange(value, pos, pos + 2));       // 16bit
        }

        return measurement;
    }

    public GlucoseMeasurement() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.mFlags);
        dest.writeInt(this.mSequenceNumber);
        dest.writeSerializable(this.mBaseTime);
        dest.writeInt(this.mTimeOffset);
        dest.writeFloat(this.mGlucoseConcentration);
        dest.writeInt(this.mType);
        dest.writeInt(this.mSampleLocation);
        dest.writeByteArray(this.mSensorStatusAnnunciation);
    }

    protected GlucoseMeasurement(Parcel in) {
        super(in);
        this.mFlags = in.readInt();
        this.mSequenceNumber = in.readInt();
        this.mBaseTime = (Calendar) in.readSerializable();
        this.mTimeOffset = in.readInt();
        this.mGlucoseConcentration = in.readFloat();
        this.mType = in.readInt();
        this.mSampleLocation = in.readInt();
        this.mSensorStatusAnnunciation = in.createByteArray();
    }

    public static final Creator<GlucoseMeasurement> CREATOR = new Creator<GlucoseMeasurement>() {
        public GlucoseMeasurement createFromParcel(Parcel source) {
            return new GlucoseMeasurement(source);
        }

        public GlucoseMeasurement[] newArray(int size) {
            return new GlucoseMeasurement[size];
        }
    };
}
