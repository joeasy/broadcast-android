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

package com.nbplus.iotapp.perferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.basdroid.common.StringUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by basagee on 2015. 10. 13..
 * Base 만들고... 하면좋겠지만 중요하지도 않으니....
 */
public class IoTServicePreference {

    private static final String LENGTH = "#LENGTH";
    private static SharedPreferences mPreferences;
    private Gson mGson;

    public static final String KEY_USE_IOT_GATEWAY = "KEY_USE_IOT_GATEWAY";
    public static final String KEY_IOT_DEVICES_LIST = "KEY_IOT_DEVICES_LIST";
    public static final String KEY_IOT_DEVICES_SCENARIOS = "KEY_IOT_DEVICES_SCENARIOS";
    public static final String KEY_IOT_UNSENT_DEVICE_DATA = "KEY_IOT_UNSENT_DEVICE_DATA";

    // 값 불러오기
    private static SharedPreferences getPreferences(Context context) {
        if (mPreferences == null) {
            String prefName = context.getApplicationContext().getPackageName() + "." + IoTServicePreference.class.getSimpleName();
            mPreferences = context.getSharedPreferences(prefName, context.MODE_PRIVATE);
        }

        return mPreferences;
    }

    /**
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * an int.
     * @see SharedPreferences#getInt(String, int)
     */
    private static int getInt(Context context, final String key, final int defValue) {
        return getPreferences(context).getInt(key, defValue);
    }

    /**
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a boolean.
     * @see SharedPreferences#getBoolean(String, boolean)
     */
    private static boolean getBoolean(Context context, final String key, final boolean defValue) {
        return getPreferences(context).getBoolean(key, defValue);
    }

    /**
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a long.
     * @see SharedPreferences#getLong(String, long)
     */
    private static long getLong(Context context, final String key, final long defValue) {
        return getPreferences(context).getLong(key, defValue);
    }

    /**
     * Returns the double that has been saved as a long raw bits value in the long preferences.
     *
     * @param key      The name of the preference to retrieve.
     * @param defValue the double Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a long.
     * @see SharedPreferences#getLong(String, long)
     */
    private static double getDouble(Context context, final String key, final double defValue) {
        return Double.longBitsToDouble(getPreferences(context).getLong(key, Double.doubleToLongBits(defValue)));
    }

    /**
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a float.
     * @see SharedPreferences#getFloat(String, float)
     */
    private static float getFloat(Context context, final String key, final float defValue) {
        return getPreferences(context).getFloat(key, defValue);
    }

    /**
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference value if it exists, or defValue.  Throws
     * ClassCastException if there is a preference with this name that is not
     * a String.
     * @see SharedPreferences#getString(String, String)
     */
    private static String getString(Context context, final String key, final String defValue) {
        return getPreferences(context).getString(key, defValue);
    }

