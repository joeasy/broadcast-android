package com.nbplus.vbroadlauncher.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 6. 2..
 *
 * 단기예보
 */
public class RadioChannelInfo {
    @SerializedName("result")
    protected Response response;

    public String getResultCode() {
        return (response != null) ? response.resultCode : "-1";
    }
    public String getResultMessage() {
        return (response != null) ? response.resultMessage : null;
    }
    public ArrayList<RadioChannel> getRadioChannelList() {
        return (response != null) ? response.radioChannels : null;
    }

    // TODO : for sample data
    public void setResultCode(String code) {
        if (response == null) {
            response = new Response();
        }
        if (code != null) {
            this.response.resultCode = code;
        } else {
            this.response.resultCode = "0000";
        }
    }
    public void setResultMessage(String msg) {
        if (response == null) {
            response = new Response();
        }
        if (msg != null) {
            this.response.resultMessage = msg;
        } else {
            this.response.resultMessage = "Success";
        }
    }
    public void setRadioChannelList(ArrayList<RadioChannel> items) {
        if (response == null) {
            response = new Response();
        }
        if (items != null) {
            this.response.radioChannels = items;
        } else {
            this.response.radioChannels = new ArrayList<>();
        }
    }
    // end of TODO

    public static class Response {
        @SerializedName("RT")
        public String resultCode;
        @SerializedName("RT_MSG")
        public String resultMessage;
        @SerializedName("RADIO_LIST")
        protected ArrayList<RadioChannel> radioChannels;
    }

    public static class RadioChannel {
        @SerializedName("RADIO_NAME")
        public String channelName;
        @SerializedName("RADIO_IMAGE")
        public String channelImage;
        @SerializedName("URL")
        public String channelUrl;
    }
}
