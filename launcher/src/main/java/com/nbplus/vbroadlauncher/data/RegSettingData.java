package com.nbplus.vbroadlauncher.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class RegSettingData implements Parcelable {
    // 마을정보
    @SerializedName("vill_code")
    String villageCode;
    @SerializedName("vill_name")
    String villageName;
    @SerializedName("is_exclusive")
    boolean isExclusive;

    // 서버정보
    @SerializedName("svc_domain")
    VBroadcastServer serverInformation;

    public ArrayList<ShortcutData> getLauncherShortcuts() {
        return launcherShortcuts;
    }

    public void setLauncherShortcuts(ArrayList<ShortcutData> launcherShortcuts) {
        this.launcherShortcuts = launcherShortcuts;
    }

    public String getVillageCode() {
        return villageCode;
    }

    public void setVillageCode(String villageCode) {
        this.villageCode = villageCode;
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public boolean isExclusive() {
        return isExclusive;
    }

    public void setIsExclusive(boolean isExclusive) {
        this.isExclusive = isExclusive;
    }

    public VBroadcastServer getServerInformation() {
        return serverInformation;
    }

    public void setServerInformation(VBroadcastServer serverInformation) {
        this.serverInformation = serverInformation;
    }

    // 숏컷정보
    @SerializedName("app_shortcuts")
    ArrayList<ShortcutData> launcherShortcuts;

    private RegSettingData() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.villageCode);
        dest.writeString(this.villageName);
        dest.writeByte(isExclusive ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.serverInformation, 0);
        dest.writeSerializable(this.launcherShortcuts);
    }

    private RegSettingData(Parcel in) {
        this.villageCode = in.readString();
        this.villageName = in.readString();
        this.isExclusive = in.readByte() != 0;
        this.serverInformation = in.readParcelable(VBroadcastServer.class.getClassLoader());
        this.launcherShortcuts = (ArrayList<ShortcutData>) in.readSerializable();
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
