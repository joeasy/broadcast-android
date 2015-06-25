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
    private int name;

    @SerializedName("domain")
    private String domain;

    @SerializedName("path")
    private String path;

    @SerializedName("icon")
    private int iconResId;

    @SerializedName("background")
    private int iconBackResId;

    @SerializedName("native_type")
    private int nativeType = 0;     // 0 : activity, 1 : fragment, 2 : dialogfragment

    @SerializedName("native_class")
    private Class nativeClass = null;     // 0 : activity, 1 : fragment, 2 : dialogfragment

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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

    public int getName() {
        return name;
    }

    public void setName(int name) {
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

    public int getNativeType() {
        return nativeType;
    }

    public void setNativeType(int nativeType) {
        this.nativeType = nativeType;
    }

    public Class getNativeClass() {
        return nativeClass;
    }

    public void setNativeClass(Class nativeClass) {
        this.nativeClass = nativeClass;
    }

    public ShortcutData(int type, int name, String path, int iconRes, int backgroundRes, int nativeType, Class clazz) {
        this.type = type;
        this.name = name;
        this.path = path;
        this.iconResId = iconRes;
        this.iconBackResId = backgroundRes;
        this.nativeType = nativeType;
        this.nativeClass = clazz;
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
        dest.writeInt(this.name);
        dest.writeString(this.domain);
        dest.writeString(this.path);
        dest.writeInt(this.iconResId);
        dest.writeInt(this.iconBackResId);
        dest.writeInt(this.nativeType);
        dest.writeSerializable(this.nativeClass);
    }

    protected ShortcutData(Parcel in) {
        this.type = (Integer) in.readValue(Integer.class.getClassLoader());
        this.name = in.readInt();
        this.domain = in.readString();
        this.path = in.readString();
        this.iconResId = in.readInt();
        this.iconBackResId = in.readInt();
        this.nativeType = in.readInt();
        this.nativeClass = (Class) in.readSerializable();
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
