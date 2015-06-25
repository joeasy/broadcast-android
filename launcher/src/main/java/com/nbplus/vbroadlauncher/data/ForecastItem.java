package com.nbplus.vbroadlauncher.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 6. 11..
 */
public class ForecastItem {
    public static final String TMX = "TMX";     // 일최고기온
    public static final String TMN = "TMN";     // 일최저기온
    public static final String REH = "REH";     // 습도
    public static final String SKY = "SKY";     // 하늘상태
    public static final String POP = "POP";     // 강수확률
    public static final String PTY = "PTY";     // 강수형태
    public static final String RN1 = "RN1";     // 1시간강수
    public static final String T3H = "T3H";     // 3시간기온
    public static final String T1H = "T1H";     // 1시간기온 - 실황에포함됨.

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
    @SerializedName("obsrValue")        // 실황일때여기에 값이 있다.
    public String obsrValue;
    @SerializedName("nx")
    public String nx;                   // 예보지점 X 좌표
    @SerializedName("ny")
    public String ny;                   // 예보지점 Y 좌표
}
