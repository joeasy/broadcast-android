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
    public static final String ACTION_SHOW_NOTIFICATION_CONTENTS = "com.nbplus.vbroadcast.action.SHOW_NOTIFICATION_CONTENTS";
    public static final String EXTRA_SHOW_NOTIFICATION_CONTENTS = "EXTRA_SHOW_NOTIFICATION_CONTENTS";

    public static final int START_ACTIVITY_REQUEST_CHECK_TTS_DATA = 1001;

    // push notification ID
    public static final int PUSH_NOTIFICATION_ALARM_ID = 1001;
    public static final int RADIO_NOTIFICATION_ID = 2001;
    public static final int PW_FIND_NOTIFICATION_ID = 2002;
    public static final int SYSTEM_ADMIN_NOTIFICATION_ID = 2003;
}
