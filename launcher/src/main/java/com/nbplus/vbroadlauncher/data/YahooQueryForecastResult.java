package com.nbplus.vbroadlauncher.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 6. 17..
 */
public class YahooQueryForecastResult {
    @SerializedName("query")
    private QueryResult query;

    public ForecastData getForecastData() {
        return (query != null && query.result != null) ? query.result.data : null;
    }

    public static class QueryResult {
        @SerializedName("count")
        public int count;
        @SerializedName("lang")
        public String lang;
        @SerializedName("created")
        public String createdDate;
        @SerializedName("results")
        public ForecastDataResultWrapper result;
    }

    public static class ForecastDataResultWrapper {
        @SerializedName("channel")
        public ForecastData data;
    }
}
