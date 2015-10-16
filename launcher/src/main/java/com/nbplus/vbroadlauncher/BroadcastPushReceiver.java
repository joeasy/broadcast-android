/*
 * Copyright (c) 2015. NB Plus (www.nbplus.co.kr)
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

package com.nbplus.vbroadlauncher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nbplus.iotlib.IoTInterface;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.data.PushMessageData;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.PushPayloadData;

import org.basdroid.common.PackageUtils;
import org.basdroid.common.StringUtils;

/**
 * 사용안함.
 */
public class BroadcastPushReceiver extends BroadcastReceiver {
    private static final String TAG = BroadcastPushReceiver.class.getName();

    public BroadcastPushReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        Intent pi;

        String action = intent.getAction();
        if (PushConstants.ACTION_PUSH_STATUS_CHANGED.equals(action)) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } else if (PushConstants.ACTION_PUSH_MESSAGE_RECEIVED.equals(action)) {
            Log.d(TAG, "Receive.. broadcast ACTION_PUSH_MESSAGE_RECEIVED from push service. re-direct to activity!!!");

            PushMessageData data = null;
            try {
                data = (PushMessageData) intent.getParcelableExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA);
                if (data == null || StringUtils.isEmptyString(data.getPayload())) {
                    Log.d(TAG, "empty push message string !!");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            PushPayloadData payloadData = null;
            try {
                Gson gson = new GsonBuilder().create();
                payloadData = gson.fromJson(data.getPayload(), new TypeToken<PushPayloadData>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (payloadData == null) {
                Log.d(TAG, "empty push message data !!");
                return;
            }

            String type = payloadData.getServiceType();
            Log.d(TAG, "HANDLER_MESSAGE_PUSH_MESAGE_RECEIVED received type = " + type + ", messageId = " + payloadData.getMessageId());
            payloadData.setAlertMessage(data.getAlert());
            switch (type) {
                // 방송알림
                case Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST :
                case Constants.PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST :
                case Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST :
                    boolean isOutdoor = LauncherSettings.getInstance(context).isOutdoorMode();
                    playNotificationAlarm(context, R.string.notification_broadcast_push);
                    if (isOutdoor) {        // 외출모드에서는 재생하지 않음.
                        Log.d(TAG, "Broadcast notification.. isOutdoor mode... ");
                        pi = new Intent();
                        pi.setAction(action);
                        pi.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED));
                        pi.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(pi);
                    } else {
                        if (Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST.equals(type) || Constants.PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST.equals(type)) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                Log.d(TAG, ">> This device version code = " + Build.VERSION.SDK_INT + ", not supported version !!");
                                Toast.makeText(context, R.string.notification_broadcast_not_support, Toast.LENGTH_SHORT);
                                break;
                            }
                        }
                        /*boolean useServiceChatHead = false;

                        if (useServiceChatHead) {
                            i = new Intent(context, RealtimeBroadcastProxyActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.setAction(action);
                            i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                            context.startActivity(i);
                        } else*/

                        String playingType = LauncherSettings.getInstance(context).getCurrentPlayingBroadcastType();

                        if (StringUtils.isEmptyString(playingType) ||
                                !Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST.equals(playingType)) {
                            // 재생중인것이 없거나... 실시간이 아닌경우. 나중에받은것이 실행
                            pi = new Intent(context, RealtimeBroadcastActivity.class);
                            pi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            pi.setAction(action);
                            pi.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                            pi.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, System.currentTimeMillis());

                            Log.d(TAG, "1. sendBroadcast() >> ACTION_PUSH_MESSAGE_RECEIVED : idx = " + pi.getLongExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, -1));
                            // 서버에서 몇십ms  단위로거의 동일 시간에 전달되는 경우 먼저온 푸시의 액티비티가 생성되기도전에
                            // broadcast 만전달될 수있다.
                            // 액티비티가 생성된 이후에 던지자.
                            //LocalBroadcastManager.getInstance(context).sendBroadcast(pi);

                            try {
                                //Thread.sleep(30);
                                context.startActivity(pi);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            // 재생중인 방송이 실시간인데... 새로운 요청도 실시간이면.. 새로운 요청이 우선.
                            if (Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST.equals(playingType) && Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST.equals(type)) {
                                pi = new Intent(context, RealtimeBroadcastActivity.class);
                                pi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                pi.setAction(action);
                                pi.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                                pi.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, System.currentTimeMillis());

                                Log.d(TAG, "2. sendBroadcast() >> ACTION_PUSH_MESSAGE_RECEIVED : idx = " + pi.getLongExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, -1));
                                // 서버에서 몇십ms  단위로거의 동일 시간에 전달되는 경우 먼저온 푸시의 액티비티가 생성되기도전에
                                // broadcast 만전달될 수있다.
                                // 액티비티가 생성된 이후에 던지자.
                                //LocalBroadcastManager.getInstance(context).sendBroadcast(pi);
                                try {
                                    //Thread.sleep(30);
                                    context.startActivity(pi);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                // 이미 우선순위높은 방송이 재생중이다.
                                Log.d(TAG, "이미 우선순위높은 방송이 재생중이다.");
                                Toast.makeText(context, payloadData.getAlertMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    break;
                // 긴급호출메시지
                case Constants.PUSH_PAYLOAD_TYPE_EMERGENCY_CALL :
                    break;
                // 주민투표
                case Constants.PUSH_PAYLOAD_TYPE_INHABITANTS_POLL :
                    // 공동구매
                case Constants.PUSH_PAYLOAD_TYPE_COOPERATIVE_BUYING :
                    int strId = Constants.PUSH_PAYLOAD_TYPE_INHABITANTS_POLL.equals(payloadData.getServiceType())
                            ? R.string.notification_inhabitant_push : R.string.notification_cooperative_buying_push;
                    playNotificationAlarm(context, strId);

                    pi = new Intent();
                    pi.setAction(action);
                    pi.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED));
                    pi.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                    Log.d(TAG, "3. sendBroadcast() >> ACTION_PUSH_MESSAGE_RECEIVED : idx = " + 0);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(pi);
                    break;
                // IOT DEVICE 제어(스마트홈 서비스)
                case Constants.PUSH_PAYLOAD_TYPE_IOT_DEVICE_CONTROL :
                    Log.d(TAG, "startService >> ACTION_SEND_IOT_COMMAND");
                    IoTInterface.getInstance().controlDevice(payloadData.getIotControlDeviceId(), payloadData.getMessage());
                    break;
                // PUSH_PAYLOAD_TYPE_PUSH_NOTIFICATION
                case Constants.PUSH_PAYLOAD_TYPE_PUSH_NOTIFICATION :
                    // 브라우저실행시...
//                    pi = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(payloadData.getMessage()));
//                    showNotification(context, Constants.SYSTEM_ADMIN_NOTIFICATION_ID, PackageUtils.getApplicationName(context), payloadData.getAlertMessage(), null, pi);

                    // bigText 사용시
                    showNotification(context, Constants.SYSTEM_ADMIN_NOTIFICATION_ID, PackageUtils.getApplicationName(context),
                            payloadData.getAlertMessage(), PackageUtils.getApplicationName(context), payloadData.getMessage(), null, null, null);
                    break;
                // IOT DEVICE 제어(스마트홈 서비스)
                case Constants.PUSH_PAYLOAD_TYPE_FIND_PASSWORD :
                    pi = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(payloadData.getMessage()));
                    showNotification(context, Constants.PW_FIND_NOTIFICATION_ID, PackageUtils.getApplicationName(context), payloadData.getAlertMessage(), null, pi);
                    break;

                default:
                    Log.d(TAG, "Unknown push payload type !!!");
                    break;
            }
        }
    }

    private void playNotificationAlarm(Context context, int textResId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSound(soundUri);
        notificationManager.notify(Constants.PUSH_NOTIFICATION_ALARM_ID, builder.build());

        Toast.makeText(context, textResId, Toast.LENGTH_SHORT).show();
    }

    private void showNotification(Context context, int notificationId, String title, String contentText, String ticker, Intent intent) {
        showNotification(context, notificationId, title, contentText, null, null, null, ticker, intent);
    }
    private void showNotification(Context context, int notificationId, String title, String contentText, String bigTitle, String bigContentText, String summaryText, String ticker, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSound(soundUri);

        builder.setSmallIcon(R.drawable.ic_notification_noti);
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
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
        }

        notificationManager.notify(notificationId, builder.build());
    }
}
