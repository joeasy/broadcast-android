package com.nbplus.push.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by basagee on 2015. 6. 30..
 */
public class PushInterfaceData {
    @SerializedName("RT")
    public String resultCode;
    @SerializedName("RT_MSG")
    public String resultMessage;

    @SerializedName("SESSION_KEY")
    public String sessionKey;
    @SerializedName("DEVICE_AUTH_KEY")
    public String deviceAuthKey;
    @SerializedName("CONN_IP")
    public String interfaceServerAddress;
    @SerializedName("CONN_PORT")
    public String interfaceServerPort;
    @SerializedName("KEEP_ALIVE_PERIOD")
    public String keepAliveSeconds;
}
