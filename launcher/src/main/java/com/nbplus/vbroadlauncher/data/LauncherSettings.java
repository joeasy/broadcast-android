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
import com.nbplus.vbroadlauncher.RadioActivity;
import com.nbplus.vbroadlauncher.service.SendEmergencyCallTask;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.StringUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by basagee on 2015. 5. 18..
 */
public class LauncherSettings {

    //TODO :: iot test
    public String getTestIoTDevices() {
        return prefs.getString("KEY_TEST_IOT_DEVICES", "");
    }

    public void setTestIoTDevices(String testIoTDevices) {
        prefs.edit().putString("KEY_TEST_IOT_DEVICES", testIoTDevices).apply();
    }

    // 맥어드레스 기반 40바이트 디바이스 UUID
    String deviceID;
    // 날씨정보에 사용할 로케이션정보
    Location preferredUserLocation;
    GeocodeData yahooGeocode;

    // 마을정보
    @SerializedName("vill_code")
    String villageCode;
    @SerializedName("vill_name")
    String villageName;
    @SerializedName("is_exclusive")
    boolean isExclusive;
    boolean isVillageNameChanged = false;

    boolean isCompletedSetup;

    boolean isCheckedTTSEngine;

    boolean isOutdoorMode = false;

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
    // 숏컷정보
    @SerializedName("app_main_shortcuts")
    ArrayList<ShortcutData> launcherMainShortcuts = new ArrayList<ShortcutData>();;

    @SerializedName("register_address")

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

    public static int[] landWallpaperResource;
    public static int[] portWallpaperResource;
    private int wallpaperId = -1;

    private Context context;
    private SharedPreferences prefs;
    private Gson gson;

    private LauncherSettings(Context context) {
        this.context = context;

        String prefName = context.getApplicationContext().getPackageName() + "_preferences";
        this.prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);

        // load from preferences..
        this.deviceID = prefs.getString(KEY_VBROADCAST_DEVICE_ID, "");
        if (StringUtils.isEmptyString(this.deviceID)) {
            String deviceID = DeviceUtils.getDeviceIdByMacAddress(context);
            prefs.edit().putString(KEY_VBROADCAST_DEVICE_ID, deviceID).apply();
            this.deviceID = deviceID;
        }

        this.isCompletedSetup = prefs.getBoolean(KEY_VBROADCAST_IS_COMPLETED_SETUP, false);
        this.isExclusive = prefs.getBoolean(KEY_VBROADCAST_IS_EXCLUSIVE_DEVICE, false);
        this.villageCode = prefs.getString(KEY_VBROADCAST_VILLAGE_CODE, "");
        this.villageName = prefs.getString(KEY_VBROADCAST_VILLAGE_NAME, "");
        this.isCheckedTTSEngine = prefs.getBoolean(KEY_IS_CHECKED_TTS, false);
        int wallpaperId = prefs.getInt(KEY_WALLPAPER_RESOURCE_ID, -1);

        this.isOutdoorMode = prefs.getBoolean(KEY_VBROADCAST_IS_OUTDOOR_MODE, false);

        landWallpaperResource = new int[]{ R.drawable.ic_bg_main_land };
        portWallpaperResource = new int[]{ R.drawable.ic_bg_main_port };

        if (wallpaperId <= 0) {
            Random oRandom = new Random();
            wallpaperId = oRandom.nextInt(landWallpaperResource.length);

            setWallpaperId(wallpaperId);
        } else {
            this.wallpaperId = wallpaperId;
        }

        /**
         * 숏컷은 단말에서 유지하기로해서..
         * 미리 고정된 값으로넣는다.
         */
        setupLauncherMainShortcuts(context);
        setupLauncherShortcuts(context);//(ArrayList<ShortcutData>)getPrefsJsonObject(KEY_VBROADCAST_SHORTCUT, new TypeToken<ArrayList<ShortcutData>>(){}.getType());
        this.preferredUserLocation = (Location)getPrefsJsonObject(KEY_VBROADCAST_PREFERRED_LOCATION, new TypeToken<Location>(){}.getType());
        this.serverInformation = (VBroadcastServer)getPrefsJsonObject(KEY_VBROADCAST_SERVER_INFO, new TypeToken<VBroadcastServer>(){}.getType());
        this.yahooGeocode = (GeocodeData)getPrefsJsonObject(KEY_VBROADCAST_YAHOO_GEOCODE, new TypeToken<GeocodeData>(){}.getType());
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

    public boolean isVillageNameChanged() {
        return isVillageNameChanged;
    }

    public void setIsVillageNameChanged(boolean isVillageNameChanged) {
        this.isVillageNameChanged = isVillageNameChanged;
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
        isVillageNameChanged = true;
        prefs.edit().putString(KEY_VBROADCAST_VILLAGE_NAME, this.villageName).apply();
    }

    public String getVillageCode() {
        return villageCode;
    }

    public void setVillageCode(String villageCode) {
        this.villageCode = villageCode;
        prefs.edit().putString(KEY_VBROADCAST_VILLAGE_CODE, this.villageCode).apply();
    }

    public int getWallpaperId() {
        return wallpaperId;
    }

    public void setWallpaperId(int resourceId) {
        this.wallpaperId = resourceId;
        prefs.edit().putInt(KEY_WALLPAPER_RESOURCE_ID, this.wallpaperId).apply();
    }

