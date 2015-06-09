package com.nbplus.vbroadlauncher.data;

/**
 * Created by basagee on 2015. 6. 1..
 */
public class Constants {
    // shortcut excution types
    public static final int SHORTCUT_TYPE_WEB_INTERFACE_SERVER = 0;
    public static final int SHORTCUT_TYPE_WEB_DOCUMENT_SERVER = 1;
    public static final int SHORTCUT_TYPE_NATIVE_INTERFACE = 2;

    // shortcut data extra
    public static final String EXTRA_NAME_SHORTCUT_DATA = "extra_shortcut_data";

    // installed application list retrieve message
    public static final int HANDLER_MESSAGE_FINISH_TASK = 0x1001;

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

    public static final String WEATHER_RESULT_OK = "0000";
}