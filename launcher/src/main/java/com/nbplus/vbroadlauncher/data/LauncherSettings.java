package com.nbplus.vbroadlauncher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.nbplus.vbroadlauncher.R;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class LauncherSettings implements Parcelable {

    // 맥어드레스 기반 40바이트 디바이스 UUID
    String deviceID;
    // 날씨정보에 사용할 로케이션정보
    PreferredLocation preferredUserLocation;

    // 마을정보
    @SerializedName("vill_code")
    String villageCode;
    @SerializedName("vill_name")
    String villageName;
    @SerializedName("is_exclusive")
    boolean isExclusive;

    boolean isCompletedSetup;

    // 서버정보
    @SerializedName("svc_domain")
    VBroadcastServer serverInformation;
    /**
     * 숏컷은 단말에서 유지하기로해서..
     * 미리 고정된 값으로넣는다.
     */
    // 숏컷정보
//    @SerializedName("app_shortcuts")
//    ArrayList<ShortcutData> launcherShortcuts = new ArrayList<ShortcutData>();;

    // when using singleton
    private volatile static LauncherSettings uniqueInstance;

    public static LauncherSettings getInstance(Context context) {
        if (uniqueInstance == null) {
            // 이렇게 하면 처음에만 동기화 된다
            synchronized (LauncherSettings.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new LauncherSettings(context);
                }
            }
        }
        return uniqueInstance;
    }

    public static final String VBROADCAST_PREFERENCE_NAME = "vbroadcast_preference";
    public static final String KEY_VBROADCAST_VILLAGE_CODE = "key_village_code";
    public static final String KEY_VBROADCAST_VILLAGE_NAME = "key_village_name";
    public static final String KEY_VBROADCAST_IS_COMPLETED_SETUP = "key_is_completed_setup";
    public static final String KEY_VBROADCAST_IS_EXCLUSIVE_DEVICE = "key_is_exclusive_device";
    public static final String KEY_VBROADCAST_PREFERRED_LOCATION = "key_preferred_location";
    public static final String KEY_VBROADCAST_DEVICE_ID = "key_device_id";
    public static final String KEY_VBROADCAST_SERVER_INFO = "key_server_info";
    public static final String KEY_VBROADCAST_SHORTCUT = "key_shortcut";

    private Context context;
    private SharedPreferences prefs;
    private Gson gson;

    private LauncherSettings(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(VBROADCAST_PREFERENCE_NAME, Context.MODE_PRIVATE);

        // load from preferences..
        this.deviceID = prefs.getString(KEY_VBROADCAST_DEVICE_ID, "");
        this.isCompletedSetup = prefs.getBoolean(KEY_VBROADCAST_IS_COMPLETED_SETUP, false);
        this.isExclusive = prefs.getBoolean(KEY_VBROADCAST_IS_EXCLUSIVE_DEVICE, false);
        this.villageCode = prefs.getString(KEY_VBROADCAST_VILLAGE_CODE, "");
        this.villageName = prefs.getString(KEY_VBROADCAST_VILLAGE_NAME, "");

        /**
         * 숏컷은 단말에서 유지하기로해서..
         * 미리 고정된 값으로넣는다.
         */
        //(ArrayList<ShortcutData>)getPrefsJsonObject(KEY_VBROADCAST_SHORTCUT, new TypeToken<ArrayList<ShortcutData>>(){}.getType());
        this.preferredUserLocation = (PreferredLocation)getPrefsJsonObject(KEY_VBROADCAST_PREFERRED_LOCATION, new TypeToken<PreferredLocation>(){}.getType());
        this.serverInformation = (VBroadcastServer)getPrefsJsonObject(KEY_VBROADCAST_SERVER_INFO, new TypeToken<VBroadcastServer>(){}.getType());
    }

    public boolean isCompletedSetup() {
        return isCompletedSetup;
    }

    public void setIsCompletedSetup(boolean isCompletedSetup) {
        this.isCompletedSetup = isCompletedSetup;
        prefs.edit().putBoolean(KEY_VBROADCAST_IS_COMPLETED_SETUP, this.isCompletedSetup).commit();
    }

    public boolean isExclusive() {
        return isExclusive;
    }

    public void setIsExclusive(boolean isExclusive) {
        this.isExclusive = isExclusive;
        prefs.edit().putBoolean(KEY_VBROADCAST_IS_EXCLUSIVE_DEVICE, this.isExclusive).commit();
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
        prefs.edit().putString(KEY_VBROADCAST_VILLAGE_NAME, this.villageName).commit();
    }

    public String getVillageCode() {
        return villageCode;
    }

    public void setVillageCode(String villageCode) {
        this.villageCode = villageCode;
        prefs.edit().putString(KEY_VBROADCAST_VILLAGE_CODE, this.villageCode).commit();
    }

    /**
     * 숏컷은 단말에서 유지하기로해서..
     * 미리 고정된 값으로넣는다.
     */
