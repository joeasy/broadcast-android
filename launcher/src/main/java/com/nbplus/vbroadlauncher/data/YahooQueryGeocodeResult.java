package com.nbplus.vbroadlauncher.data;

import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

/**
 * Created by basagee on 2015. 6. 17..
 */
public class YahooQueryGeocodeResult {
    @SerializedName("query")
    private QueryResult query;

    public GeocodeData getGeocodeData() {
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
        public GeocodeResultWrapper result;
    }

    public static class GeocodeResultWrapper {
        @SerializedName("Result")
        public GeocodeData data;
    }
}
