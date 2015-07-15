package com.nbplus.vbroadlistener.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.nbplus.vbroadlistener.R;
import com.nbplus.vbroadlistener.data.VBroadcastServer;

import org.basdroid.common.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class LauncherSettings implements Parcelable {

    // 맥어드레스 기반 40바이트 디바이스 UUID
    String deviceID;
    // 마을정보
    @SerializedName("vill_code")
    String villageCode;
    @SerializedName("vill_name")
    String villageName;
    @SerializedName("is_exclusive")
    boolean isExclusive;
    boolean isCompletedSetup;

    public static int[] landWallpaperResource;
    public static int[] portWallpaperResource;
    private int wallpaperId = -1;

    boolean isCheckedTTSEngine;

    // 서버정보
    @SerializedName("svc_domain")
    VBroadcastServer serverInformation;
    @SerializedName("register_address")
    //String initialPageAddress = "http://175.207.46.132:8010/web_test/listen_test.html";
    String initialPageAddress = "http://175.207.46.132:8080/common/selectServer.rmc";

    public static final String firstPageContext = "/login.rmc";

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
    public static final String KEY_VBROADCAST_YAHOO_GEOCODE = "key_yahoo_geocode";
    public static final String KEY_VBROADCAST_DEVICE_ID = "key_device_id";
    public static final String KEY_VBROADCAST_SERVER_INFO = "key_server_info";
    public static final String KEY_VBROADCAST_SHORTCUT = "key_shortcut";
    public static final String KEY_WALLPAPER_RESOURCE_ID = "key_wallpaper_resource_id";
    public static final String KEY_VBROADCAST_IS_OUTDOOR_MODE = "key_is_outdoor_mode";
    public static final String KEY_IS_CHECKED_TTS = "key_is_checked_tts";

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
        this.isCheckedTTSEngine = prefs.getBoolean(KEY_IS_CHECKED_TTS, false);
        int wallpaperId = prefs.getInt(KEY_WALLPAPER_RESOURCE_ID, -1);

        landWallpaperResource = new int[]{ R.drawable.ic_bg_main_land };
        portWallpaperResource = new int[]{ R.drawable.ic_bg_main_port };

        if (wallpaperId <= 0) {
            Random oRandom = new Random();
            wallpaperId = oRandom.nextInt(landWallpaperResource.length);

            setWallpaperId(wallpaperId);
        } else {
            this.wallpaperId = wallpaperId;
        }

        this.serverInformation = (VBroadcastServer)getPrefsJsonObject(KEY_VBROADCAST_SERVER_INFO, new TypeToken<VBroadcastServer>(){}.getType());
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
        prefs.edit().putString(KEY_VBROADCAST_DEVICE_ID, deviceID).apply();
    }

    public boolean isCheckedTTSEngine() {
        return isCheckedTTSEngine;
    }

    public void setIsCheckedTTSEngine(boolean isCheckedTTSEngine) {
        this.isCheckedTTSEngine = isCheckedTTSEngine;
        prefs.edit().putBoolean(KEY_IS_CHECKED_TTS, this.isCheckedTTSEngine).apply();
    }

    public boolean isCompletedSetup() {
        return isCompletedSetup;
    }

    public void setIsCompletedSetup(boolean isCompletedSetup) {
        this.isCompletedSetup = isCompletedSetup;
        prefs.edit().putBoolean(KEY_VBROADCAST_IS_COMPLETED_SETUP, this.isCompletedSetup).apply();
    }

    public boolean isExclusive() {
        return isExclusive;
    }

    public void setIsExclusive(boolean isExclusive) {
        this.isExclusive = isExclusive;
        prefs.edit().putBoolean(KEY_VBROADCAST_IS_EXCLUSIVE_DEVICE, this.isExclusive).apply();
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
        prefs.edit().putString(KEY_VBROADCAST_VILLAGE_NAME, this.villageName).apply();
    }

    public String getVillageCode() {
        return villageCode;
    }

    public void setVillageCode(String villageCode) {
        this.villageCode = villageCode;
        prefs.edit().putString(KEY_VBROADCAST_VILLAGE_CODE, this.villageCode).apply();
    }

    public String getRegisterAddress() {
        return initialPageAddress;
    }

    public void setRegisterAddress(String registerAddress) {
        this.initialPageAddress = registerAddress;
    }

    public int getWallpaperId() {
        return wallpaperId;
    }

    public void setWallpaperId(int resourceId) {
        this.wallpaperId = resourceId;
        prefs.edit().putInt(KEY_WALLPAPER_RESOURCE_ID, this.wallpaperId).apply();
    }

    public VBroadcastServer getServerInformation() {
        return serverInformation;
    }

    public void setServerInformation(VBroadcastServer serverInformation) {
        String addr = serverInformation.getApiServer();
        if (addr.endsWith("/")) {
            addr = addr.substring(0, addr.length() - 2);
        }
        addr = serverInformation.getDocServer();
        if (addr.endsWith("/")) {
            addr = addr.substring(0, addr.length() - 2);
        }
        addr = serverInformation.getPushInterfaceServer();
        if (addr.endsWith("/")) {
            addr = addr.substring(0, addr.length() - 2);
        }
        this.serverInformation = serverInformation;
        setPrefsJsonObject(KEY_VBROADCAST_SERVER_INFO, this.serverInformation);
    }

    public void setPrefsJsonObject(String key, Object obj) {
        if (obj == null) {
            prefs.edit().putString(key, "").apply();
        } else {
            if (gson == null) {
                gson = new GsonBuilder().create();
            }
            prefs.edit().putString(key, gson.toJson(obj)).apply();
        }
    }

    public Object getPrefsJsonObject(String key, Type tokenType) {
        Object obj = null;
        String savedValue = prefs.getString(key, "");
        if (StringUtils.isEmptyString(savedValue)) {
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
        dest.writeString(this.villageCode);
        dest.writeString(this.villageName);
        dest.writeByte(isExclusive ? (byte) 1 : (byte) 0);
        dest.writeByte(isCompletedSetup ? (byte) 1 : (byte) 0);
        dest.writeInt(this.wallpaperId);
        dest.writeParcelable(this.serverInformation, 0);
        dest.writeString(this.initialPageAddress);
    }

    private LauncherSettings(Parcel in) {
        this.deviceID = in.readString();
        this.villageCode = in.readString();
        this.villageName = in.readString();
        this.isExclusive = in.readByte() != 0;
        this.isCompletedSetup = in.readByte() != 0;
        this.wallpaperId = in.readInt();
        this.serverInformation = in.readParcelable(VBroadcastServer.class.getClassLoader());
        this.initialPageAddress = in.readString();
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
