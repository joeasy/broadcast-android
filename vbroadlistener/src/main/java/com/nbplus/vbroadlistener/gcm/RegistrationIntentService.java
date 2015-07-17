/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nbplus.vbroadlistener.gcm;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.nbplus.vbroadlistener.data.BaseApiResult;
import com.nbplus.vbroadlistener.data.Constants;
import com.nbplus.vbroadlistener.data.VBroadcastServer;
import com.nbplus.vbroadlistener.preference.LauncherSettings;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.StringUtils;
import org.basdroid.volley.GsonRequest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String action = intent.getAction();
        String token = null;
        if (Constants.REGISTER_GCM.equals(action) || Constants.UPDATE_GCM.equals(action)) {
            try {
                // In the (unlikely) event that multiple refresh operations occur simultaneously,
                // ensure that they are processed sequentially.
                synchronized (TAG) {

                    // [START register_for_gcm]
                    // Initially this call goes out to the network to retrieve the token, subsequent calls
                    // are local.
                    // [START get_token]
                    LauncherSettings.getInstance(this).setGcmToken("");
                    LauncherSettings.getInstance(this).setGcmSentToServerStatus(false);
                    LauncherSettings.getInstance(this).setGcmRegisteredStatus(false);

                    InstanceID instanceID = InstanceID.getInstance(this);
                    token = instanceID.getToken(Constants.GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    // [END get_token]
                    Log.i(TAG, "GCM Registration Token: " + token);

                    /**
                     * 2015.07.17
                     * 마을방송이 웹앱과 연동되는 부분도 있고... 서버쪽에서 잡은 시나리오 때문에
                     * 초기 등록은 웹앱이 담당하고 이후에 변동이 되는경우는 Native에서 전달한다.
                     */
                    if (Constants.UPDATE_GCM.equals(action)) {
                        boolean result = sendRegistrationToServer(token);
                        LauncherSettings.getInstance(this).setGcmSentToServerStatus(result);
                        if (!result) {
                            Log.i(TAG, "setAlarm() = 5min");
                            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                            Intent i = new Intent(this, RegistrationIntentService.class);
                            i.setAction(Constants.UPDATE_GCM);
                            PendingIntent pIntent = PendingIntent.getService(this, 0, i, 0);

                            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 60 * 5 * 1000, pIntent);
                            startService(i);
                        }
                    } else {
                        LauncherSettings.getInstance(this).setGcmSentToServerStatus(true);
                        // Subscribe to topic channels
                        subscribeTopics(token);
                    }

                    // You should store a boolean that indicates whether the generated token has been
                    // sent to your server. If the boolean is false, send the token to your server,
                    // otherwise your server should have already received the token.
                    LauncherSettings.getInstance(this).setGcmRegisteredStatus(true);
                    LauncherSettings.getInstance(this).setGcmToken(token);
                    // [END register_for_gcm]
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed to complete token refresh", e);
                // If an exception happens while fetching the new token or updating our registration data
                // on a third-party server, this ensures that we'll attempt the update at a later time.
                LauncherSettings.getInstance(this).setGcmSentToServerStatus(false);
                if (!StringUtils.isEmptyString(token)) {
                    LauncherSettings.getInstance(this).setGcmToken(token);
                    LauncherSettings.getInstance(this).setGcmRegisteredStatus(true);
                }
            }
            // Notify UI that registration has completed, so the progress indicator can be hidden.
            Intent registrationComplete = new Intent(Constants.REGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        } else if (Constants.UNREGISTER_GCM.equals(action)) {
            try {
                // In the (unlikely) event that multiple refresh operations occur simultaneously,
                // ensure that they are processed sequentially.
                synchronized (TAG) {

                    // [START register_for_gcm]
                    // Initially this call goes out to the network to retrieve the token, subsequent calls
                    // are local.
                    // [START get_token]
                    InstanceID instanceID = InstanceID.getInstance(this);
                    token = instanceID.getToken(Constants.GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    // TODO: Implement this method to send any registration to your app's servers.
                    sendUnRegistrationToServer(token);

                    // Subscribe to topic channels
                    unSubscribeTopics(token);

                    instanceID.deleteToken(Constants.GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                    // [END get_token]
                    Log.i(TAG, "GCM Registration Token: " + token);

                    // You should store a boolean that indicates whether the generated token has been
                    // sent to your server. If the boolean is false, send the token to your server,
                    // otherwise your server should have already received the token.
                    LauncherSettings.getInstance(this).setGcmSentToServerStatus(false);
                    LauncherSettings.getInstance(this).setGcmRegisteredStatus(false);
                    LauncherSettings.getInstance(this).setGcmToken("");
                    // [END register_for_gcm]
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed to complete token refresh", e);
                // If an exception happens while fetching the new token or updating our registration data
                // on a third-party server, this ensures that we'll attempt the update at a later time.
                LauncherSettings.getInstance(this).setGcmSentToServerStatus(false);
                LauncherSettings.getInstance(this).setGcmRegisteredStatus(false);
                LauncherSettings.getInstance(this).setGcmToken("");
            }
            // Notify UI that registration has completed, so the progress indicator can be hidden.
            Intent registrationComplete = new Intent(Constants.UNREGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        }
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private boolean sendRegistrationToServer(String token) {
        boolean result = false;
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        BaseApiResult response = null;

        VBroadcastServer serverInfo = LauncherSettings.getInstance(this).getServerInformation();
        if (serverInfo == null || StringUtils.isEmptyString(serverInfo.getApiServer())) {
            Log.d(TAG, ">> I can't find api server information...");
            return result;
        }
        String url = serverInfo.getApiServer() + Constants.API_GCM_TOKEN_UPDATE_CONTEXT_PATH;
        Uri.Builder builder = Uri.parse(url).buildUpon();
        url = builder.toString();

        if (StringUtils.isEmptyString(LauncherSettings.getInstance(this).getDeviceID())) {
            String deviceID = DeviceUtils.getDeviceIdByMacAddress(this);
            LauncherSettings.getInstance(this).setDeviceID(deviceID);
        }

        String strRequestBody = String.format("{\"DEVICE_ID\" : \"%s\", \"PUSH_TOKEN\" : \"%s\", \"APP_ID\" : \"%s\"}",
                LauncherSettings.getInstance(this).getDeviceID(),
                token,
                getApplicationContext().getPackageName());
        RequestFuture<BaseApiResult> future = RequestFuture.newFuture();

        GsonRequest request = new GsonRequest(Request.Method.POST, url, strRequestBody, BaseApiResult.class, future, future);
        requestQueue.add(request);

        try {
            response = future.get(); // this will block (forever)
            if (Constants.API_RESP_OK.equals(response.getResultCode())) {
                result = true;
            }
            Log.d(TAG, "GCM token update send rt =" + response.getResultCode());
        } catch (InterruptedException e) {
            // exception handling
            e.printStackTrace();
        } catch (ExecutionException e) {
            // exception handling
            e.printStackTrace();
        }
        return result;
    }

    private void sendUnRegistrationToServer(String token) {
        // Add custom implementation, as needed.
    }
    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        for (String topic : TOPICS) {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [START subscribe_topics]
    private void unSubscribeTopics(String token) throws IOException {
        for (String topic : TOPICS) {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            pubSub.unsubscribe(token, "/topics/" + topic);
        }
    }
    // [END subscribe_topics]

}