//    public ArrayList<ShortcutData> getLauncherShortcuts() {
//        return launcherShortcuts;
//    }
//
//    public void setLauncherShortcuts(ArrayList<ShortcutData> launcherShortcuts) {
//        this.launcherShortcuts = launcherShortcuts;
//        setPrefsJsonObject(KEY_VBROADCAST_SHORTCUT, this.launcherShortcuts);
//    }

    public VBroadcastServer getServerInformation() {
        return serverInformation;
    }

    public void setServerInformation(VBroadcastServer serverInformation) {
        this.serverInformation = serverInformation;
        setPrefsJsonObject(KEY_VBROADCAST_SERVER_INFO, this.serverInformation);
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
        prefs.edit().putString(KEY_VBROADCAST_DEVICE_ID, deviceID).commit();
    }

    public PreferredLocation getPreferredUserLocation() {
        return preferredUserLocation;
    }

    public void setPreferredUserLocation(PreferredLocation preferredUserLocation) {
        this.preferredUserLocation = preferredUserLocation;
        setPrefsJsonObject(KEY_VBROADCAST_PREFERRED_LOCATION, this.preferredUserLocation);
    }

    public void setPrefsJsonObject(String key, Object obj) {
        if (obj == null) {
            prefs.edit().putString(key, "").commit();
        } else {
            if (gson == null) {
                gson = new GsonBuilder().create();
            }
            prefs.edit().putString(key, gson.toJson(obj)).commit();
        }
    }

    public Object getPrefsJsonObject(String key, Type tokenType) {
        Object obj = null;
        String savedValue = prefs.getString(key, "");
        if (savedValue.equals("")) {
            obj = null;
        } else {
            if (gson == null) {
                gson = new GsonBuilder().create();
            }
            obj = gson.fromJson(savedValue, tokenType);
        }

        return obj;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.deviceID);
        dest.writeParcelable(this.preferredUserLocation, 0);
        dest.writeString(this.villageCode);
        dest.writeString(this.villageName);
        dest.writeByte(isExclusive ? (byte) 1 : (byte) 0);
        dest.writeByte(isCompletedSetup ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.serverInformation, 0);
        /**
         * 숏컷은 단말에서 유지하기로해서..
         * 미리 고정된 값으로넣는다.
         */
//        dest.writeSerializable(this.launcherShortcuts);
    }

    private LauncherSettings(Parcel in) {
        this.deviceID = in.readString();
        this.preferredUserLocation = in.readParcelable(PreferredLocation.class.getClassLoader());
        this.villageCode = in.readString();
        this.villageName = in.readString();
        this.isExclusive = in.readByte() != 0;
        this.isCompletedSetup = in.readByte() != 0;
        this.serverInformation = in.readParcelable(VBroadcastServer.class.getClassLoader());
        /**
         * 숏컷은 단말에서 유지하기로해서..
         * 미리 고정된 값으로넣는다.
         */
//        this.launcherShortcuts = (ArrayList<ShortcutData>) in.readSerializable();
    }

    public static final Creator<LauncherSettings> CREATOR = new Creator<LauncherSettings>() {
        public LauncherSettings createFromParcel(Parcel source) {
            return new LauncherSettings(source);
        }

        public LauncherSettings[] newArray(int size) {
            return new LauncherSettings[size];
        }
    };
}
