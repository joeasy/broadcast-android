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

import android.util.Log;

import com.nbplus.iotlib.data.IoTDevice;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by basagee on 2015. 9. 14..
 */
public class DataParser {
    private static final String TAG = DataParser.class.getSimpleName();

    // convert from byte array to String
    public static String getString(byte[] value) {
        if (value != null) {
            return new String(value);
        }
        return null;
    }

    public static byte[] getBytes(String value) {
        if (value != null) {
            return value.getBytes(Charset.forName("UTF-8"));
        }
        return new byte[]{};
    }

    /**
     * get unsigned int8
     * @param value
     * @return
     */
    public static int getUint8(byte value) {
        return value & 0xff;
    }

    /**
     * get signed int8
     * @param value
     * @return
     */
    public static int getInt8(byte value) {
        int number = value & 0x7F;
        if ((value & 0x80) > 0) { // negative
            // 2's complement
            number = (0x7F - number + 1) * -1;
        }
        return number;
    }
    /**
     * covert byte array[2] to uint16 / int16
     * @param value
     * @return
     */
    public static int getUint16(byte[] value) {
        if (value == null || value.length < 2) {
            Log.e(TAG, "value is null or invalid length");
            return -1;
        }

        return ((0xff & value[1]) << 8 | (0xff & value[0]));
    }

    public static String getUuid32String(byte[] value) {
        String result = "";
        int length = value.length;
        if (length != 8) {
            return "";
        }

        for (int i = 8; i > 0; i -= 2) {
            byte[] temp = Arrays.copyOfRange(value, i - 2, i);
            result += getUint16String(temp);
        }
        return result;
    }

    public static String getUuid128String(byte[] value) {
        String result = "";
        int length = value.length;
        if (length != 16) {
            return "";
        }

        for (int i = 16; i > 0; i -= 2) {
            byte[] temp = Arrays.copyOfRange(value, i - 2, i);
            if (i == 12 || i == 10 || i == 8 || i == 6) {
                result += "-";
            }
            result += getUint16String(temp);
        }
        return result;
    }

    /**
     * covert byte array[2] - uint16 to string
     * @param value
     * @return
     */
    public static String getUint16String(byte[] value) {
        if (value == null || value.length < 2) {
            Log.e(TAG, "value is null or invalid length");
            return null;
        }
        return String.format("%04x", getUint16(value));
    }

    /**
     * UUID16 형태의 리스트를 가져오는데 사용한다.
     * Advertising 데이터의 UUID16의경우 리스트로 전달될 수 있다.
     * @param value
     * @return
     */
    public static ArrayList<String> getUint16StringArray(byte[] value) {
        if (value == null || value.length < 2) {
            Log.e(TAG, "value is null or invalid length");
            return new ArrayList<String>();
        }

        int len = value.length;
        ArrayList<String> strArrays = new ArrayList<String>();
        for (int i = 0; i < len; i += 2) {
            if (i + 2 > len) {
                Log.w(TAG, "array length .... ");
                continue;
            }
            byte[] temp = Arrays.copyOfRange(value, i, i + 2);
            strArrays.add(getUint16String(temp));
        }

        return strArrays;
    }

    public static ArrayList<String> getUint32StringArray(byte[] value) {
        final int bitlen32 = 8;
        if (value == null || value.length < 8 || value.length % 8 != 0 ) {
            Log.e(TAG, "value is null or invalid length");
            return new ArrayList<String>();
        }

        int len = value.length;
        ArrayList<String> strArrays = new ArrayList<String>();
        for (int i = 0; i < len; i += bitlen32) {
            String str128 = "";
            byte[] temp = Arrays.copyOfRange(value, i, i + bitlen32);
            strArrays.add(getUuid32String(temp));
        }
        return strArrays;
    }

    public static ArrayList<String> getUint128StringArray(byte[] value) {
        final int bitlen128 = 16;
        if (value == null || value.length < 16 || value.length % 16 != 0 ) {
            Log.e(TAG, "value is null or invalid length");
            return new ArrayList<String>();
        }

        int len = value.length;
        ArrayList<String> strArrays = new ArrayList<String>();
        for (int i = 0; i < len; i += bitlen128) {
            String str128 = "";
            byte[] temp = Arrays.copyOfRange(value, i, i + bitlen128);
            strArrays.add(getUuid128String(temp));
        }
        return strArrays;
    }

    /**
     * covert byte array to hex string
     * @param value
     * @return
     */
    public static String getHexString(byte[] value) {
        if (value == null) {
            return null;
        }
        String str = "";
        int length = value.length;
        for (int i = 0; i < length; i ++) {
            String num = String.format("%02x", value[i]);
            str += num;
        }
        return str.toUpperCase();
    }