    /**
     * @param key      The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return Returns the preference values if they exist, or defValues.
     * Throws ClassCastException if there is a preference with this name
     * that is not a Set.
     * @see SharedPreferences#getStringSet(String, Set)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static Set<String> getStringSet(Context context, final String key, final Set<String> defValue) {
        SharedPreferences prefs = getPreferences(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return prefs.getStringSet(key, defValue);
        } else {
            if (prefs.contains(key + LENGTH)) {
                HashSet<String> set = new HashSet<>();
                // Workaround for pre-HC's lack of StringSets
                int stringSetLength = prefs.getInt(key + LENGTH, -1);
                if (stringSetLength >= 0) {
                    for (int i = 0; i < stringSetLength; i++) {
                        prefs.getString(key + "[" + i + "]", null);
                    }
                }
                return set;
            }
        }
        return defValue;
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see SharedPreferences.Editor#putLong(String, long)
     */
    private static void putLong(Context context, final String key, final long value) {
        final SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putLong(key, value);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see SharedPreferences.Editor#putInt(String, int)
     */
    private static void putInt(Context context, final String key, final int value) {
        final SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt(key, value);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    /**
     * Saves the double as a long raw bits inside the preferences.
     *
     * @param key   The name of the preference to modify.
     * @param value The double value to be save in the preferences.
     * @see SharedPreferences.Editor#putLong(String, long)
     */
    private static void putDouble(Context context, final String key, final double value) {
        final SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putLong(key, Double.doubleToRawLongBits(value));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see SharedPreferences.Editor#putFloat(String, float)
     */
    private static void putFloat(Context context, final String key, final float value) {
        final SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putFloat(key, value);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see SharedPreferences.Editor#putBoolean(String, boolean)
     */
    private static void putBoolean(Context context, final String key, final boolean value) {
        final SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(key, value);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see SharedPreferences.Editor#putString(String, String)
     */
    private static void putString(Context context, final String key, final String value) {
        final SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(key, value);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @see SharedPreferences.Editor#putStringSet(String, Set)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void putStringSet(Context context, final String key, final Set<String> value) {
        final SharedPreferences.Editor editor = getPreferences(context).edit();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            editor.putStringSet(key, value);
        } else {
            // Workaround for pre-HC's lack of StringSets
            int stringSetLength = 0;
            if (getPreferences(context).contains(key + LENGTH)) {
                // First read what the value was
                stringSetLength = getPreferences(context).getInt(key + LENGTH, -1);
            }
            editor.putInt(key + LENGTH, value.size());
            int i = 0;
            for (String aValue : value) {
                editor.putString(key + "[" + i + "]", aValue);
                i++;
            }
            for (; i < stringSetLength; i++) {
                // Remove any remaining values
                editor.remove(key + "[" + i + "]");
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    private void putJsonObject(Context context, String key, Object obj) {
        if (obj == null) {
            getPreferences(context).edit().putString(key, "").apply();
        } else {
            if (mGson == null) {
                mGson = new GsonBuilder().create();
            }
            getPreferences(context).edit().putString(key, mGson.toJson(obj)).apply();
        }
    }

    private Object getJsonObject(Context context, String key, Type tokenType) {
        Object obj = null;
        String savedValue = getPreferences(context).getString(key, "");
        if (StringUtils.isEmptyString(savedValue)) {
            obj = null;
        } else {
            if (mGson == null) {
                mGson = new GsonBuilder().create();
            }
            obj = mGson.fromJson(savedValue, tokenType);
        }

        return obj;
    }

    // 값(Key Data) 삭제하기
    public static void remove(Context context, String key) {
        SharedPreferences pref = getPreferences(context);
        if (pref != null && !StringUtils.isEmptyString(key)) {
            SharedPreferences.Editor editor = pref.edit();
            editor.remove(key);
            editor.commit();
        }
    }

    // 값(ALL Data) 삭제하기
    public static void clear(Context context){
        SharedPreferences pref = getPreferences(context);
        if (pref != null) {
            SharedPreferences.Editor editor = pref.edit();
            editor.clear();
            editor.commit();
        }
    }

    /**
     * Public methods for IoT settings...
     *
     */
    public static boolean isUseIoTGateway(Context context) {
        return getBoolean(context, KEY_USE_IOT_GATEWAY, true);
    }

    public static void setIoTDevicesList (Context context, String json) {
        putString(context, KEY_IOT_DEVICES_LIST, json);
    }
    public static String getIoTDevicesList (Context context) {
        return getString(context, KEY_IOT_DEVICES_LIST, "");
    }
    public static String getIoTDeviceScenarioMap (Context context) {
        return getString(context, KEY_IOT_DEVICES_SCENARIOS, "");
    }
    public static void setIoTDeviceScenarioMap (Context context, String json) {
        putString(context, KEY_IOT_DEVICES_SCENARIOS, json);
    }
    public static String getUnSentCollectedData (Context context) {
        return getString(context, KEY_IOT_UNSENT_DEVICE_DATA, "");
    }
    public static void setUnSentCollectedData (Context context, String json) {
        putString(context, KEY_IOT_UNSENT_DEVICE_DATA, json);
    }
}
