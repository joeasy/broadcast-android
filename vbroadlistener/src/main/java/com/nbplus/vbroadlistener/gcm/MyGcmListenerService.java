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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nbplus.vbroadlistener.BroadcastWebViewActivity;
import com.nbplus.vbroadlistener.R;
import com.nbplus.vbroadlistener.data.Constants;
import com.nbplus.vbroadlistener.data.PushPayloadData;
import com.nbplus.vbroadlistener.data.VBroadcastServer;
import com.nbplus.vbroadlistener.preference.LauncherSettings;

import org.basdroid.common.PackageUtils;
import org.basdroid.common.StringUtils;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * 2015.07.15
     *
     안녕하세요 고성진 입니다.
     단말 알림창에 보여주어야 할 메시지는 push 규격서에 alert 이라고 하여 보내주고 있습니다.

     GCM
     {
     “registration_ids” : [“111”],
     “data”:{
     “alert”:”새로운 방송이 있습니다.”,
     “messageid” : “123”,
     “payload_data” : “{
     "FROM": "김OO",
     "ADDRESS": "OO 마을",
     "MESSAGE": “http://{방송URL}”,
     "SERVICE_TYPE": “00”,
     “IOT_DEVICE_ID” : “”  *SERVICE_TYPE이 06:원격제어 일 경우만 내려감
     }”
     }
     }

     APNS
     {
     "aps":{
     "sound":"default","alert":"새로운 방송이 있습니다.","badge":45
     },

     “messageid”:”123123”,
     “payload_data”:”{
     "FROM": "김OO",
     "ADDRESS": "OO 마을",
     "MESSAGE": “http://{방송URL}”,
     "SERVICE_TYPE": “00”,
     “IOT_DEVICE_ID” : “”  *SERVICE_TYPE이 06:원격제어 일 경우만 내려감
     }
     }

     */
    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "From: " + from);
        String alert = data.getString(Constants.GCM_DATA_KEY_ALERT);
        Log.d(TAG, "Alert: " + alert);
        String messageId = data.getString(Constants.GCM_DATA_KEY_MESSAGE_ID);
        Log.d(TAG, "MessagID: " + messageId);

        String payload = data.getString(Constants.GCM_DATA_KEY_PAYLOAD);
        Intent pi = null;
        String moveUrl = null;
        int notificationId = 0;

                PushPayloadData payloadData = null;
        try {
            Gson gson = new GsonBuilder().create();
            payloadData = gson.fromJson(payload, new TypeToken<PushPayloadData>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (payloadData == null) {
            Log.d(TAG, "empty push message data !!");
            return;
        }

        String type = payloadData.getServiceType();
        payloadData.setAlertMessage(alert);
        payloadData.setMessageId(messageId);

        /**
         * 2014. 08. 08
         * 비밀번호찾기를 제외한 나머지 알림은 최신 + 이전 알림까지 확인할 수 있도록
         * Notification을 별개로 띄운다.
         */
        switch (type) {
            // 방송알림
            case Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST :
            case Constants.PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST :
            case Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST :
                pi = new Intent(this, BroadcastWebViewActivity.class);
                pi.setAction(Constants.ACTION_SHOW_NOTIFICATION_CONTENTS);

                if (!StringUtils.isEmptyString(payloadData.getMessage()) && Patterns.WEB_URL.matcher(payloadData.getMessage()).matches()) {
                    moveUrl = payloadData.getMessage();
                } else {
                    VBroadcastServer server = LauncherSettings.getInstance(this).getServerInformation();
                    if (server == null || StringUtils.isEmptyString(server.getDocServer())) {
                        moveUrl = LauncherSettings.getInstance(this).getRegisterAddress();
                    } else {
                        moveUrl = server.getDocServer() + Constants.BROADCAST_LIST_CONTEXT_PATH;
                    }
                }

                Log.d(TAG, ">>> Broadcast noti push url = " + moveUrl);
                pi.putExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS, moveUrl);

                //notificationId = Constants.BROADCAST_EVENT_NOTIFICATION_ID;
                try {
                    notificationId += Integer.parseInt(payloadData.getMessageId());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    notificationId = Constants.BROADCAST_EVENT_NOTIFICATION_ID;
                }
                Log.d(TAG, ">>> notificationId = " + notificationId);
                pi.putExtra("xxx", notificationId);

                showNotification(this, notificationId, R.drawable.ic_notification_radio, PackageUtils.getApplicationName(this), payloadData.getAlertMessage(), null, pi);
                break;
            // 긴급호출메시지
            case Constants.PUSH_PAYLOAD_TYPE_EMERGENCY_CALL :
                Log.d(TAG, ">> Constants.PUSH_PAYLOAD_TYPE_EMERGENCY_CALL = " + payloadData.getAlertMessage());

                final String lat = payloadData.getLatitude();
                final String lon = payloadData.getLongitude();

                if (!StringUtils.isEmptyString(lat) && !StringUtils.isEmptyString(lon)) {
                    Log.d(TAG, ">> Emergency geocode lat = " + lat + ", lon = " + lon);
                    pi = new Intent(this, BroadcastWebViewActivity.class);
                    pi.setAction(Constants.ACTION_SHOW_NOTIFICATION_EMERGENCY_CALL);
                    pi.putExtra(Constants.EXTRA_SHOW_NOTIFICATION_EMERGENCY_LAT, lat);
                    pi.putExtra(Constants.EXTRA_SHOW_NOTIFICATION_EMERGENCY_LON, lon);
                }

                pi.putExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS, moveUrl);

                //notificationId = Constants.EMERGENCY_CALL_EVENT_NOTIFICATION_ID;
                try {
                    notificationId += Integer.parseInt(payloadData.getMessageId());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    notificationId = Constants.EMERGENCY_CALL_EVENT_NOTIFICATION_ID;
                }

                if (StringUtils.isEmptyString(payloadData.getMessage())) {
                    showNotification(this, notificationId, R.drawable.ic_notification_noti, PackageUtils.getApplicationName(this), payloadData.getAlertMessage(), null, pi);
                } else {
                    // bigText 사용시
                    Log.d(TAG, ">> Constants.PUSH_PAYLOAD_TYPE_EMERGENCY_CALL bigText = " + payloadData.getMessage());
                    showNotification(this, notificationId, R.drawable.ic_notification_noti, PackageUtils.getApplicationName(this),
                            payloadData.getAlertMessage(), PackageUtils.getApplicationName(this), payloadData.getMessage(), null, null, pi);
                }
                break;
            // 주민투표
            case Constants.PUSH_PAYLOAD_TYPE_INHABITANTS_POLL :
                pi = new Intent(this, BroadcastWebViewActivity.class);
                pi.setAction(Constants.ACTION_SHOW_NOTIFICATION_CONTENTS);
                if (!StringUtils.isEmptyString(payloadData.getMessage()) && Patterns.WEB_URL.matcher(payloadData.getMessage()).matches()) {
                    moveUrl = payloadData.getMessage();
                } else {
                    VBroadcastServer server = LauncherSettings.getInstance(this).getServerInformation();
                    if (server == null || StringUtils.isEmptyString(server.getDocServer())) {
                        moveUrl = LauncherSettings.getInstance(this).getRegisterAddress();
                    } else {
                        moveUrl = server.getDocServer() + Constants.INHABITANT_POLL_LIST_CONTEXT_PATH;
                    }
                }
                //notificationId = Constants.INHABITANT_POLL_EVENT_NOTIFICATION_ID;
                try {
                    notificationId += Integer.parseInt(payloadData.getMessageId());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    notificationId = Constants.INHABITANT_POLL_EVENT_NOTIFICATION_ID;
                }

                pi.putExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS, moveUrl);
                showNotification(this, notificationId, R.drawable.ic_notification_noti, PackageUtils.getApplicationName(this), payloadData.getAlertMessage(), null, pi);
                break;
                // 공동구매
            case Constants.PUSH_PAYLOAD_TYPE_COOPERATIVE_BUYING :
                pi = new Intent(this, BroadcastWebViewActivity.class);
                pi.setAction(Constants.ACTION_SHOW_NOTIFICATION_CONTENTS);
                if (!StringUtils.isEmptyString(payloadData.getMessage()) && Patterns.WEB_URL.matcher(payloadData.getMessage()).matches()) {
                    moveUrl = payloadData.getMessage();
                } else {
                    VBroadcastServer server = LauncherSettings.getInstance(this).getServerInformation();
                    if (server == null || StringUtils.isEmptyString(server.getDocServer())) {
                        moveUrl = LauncherSettings.getInstance(this).getRegisterAddress();
                    } else {
                        moveUrl = server.getDocServer() + Constants.COOPERATIVE_BUYING_LIST_CONTEXT_PATH;
                    }
                }
                pi.putExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS, moveUrl);
                //notificationId = Constants.COOPERATIVE_BUYING_EVENT_NOTIFICATION_ID;
                try {
                    notificationId += Integer.parseInt(payloadData.getMessageId());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    notificationId = Constants.COOPERATIVE_BUYING_EVENT_NOTIFICATION_ID;
                }
                showNotification(this, notificationId, R.drawable.ic_notification_noti, PackageUtils.getApplicationName(this), payloadData.getAlertMessage(), null, pi);
                break;
            // IOT DEVICE 제어(스마트홈 서비스)
            case Constants.PUSH_PAYLOAD_TYPE_IOT_DEVICE_CONTROL :
                // 원격제어는무시한다.
                break;
            // 관리자용이란다.
            case Constants.PUSH_PAYLOAD_TYPE_PUSH_NOTIFICATION :
                // 브라우저실행시...
