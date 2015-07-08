package com.nbplus.vbroadlistener.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class RegSettingData implements Parcelable {
    // 마을정보
    // 서버정보
    @SerializedName("svc_domain")
    VBroadcastServer serverInformation;

    public VBroadcastServer getServerInformation() {
        return serverInformation;
    }

    public void setServerInformation(VBroadcastServer serverInformation) {
        this.serverInformation = serverInformation;
    }

    private RegSettingData() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.serverInformation, 0);
    }

    private RegSettingData(Parcel in) {
        this.serverInformation = in.readParcelable(VBroadcastServer.class.getClassLoader());
    }

    public static final Creator<RegSettingData> CREATOR = new Creator<RegSettingData>() {
        public RegSettingData createFromParcel(Parcel source) {
            return new RegSettingData(source);
        }

        public RegSettingData[] newArray(int size) {
            return new RegSettingData[size];
        }
    };
}
