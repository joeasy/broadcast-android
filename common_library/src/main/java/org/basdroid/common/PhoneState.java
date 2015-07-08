package org.basdroid.common;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Created by basagee on 2015. 4. 30..
 */
public class PhoneState {
    public static final String TAG = "PhoneState";

    public static boolean hasPhoneCallAbility(Context context) {
        if (((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
            // no phone
            return false;
        } else {
            if (((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number() == null) {
                // no phone
                return false;
            }
        }

        return true;
    }

    public static String getLineNumber1(Context context) {
        TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony.getLine1Number();
    }
}