    public boolean isOutdoorMode() {
        return isOutdoorMode;
    }

    public void setIsOutdoorMode(boolean isOutdoorMode) {
        this.isOutdoorMode = isOutdoorMode;
        prefs.edit().putBoolean(KEY_VBROADCAST_IS_OUTDOOR_MODE, this.isOutdoorMode).apply();
    }

    /**
     * 숏컷은 단말에서 유지하기로해서..
     * 미리 고정된 값으로넣는다.
     */
    public ArrayList<ShortcutData> getLauncherShortcuts() {
        return launcherShortcuts;
    }

    public ArrayList<ShortcutData> getLauncherMainShortcuts() {
        return launcherMainShortcuts;
    }

    public void setupLauncherMainShortcuts(Context context) {
        launcherMainShortcuts.clear();
        ShortcutData data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                R.string.shortcut_btn_show_broadcast,
                context.getResources().getString(R.string.shortcut_addr_show_broadcast),
                R.drawable.ic_menu_01,
                R.drawable.ic_menu_main_01_selector,
                0,
                null,
                new String[]{Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST, Constants.PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST, Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST});
        launcherMainShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_INTERFACE_SERVER,
                R.string.shortcut_btn_call_emergency,
                context.getResources().getString(R.string.shortcut_addr_call_emergency),
                R.drawable.ic_menu_02,
                R.drawable.ic_menu_main_02_selector,
                0,
                SendEmergencyCallTask.class);
        launcherMainShortcuts.add(data);
    }

    public void setupLauncherShortcuts(Context context) {
        launcherShortcuts.clear();
        ShortcutData data = new ShortcutData(Constants.SHORTCUT_TYPE_NATIVE_INTERFACE,
                R.string.shortcut_btn_radio,
                context.getResources().getString(R.string.shortcut_addr_radio),
                R.drawable.ic_menu_03,
                R.drawable.ic_menu_shortcut_01_selector,
                0,
                RadioActivity.class);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                R.string.shortcut_btn_participation,
                context.getResources().getString(R.string.shortcut_addr_participation),
                R.drawable.ic_menu_04,
                R.drawable.ic_menu_shortcut_02_selector,
                0,
                null,
                new String[]{Constants.PUSH_PAYLOAD_TYPE_INHABITANTS_POLL, Constants.PUSH_PAYLOAD_TYPE_COOPERATIVE_BUYING});
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                R.string.shortcut_btn_additional_function,
                context.getResources().getString(R.string.shortcut_addr_additional_function),
                R.drawable.ic_menu_05,
                R.drawable.ic_menu_shortcut_03_selector,
                0,
                null);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                R.string.shortcut_btn_official_address,
                context.getResources().getString(R.string.shortcut_addr_official_address),
                R.drawable.ic_menu_06,
                R.drawable.ic_menu_shortcut_04_selector,
                0,
                null);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                R.string.shortcut_btn_smart_home,
                context.getResources().getString(R.string.shortcut_addr_smart_home),
                R.drawable.ic_menu_07,
                R.drawable.ic_menu_shortcut_05_selector,
                0,
                null);
        launcherShortcuts.add(data);
        data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                R.string.shortcut_btn_my_information,
                context.getResources().getString(R.string.shortcut_addr_my_information),
                R.drawable.ic_menu_08,
                R.drawable.ic_menu_shortcut_06_selector,
                0,
                null);
        launcherShortcuts.add(data);
    }

    public VBroadcastServer getServerInformation() {
        return serverInformation;
    }

    public void setServerInformation(VBroadcastServer serverInformation) {
        String addr = serverInformation.getApiServer();
        if (!StringUtils.isEmptyString(addr) && addr.endsWith("/")) {
            addr = addr.substring(0, addr.length() - 2);
        }
        addr = serverInformation.getDocServer();
        if (!StringUtils.isEmptyString(addr) && addr.endsWith("/")) {
            addr = addr.substring(0, addr.length() - 2);
        }
        addr = serverInformation.getPushInterfaceServer();
        if (!StringUtils.isEmptyString(addr) && addr.endsWith("/")) {
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
        prefs.edit().putString(KEY_VBROADCAST_DEVICE_ID, deviceID).apply();
    }

    public Location getPreferredUserLocation() {
        return preferredUserLocation;
    }

    public void setPreferredUserLocation(Location preferredUserLocation) {
        this.preferredUserLocation = preferredUserLocation;
        setPrefsJsonObject(KEY_VBROADCAST_PREFERRED_LOCATION, this.preferredUserLocation);
    }

    public GeocodeData getGeocodeData() {
        return yahooGeocode;
    }

    public void setGeocodeData(GeocodeData geocode) {
        this.yahooGeocode = geocode;
        setPrefsJsonObject(KEY_VBROADCAST_YAHOO_GEOCODE, this.yahooGeocode);
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

    /**
     * 재생중인 방송 priority 에 따라 종료여부 판단을위해..
     * 실시간방송이 재생중인 상태에서 일반방송이나 문자방송이 오면 무시하기 위함.
     */
    private String currentPlayingBroadcastType = null;

    public void setCurrentPlayingBroadcastType(String type) {
        currentPlayingBroadcastType = type;
    }

    public String getCurrentPlayingBroadcastType() {
        return currentPlayingBroadcastType;
    }
}
