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

package com.nbplus.iotlib.data;

/**
 * Created by basagee on 2015. 10. 12..
 */
public class IoTConstants {
    public static final int IOT_GATEWAY_SERVER_PORT = 10089;
    public static final int IOT_GATEWAY_SERVER_THREAD_POOL_SIZE = 1;

    public static final boolean USE_ANOTHER_APP = false;
    public static final String NBPLUS_IOT_APP_PACKAGE_NAME = "com.nbplus.iotapp";

    public static final String ACTION_SERVICE_CREATE_BROADCAST = "com.nbplus.iot.ACTION_SERVICE_CREATE_BROADCAST";
    public static final String ACTION_RECEIVE_EMERGENCY_CALL_DEVICE_BROADCAST = "com.nbplus.iot.ACTION_RECEIVE_EMERGENCY_CALL_DEVICE_BROADCAST";
    public static final String ACTION_IOT_DATA_SYNC_COMPLETED = "com.nbplus.iot.ACTION_IOT_DATA_SYNC_COMPLETED";
    public static final String ACTION_IOT_SERVICE_STATUS_CHANGED = "com.nbplus.iot.ACTION_IOT_SERVICE_STATUS_CHANGED";

    public static final String EXTRA_SERVICE_STATUS = "EXTRA_SERVICE_STATUS";
}
