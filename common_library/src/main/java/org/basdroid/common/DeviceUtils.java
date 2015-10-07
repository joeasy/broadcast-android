/*
 * Copyright (c) 2015. Basagee Yun. (www.basagee.tk)
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

package org.basdroid.common;

/**
 * # ODIN-1 Specifications
 *  Featured Updated Apr 10, 2012 by j...@mobclix.com
 *  ### Nomenclature
 *      The identifier generated in this implementation of ODIN is officially designated ODIN-1. In the event of a major revision, the new identifier will be termed ODIN-2 and so forth.
 *
 * ### Generating an ODIN-1
 *      Creating an ODIN-1 is designed to be as simple as possible:
 *
 * 1. Identifier Seed Step: Lookup the following Identifier Seed for each of the following platforms. The seed should be left unaltered from the format returned by the operating system.
 *
 *                      iOS                   Android            Windows Phone
 * Identifier Seed   802.11 MAC Address      ANDROID_ID         DeviceUniqueId
 * Format               Byte Array           Java String          C# String
 * // NOTE: iOS returns MAC Address NOT a string, but a 6-byte array.
 *
 * // A human readable MAC Address may be represented as the following:
 *
 * @"1a:2b:3c:4d:5e:6f";
 * @"1A2B3C4D5E6F";
 *
 * // However, representing it as a raw byte array prevents any ambiguity around punctuation and capitalization:
 *
 * @[0x1a, 0x2b, 0x3c, 0x4d, 0x5e, 0x6f];
 * 2. Hash Step: Pass the Identifier Seed through the SHA-1 hash function.
 *
 * 3.The resulting message digest is ODIN-1.
 *
 * // The format of this hash should be a 40 lowercase character string:
 *
 * @"82a53f1222f8781a5063a773231d4a7ee41bdd6f"
 *
 * Security
 *     Because ODIN-1 is personally identifying information, any ODIN-1 must not be transmitted over unsecure connections as plain text.
 *
 */
/**
 * Created by basagee on 2015. 5. 6..
 */

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.security.MessageDigest;

public class DeviceUtils {
    private final static String TAG = DeviceUtils.class.getSimpleName();

    /*
     * Returns the ODIN-1 String for the Android device. For devices that have a null
     * or invalid ANDROID_ID (such as the emulator), a null value will be returned.
     *
     * This code is designed to be built against an Android API level 3 or greater,
     * but supports all Android API levels.
     *
     * @param  context   the context of the application.
     *
     * @return           the ODIN-1 string or null if the ANDROID_ID is invalid.
     */
    public static String getDeviceIdByAndroidID(Context context) {
        String androidId;
        try {
            androidId = Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {

            // In Android API levels 1-2, Settings.Secure wasn't implemented.
            // Fall back to deprecated methods.
            try {
                androidId = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
            } catch (Exception e1) {
                Log.i(TAG, "Error generating ODIN-1: ", e1);
                return null;
            }
        }

        return SHA1(androidId);
    }

    public static String getDeviceIdByMacAddress(Context context) {
        byte[] macAddress;

        if (NetworkUtils.isWifiEnabled(context) == false) {
            NetworkUtils.enableWifiNetwork(context);
        }

        macAddress = NetworkUtils.getHexDecimalMacAddress(context);
        return SHA1(macAddress);
    }

    public static String getUuidFromMacAddress(Context context, String address) {
        byte[] macAddress;

        if (NetworkUtils.isWifiEnabled(context) == false) {
            NetworkUtils.enableWifiNetwork(context);
        }

        macAddress = NetworkUtils.getHexDecimalAddress(context, address);
        return SHA1(macAddress);
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfByte = (data[i] >>> 4) & 0x0F;
            int twoHalfs = 0;
            do {
                if ((0 <= halfByte) && (halfByte <= 9))
                    buf.append((char) ('0' + twoHalfs));
                else
                    buf.append((char) ('a' + (halfByte - 10)));
                halfByte = data[i] & 0x0F;
            } while(twoHalfs++ < 1);
        }
        return buf.toString();
    }

    private static String SHA1(String text) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance(StringUtils.SHA1_ALGORITHM);
            byte[] sha1hash = new byte[40];
            md.update(text.getBytes(StringUtils.ISO_8859_1_CHAR_SET), 0, text.length());
            sha1hash = md.digest();

            return convertToHex(sha1hash);
        } catch (Exception e) {
            Log.i(TAG, "Error generating generating SHA-1: ", e);
            return null;
        }
    }
    private static String SHA1(byte[] bytes) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance(StringUtils.SHA1_ALGORITHM);
            byte[] sha1hash = new byte[40];
            md.update(bytes, 0, bytes.length);
            sha1hash = md.digest();

            return convertToHex(sha1hash);
        } catch (Exception e) {
            Log.i(TAG, "Error generating generating SHA-1: ", e);
            return null;
        }
    }
    /** Returns the consumer friendly device name */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        if (manufacturer.equalsIgnoreCase("HTC")) {
            // make sure "HTC" is fully capitalized.
            return "HTC " + model;
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (StringUtils.isEmptyString(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase += Character.toUpperCase(c);
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase += c;
        }
        return phrase;
    }
}

