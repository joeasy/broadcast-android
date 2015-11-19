package com.nbplus.push.data;

/**
 * Created by basagee on 2015. 6. 29..
 */
public class PushConstants {
    // broadcast permission and action
    public static final String PERMISSION_PUSH_RECEIVE = "com.nbplus.pushservice.permission.RECEIVE";
    public static final String ACTION_PUSH_MESSAGE_RECEIVED = "com.nbplus.pushservice.intent.action.PUSH_MESSAGE_RECEIVED";
    public static final String EXTRA_PUSH_MESSAGE_DATA = "extra_push_message_data";

    public static final String ACTION_SETUP_PUSH_IF_SERVER = "com.nbplus.pushservice.intent.action.SETUP_PUSH_GW";
    public static final String ACTION_GET_STATUS = "com.nbplus.pushservice.intent.action.GET_TATUS";
    public static final String ACTION_START_SERVICE = "com.nbplus.pushservice.intent.action.START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.nbplus.pushservice.intent.action.STOP_SERVICE";
    public static final String EXTRA_START_SERVICE_IFADDRESS = "extra_push_start_service_ifaddress";

    // send to application
    public static final String ACTION_PUSH_STATUS_CHANGED = "com.nbplus.pushservice.intent.action.PUSH_STATUS_CHANGED";
    public static final String EXTRA_PUSH_STATUS_VALUE = "extra_push_status_value";
    public static final String EXTRA_PUSH_STATUS_WHAT = "extra_push_status_what";
    public static final int PUSH_STATUS_VALUE_DISCONNECTED = 0;
    public static final int PUSH_STATUS_VALUE_CONNECTED = 1;
    public static final int PUSH_STATUS_WHAT_NORMAL = 0;
    public static final int PUSH_STATUS_WHAT_NETORSERVER = 1;
    public static final int PUSH_STATUS_WHAT_SERVICE_ERROR = 2;

    // push data type from server..
    public static final char PUSH_MESSAGE_TYPE_CONNECTION_REQUEST = '0';
    public static final char PUSH_MESSAGE_TYPE_CONNECTION_RESPONSE = '1';
    public static final char PUSH_MESSAGE_TYPE_PUSH_REQUEST = '2';
    public static final char PUSH_MESSAGE_TYPE_PUSH_RESPONSE = '3';
    public static final char PUSH_MESSAGE_TYPE_KEEP_ALIVE_REQUEST = '4';
    public static final char PUSH_MESSAGE_TYPE_KEEP_ALIVE_RESPONSE = '5';
    public static final char PUSH_MESSAGE_TYPE_KEEP_ALIVE_CHANGE_REQUEST = '6';
    public static final char PUSH_MESSAGE_TYPE_KEEP_ALIVE_CHANGE_RESPONSE = '7';
    public static final char PUSH_MESSAGE_TYPE_PUSH_AGENT_UPDATE_REQUEST = '8';
    public static final char PUSH_MESSAGE_TYPE_APP_UPDATE_REQUEST = 'a';
    public static final char PUSH_MESSAGE_TYPE_APP_UPDATE_RESPONSE = 'b';

    public static final String KEY_DEVICE_ID = "key_device_id";

    public static final String RESULT_OK = "0000";
    public static final int HANDLER_MESSAGE_GET_PUSH_GATEWAY_DATA = 1001;
    public static final int HANDLER_MESSAGE_RETRY_MESSAGE = 1002;
    public static final int HANDLER_MESSAGE_SEND_PUSH_STATUS = 1003;
    public static final int HANDLER_MESSAGE_CONNECTIVITY_CHANGED = 1004;
    public static final int HANDLER_MESSAGE_RECEIVE_PUSH_DATA = 10001;

    public static final String PREF_KEY_PUSH_IF_ADDRESS = "PREF_KEY_PUSH_IF_ADDRESS";
}
