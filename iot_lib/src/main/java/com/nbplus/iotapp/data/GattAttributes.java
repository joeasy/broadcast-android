/*
 * Copyright (C) 2013 The Android Open Source Project
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
 */

package com.nbplus.iotapp.data;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    // client characteristic config...
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String GATT_BASE_UUID128_FROM_UUID16_FORMATTING = "0000%s-0000-1000-8000-00805f9b34fb";
    public static final String GATT_BASE_UUID128_FROM_UUID32_FORMATTING = "%s-0000-1000-8000-00805f9b34fb";

    private static HashMap<String, String> sServices = new HashMap();
    private static HashMap<String, String> sCharacteristics = new HashMap<String, String>();

    // xiaomi
    public static final String XIAOMI_MAC_ADDRESS_FILTER = "88:0F:10";
    public static final String MISCALE_CHARACTERISTIC_CONTROL_POINT = "00001542-0000-3512-2118-0009af100700";      //??
    public static final String MISCALE_CHARACTERISTIC_2A2F = "00002a2f-0000-3512-2118-0009af100700";
    public static final String MISCALE_WEIGHT_SERVICE_UUID = "00001530-0000-3512-2118-0009af100700";

    public static final String MIBAND_MILI_SERVICE_UUID = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "fee0");
    public static final String MIBAND_CHARACTERISTIC_DEVICE_INFO = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff01");
    public static final String MIBAND_CHARACTERISTIC_DEVICE_NAME = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff02");
    public static final String MIBAND_CHARACTERISTIC_NOTIFICATION = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff03");
    public static final String MIBAND_CHARACTERISTIC_USER_INFO = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff04");
    public static final String MIBAND_CHARACTERISTIC_CONTROL_POINT = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff05");
    public static final String MIBAND_CHARACTERISTIC_REALTIME_STEPS = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff06");
    public static final String MIBAND_CHARACTERISTIC_ACTIVITY_DATA = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff07");
    public static final String MIBAND_CHARACTERISTIC_FIRMWARE_DATA = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff08");
    public static final String MIBAND_CHARACTERISTIC_LE_PARAMS = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff09");
    public static final String MIBAND_CHARACTERISTIC_DATE_TIME = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff0a");
    public static final String MIBAND_CHARACTERISTIC_STATISTICS = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff0b");
    public static final String MIBAND_CHARACTERISTIC_BATTERY = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff0c");
    public static final String MIBAND_CHARACTERISTIC_TEST = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff0d");
    public static final String MIBAND_CHARACTERISTIC_SENSOR_DATA = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff0e");
    public static final String MIBAND_CHARACTERISTIC_PAIR = String.format(GATT_BASE_UUID128_FROM_UUID16_FORMATTING, "ff0f");

    public static final byte ALIAS_LEN = 0xa;
    public static final byte NOTIFY_AUTHENTICATION_FAILED = 0x6;
    public static final byte NOTIFY_AUTHENTICATION_SUCCESS = 0x5;
    public static final byte NOTIFY_CONN_PARAM_UPDATE_FAILED = 0x3;
    public static final byte NOTIFY_CONN_PARAM_UPDATE_SUCCESS = 0x4;
    public static final int NOTIFY_DEVICE_MALFUNCTION = 0xff;
    public static final byte NOTIFY_FIRMWARE_UPDATE_FAILED = 0x1;
    public static final byte NOTIFY_FIRMWARE_UPDATE_SUCCESS = 0x2;
    public static final byte NOTIFY_FITNESS_GOAL_ACHIEVED = 0x7;
    public static final byte NOTIFY_FW_CHECK_FAILED = 0xb;
    public static final byte NOTIFY_FW_CHECK_SUCCESS = 0xc;
    public static final byte NOTIFY_NORMAL = 0x0;
    public static final int NOTIFY_PAIR_CANCEL = 0xef;
    public static final byte NOTIFY_RESET_AUTHENTICATION_FAILED = 0x9;
    public static final byte NOTIFY_RESET_AUTHENTICATION_SUCCESS = 0xa;
    public static final byte NOTIFY_SET_LATENCY_SUCCESS = 0x8;
    public static final byte NOTIFY_STATUS_MOTOR_ALARM = 0x11;
    public static final byte NOTIFY_STATUS_MOTOR_AUTH = 0x13;
    public static final byte NOTIFY_STATUS_MOTOR_AUTH_SUCCESS = 0x15;
    public static final byte NOTIFY_STATUS_MOTOR_CALL = 0xe;
    public static final byte NOTIFY_STATUS_MOTOR_DISCONNECT = 0xf;
    public static final byte NOTIFY_STATUS_MOTOR_GOAL = 0x12;
    public static final byte NOTIFY_STATUS_MOTOR_NOTIFY = 0xd;
    public static final byte NOTIFY_STATUS_MOTOR_SHUTDOWN = 0x14;
    public static final byte NOTIFY_STATUS_MOTOR_SMART_ALARM = 0x10;
    public static final byte NOTIFY_STATUS_MOTOR_TEST = 0x16;
    public static final byte NOTIFY_UNKNOWN = -0x1;

    // for services
    public static final String GENERIC_ACCESS_SERVICE_UUID = "00001800-0000-1000-8000-00805f9b34fb";
    public static final String GENERIC_ATTRIBUTE_SERVICE_UUID = "00001801-0000-1000-8000-00805f9b34fb";
    public static final String IMMEDIATE_ALERT_SERVICE_UUID = "00001802-0000-1000-8000-00805f9b34fb";
    public static final String LINK_LOSS_SERVICE_UUID = "00001803-0000-1000-8000-00805f9b34fb";
    public static final String TX_POWER_SERVICE_UUID = "00001804-0000-1000-8000-00805f9b34fb";
    public static final String CURRENT_TIME_SERVICE_UUID = "00001805-0000-1000-8000-00805f9b34fb";
    public static final String REFERENCE_TIME_UPDATE_SERVICE_UUID = "00001806-0000-1000-8000-00805f9b34fb";
    public static final String NEXT_DST_CHANGE_SERVICE_UUID = "00001807-0000-1000-8000-00805f9b34fb";
    public static final String GLUCOSE_SERVICE_UUID = "00001808-0000-1000-8000-00805f9b34fb";
    public static final String HEALTH_THERMOMETER_SERVICE_UUID = "00001809-0000-1000-8000-00805f9b34fb";
    public static final String DEVICE_INFORMATION_SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb";
    public static final String HEART_RATE_SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb";
    public static final String PHONE_ALERT_STATUS_SERVICE_UUID = "0000180e-0000-1000-8000-00805f9b34fb";
    public static final String BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
    public static final String BLOOD_PRESSURE_SERVICE_UUID = "00001810-0000-1000-8000-00805f9b34fb";
    public static final String ALERT_NOTIFICATION_SERVICE_UUID = "00001811-0000-1000-8000-00805f9b34fb";
    public static final String HUMAN_INTERFACE_DEVICE_SERVICE_UUID = "00001812-0000-1000-8000-00805f9b34fb";
    public static final String SCAN_PARAMETERS_SERVICE_UUID = "00001813-0000-1000-8000-00805f9b34fb";
    public static final String RUNNING_SPEED_AND_CADENCE_SERVICE_UUID = "00001814-0000-1000-8000-00805f9b34fb";
    public static final String AUTOMATION_IO_SERVICE_UUID = "00001815-0000-1000-8000-00805f9b34fb";
    public static final String CYCLING_SPEED_AND_CADENCE_SERVICE_UUID = "00001816-0000-1000-8000-00805f9b34fb";
    public static final String CYCLING_POWER_SERVICE_UUID = "00001818-0000-1000-8000-00805f9b34fb";
    public static final String LOCATION_AND_NAVIGATION_SERVICE_UUID = "00001819-0000-1000-8000-00805f9b34fb";
    public static final String ENVIRONMENTAL_SENSING_SERVICE_UUID = "0000181a-0000-1000-8000-00805f9b34fb";
    public static final String BODY_COMPOSITION_SERVICE_UUID = "0000181b-0000-1000-8000-00805f9b34fb";
    public static final String USER_DATA_SERVICE_UUID = "0000181c-0000-1000-8000-00805f9b34fb";
    public static final String WEIGHT_SCALE_SERVICE_UUID = "0000181d-0000-1000-8000-00805f9b34fb";
    public static final String BOND_MANAGEMENT_SERVICE_UUID = "0000181e-0000-1000-8000-00805f9b34fb";
    public static final String CONTINUOUS_GLUCOSE_MONITORING_SERVICE_UUID = "0000181f-0000-1000-8000-00805f9b34fb";
    public static final String INTERNET_PROTOCOL_SUPPORT_SERVICE_UUID = "00001820-0000-1000-8000-00805f9b34fb";
    public static final String INDOOR_POSITIONING_SERVICE_UUID = "00001821-0000-1000-8000-00805f9b34fb";
    public static final String PULSE_OXIMETER_SERVICE_UUID = "00001822-0000-1000-8000-00805f9b34fb";

    // for characteristics
    public static final String AEROBIC_HEART_RATE_LOWER_LIMIT = "00002a7e-0000-1000-8000-00805f9b34fb";
    public static final String AEROBIC_HEART_RATE_UPPER_LIMIT = "00002a84-0000-1000-8000-00805f9b34fb";
    public static final String AEROBIC_THRESHOLD = "00002a7f-0000-1000-8000-00805f9b34fb";
    public static final String AGE = "00002a80-0000-1000-8000-00805f9b34fb";
    public static final String AGGREGATE = "00002a5a-0000-1000-8000-00805f9b34fb";
    public static final String ALERT_CATEGORY_ID = "00002a43-0000-1000-8000-00805f9b34fb";
    public static final String ALERT_CATEGORY_ID_BIT_MASK = "00002a42-0000-1000-8000-00805f9b34fb";
    public static final String ALERT_LEVEL = "00002a06-0000-1000-8000-00805f9b34fb";
    public static final String ALERT_NOTIFICATIION_CONTROL_POINT = "00002a44-0000-1000-8000-00805f9b34fb";
    public static final String ALERT_STATUS = "00002a3f-0000-1000-8000-00805f9b34fb";
    public static final String ALTITUDE = "00002ab3-0000-1000-8000-00805f9b34fb";
    public static final String ANAEROBIC_HEART_RATE_LOWER_LIMIT = "00002a81-0000-1000-8000-00805f9b34fb";
    public static final String ANAEROBIC_HEART_RATE_UPPER_LIMIT = "00002a82-0000-1000-8000-00805f9b34fb";
    public static final String ANAEROBIC_THRESHOLD = "00002a83-0000-1000-8000-00805f9b34fb";
    public static final String ANALOG = "00002a58-0000-1000-8000-00805f9b34fb";
    public static final String APPARENT_WIND_DIRECTION = "00002a73-0000-1000-8000-00805f9b34fb";
    public static final String APPARENT_WIND_SPEED = "00002a72-0000-1000-8000-00805f9b34fb";
    public static final String APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb";
    public static final String BAROMETRIC_PRESSURE_TREND = "00002aa3-0000-1000-8000-00805f9b34fb";
    public static final String BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";
    public static final String BLOOD_PRESSURE_FEATURE = "00002a49-0000-1000-8000-00805f9b34fb";
    public static final String BLOOD_PRESSURE_MEASUREMENT = "00002a35-0000-1000-8000-00805f9b34fb";
    public static final String BODY_COMPOSITION_FEATURE = "00002a9b-0000-1000-8000-00805f9b34fb";
    public static final String BODY_COMPOSITION_MEASUREMENT = "00002a9c-0000-1000-8000-00805f9b34fb";
    public static final String BODY_SENSOR_LOCATION = "00002a38-0000-1000-8000-00805f9b34fb";
    public static final String BOND_MANAGEMENT_CONTROL_POINT = "00002aa4-0000-1000-8000-00805f9b34fb";
    public static final String BOND_MANAGEMENT_FEATURE = "00002aa5-0000-1000-8000-00805f9b34fb";
    public static final String BOOT_KEYBOARD_INPUT_REPORT = "00002a22-0000-1000-8000-00805f9b34fb";
    public static final String BOOT_KEYBOARD_OUTPUT_REPORT = "00002a32-0000-1000-8000-00805f9b34fb";
    public static final String BOOT_MOUSE_INPUT_REPORT = "00002a33-0000-1000-8000-00805f9b34fb";
    public static final String CENTRAL_ADDRESS_RESOLUTION = "00002aa6-0000-1000-8000-00805f9b34fb";
    public static final String CGM_FEATURE = "00002aa8-0000-1000-8000-00805f9b34fb";
    public static final String CGM_MEASUREMENT = "00002aa7-0000-1000-8000-00805f9b34fb";
    public static final String CGM_SESSION_RUN_TIME = "00002aab-0000-1000-8000-00805f9b34fb";
    public static final String CGM_SESSION_START_TIME = "00002aaa-0000-1000-8000-00805f9b34fb";
    public static final String CGM_SPECIFIC_OPS_CONTROL_POINT = "00002aac-0000-1000-8000-00805f9b34fb";
    public static final String CGM_STATUS = "00002aa9-0000-1000-8000-00805f9b34fb";
    public static final String CSC_FEATURE = "00002a5c-0000-1000-8000-00805f9b34fb";
    public static final String CSC_MEASUREMENT = "00002a5b-0000-1000-8000-00805f9b34fb";
    public static final String CURRENT_TIME = "00002a2b-0000-1000-8000-00805f9b34fb";
    public static final String CYCLING_POWER_CONTROL_POINT = "00002a66-0000-1000-8000-00805f9b34fb";
    public static final String CYCLING_POWER_FEATURE = "00002a65-0000-1000-8000-00805f9b34fb";
    public static final String CYCLING_POWER_MEASUREMENT = "00002a63-0000-1000-8000-00805f9b34fb";
    public static final String CYCLING_POWER_VECTOR = "00002a64-0000-1000-8000-00805f9b34fb";
    public static final String DATABASE_CHANGE_INCREMENT = "00002a99-0000-1000-8000-00805f9b34fb";
    public static final String DATE_OF_BIRTH = "00002a85-0000-1000-8000-00805f9b34fb";
    public static final String DATE_OF_THRESHOLD_ASSESSMENT = "00002a86-0000-1000-8000-00805f9b34fb";
    public static final String DATE_TIME = "00002a08-0000-1000-8000-00805f9b34fb";
    public static final String DAY_DATE_TIME = "00002a0a-0000-1000-8000-00805f9b34fb";
    public static final String DAY_OF_WEEK = "00002a09-0000-1000-8000-00805f9b34fb";
    public static final String DESCRIPTOR_VALUE_CHANGED = "00002a7d-0000-1000-8000-00805f9b34fb";
    public static final String DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    public static final String DEW_POINT = "00002a7b-0000-1000-8000-00805f9b34fb";
    public static final String DIGITAL = "00002a56-0000-1000-8000-00805f9b34fb";
    public static final String DST_OFFSET = "00002a0d-0000-1000-8000-00805f9b34fb";
    public static final String ELEVATION = "00002a6c-0000-1000-8000-00805f9b34fb";
    public static final String EMAIL_ADDRESS = "00002a87-0000-1000-8000-00805f9b34fb";
    public static final String EXACT_TIME_256 = "00002a0c-0000-1000-8000-00805f9b34fb";
    public static final String FAT_BURN_HEART_RATE_LOWER_LIMIT = "00002a88-0000-1000-8000-00805f9b34fb";
    public static final String FAT_BURN_HEART_RATE_UPPER_LIMIT = "00002a89-0000-1000-8000-00805f9b34fb";
    public static final String FIRMWARE_REVISION_STRING = "00002a26-0000-1000-8000-00805f9b34fb";
    public static final String FIRST_NAME = "00002a8a-0000-1000-8000-00805f9b34fb";
    public static final String FIVE_ZONE_HEART_RATE_LIMITS = "00002a8b-0000-1000-8000-00805f9b34fb";
    public static final String FLOOR_NUMBER = "00002ab2-0000-1000-8000-00805f9b34fb";
    public static final String GENDER = "00002a8c-0000-1000-8000-00805f9b34fb";
    public static final String GLUCOSE_FEATURE = "00002a51-0000-1000-8000-00805f9b34fb";
    public static final String GLUCOSE_MEASUREMENT = "00002a18-0000-1000-8000-00805f9b34fb";
    public static final String GLUCOSE_MEASUREMENT_CONTEXT = "00002a34-0000-1000-8000-00805f9b34fb";
    public static final String GUST_FACTOR = "00002a74-0000-1000-8000-00805f9b34fb";
    public static final String HARDWARE_REVISION_STRING = "00002a27-0000-1000-8000-00805f9b34fb";
    public static final String HEART_RATE_CONTROL_POINT = "00002a39-0000-1000-8000-00805f9b34fb";
    public static final String HEART_RATE_MAX = "00002a8d-0000-1000-8000-00805f9b34fb";
    public static final String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static final String HEAT_INDEX = "00002a7a-0000-1000-8000-00805f9b34fb";
    public static final String HEIGHT = "00002a8e-0000-1000-8000-00805f9b34fb";
    public static final String HID_CONTROL_POINT = "00002a4c-0000-1000-8000-00805f9b34fb";
    public static final String HID_INFORMATION = "00002a4a-0000-1000-8000-00805f9b34fb";
    public static final String HIP_CIRCUMFERENCE = "00002a8f-0000-1000-8000-00805f9b34fb";
    public static final String HUMIDITY = "00002a6f-0000-1000-8000-00805f9b34fb";
    public static final String IEEE_11073_20601_REGULATORY_CERT_DATA_LIST = "00002a2a-0000-1000-8000-00805f9b34fb";
    public static final String INDOOR_POSITIONING_CONFIGURATION = "00002aad-0000-1000-8000-00805f9b34fb";
    public static final String INTERMEDIATE_CUFF_PRESSURE = "00002a36-0000-1000-8000-00805f9b34fb";
    public static final String INTERMEDIATE_TEMPERATURE = "00002a1e-0000-1000-8000-00805f9b34fb";
    public static final String IRRADIANCE = "00002a77-0000-1000-8000-00805f9b34fb";
    public static final String LANGUAGE = "00002aa2-0000-1000-8000-00805f9b34fb";
    public static final String LAST_NAME = "00002a90-0000-1000-8000-00805f9b34fb";
    public static final String LATITUDE = "00002aae-0000-1000-8000-00805f9b34fb";
    public static final String LN_CONTROL_POINT = "00002a6b-0000-1000-8000-00805f9b34fb";
    public static final String LN_FEATURE = "00002a6a-0000-1000-8000-00805f9b34fb";
    public static final String LOCAL_EAST_COORDINATE = "00002ab1-0000-1000-8000-00805f9b34fb";
    public static final String LOCAL_NORTH_COORDINATE = "00002ab0-0000-1000-8000-00805f9b34fb";
    public static final String LOCAL_TIME_INFORMATION = "00002a0f-0000-1000-8000-00805f9b34fb";
    public static final String LOCATION_AND_SPEED = "00002a67-0000-1000-8000-00805f9b34fb";
    public static final String LOCATION_NAME = "00002ab5-0000-1000-8000-00805f9b34fb";
    public static final String LONGITUDE = "00002aaf-0000-1000-8000-00805f9b34fb";
    public static final String MAGNETIC_DECLINATION = "00002a2c-0000-1000-8000-00805f9b34fb";
    public static final String MAGNETIC_FLUX_DENSITY_2D = "00002aa0-0000-1000-8000-00805f9b34fb";
    public static final String MAGNETIC_FLUX_DENSITY_3D = "00002aa1-0000-1000-8000-00805f9b34fb";
    public static final String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    public static final String MAXIMUM_RECOMMENDED_HEART_RATE = "00002a91-0000-1000-8000-00805f9b34fb";
    public static final String MEASUREMENT_INTERVAL = "00002a21-0000-1000-8000-00805f9b34fb";
    public static final String MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
    public static final String NAVIGATION = "00002a68-0000-1000-8000-00805f9b34fb";
    public static final String NEW_ALERT = "00002a46-0000-1000-8000-00805f9b34fb";
    public static final String PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb";
    public static final String PERIPHERAL_PRIVACY_FLAG = "00002a02-0000-1000-8000-00805f9b34fb";
    public static final String PLX_CONTINUOUS_MEASUREMENT = "00002a5f-0000-1000-8000-00805f9b34fb";
    public static final String PLX_FEATURES = "00002a60-0000-1000-8000-00805f9b34fb";
    public static final String PLX_SPOT_CHECK_MEASUREMENT = "00002a5e-0000-1000-8000-00805f9b34fb";
    public static final String PNP_ID = "00002a50-0000-1000-8000-00805f9b34fb";
    public static final String POLLEN_CONCENTRATION = "00002a75-0000-1000-8000-00805f9b34fb";
    public static final String POSITION_QUALITY = "00002a69-0000-1000-8000-00805f9b34fb";
    public static final String PRESSURE = "00002a6d-0000-1000-8000-00805f9b34fb";
    public static final String PROTOCOL_MODE = "00002a4e-0000-1000-8000-00805f9b34fb";
    public static final String RAINFALL = "00002a78-0000-1000-8000-00805f9b34fb";
    public static final String RECONNECTION_ADDRESS = "00002a03-0000-1000-8000-00805f9b34fb";
    public static final String RECORD_ACCESS_CONTROL_POINT = "00002a52-0000-1000-8000-00805f9b34fb";
    public static final String REFERENCE_TIME_INFORMATION = "00002a14-0000-1000-8000-00805f9b34fb";
    public static final String REPORT = "00002a4d-0000-1000-8000-00805f9b34fb";
    public static final String REPORT_MAP = "00002a4b-0000-1000-8000-00805f9b34fb";
    public static final String RESTING_HEART_RATE = "00002a92-0000-1000-8000-00805f9b34fb";
    public static final String RINGER_CONTROL_POINT = "00002a40-0000-1000-8000-00805f9b34fb";
    public static final String RINGER_SETTING = "00002a41-0000-1000-8000-00805f9b34fb";
    public static final String RSC_FEATURE = "00002a54-0000-1000-8000-00805f9b34fb";
    public static final String RSC_MEASUREMENT = "00002a53-0000-1000-8000-00805f9b34fb";
    public static final String SC_CONTROL_POINT = "00002a55-0000-1000-8000-00805f9b34fb";
    public static final String SCAN_INTERVAL_WINDOW = "00002a4f-0000-1000-8000-00805f9b34fb";
    public static final String SCAN_REFRESH = "00002a31-0000-1000-8000-00805f9b34fb";
    public static final String SENSOR_LOCATION = "00002a5d-0000-1000-8000-00805f9b34fb";
    public static final String SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
    public static final String SERVICE_CHANGED = "00002a05-0000-1000-8000-00805f9b34fb";
    public static final String SOFTWARE_REVISION_STRING = "00002a28-0000-1000-8000-00805f9b34fb";
    public static final String SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS = "00002a93-0000-1000-8000-00805f9b34fb";
    public static final String SUPPORTED_NEW_ALERT_CATEGORY = "00002a47-0000-1000-8000-00805f9b34fb";
    public static final String SUPPORTED_UNREAD_ALERT_CATEGORY = "00002a48-0000-1000-8000-00805f9b34fb";
    public static final String SYSTEM_ID = "00002a23-0000-1000-8000-00805f9b34fb";
    public static final String TEMPERATURE = "00002a6e-0000-1000-8000-00805f9b34fb";
    public static final String TEMPERATURE_MEASUREMENT = "00002a1c-0000-1000-8000-00805f9b34fb";
    public static final String TEMPERATURE_TYPE = "00002a1d-0000-1000-8000-00805f9b34fb";
    public static final String THREE_ZONE_HEART_RATE_LIMITS = "00002a94-0000-1000-8000-00805f9b34fb";
    public static final String TIME_ACCURACY = "00002a12-0000-1000-8000-00805f9b34fb";
    public static final String TIME_SOURCE = "00002a13-0000-1000-8000-00805f9b34fb";
    public static final String TIME_UPDATE_CONTROL_POINT = "00002a16-0000-1000-8000-00805f9b34fb";
    public static final String TIME_UPDATE_STATE = "00002a17-0000-1000-8000-00805f9b34fb";
    public static final String TIME_WITH_DST = "00002a11-0000-1000-8000-00805f9b34fb";
    public static final String TIME_ZONE = "00002a0e-0000-1000-8000-00805f9b34fb";
    public static final String TRUE_WIND_DIRECTION = "00002a71-0000-1000-8000-00805f9b34fb";
    public static final String TRUE_WIND_SPEED = "00002a70-0000-1000-8000-00805f9b34fb";
    public static final String TWO_ZONE_HEART_RATE_LIMIT = "00002a95-0000-1000-8000-00805f9b34fb";
    public static final String TX_POWER_LEVEL = "00002a07-0000-1000-8000-00805f9b34fb";
    public static final String UNCERTAINTY = "00002ab4-0000-1000-8000-00805f9b34fb";
    public static final String UNREAD_ALERT_STATUS = "00002a45-0000-1000-8000-00805f9b34fb";
    public static final String USER_CONTROL_POINT = "00002a9f-0000-1000-8000-00805f9b34fb";
    public static final String USER_INDEX = "00002a9a-0000-1000-8000-00805f9b34fb";
    public static final String UV_INDEX = "00002a76-0000-1000-8000-00805f9b34fb";
    public static final String VO2_MAX = "00002a96-0000-1000-8000-00805f9b34fb";
    public static final String WAIST_MEASUREMENT = "00002a97-0000-1000-8000-00805f9b34fb";
    public static final String WEIGHT = "00002a98-0000-1000-8000-00805f9b34fb";
    public static final String WEIGHT_MEASUREMENT = "00002a9d-0000-1000-8000-00805f9b34fb";
    public static final String WEIGHT_SCALE_FEATURE = "00002a9e-0000-1000-8000-00805f9b34fb";
    public static final String WIND_CHILL = "00002a79-0000-1000-8000-00805f9b34fb";


    static {
        // Services.
        sServices.put(GENERIC_ACCESS_SERVICE_UUID, "Generic Access Service");
        sServices.put(GENERIC_ATTRIBUTE_SERVICE_UUID, "Generic Attribute Service");
        sServices.put(IMMEDIATE_ALERT_SERVICE_UUID, "Immediate Alert Service");
        sServices.put(LINK_LOSS_SERVICE_UUID, "Link Loss Service");
        sServices.put(TX_POWER_SERVICE_UUID, "Tx Power Service");
        sServices.put(CURRENT_TIME_SERVICE_UUID, "Current Time Service");
        sServices.put(REFERENCE_TIME_UPDATE_SERVICE_UUID, "Reference Time Update Service");
        sServices.put(NEXT_DST_CHANGE_SERVICE_UUID, "Next DST Change Service");
        sServices.put(GLUCOSE_SERVICE_UUID, "Glucose Service");
        sServices.put(HEALTH_THERMOMETER_SERVICE_UUID, "Health Thermometer Service");
        sServices.put(DEVICE_INFORMATION_SERVICE_UUID, "Device Information Service");
        sServices.put(HEART_RATE_SERVICE_UUID, "Heart Rate Service");
        sServices.put(PHONE_ALERT_STATUS_SERVICE_UUID, "Phone Alert Status Service");
        sServices.put(BATTERY_SERVICE_UUID, "Battery Service");
        sServices.put(BLOOD_PRESSURE_SERVICE_UUID, "Blood Pressure Service");
        sServices.put(ALERT_NOTIFICATION_SERVICE_UUID, "Alert Notification Service");
        sServices.put(HUMAN_INTERFACE_DEVICE_SERVICE_UUID, "Human Interface Device Service");
        sServices.put(SCAN_PARAMETERS_SERVICE_UUID, "Scan Parameter Service");
        sServices.put(RUNNING_SPEED_AND_CADENCE_SERVICE_UUID, "Running Speed And Cadence Service");
        sServices.put(AUTOMATION_IO_SERVICE_UUID, "Automation IO Service");
        sServices.put(CYCLING_SPEED_AND_CADENCE_SERVICE_UUID, "Cycling Speed And Cadence Service");
        sServices.put(CYCLING_POWER_SERVICE_UUID, "Cycling Power Service");
        sServices.put(LOCATION_AND_NAVIGATION_SERVICE_UUID, "Location And Navigation Service");
        sServices.put(ENVIRONMENTAL_SENSING_SERVICE_UUID, "Environmental Sensing Service");
        sServices.put(BODY_COMPOSITION_SERVICE_UUID, "Body Composition Service");
        sServices.put(USER_DATA_SERVICE_UUID, "User Data Service");
        sServices.put(WEIGHT_SCALE_SERVICE_UUID, "Weight Scale Service");
        sServices.put(BOND_MANAGEMENT_SERVICE_UUID, "Bond Management Service");
        sServices.put(CONTINUOUS_GLUCOSE_MONITORING_SERVICE_UUID, "Continuous Glucose Monitoring Service");
        sServices.put(INTERNET_PROTOCOL_SUPPORT_SERVICE_UUID, "Internet Protocol Support Service");
        sServices.put(INDOOR_POSITIONING_SERVICE_UUID, "Indoor Positioning Service");
        sServices.put(PULSE_OXIMETER_SERVICE_UUID, "Pulse Oximeter Service");

        sServices.put(MISCALE_WEIGHT_SERVICE_UUID, "XIAOMI Weight Service");
        sServices.put(MIBAND_MILI_SERVICE_UUID, "XIAOMI Band Mili Service");

        // for characteristics
        sCharacteristics.put(AEROBIC_HEART_RATE_LOWER_LIMIT, "AEROBIC_HEART_RATE_LOWER_LIMIT Characteristic");
        sCharacteristics.put(AEROBIC_HEART_RATE_UPPER_LIMIT, "AEROBIC_HEART_RATE_UPPER_LIMIT Characteristic");
        sCharacteristics.put(AEROBIC_THRESHOLD, "AEROBIC_THRESHOLD Characteristic");
        sCharacteristics.put(AGE, "AGE Characteristic");
        sCharacteristics.put(AGGREGATE, "AGGREGATE Characteristic");
        sCharacteristics.put(ALERT_CATEGORY_ID, "ALERT_CATEGORY_ID Characteristic");
        sCharacteristics.put(ALERT_CATEGORY_ID_BIT_MASK, "ALERT_CATEGORY_ID_BIT_MASK Characteristic");
        sCharacteristics.put(ALERT_LEVEL, "ALERT_LEVEL Characteristic");
        sCharacteristics.put(ALERT_NOTIFICATIION_CONTROL_POINT, "ALERT_NOTIFICATIION_CONTROL_POINT Characteristic");
        sCharacteristics.put(ALERT_STATUS, "ALERT_STATUS Characteristic");
        sCharacteristics.put(ALTITUDE, "ALTITUDE Characteristic");
        sCharacteristics.put(ANAEROBIC_HEART_RATE_LOWER_LIMIT, "ANAEROBIC_HEART_RATE_LOWER_LIMIT Characteristic");
        sCharacteristics.put(ANAEROBIC_HEART_RATE_UPPER_LIMIT, "ANAEROBIC_HEART_RATE_UPPER_LIMIT Characteristic");
        sCharacteristics.put(ANAEROBIC_THRESHOLD, "ANAEROBIC_THRESHOLD Characteristic");
        sCharacteristics.put(ANALOG, "ANALOG Characteristic");
        sCharacteristics.put(APPARENT_WIND_DIRECTION, "APPARENT_WIND_DIRECTION Characteristic");
        sCharacteristics.put(APPARENT_WIND_SPEED, "APPARENT_WIND_SPEED Characteristic");
        sCharacteristics.put(APPEARANCE, "APPEARANCE Characteristic");
        sCharacteristics.put(BAROMETRIC_PRESSURE_TREND, "BAROMETRIC_PRESSURE_TREND Characteristic");
        sCharacteristics.put(BATTERY_LEVEL, "BATTERY_LEVEL Characteristic");
        sCharacteristics.put(BLOOD_PRESSURE_FEATURE, "BLOOD_PRESSURE_FEATURE Characteristic");
        sCharacteristics.put(BLOOD_PRESSURE_MEASUREMENT, "BLOOD_PRESSURE_MEASUREMENT Characteristic");
        sCharacteristics.put(BODY_COMPOSITION_FEATURE, "BODY_COMPOSITION_FEATURE Characteristic");
        sCharacteristics.put(BODY_COMPOSITION_MEASUREMENT, "BODY_COMPOSITION_MEASUREMENT Characteristic");
        sCharacteristics.put(BODY_SENSOR_LOCATION, "BODY_SENSOR_LOCATION Characteristic");
        sCharacteristics.put(BOND_MANAGEMENT_CONTROL_POINT, "BOND_MANAGEMENT_CONTROL_POINT Characteristic");
        sCharacteristics.put(BOND_MANAGEMENT_FEATURE, "BOND_MANAGEMENT_FEATURE Characteristic");
        sCharacteristics.put(BOOT_KEYBOARD_INPUT_REPORT, "BOOT_KEYBOARD_INPUT_REPORT Characteristic");
        sCharacteristics.put(BOOT_KEYBOARD_OUTPUT_REPORT, "BOOT_KEYBOARD_OUTPUT_REPORT Characteristic");
        sCharacteristics.put(BOOT_MOUSE_INPUT_REPORT, "BOOT_MOUSE_INPUT_REPORT Characteristic");
        sCharacteristics.put(CENTRAL_ADDRESS_RESOLUTION, "CENTRAL_ADDRESS_RESOLUTION Characteristic");
        sCharacteristics.put(CGM_FEATURE, "CGM_FEATURE Characteristic");
        sCharacteristics.put(CGM_MEASUREMENT, "CGM_MEASUREMENT Characteristic");
        sCharacteristics.put(CGM_SESSION_RUN_TIME, "CGM_SESSION_RUN_TIME Characteristic");
        sCharacteristics.put(CGM_SESSION_START_TIME, "CGM_SESSION_START_TIME Characteristic");
        sCharacteristics.put(CGM_SPECIFIC_OPS_CONTROL_POINT, "CGM_SPECIFIC_OPS_CONTROL_POINT Characteristic");
        sCharacteristics.put(CGM_STATUS, "CGM_STATUS Characteristic");
        sCharacteristics.put(CSC_FEATURE, "CSC_FEATURE Characteristic");
        sCharacteristics.put(CSC_MEASUREMENT, "CSC_MEASUREMENT Characteristic");
        sCharacteristics.put(CURRENT_TIME, "CURRENT_TIME Characteristic");
        sCharacteristics.put(CYCLING_POWER_CONTROL_POINT, "CYCLING_POWER_CONTROL_POINT Characteristic");
        sCharacteristics.put(CYCLING_POWER_FEATURE, "CYCLING_POWER_FEATURE Characteristic");
        sCharacteristics.put(CYCLING_POWER_MEASUREMENT, "CYCLING_POWER_MEASUREMENT Characteristic");
        sCharacteristics.put(CYCLING_POWER_VECTOR, "CYCLING_POWER_VECTOR Characteristic");
        sCharacteristics.put(DATABASE_CHANGE_INCREMENT, "DATABASE_CHANGE_INCREMENT Characteristic");
        sCharacteristics.put(DATE_OF_BIRTH, "DATE_OF_BIRTH Characteristic");
        sCharacteristics.put(DATE_OF_THRESHOLD_ASSESSMENT, "DATE_OF_THRESHOLD_ASSESSMENT Characteristic");
        sCharacteristics.put(DATE_TIME, "DATE_TIME Characteristic");
        sCharacteristics.put(DAY_DATE_TIME, "DAY_DATE_TIME Characteristic");
        sCharacteristics.put(DAY_OF_WEEK, "DAY_OF_WEEK Characteristic");
        sCharacteristics.put(DESCRIPTOR_VALUE_CHANGED, "DESCRIPTOR_VALUE_CHANGED Characteristic");
        sCharacteristics.put(DEVICE_NAME, "DEVICE_NAME Characteristic");
        sCharacteristics.put(DEW_POINT, "DEW_POINT Characteristic");
        sCharacteristics.put(DIGITAL, "DIGITAL Characteristic");
        sCharacteristics.put(DST_OFFSET, "DST_OFFSET Characteristic");
        sCharacteristics.put(ELEVATION, "ELEVATION Characteristic");
        sCharacteristics.put(EMAIL_ADDRESS, "EMAIL_ADDRESS Characteristic");
        sCharacteristics.put(EXACT_TIME_256, "EXACT_TIME_256 Characteristic");
        sCharacteristics.put(FAT_BURN_HEART_RATE_LOWER_LIMIT, "FAT_BURN_HEART_RATE_LOWER_LIMIT Characteristic");
        sCharacteristics.put(FAT_BURN_HEART_RATE_UPPER_LIMIT, "FAT_BURN_HEART_RATE_UPPER_LIMIT Characteristic");
        sCharacteristics.put(FIRMWARE_REVISION_STRING, "FIRMWARE_REVISION_STRING Characteristic");
        sCharacteristics.put(FIRST_NAME, "FIRST_NAME Characteristic");
        sCharacteristics.put(FIVE_ZONE_HEART_RATE_LIMITS, "FIVE_ZONE_HEART_RATE_LIMITS Characteristic");
        sCharacteristics.put(FLOOR_NUMBER, "FLOOR_NUMBER Characteristic");
        sCharacteristics.put(GENDER, "GENDER Characteristic");
        sCharacteristics.put(GLUCOSE_FEATURE, "GLUCOSE_FEATURE Characteristic");
        sCharacteristics.put(GLUCOSE_MEASUREMENT, "GLUCOSE_MEASUREMENT Characteristic");
        sCharacteristics.put(GLUCOSE_MEASUREMENT_CONTEXT, "CLUCOSE_MEASUREMENT_CONTEXT Characteristic");
        sCharacteristics.put(GUST_FACTOR, "GUST_FACTOR Characteristic");
        sCharacteristics.put(HARDWARE_REVISION_STRING, "HARDWARE_REVISION_STRING Characteristic");
        sCharacteristics.put(HEART_RATE_CONTROL_POINT, "HEART_RATE_CONTROL_POINT Characteristic");
        sCharacteristics.put(HEART_RATE_MAX, "HEART_RATE_MAX Characteristic");
        sCharacteristics.put(HEART_RATE_MEASUREMENT, "HEART_RATE_MEASUREMENT Characteristic");
        sCharacteristics.put(HEAT_INDEX, "HEAT_INDEX Characteristic");
        sCharacteristics.put(HEIGHT, "HEIGHT Characteristic");
        sCharacteristics.put(HID_CONTROL_POINT, "HID_CONTROL_POINT Characteristic");
        sCharacteristics.put(HID_INFORMATION, "HID_INFORMATION Characteristic");
        sCharacteristics.put(HIP_CIRCUMFERENCE, "HIP_CIRCUMFERENCE Characteristic");
        sCharacteristics.put(HUMIDITY, "HUMIDITY Characteristic");
        sCharacteristics.put(IEEE_11073_20601_REGULATORY_CERT_DATA_LIST, "IEEE_11073_20601_REGULATORY_CERT_DATA_LIST Characteristic");
        sCharacteristics.put(INDOOR_POSITIONING_CONFIGURATION, "INDOOR_POSITIONING_CONFIGURATION Characteristic");
        sCharacteristics.put(INTERMEDIATE_CUFF_PRESSURE, "INTERMEDIATE_CUFF_PRESSURE Characteristic");
        sCharacteristics.put(INTERMEDIATE_TEMPERATURE, "INTERMEDIATE_TEMPERATURE Characteristic");
        sCharacteristics.put(IRRADIANCE, "IRRADIANCE Characteristic");
        sCharacteristics.put(LANGUAGE, "LANGUAGE Characteristic");
        sCharacteristics.put(LAST_NAME, "LAST_NAME Characteristic");
        sCharacteristics.put(LATITUDE, "LATITUDE Characteristic");
        sCharacteristics.put(LN_CONTROL_POINT, "LN_CONTROL_POINT Characteristic");
        sCharacteristics.put(LN_FEATURE, "LN_FEATURE Characteristic");
        sCharacteristics.put(LOCAL_EAST_COORDINATE, "LOCAL_EAST_COORDINATE Characteristic");
        sCharacteristics.put(LOCAL_NORTH_COORDINATE, "LOCAL_NORTH_COORDINATE Characteristic");
        sCharacteristics.put(LOCAL_TIME_INFORMATION, "LOCAL_TIME_INFORMATION Characteristic");
        sCharacteristics.put(LOCATION_AND_SPEED, "LOCATION_AND_SPEED Characteristic");
        sCharacteristics.put(LOCATION_NAME, "LOCATION_NAME Characteristic");
        sCharacteristics.put(LONGITUDE, "LONGITUDE Characteristic");
        sCharacteristics.put(MAGNETIC_DECLINATION, "MAGNETIC_DECLINATION Characteristic");
        sCharacteristics.put(MAGNETIC_FLUX_DENSITY_2D, "MAGNETIC_FLUX_DENSITY_2D Characteristic");
        sCharacteristics.put(MAGNETIC_FLUX_DENSITY_3D, "MAGNETIC_FLUX_DENSITY_3D Characteristic");
        sCharacteristics.put(MANUFACTURER_NAME_STRING, "MANUFACTURER_NAME_STRING Characteristic");
        sCharacteristics.put(MAXIMUM_RECOMMENDED_HEART_RATE, "MAXIMUM_RECOMMENDED_HEART_RATE Characteristic");
        sCharacteristics.put(MEASUREMENT_INTERVAL, "MEASUREMENT_INTERVAL Characteristic");
        sCharacteristics.put(MODEL_NUMBER_STRING, "MODEL_NUMBER_STRING Characteristic");
        sCharacteristics.put(NAVIGATION, "NAVIGATION Characteristic");
        sCharacteristics.put(NEW_ALERT, "NEW_ALERT Characteristic");
        sCharacteristics.put(PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, "PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS Characteristic");
        sCharacteristics.put(PERIPHERAL_PRIVACY_FLAG, "PERIPHERAL_PRIVACY_FLAG Characteristic");
        sCharacteristics.put(PLX_CONTINUOUS_MEASUREMENT, "PLX_CONTINUOUS_MEASUREMENT Characteristic");
        sCharacteristics.put(PLX_FEATURES, "PLX_FEATURES Characteristic");
        sCharacteristics.put(PLX_SPOT_CHECK_MEASUREMENT, "PLX_SPOT_CHECK_MEASUREMENT Characteristic");
        sCharacteristics.put(PNP_ID, "PNP_ID Characteristic");
        sCharacteristics.put(POLLEN_CONCENTRATION, "POLLEN_CONCENTRATION Characteristic");
        sCharacteristics.put(POSITION_QUALITY, "POSITION_QUALITY Characteristic");
        sCharacteristics.put(PRESSURE, "PRESSURE Characteristic");
        sCharacteristics.put(PROTOCOL_MODE, "PROTOCOL_MODE Characteristic");
        sCharacteristics.put(RAINFALL, "RAINFALL Characteristic");
        sCharacteristics.put(RECONNECTION_ADDRESS, "RECONNECTION_ADDRESS Characteristic");
        sCharacteristics.put(RECORD_ACCESS_CONTROL_POINT, "RECORD_ACCESS_CONTROL_POINT Characteristic");
        sCharacteristics.put(REFERENCE_TIME_INFORMATION, "REFERENCE_TIME_INFORMATION Characteristic");
        sCharacteristics.put(REPORT, "REPORT Characteristic");
        sCharacteristics.put(REPORT_MAP, "REPORT_MAP Characteristic");
        sCharacteristics.put(RESTING_HEART_RATE, "RESTING_HEART_RATE Characteristic");
        sCharacteristics.put(RINGER_CONTROL_POINT, "RINGER_CONTROL_POINT Characteristic");
        sCharacteristics.put(RINGER_SETTING, "RINGER_SETTING Characteristic");
        sCharacteristics.put(RSC_FEATURE, "RSC_FEATURE Characteristic");
        sCharacteristics.put(RSC_MEASUREMENT, "RSC_MEASUREMENT Characteristic");
        sCharacteristics.put(SC_CONTROL_POINT, "SC_CONTROL_POINT Characteristic");
        sCharacteristics.put(SCAN_INTERVAL_WINDOW, "SCAN_INTERVAL_WINDOW Characteristic");
        sCharacteristics.put(SCAN_REFRESH, "SCAN_REFRESH Characteristic");
        sCharacteristics.put(SENSOR_LOCATION, "SENSOR_LOCATION Characteristic");
        sCharacteristics.put(SERIAL_NUMBER_STRING, "SERIAL_NUMBER_STRING Characteristic");
        sCharacteristics.put(SERVICE_CHANGED, "SERVICE_CHANGED Characteristic");
        sCharacteristics.put(SOFTWARE_REVISION_STRING, "SOFTWARE_REVISION_STRING Characteristic");
        sCharacteristics.put(SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS, "SPORT_TYPE_FOR_AEROBIC_AND_ANAEROBIC_THRESHOLDS Characteristic");
        sCharacteristics.put(SUPPORTED_NEW_ALERT_CATEGORY, "SUPPORTED_NEW_ALERT_CATEGORY Characteristic");
        sCharacteristics.put(SUPPORTED_UNREAD_ALERT_CATEGORY, "SUPPORTED_UNREAD_ALERT_CATEGORY Characteristic");
        sCharacteristics.put(SYSTEM_ID, "SYSTEM_ID Characteristic");
        sCharacteristics.put(TEMPERATURE, "TEMPERATURE Characteristic");
        sCharacteristics.put(TEMPERATURE_MEASUREMENT, "TEMPERATURE_MEASUREMENT Characteristic");
        sCharacteristics.put(TEMPERATURE_TYPE, "TEMPERATURE_TYPE Characteristic");
        sCharacteristics.put(THREE_ZONE_HEART_RATE_LIMITS, "THREE_ZONE_HEART_RATE_LIMITS Characteristic");
        sCharacteristics.put(TIME_ACCURACY, "TIME_ACCURACY Characteristic");
        sCharacteristics.put(TIME_SOURCE, "TIME_SOURCE Characteristic");
        sCharacteristics.put(TIME_UPDATE_CONTROL_POINT, "TIME_UPDATE_CONTROL_POINT Characteristic");
        sCharacteristics.put(TIME_UPDATE_STATE, "TIME_UPDATE_STATE Characteristic");
        sCharacteristics.put(TIME_WITH_DST, "TIME_WITH_DST Characteristic");
        sCharacteristics.put(TIME_ZONE, "TIME_ZONE Characteristic");
        sCharacteristics.put(TRUE_WIND_DIRECTION, "TRUE_WIND_DIRECTION Characteristic");
        sCharacteristics.put(TRUE_WIND_SPEED, "TRUE_WIND_SPEED Characteristic");
        sCharacteristics.put(TWO_ZONE_HEART_RATE_LIMIT, "TWO_ZONE_HEART_RATE_LIMIT Characteristic");
        sCharacteristics.put(TX_POWER_LEVEL, "TX_POWER_LEVEL Characteristic");
        sCharacteristics.put(UNCERTAINTY, "UNCERTAINTY Characteristic");
        sCharacteristics.put(UNREAD_ALERT_STATUS, "UNREAD_ALERT_STATUS Characteristic");
        sCharacteristics.put(USER_CONTROL_POINT, "USER_CONTROL_POINT Characteristic");
        sCharacteristics.put(USER_INDEX, "USER_INDEX Characteristic");
        sCharacteristics.put(UV_INDEX, "UV_INDEX Characteristic");
        sCharacteristics.put(VO2_MAX, "VO2_MAX Characteristic");
        sCharacteristics.put(WAIST_MEASUREMENT, "WAIST_MEASUREMENT Characteristic");
        sCharacteristics.put(WEIGHT, "WEIGHT Characteristic");
        sCharacteristics.put(WEIGHT_MEASUREMENT, "WEIGHT_MEASUREMENT Characteristic");
        sCharacteristics.put(WEIGHT_SCALE_FEATURE, "WEIGHT_SCALE_FEATURE Characteristic");
        sCharacteristics.put(WIND_CHILL, "WIND_CHILL Characteristic");

        sCharacteristics.put(MISCALE_CHARACTERISTIC_CONTROL_POINT, "XIAOMI Weight control point Characteristic");
        sCharacteristics.put(MISCALE_CHARACTERISTIC_2A2F, "XIAOMI Weight 2a2f Characteristic");

        sCharacteristics.put(MIBAND_CHARACTERISTIC_DEVICE_INFO, "MI Band Device Info Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_DEVICE_NAME, "MI Band Device Name Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_NOTIFICATION, "MI Band Notification Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_USER_INFO, "MI Band UserInfo Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_CONTROL_POINT, "MI Band Control Point Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_REALTIME_STEPS, "MI Band Realtime Steps Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_ACTIVITY_DATA, "MI Band Activity Data Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_FIRMWARE_DATA, "MI Band Firmware Data Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_LE_PARAMS, "MI Band Le Params Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_DATE_TIME, "MI Band Date Time Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_STATISTICS, "MI Band Statistics Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_BATTERY, "MI Band Battery Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_TEST, "MI Band Test Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_SENSOR_DATA, "MI Band Sensor Data Characteristic");
        sCharacteristics.put(MIBAND_CHARACTERISTIC_PAIR, "MI Band Pair Characteristic");
    }

    public static String lookupService(String uuid, String defaultName) {
        String name = sServices.get(uuid);
        return name == null ? defaultName : name;
    }
    public static String lookupCharacteristics(String uuid, String defaultName) {
        String name = sCharacteristics.get(uuid);
        return name == null ? defaultName : name;
    }

    public static boolean isBleHealthService(String uuid) {
        if (uuid == null) {
            return false;
        }

        if (uuid.equals(GLUCOSE_SERVICE_UUID) || uuid.equals(WEIGHT_SCALE_SERVICE_UUID) || uuid.equals(HEALTH_THERMOMETER_SERVICE_UUID)
                || uuid.equals(CONTINUOUS_GLUCOSE_MONITORING_SERVICE_UUID) || uuid.equals(HEART_RATE_SERVICE_UUID)
                || uuid.equals(BLOOD_PRESSURE_SERVICE_UUID) || uuid.equals(BODY_COMPOSITION_SERVICE_UUID)
                || uuid.equals(CYCLING_SPEED_AND_CADENCE_SERVICE_UUID) || uuid.equals(RUNNING_SPEED_AND_CADENCE_SERVICE_UUID)
                || uuid.equals(MIBAND_MILI_SERVICE_UUID)) {
            return true;
        }

        return false;
    }
}
