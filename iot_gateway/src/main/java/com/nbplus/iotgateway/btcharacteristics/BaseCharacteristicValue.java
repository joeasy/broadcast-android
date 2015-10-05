package com.nbplus.iotgateway.btcharacteristics;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by basagee on 2015. 9. 14..
 */
public class BaseCharacteristicValue implements Parcelable {

    private String mPrimaryAddress;
    private String mSecondaryAddress;
    private String mUuid;

    public String getUuidString() {
        return mUuid;
    }

    public void setUuidString(String mUuid) {
        this.mUuid = mUuid;
    }

    public String getPrimaryAddress() {
        return mPrimaryAddress;
    }

    public void setPrimaryAddress(String mPrimaryAddress) {
        this.mPrimaryAddress = mPrimaryAddress;
    }

    public String getSecondaryAddress() {
        return mSecondaryAddress;
    }

    public void setSecondaryAddress(String mSecondaryAddress) {
        this.mSecondaryAddress = mSecondaryAddress;
    }

    public BaseCharacteristicValue() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPrimaryAddress);
        dest.writeString(this.mSecondaryAddress);
        dest.writeString(this.mUuid);
    }

    protected BaseCharacteristicValue(Parcel in) {
        this.mPrimaryAddress = in.readString();
        this.mSecondaryAddress = in.readString();
        this.mUuid = in.readString();
    }

    public static final Parcelable.Creator<BaseCharacteristicValue> CREATOR = new Parcelable.Creator<BaseCharacteristicValue>() {
        public BaseCharacteristicValue createFromParcel(Parcel source) {
            return new BaseCharacteristicValue(source);
        }

        public BaseCharacteristicValue[] newArray(int size) {
            return new BaseCharacteristicValue[size];
        }
    };
}
