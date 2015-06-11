package com.nbplus.vbroadlauncher.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 6. 11..
 */
public class ForecastItem {
    @SerializedName("baseDate")
    public String baseDate;             // 발표일자
    @SerializedName("baseTime")
    public String baseTime;             // 발표시간
    @SerializedName("fcstDate")
    public String fcstDate;             // 예보일자
    @SerializedName("fcstTime")
    public String fcstTime;             // 예보시간
    @SerializedName("category")
    public String category;             // 자료구분문자
    @SerializedName("fcstValue")
    public String fcstValue;            // 예보값
    @SerializedName("nx")
    public String nx;                   // 예보지점 X 좌표
    @SerializedName("ny")
    public String ny;                   // 예보지점 Y 좌표
}
