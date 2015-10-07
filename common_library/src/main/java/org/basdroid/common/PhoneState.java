/*
 * Copyright (c) 2015. Basagee Yun. (www.basagee.tk)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
