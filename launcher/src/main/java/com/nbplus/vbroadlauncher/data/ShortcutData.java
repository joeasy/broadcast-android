package com.nbplus.vbroadlauncher.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class ShortcutData implements Parcelable {
    @SerializedName("type")
    private Integer type;
    @SerializedName("name")
    private String name;
    @SerializedName("path")
    private String path;

    @SerializedName("btn_res")
    private int btnResId;

    @SerializedName("icon")
    private int iconResId;

    @SerializedName("background")
    private int iconBackResId;

    public int getBtnResId() {
        return btnResId;
    }

    public void setBtnResId(int btnResId) {
        this.btnResId = btnResId;
    }

    public int getIconBackResId() {
        return iconBackResId;
    }

    public void setIconBackResId(int iconBackResId) {
        this.iconBackResId = iconBackResId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public ShortcutData(int btnResId, int type, String name, String path, int iconRes, int backgroundRes) {
        this.btnResId = btnResId;
        this.type = type;
        this.name = name;
        this.path = path;
        this.iconResId = iconRes;
        this.iconBackResId = backgroundRes;
    }

    public ShortcutData() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.type);
        dest.writeString(this.name);
        dest.writeString(this.path);
        dest.writeInt(this.btnResId);
        dest.writeInt(this.iconResId);
        dest.writeInt(this.iconBackResId);
    }

    private ShortcutData(Parcel in) {
        this.type = (Integer) in.readValue(Integer.class.getClassLoader());
        this.name = in.readString();
        this.path = in.readString();
        this.btnResId = in.readInt();
        this.iconResId = in.readInt();
        this.iconBackResId = in.readInt();
    }

    public static final Creator<ShortcutData> CREATOR = new Creator<ShortcutData>() {
        public ShortcutData createFromParcel(Parcel source) {
            return new ShortcutData(source);
        }

        public ShortcutData[] newArray(int size) {
            return new ShortcutData[size];
        }
    };
}
