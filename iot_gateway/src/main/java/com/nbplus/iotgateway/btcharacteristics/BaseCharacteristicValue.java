/*
 * Copyright (c) 2015. NB Plus (www.nbplus.co.kr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
