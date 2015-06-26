package com.nbplus.vbroadlistener.data;

/**
 * Created by basagee on 2015. 6. 15..
 */
public class Constants {

    // GCM Sender ID
    public static final String GCM_SENDER_ID = "356837636067";

    public static final String GCM_REGISTERED_STATUS = "gcmRegisteredStatus";
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String GCM_TOKEN_VALUE = "gcmTokenValue";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String UNREGISTRATION_COMPLETE = "unRegistrationComplete";

    public static final int HANDLER_MESSAGE_UPDATE_GCM_DEVICE_TOKEN = 1001;
    public static final int HANDLER_MESSAGE_UNREGISTER_GCM = 1002;

    public static final String REGISTER_GCM = "com.nbplus.vbroadcast.action.REGISTER_GCM";
    public static final String UNREGISTER_GCM = "com.nbplus.vbroadcast.action.UNREGISTER_GCM";

}