//                    pi = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(payloadData.getMessage()));
//                    showNotification(context, Constants.SYSTEM_ADMIN_NOTIFICATION_ID, PackageUtils.getApplicationName(context), payloadData.getAlertMessage(), null, pi);

                pi.putExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS, moveUrl);
                //notificationId = Constants.SYSTEM_ADMIN_NOTIFICATION_ID;
                try {
                    notificationId += Integer.parseInt(payloadData.getMessageId());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    notificationId = Constants.SYSTEM_ADMIN_NOTIFICATION_ID;
                }
                // bigText 사용시
                showNotification(this, notificationId, R.drawable.ic_notification_noti, PackageUtils.getApplicationName(this),
                        payloadData.getAlertMessage(), PackageUtils.getApplicationName(this), payloadData.getMessage(), null, null, null);
                break;
            // 비밀번호찾기
            case Constants.PUSH_PAYLOAD_TYPE_FIND_PASSWORD :
                pi = new Intent(this, BroadcastWebViewActivity.class);
                pi.setAction(Constants.ACTION_SHOW_NOTIFICATION_CONTENTS);
                if (!StringUtils.isEmptyString(payloadData.getMessage()) && Patterns.WEB_URL.matcher(payloadData.getMessage()).matches()) {
                    moveUrl = payloadData.getMessage();
                } else {
                    Log.e(TAG, "wrong password find .... url ");
                }
                pi.putExtra(Constants.EXTRA_SHOW_NOTIFICATION_CONTENTS, moveUrl);
                showNotification(this, Constants.PW_FIND_NOTIFICATION_ID, R.drawable.ic_notification_noti, PackageUtils.getApplicationName(this), payloadData.getAlertMessage(), null, pi);
                break;

            default:
                Log.d(TAG, "Unknown push payload type !!!");
                break;
        }

        VBroadcastServer server = LauncherSettings.getInstance(this).getServerInformation();
        if (server != null && !StringUtils.isEmptyString(server.getApiServer())) {
            SendGcmResultTask task = new SendGcmResultTask();
            if (task != null) {
                task.setSendGcmResultData(this, server.getApiServer() + Constants.API_GCM_SEND_RESULT_CONTEXT_PATH, payloadData.getMessageId());
                task.execute();
            }
        }
    }
    // [END receive_message]

    private void playNotificationAlarm(Context context, int textResId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSound(soundUri);
        notificationManager.notify(Constants.PUSH_NOTIFICATION_ALARM_ID, builder.build());

        Toast.makeText(context, textResId, Toast.LENGTH_SHORT).show();
    }

    private void showNotification(Context context, int notificationId, int smallIconId, String title, String contentText, String ticker, Intent intent) {
        showNotification(context, notificationId, smallIconId, title, contentText, null, null, null, ticker, intent);
    }
    private void showNotification(Context context, int notificationId, int smallIconId, String title, String contentText, String bigTitle, String bigContentText, String summaryText, String ticker, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSound(soundUri);

        if (smallIconId == 0) {
            builder.setSmallIcon(R.mipmap.ic_launcher);
        } else {
            builder.setSmallIcon(smallIconId);
        }
        builder.setWhen(System.currentTimeMillis());
        //builder.setNumber(10);

        if (!StringUtils.isEmptyString(ticker)) {
            builder.setTicker(ticker);
        }

        if (StringUtils.isEmptyString(title)) {
            builder.setContentTitle(PackageUtils.getApplicationName(context));
        } else {
            builder.setContentTitle(title);
        }
        builder.setContentText(contentText);
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        builder.setAutoCancel(true);

        // big title and text
        if (!StringUtils.isEmptyString(bigTitle) && !StringUtils.isEmptyString(bigContentText)) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle(builder);
            if (!StringUtils.isEmptyString(summaryText)) {
                style.setSummaryText(summaryText);
            }
            style.setBigContentTitle(bigTitle);
            style.bigText(bigContentText);

            builder.setStyle(style);
        }

        if (intent != null) {
            intent.setFlags(intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
        }

        notificationManager.notify(notificationId, builder.build());
    }
}
