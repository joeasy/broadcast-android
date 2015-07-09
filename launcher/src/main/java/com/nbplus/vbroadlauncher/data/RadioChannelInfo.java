package com.nbplus.vbroadlauncher.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 6. 2..
 *
 * 단기예보
 */
public class RadioChannelInfo extends BaseApiResult {

    @SerializedName("RADIO_LIST")
    protected ArrayList<RadioChannel> radioChannels;

    public ArrayList<RadioChannel> getRadioChannelList() {
        return radioChannels;
    }

    public void setResultCode(String code) {
        resultCode = code;
    }
    public void setResultMessage(String msg) {
        this.resultMessage = msg;
    }
    public void setRadioChannelList(ArrayList<RadioChannel> items) {
        this.radioChannels = items;
    }

    public static class RadioChannel implements Parcelable {
        @SerializedName("RADIO_NAME")
        public String channelName;
        @SerializedName("RADIO_IMAGE")
        public String channelImage;
        @SerializedName("URL")
        public String channelUrl;

        // 재생위치를 기억하기 위한 index.
        public int index;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.channelName);
            dest.writeString(this.channelImage);
            dest.writeString(this.channelUrl);
            dest.writeInt(this.index);
        }

        public RadioChannel() {
        }

        protected RadioChannel(Parcel in) {
            this.channelName = in.readString();
            this.channelImage = in.readString();
            this.channelUrl = in.readString();
            this.index = in.readInt();
        }

        public static final Parcelable.Creator<RadioChannel> CREATOR = new Parcelable.Creator<RadioChannel>() {
            public RadioChannel createFromParcel(Parcel source) {
                return new RadioChannel(source);
            }

            public RadioChannel[] newArray(int size) {
                return new RadioChannel[size];
            }
        };
    }
}
