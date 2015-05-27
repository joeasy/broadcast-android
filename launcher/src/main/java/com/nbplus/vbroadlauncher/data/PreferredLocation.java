package com.nbplus.vbroadlauncher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class PreferredLocation implements Parcelable {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
    }

    public PreferredLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private PreferredLocation(Parcel in) {
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
    }

    public static final Creator<PreferredLocation> CREATOR = new Creator<PreferredLocation>() {
        public PreferredLocation createFromParcel(Parcel source) {
            return new PreferredLocation(source);
        }

        public PreferredLocation[] newArray(int size) {
            return new PreferredLocation[size];
        }
    };
}
