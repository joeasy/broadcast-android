package com.nbplus.vbroadlauncher.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 6. 17..
 */
public class GeocodeData implements Parcelable {
    @SerializedName("woeid")
    public String woeid;
    @SerializedName("woetype")
    public String woetype;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.woeid);
        dest.writeString(this.woetype);
    }

    public GeocodeData() {
    }

    protected GeocodeData(Parcel in) {
        this.woeid = in.readString();
        this.woetype = in.readString();
    }

    public static final Creator<GeocodeData> CREATOR = new Creator<GeocodeData>() {
        public GeocodeData createFromParcel(Parcel source) {
            return new GeocodeData(source);
        }

        public GeocodeData[] newArray(int size) {
            return new GeocodeData[size];
        }
    };
}
