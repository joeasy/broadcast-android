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

package com.nbplus.vbroadlistener.data;

/**
 * Created by basagee on 2015. 6. 15..
 */
public class Constants {

    // GCM Sender ID
    public static final String GCM_SENDER_ID = "356837636067";

    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String UNREGISTRATION_COMPLETE = "unRegistrationComplete";

    public static final int HANDLER_MESSAGE_UPDATE_GCM_DEVICE_TOKEN = 1001;
    public static final int HANDLER_MESSAGE_UNREGISTER_GCM = 1002;

    public static final String REGISTER_GCM = "com.nbplus.vbroadcast.action.REGISTER_GCM";
    public static final String UPDATE_GCM = "com.nbplus.vbroadcast.action.UPDATE_GCM";
    public static final String UNREGISTER_GCM = "com.nbplus.vbroadcast.action.UNREGISTER_GCM";
    public static final String ACTION_SHOW_NOTIFICATION_CONTENTS = "com.nbplus.vbroadcast.action.SHOW_NOTIFICATION_CONTENTS";
    public static final String ACTION_SHOW_NOTIFICATION_EMERGENCY_CALL = "com.nbplus.vbroadcast.action.SHOW_NOTIFICATION_EMERGENCY_CALL";
    public static final String EXTRA_SHOW_NOTIFICATION_CONTENTS = "EXTRA_SHOW_NOTIFICATION_CONTENTS";
    public static final String EXTRA_SHOW_NOTIFICATION_EMERGENCY_LAT = "EXTRA_SHOW_NOTIFICATION_EMERGENCY_LAT";
    public static final String EXTRA_SHOW_NOTIFICATION_EMERGENCY_LON = "EXTRA_SHOW_NOTIFICATION_EMERGENCY_LON";

    public static final int START_ACTIVITY_REQUEST_CHECK_TTS_DATA = 1001;

    // push notification ID
    public static final int PUSH_NOTIFICATION_ALARM_ID = 1001;
    public static final int RADIO_NOTIFICATION_ID = 2001;
    public static final int PW_FIND_NOTIFICATION_ID = 2002;
    public static final int SYSTEM_ADMIN_NOTIFICATION_ID = 2003;
    public static final int BROADCAST_EVENT_NOTIFICATION_ID = 2004;
    public static final int EMERGENCY_CALL_EVENT_NOTIFICATION_ID = 2005;
    public static final int INHABITANT_POLL_EVENT_NOTIFICATION_ID = 2006;
    public static final int COOPERATIVE_BUYING_EVENT_NOTIFICATION_ID = 2007;

    // gcm data key
    public static final String GCM_DATA_KEY_ALERT = "alert";
    public static final String GCM_DATA_KEY_MESSAGE_ID = "messageid";
    public static final String GCM_DATA_KEY_PAYLOAD = "payload_data";

    // push payload data type
    public static final String PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST = "00";
    public static final String PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST = "01";
    public static final String PUSH_PAYLOAD_TYPE_TEXT_BROADCAST = "02";
    public static final String PUSH_PAYLOAD_TYPE_EMERGENCY_CALL = "03";
    public static final String PUSH_PAYLOAD_TYPE_INHABITANTS_POLL  = "04";
    public static final String PUSH_PAYLOAD_TYPE_COOPERATIVE_BUYING = "05";
    public static final String PUSH_PAYLOAD_TYPE_IOT_DEVICE_CONTROL = "06";
    public static final String PUSH_PAYLOAD_TYPE_PUSH_NOTIFICATION = "07";
    public static final String PUSH_PAYLOAD_TYPE_FIND_PASSWORD = "08";

    public static final String BROADCAST_LIST_CONTEXT_PATH = "/broadcasting/broadcasting/getBroadcastingList.rmc";
    public static final String INHABITANT_POLL_LIST_CONTEXT_PATH = "/participation/residentvote/getResidentVoteList.rmc";
    public static final String COOPERATIVE_BUYING_LIST_CONTEXT_PATH = "/participation/groupPurchase/getGroupPurchaseList.rmc";

    public static final String API_GCM_SEND_RESULT_CONTEXT_PATH = "/is/api/appRequest/RegistPushRequestResult";
    public static final String API_GCM_TOKEN_UPDATE_CONTEXT_PATH = "/is/api/appRequest/UpdatePushInfo";

    public static final String API_RESP_OK = "0000";
}
