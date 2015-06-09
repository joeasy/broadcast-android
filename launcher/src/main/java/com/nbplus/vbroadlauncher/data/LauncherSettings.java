package com.nbplus.vbroadlauncher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.nbplus.vbroadlauncher.R;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class LauncherSettings implements Parcelable {

    // 맥어드레스 기반 40바이트 디바이스 UUID
    String deviceID;
    // 날씨정보에 사용할 로케이션정보
    Location preferredUserLocation;

    // 마을정보
    @SerializedName("vill_code")
    String villageCode;
    @SerializedName("vill_name")
    String villageName;
    @SerializedName("is_exclusive")
    boolean isExclusive;

    boolean isCompletedSetup;

    int wallpagerResource;

    // 서버정보
    @SerializedName("svc_domain")
    VBroadcastServer serverInformation;
    /**
     * 숏컷은 단말에서 유지하기로해서..
     * 미리 고정된 값으로넣는다.
     */
    // 숏컷정보
    @SerializedName("app_shortcuts")
    ArrayList<ShortcutData> launcherShortcuts = new ArrayList<ShortcutData>();;

    @SerializedName("register_address")
    String registerAddress = "http://175.207.46.128:8001/test/test.html";

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
        setupLauncherShortcuts(context);//(ArrayList<ShortcutData>)getPrefsJsonObject(KEY_VBROADCAST_SHORTCUT, new TypeToken<ArrayList<ShortcutData>>(){}.getType());
        this.preferredUserLocation = (Location)getPrefsJsonObject(KEY_VBROADCAST_PREFERRED_LOCATION, new TypeToken<Location>(){}.getType());
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

    public String getRegisterAddress() {
        return registerAddress;
    }

    public void setRegisterAddress(String registerAddress) {
        this.registerAddress = registerAddress;
    }

    public int getWallpagerResource() {
        return wallpagerResource;
    }

    public void setWallpagerResource(int wallpagerResource) {
        this.wallpagerResource = wallpagerResource;
    }

    /**
     * 숏컷은 단말에서 유지하기로해서..
     * 미리 고정된 값으로넣는다.
     */
    public ArrayList<ShortcutData> getLauncherShortcuts() {
        return launcherShortcuts;
    }

    public void setupLauncherShortcuts(Context context) {
        /**
        <!--shortcut-->
        <string name="shortcut_btn_emergency_call">Emergency Call</string>
        <string name="shortcut_btn_new_broadcast">New Broadcast</string>
        <string name="shortcut_btn_new_participation">New Participation</string>
        <string name="shortcut_btn_my_settings">My Settings</string>
        <string name="shortcut_btn_additional_function">Additional Function</string>
        <string name="shortcut_btn_radio">Internet Radio</string>
        <string name="shortcut_addr_emergency_call">/test/test.html</string>
        <string name="shortcut_addr_new_broadcast">/test/test.html</string>
        <string name="shortcut_addr_new_participation">/test/test.html</string>
        <string name="shortcut_addr_my_settings">/test/test.html</string>
        <string name="shortcut_addr_additional_function">/test/test.html</string>
        <string name="shortcut_addr_radio">/test/test.html</string>
        */
        launcherShortcuts.clear();
        ShortcutData data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_INTERFACE_SERVER,
                context.getResources().getString(R.string.shortcut_btn_emergency_call),
                context.getResources().getString(R.string.shortcut_addr_emergency_call),
                R.drawable.ic_launcher,
                R.drawable.launcher_shortcut_background_blue);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                context.getResources().getString(R.string.shortcut_btn_new_broadcast),
                context.getResources().getString(R.string.shortcut_addr_new_broadcast),
                R.drawable.ic_launcher,
                R.drawable.launcher_shortcut_background_black);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                context.getResources().getString(R.string.shortcut_btn_new_participation),
                context.getResources().getString(R.string.shortcut_addr_new_participation),
                R.drawable.ic_launcher,
                R.drawable.launcher_shortcut_background_blue);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                context.getResources().getString(R.string.shortcut_btn_my_settings),
                context.getResources().getString(R.string.shortcut_addr_my_settings),
                R.drawable.ic_launcher,
                R.drawable.launcher_shortcut_background_black);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                context.getResources().getString(R.string.shortcut_btn_additional_function),
                context.getResources().getString(R.string.shortcut_addr_additional_function),
                R.drawable.ic_launcher,
                R.drawable.launcher_shortcut_background_blue);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_NATIVE_INTERFACE,
                context.getResources().getString(R.string.shortcut_btn_radio),
                context.getResources().getString(R.string.shortcut_addr_radio),
                R.drawable.ic_launcher,
                R.drawable.launcher_shortcut_background_black);
        launcherShortcuts.add(data);
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

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
        prefs.edit().putString(KEY_VBROADCAST_DEVICE_ID, deviceID).commit();
    }

    public Location getPreferredUserLocation() {
        return preferredUserLocation;
    }

    public void setPreferredUserLocation(Location preferredUserLocation) {
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
        dest.writeInt(this.wallpagerResource);
        dest.writeParcelable(this.serverInformation, 0);
        dest.writeSerializable(this.launcherShortcuts);
        dest.writeString(this.registerAddress);
    }

    private LauncherSettings(Parcel in) {
        this.deviceID = in.readString();
        this.preferredUserLocation = in.readParcelable(Location.class.getClassLoader());
        this.villageCode = in.readString();
        this.villageName = in.readString();
        this.isExclusive = in.readByte() != 0;
        this.isCompletedSetup = in.readByte() != 0;
        this.wallpagerResource = in.readInt();
        this.serverInformation = in.readParcelable(VBroadcastServer.class.getClassLoader());
        this.launcherShortcuts = (ArrayList<ShortcutData>) in.readSerializable();
        this.registerAddress = in.readString();
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
