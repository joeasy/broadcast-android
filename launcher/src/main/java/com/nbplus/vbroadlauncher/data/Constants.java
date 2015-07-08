package com.nbplus.vbroadlauncher.data;

/**
 * Created by basagee on 2015. 6. 1..
 */
public class Constants {
    public static final int RADIO_CHANNEL_GRIDVIEW_SIZE = 6;

    public static final int SUCCESS_RESULT = 0;

    public static final int FAILURE_RESULT = 1;

    public static final String PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationaddress";
    public static final String ACTION_BROWSER_ACTIVITY_CLOSE = "com.nbplus.vbroadlauncher.intent.action.BROWSER_ACTIVITY_CLOSE";

    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";

    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String RESULT_MESSAGE = PACKAGE_NAME + ".RESULT_MESSAGE";

    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";

    public static final String WEATHER_SERVICE_DEFAULT_TIMER = "com.nbplus.vbroadlauncher.intent.action.weaterupdate_default";
    public static final String WEATHER_SERVICE_GRIB_UPDATE_ACTION = "com.nbplus.vbroadlauncher.intent.action.weaterupdate_grib";
    public static final String WEATHER_SERVICE_TIME_UPDATE_ACTION = "com.nbplus.vbroadlauncher.intent.action.weaterupdate_time";
    public static final String WEATHER_SERVICE_SPACE_UPDATE_ACTION = "com.nbplus.vbroadlauncher.intent.action.weaterupdate_space";
    public static final String LOCATION_CHANGED_ACTION = "com.nbplus.vbroadlauncher.intent.action.locationchanged";

    // shortcut excution types
    public static final int SHORTCUT_TYPE_WEB_INTERFACE_SERVER = 0;
    public static final int SHORTCUT_TYPE_WEB_DOCUMENT_SERVER = 1;
    public static final int SHORTCUT_TYPE_NATIVE_INTERFACE = 2;

    // shortcut data extra
    public static final String EXTRA_NAME_SHORTCUT_DATA = "extra_shortcut_data";

    // installed application list retrieve message
    public static final int HANDLER_MESSAGE_INSTALLED_APPLIST_TASK = 1001;
    public static final int HANDLER_MESSAGE_GET_RADIO_CHANNEL_TASK = 2001;
    public static final int HANDLER_MESSAGE_PLAY_RADIO_CHANNEL_TIMEOUT = 2002;
    public static final int HANDLER_MESSAGE_SEND_EMERGENCY_CALL_COMPLETE_TASK = 3001;
    public static final int HANDLER_MESSAGE_PUSH_STATUS_CHANGED = 4001;
    public static final int HANDLER_MESSAGE_PUSH_MESAGE_RECEIVED = 4002;

    public static final int START_ACTIVITY_REQUEST_CHECK_TTS_DATA = 1001;

    // weather open api key
    public static final String WEATHER_OPEN_API_KEY = "2GjUf7yXsE0w7ayKJ2jGnYctDTSyZYu1IoPphFuLaBq6Ij0as1Bks1KTLcVE7pqt9E76kdfLxtGc4ocQ9Lxpdg%3D%3D";
    // server
    public static final String WEATHER_SERVER_PREFIX = "http://newsky2.kma.go.kr/service/SecndSrtpdFrcstInfoService/";
    // 실황조회
    public static final String WEATHER_SERVICE_GRIB = "ForecastGrib";
    // 초단기 예보조회
    public static final String WEATHER_SERVICE_TIMEDATA = "ForecastTimeData";
    // 단기예보조회
    public static final String WEATHER_SERVICE_SPACEDATA = "ForecastSpaceData";
    // parameter
    public static final String WEATHER_PARAM_SERVICE_KEY = "ServiceKey=";
    // parameter
    public static final String WEATHER_PARAM_TYPE = "_type=json";
    public static final String WEATHER_PARAM_TYPE_VALUE = "json";

    public static final int NUM_GRID_X = 149;
    public static final int NUM_GRID_Y = 253;
    public static final double DEFAULT_MAP_RADIUS = 6371.00877;
    public static final double DEFAULT_GRID_DISTANCE = 5.0;
    public static final double DEFAULT_STANDARD_LATITUDE_1 = 30.0;
    public static final double DEFAULT_STANDARD_LATITUDE_2 = 60.0;
    public static final double DEFAULT_LATITUDE = 38.0;
    public static final double DEFAULT_LONGITITUDE = 126.0;
    public static final double DEFAULT_GRID_X = 210 / DEFAULT_GRID_DISTANCE;
    public static final double DEFAULT_GRID_Y = 675 / DEFAULT_GRID_DISTANCE;

    public static final String RESULT_OK = "0000";

    // for yahoo weather
//    public static final String YAHOO_APP_ID = "e1szeL7k";
//    public static final String GEO_API = "http://where.yahooapis.com/geocode?location=%f,%f&flags=J&gflags=R&appid=" + YAHOO_APP_ID;
    public static final String YAHOO_WEATHER_API = "http://query.yahooapis.com/v1/public/yql?q=%s&format=json";
    public static final String YAHOO_WOEID_QUERY = "select * from geo.placefinder where text=\"%s,%s\" and gflags=\"R\"";
    public static final String YAHOO_WEATHER_QUERY = "select * from weather.forecast where woeid=%s and u=\"c\"";

    public static final String YAHOO_QUERY_WOEID_RESULT = "Result";
    public static final String YAHOO_QUERY_WEATHER_RESULT = "channel";

    //public static final String WEEK_WEATHER_YQL = "select item from weather.forecast where woeid=%s&d=7&format=json";
    public static final String WEATHER_SERVICE_UPDATE_ACTION = "com.nbplus.vbroadlauncher.intent.action.weaterupdate";

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
/**
 *  removed.. interface 0.7
 *
    public static final String PUSH_PAYLOAD_TYPE_APP_UPDATE = "11";
 */

    public static final String ACTION_LAUNCHER_ACTIVITY_RUNNING = "com.nbplus.vbroadlauncher.intent.action.LAUNCHER_ACTIVITY_RUNNING";
    public static final String EXTRA_LAUNCHER_ACTIVITY_RUNNING = "EXTRA_LAUNCHER_ACTIVITY_RUNNING";

}
