package com.nbplus.iotgateway.data;

/**
 * Created by basagee on 2015. 8. 6..
 */
public class Constants {
    // from App to Service
    public static final String ACTION_START_IOT_SERVICE = "com.nbplus.iotgateway.action.START_IOT_SERVICE";
    public static final String ACTION_GET_IOT_DEVICE_LIST = "com.nbplus.iotgateway.action.IOT_DEVICE_LIST";
    public static final String ACTION_SEND_IOT_COMMAND = "com.nbplus.iotgateway.action.SEND_IOT_COMMAND";
    // TODO : iot test
    public static final String ACTION_TEST_UPDATE_IOT_DATA = "com.nbplus.iotgateway.action.TEST_UPDATE_IOT_DATA";

    // from Service to App
    public static final String ACTION_IOT_DEVICE_LIST = "com.nbplus.iotgateway.action.IOT_DEVICE_LIST";
    public static final String ACTION_IOT_GATEWAY_DATA = "com.nbplus.iotgateway.action.IOT_GATEWAY_DATA";
    // do not use. 서비스에서 알아서 서버에 보낸다.
    public static final String ACTION_UPDATE_IOT_DEVICE_DATA = "com.nbplus.iotgateway.action.UPDATE_IOT_DEVICE_DATA";

    public static final String EXTRA_IOT_GATEWAY_DATA = "EXTRA_IOT_GATEWAY_DATA";
    public static final String EXTRA_IOT_DEVICE_LIST = "EXTRA_IOT_DEVICE_LIST";
    public static final String EXTRA_IOT_SEND_COMM_DEVICE_ID = "EXTRA_IOT_SEND_COMM_DEVICE_ID";
    public static final String EXTRA_IOT_SEND_COMM_COMMAND_ID = "EXTRA_IOT_SEND_COMM_COMMAND_ID";
    // from App to service
    public static final String EXTRA_IOT_GATEWAY_CONNECTION_INFO = "EXTRA_IOT_DEVICE_LIST";

    enum IoTGatewayConnType {
        TYPE_TCP,           // tcp : 2015년개발용
        TYPE_HTTP,          // http
        TYPE_COAP,          // coap
        TYPE_MQTT
    }
}
