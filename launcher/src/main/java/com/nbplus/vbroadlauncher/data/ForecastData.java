package com.nbplus.vbroadlauncher.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 6. 17..
 */
public class ForecastData {
    @SerializedName("title")
    public String title;

    @SerializedName("atmosphere")
    public Atmosphere atmosphere;

    @SerializedName("item")
    public ForecastItem item;

    public static class Atmosphere {
        @SerializedName("humidity")     // 습도
        public String humidity;
        @SerializedName("visibility")   // 가시거리
        public String visibility;
        @SerializedName("pressure")     // 기압
        public String pressure;
    }

    public static class ForecastItem {
        @SerializedName("condition")
        public Condition currentCondition;
        @SerializedName("forecast")
        public ArrayList<Forecast> weekCondition;
    }

    // 오늘 현재 상태
    public static class Condition {
        @SerializedName("temp")
        public String temperature;
        @SerializedName("code")
        public String conditionCode;
        @SerializedName("date")
        public String date;
    }

    // 5 일간 요약 데이터
    public static class Forecast {
        @SerializedName("low")
        public String low;
        @SerializedName("high")
        public String high;
        @SerializedName("code")
        public String conditionCode;
        @SerializedName("date")
        public String date;
    }
}
