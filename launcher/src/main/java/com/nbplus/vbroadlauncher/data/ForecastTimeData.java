package com.nbplus.vbroadlauncher.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 6. 2..
 *
 * 단기예보
 */
public class ForecastTimeData {
    @SerializedName("response")
    protected Response response;

    public String getResultCode() {
        return (response != null && response.header != null) ? response.header.resultCode : "-1";
    }
    public String getResultMessage() {
        return (response != null && response.header != null) ? response.header.resultMessage : null;
    }
    public ArrayList<ForecastItem> getWeatherItems() {
        return (response != null && response.body != null && response.body.itemRoot != null) ? response.body.itemRoot.items : null;
    }
    public int getTotalCount() {
        return (response != null && response.body != null) ? response.body.totalCount : null;
    }
    public int getRowsPerPage() {
        return (response != null && response.body != null) ? response.body.rowsPerPage : null;
    }
    public int getNumOfPage() {
        return (response != null && response.body != null) ? response.body.pageNo : 0;
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
        // json 그지같이만들었따.
        @SerializedName("items")
        public Item itemRoot;
    }

    public static class Item {
        @SerializedName("item")
        public ArrayList<ForecastItem> items;
    }
}
