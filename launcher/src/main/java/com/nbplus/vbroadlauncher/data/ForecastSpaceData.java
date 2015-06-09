package com.nbplus.vbroadlauncher.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 6. 2..
 *
 * 단기예보
 */
public class ForecastSpaceData {
    @SerializedName("response")
    protected Response response;

    public String getResultCode() {
        return (response != null && response.header != null) ? response.header.resultCode : "-1";
    }
    public String getResultMessage() {
        return (response != null && response.header != null) ? response.header.resultMessage : null;
    }
    public ArrayList<Item> getWeatherItems() {
        return (response != null && response.body != null) ? response.body.items : null;
    }
    public Body getBody() {
        return (response != null) ? response.body : null;
    }

    public static class Response {
        @SerializedName("header")
        public Header header;
        @SerializedName("body")
        protected Body body;
    }

    public static class Header {
        @SerializedName("resultCode")
        public String resultCode;
        @SerializedName("resultMsg")
        public String resultMessage;
    }

    public static class Body {
        @SerializedName("pageNo")
        public int pageNo;
        @SerializedName("numOfRows")
        public int rowsPerPage;
        @SerializedName("totalCount")
        public int totalCount;
        @SerializedName("items")
        public ArrayList<Item> items;
    }

    public static class Item {
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
}