    public static byte[] getHextoBytes(String value) {
        if (value == null) {
            return null;
        }
        int len = value.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(value.charAt(i), 16) << 4)
                    + Character.digit(value.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * convert byte array to date_time
     * @param value
     * @return
     */
    public static Calendar getTime(byte[] value) {
        /*
         * org.bluetooth.characteristic.date_time
         * Year: uint16
         * Month: uint8
         * Day: uint8
         * Hours: uint8
         * Minutes: uint8
         * Seconds: uint8
         */
        if (value == null || value.length < Constants.DATE_TIME_LEN) {
            Log.w(TAG, "getTime() : invalid parameter.. ");
            return null;
        }

        Calendar cal = Calendar.getInstance();

        int pos = 2;
        int year = getUint16(Arrays.copyOfRange(value, 0, pos));
        int month = getUint8(value[pos++]);
        int day = getUint8(value[pos++]);
        int hour = getUint8(value[pos++]);
        Log.d(TAG, "hour >>>> " + hour);
        int min = getUint8(value[pos++]);
        int sec = getUint8(value[pos++]);
        cal.set(year, month, day, hour, min, sec);

        return cal;
    }

    public static int getServiceDataUuid(AdRecord serviceData) {
        if (serviceData.getType() != AdRecord.TYPE_SERVICEDATA) return -1;

        byte[] raw = serviceData.getValue();
        //Find UUID data in byte array
        int uuid = (raw[1] & 0xFF) << 8;
        uuid += (raw[0] & 0xFF);

        return uuid;
    }

    public static byte[] getServiceData(AdRecord serviceData) {
        if (serviceData.getType() != AdRecord.TYPE_SERVICEDATA) return null;

        byte[] raw = serviceData.getValue();
        //Chop out the uuid
        return Arrays.copyOfRange(raw, 2, raw.length);
    }

    public static float getFloat(byte [] array) {

        if (array.length != 4) {
            return 0.0F;
        }

        long int_data = getUnsignedNumber(array);

        int mantissa = (int)(int_data & 0x00FFFFFF);
        int exponent = (int) (int_data >> 24);
        float output = 0;

        if (mantissa >= 0x007FFFFE && mantissa <= 0x00800002) {
            return 0.0F;
        } else {
            if (mantissa >= 0x800000) {
                mantissa = -((0xFFFFFF + 1) - mantissa);
            }
            output = (mantissa * (float) Math.pow(10, exponent));
        }

        return output;
    }

    public static float getSfloat(byte [] array) {

        if (array.length < 2) {
            return 0.f;
        }

        long int_data = getUnsignedNumber(array);
        int mantissa = (int) (int_data & 0x0FFF);
        int exponent = (int) (int_data >> 12);
        float output = 0.F;

        if (exponent >= 0x0008) {
            exponent = -((0x000F + 1) - exponent);
        }

        if (mantissa >= 0x07FE && mantissa <= 0x0802) {
        } else {
            if (mantissa >= 0x0800) {
                mantissa = -((0x0FFF + 1) - mantissa);
            }
            output = mantissa * (float) Math.pow(10, exponent);
        }

        Log.d(TAG, ">>> output = " + output);
        return output;
    }

    public static long getUnsignedNumber(byte [] array) {
        long value = 0;
        for (int i = 0; i < array.length; i ++) {
            value += (array[i] << (8 * i) ) & (0x0FF << (8 * i));
        }
        return value;
    }

    public static ArrayList<String> getUuids(HashMap<Integer, AdRecord> adRecords) {
        byte[] uuidBytes = null;
        ArrayList<String> uuids = null;

        if (adRecords.get(AdRecord.TYPE_UUID16) != null) {
            uuidBytes = adRecords.get(AdRecord.TYPE_UUID16).getValue();
            uuids = DataParser.getUint16StringArray(uuidBytes);
        } else if (adRecords.get(AdRecord.TYPE_UUID16_INC) != null) {
            uuidBytes = adRecords.get(AdRecord.TYPE_UUID16_INC).getValue();
            uuids = DataParser.getUint16StringArray(uuidBytes);
        } else if (adRecords.get(AdRecord.TYPE_UUID32) != null) {
            uuidBytes = adRecords.get(AdRecord.TYPE_UUID32).getValue();
            uuids = DataParser.getUint32StringArray(uuidBytes);
        } else if (adRecords.get(AdRecord.TYPE_UUID32_INC) != null) {
            uuidBytes = adRecords.get(AdRecord.TYPE_UUID32_INC).getValue();
            uuids = DataParser.getUint32StringArray(uuidBytes);
        } else if (adRecords.get(AdRecord.TYPE_UUID128) != null) {
            uuidBytes = adRecords.get(AdRecord.TYPE_UUID128).getValue();
            uuids = DataParser.getUint128StringArray(uuidBytes);
        } else if (adRecords.get(AdRecord.TYPE_UUID128_INC) != null) {
            uuidBytes = adRecords.get(AdRecord.TYPE_UUID128_INC).getValue();
            uuids = DataParser.getUint128StringArray(uuidBytes);
        }

        return uuids;
    }

    public static int getUuidLength(HashMap<Integer, AdRecord> adRecords) {
        byte[] uuidBytes = null;
        ArrayList<String> uuids = null;

        if (adRecords.get(AdRecord.TYPE_UUID16) != null) {
            return IoTDevice.DEVICE_BT_UUID_LEN_16;
        } else if (adRecords.get(AdRecord.TYPE_UUID16_INC) != null) {
            return IoTDevice.DEVICE_BT_UUID_LEN_16;
        } else if (adRecords.get(AdRecord.TYPE_UUID32) != null) {
            return IoTDevice.DEVICE_BT_UUID_LEN_32;
        } else if (adRecords.get(AdRecord.TYPE_UUID32_INC) != null) {
            return IoTDevice.DEVICE_BT_UUID_LEN_32;
        } else if (adRecords.get(AdRecord.TYPE_UUID128) != null) {
            return IoTDevice.DEVICE_BT_UUID_LEN_128;
        } else if (adRecords.get(AdRecord.TYPE_UUID128_INC) != null) {
            return IoTDevice.DEVICE_BT_UUID_LEN_128;
        }

        return IoTDevice.DEVICE_BT_UUID_LEN_16;
    }

}
