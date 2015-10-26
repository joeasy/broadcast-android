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

import android.content.Context;
import android.util.Log;

/**
 * 커맨드는 request, response 및 notification 으로 구성
 * 프로세스간 IPC는 Messenger를 이용한다.
 *
 * 전달되는 Message는
 * msg.what => command code
 * msg.replyTo => messenger 객체 (register에서만 전달된다. 서비스에서 messenger 객체를 관리한다.)
 *
 * msg.data => Bundle 값
 * | key | recommended | description |
 * |:---:|:---:|---|
 * |KEY_MSGID|M|현재 요청하는 메시지의 ID값( unique key = package name + system time |
 * |KEY_CMD|O|response 인 경우에만 포함된다.|
 * |KEY_RESULT|O|response 인 경우에만 포함된다.(serializable), IoTResultCodes 참조|
 * |KEY_SERVICE_STATUS|O|서비스 상태, IoTServiceStatus 참조|
 * |KEY_SERVICE_STATUS_CODE|O|서비스 상태 code , IoTResultCodes 참조|
 */
public class IoTServiceCommand {
    public static final int COMMAND_BASE_VALUE = 1000;

    // request
    public static final int REGISTER_SERVICE = COMMAND_BASE_VALUE + 1;
    public static final int UNREGISTER_SERVICE = REGISTER_SERVICE + 1;
    public static final int GET_DEVICE_LIST = UNREGISTER_SERVICE + 1;
    public static final int DEVICE_BONDING = GET_DEVICE_LIST + 1;
    public static final int DEVICE_UN_BONDING = DEVICE_BONDING + 1;
    public static final int CONTROL_DEVICE = DEVICE_UN_BONDING + 1;     // IR 등에대한 제어
    public static final int GET_DEVICE_DATA = CONTROL_DEVICE + 1;

    /**
     * from service to application.
     */
    /**
     * 디바이스리스트 갱신에 대한 notification
     */
    public static final int DEVICE_LIST_NOTIFICATION = GET_DEVICE_DATA + 1;
    /**
     * 어플리케이션에서 데이터조회 요청한 디바이스에 대해서
     * 시나리오에 따라 데이터 조회(notify/indicate, read, write) 등등의 조합으로 처리결과
     */
    public static final int DEVICE_DATA_NOTIFICATION = DEVICE_LIST_NOTIFICATION + 1;
    public static final int DEVICE_CONTROL_NOTIFICATION = DEVICE_DATA_NOTIFICATION + 1;

    // 위의 request 에 대한 결과를 전달하기 위함.
    // - message의 data 전달되는 데이터 형식
    // Bundle b = new Bundle(); b.putInt(REQUEST_CODE); b.putXXXXX~~~~
    public static final int COMMAND_RESPONSE = DEVICE_CONTROL_NOTIFICATION + 1;
    // 서비스 상태 변경..
    public static final int SERVICE_STATUS_NOTIFICATION = COMMAND_RESPONSE + 1;

    /**
     * bundle data key
     */
    public static final String KEY_MSGID = "KEY_MSGID";
    public static final String KEY_CMD = "KEY_CMD";
    public static final String KEY_NEXT_CMD = "KEY_NEXT_CMD";
    public static final String KEY_RESULT = "KEY_RESULT";
    public static final String KEY_SERVICE_STATUS = "KEY_SERVICE_STATUS";
    public static final String KEY_SERVICE_STATUS_CODE = "KEY_SERVICE_STATUS_CODE";

    public static final String KEY_DEVICE_TYPE = "KEY_DEVICE_TYPE";
    public static final String KEY_UUID = "KEY_UUID";
    public static final String KEY_DATA = "KEY_DATA";

    public static String generateMessageId(Context context) {
        if (context != null) {
            String packName = context.getApplicationContext().getPackageName();
            long currTimeMs = System.currentTimeMillis();

            return packName + "_" + currTimeMs;
        }

        return null;
    }
}

