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
public class Constants {
    public static final boolean USE_ANOTHER_APP = false;
    public static final String NBPLUS_IOT_APP_PACKAGE_NAME = "com.nbplus.iotapp";

    public static final String ACTION_SERVICE_CREATE_BROADCAST = "com.nbplus.iot.ACTION_SERVICE_CREATE_BROADCAST";
    /**
     * local broadcast action & extra
     * 라이브러리에서 실제 어플리케이션으로 데이터를 전달하는 경우.
     */
    // 디바이스 리스트 전달
    public static final String ACTION_IOT_DEVICE_LIST = "com.nbplus.iot.ACTION_IOT_DEVICE_LIST";
}
