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

package com.nbplus.iotapp.data;

/**
 * Created by basagee on 2015. 9. 11..
 */
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Dave Smith
 * Double Encore, Inc.
 * AdRecord
 */
public class AdRecord implements Parcelable, Serializable {
    private static final long serialVersionUID = 1L;

    /* An incomplete list of the Bluetooth GAP AD Type identifiers */
    public static final int TYPE_FLAGS = 0x1;
    public static final int TYPE_UUID16_INC = 0x2;
    public static final int TYPE_UUID16 = 0x3;
    public static final int TYPE_UUID32_INC = 0x4;
    public static final int TYPE_UUID32 = 0x5;
    public static final int TYPE_UUID128_INC = 0x6;
    public static final int TYPE_UUID128 = 0x7;
    public static final int TYPE_NAME_SHORT = 0x8;
    public static final int TYPE_NAME = 0x9;
    public static final int TYPE_TRANSMITPOWER = 0xA;
    public static final int TYPE_CONNINTERVAL = 0x12;
    public static final int TYPE_SERVICEDATA = 0x16;
    public static final int TYPE_APPEARANCE = 0x19;
    public static final int TYPE_VENDOR_SPECIFIC = 0xFF;    // https://www.bluetooth.org/en-us/specification/assigned-numbers/company-identifiers

    /*
        enum Flags_t
        A list of values for the FLAGS AD Type.

        Note:
        You can use more than one value in the FLAGS AD Type (ex. LE_GENERAL_DISCOVERABLE and BREDR_NOT_SUPPORTED).
        Source
        Bluetooth  Core Specification 4.0 (Vol. 3), Part C, Section 18.1
        Enumerator:
        LE_LIMITED_DISCOVERABLE
        *Peripheral device is discoverable for a limited period of time

        LE_GENERAL_DISCOVERABLE
        Peripheral device is discoverable at any moment.

        BREDR_NOT_SUPPORTED
        Peripheral device is LE only.

        SIMULTANEOUS_LE_BREDR_C
        Not relevant - central mode only.

        SIMULTANEOUS_LE_BREDR_H
        Not relevant - central mode only.
     */

    /*
     * Read out all the AD structures from the raw scan record
     */
    public static HashMap<Integer, AdRecord> parseScanRecord(byte[] scanRecord) {
        HashMap<Integer, AdRecord> records = new HashMap<Integer, AdRecord>();

        if (scanRecord == null || scanRecord.length == 0) {
            return records;
        }
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index++];
            //Done once we run out of records
            if (length == 0) break;

            int type = scanRecord[index];
            //Done if our record isn't a valid type
            if (type == 0) break;


            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);

            records.put(type, new AdRecord(length, type, data));
            //Advance
            index += length;
        }

        return records;
    }

    /* Helper functions to parse out common data payloads from an AD structure */


    /* Model Object Definition */

    private int mLength;
    private int mType;
    /**
     * preference 에 저장할 예정이므로...
     * String 으로.
     */
    private String mData;

    public AdRecord(int length, int type, byte[] data) {
        mLength = length;
        mType = type;
        mData = DataParser.getHexString(data);
        //mData = data;
    }

    public int getLength() {
        return mLength;
    }

    public int getType() {
        return mType;
    }

    public byte[] getValue() {
        return DataParser.getHextoBytes(mData);
    }

    @Override
    public String toString() {
        switch (mType) {
            case TYPE_FLAGS:
                return "Flags";
            case TYPE_NAME_SHORT:
            case TYPE_NAME:
                return "Name";
            case TYPE_UUID16:
            case TYPE_UUID16_INC:
                return "UUIDs";
            case TYPE_TRANSMITPOWER:
                return "Transmit Power";
            case TYPE_CONNINTERVAL:
                return "Connect Interval";
            case TYPE_SERVICEDATA:
                return "Service Data";
            default:
                return "Unknown Structure: "+mType;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mLength);
        dest.writeInt(this.mType);
        dest.writeString(this.mData);
    }

    protected AdRecord(Parcel in) {
        this.mLength = in.readInt();
        this.mType = in.readInt();
        this.mData = in.readString();
    }

    public static final Creator<AdRecord> CREATOR = new Creator<AdRecord>() {
        public AdRecord createFromParcel(Parcel source) {
            return new AdRecord(source);
        }

        public AdRecord[] newArray(int size) {
            return new AdRecord[size];
        }
    };
}
