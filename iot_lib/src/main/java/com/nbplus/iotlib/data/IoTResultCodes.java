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
 * Created by basagee on 2015. 10. 13..
 */
// device list option
public enum IoTResultCodes {
    SUCCESS (0),
    // app check
    BIND_SERVICE_FAILED(100),
    IOT_APP_NOT_INSTALLED(101),

    // service process crashed.
    SERVICE_DISCONNECTED(102),

    // internal bluetooth error
    BLE_NOT_SUPPORTED (1000),
    BLUETOOTH_NOT_SUPPORTED (1001),
    BLUETOOTH_NOT_ENABLED (1002),
    // request argument ...
    INVALID_REQUEST_ARGUMENTS (1003)
    ;

    private final int value;

    private IoTResultCodes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
