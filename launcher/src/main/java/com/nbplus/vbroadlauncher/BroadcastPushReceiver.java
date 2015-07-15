package com.nbplus.vbroadlauncher;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.data.PushMessageData;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.PushPayloadData;

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
        String action = intent.getAction();
        if (PushConstants.ACTION_PUSH_STATUS_CHANGED.equals(action)) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } else if (PushConstants.ACTION_PUSH_MESSAGE_RECEIVED.equals(action)) {
            Log.d(TAG, "Receive.. broadcast ACTION_PUSH_MESSAGE_RECEIVED from push service. re-direct to activity!!!");

            PushMessageData data = (PushMessageData)intent.getParcelableExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA);
            Log.d(TAG, "HANDLER_MESSAGE_PUSH_MESAGE_RECEIVED received = " + data);
            if (data == null || StringUtils.isEmptyString(data.getPayload())) {
                Log.d(TAG, "empty push message string !!");
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
            Intent i = new Intent();
            switch (type) {
                // 방송알림
                case Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST :
                case Constants.PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST :
                case Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST :
                    boolean isOutdoor = LauncherSettings.getInstance(context).isOutdoorMode();
                    playNotificationAlarm(context, R.string.notification_broadcast_push);
                    if (isOutdoor) {        // 외출모드에서는 재생하지 않음.
                        Log.d(TAG, "Broadcast notification.. isOutdoor mode... ");

                        i.setAction(action);
                        i.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED));
                        i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                    } else {
                        boolean useServiceChatHead = false;

                        if (useServiceChatHead) {
                            i = new Intent(context, RealtimeBroadcastProxyActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                            context.startActivity(i);
                        } else {
                            i = new Intent(context, RealtimeBroadcastActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                            i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_INDEX, System.currentTimeMillis());

                            LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                            context.startActivity(i);
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

                    i.setAction(action);
                    i.putExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED));
                    i.putExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, payloadData);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                    break;
                // IOT DEVICE 제어(스마트홈 서비스)
                case Constants.PUSH_PAYLOAD_TYPE_IOT_DEVICE_CONTROL :
                    break;
                // IOT DEVICE 제어(스마트홈 서비스)
                case Constants.PUSH_PAYLOAD_TYPE_PUSH_NOTIFICATION :
                    break;
                // IOT DEVICE 제어(스마트홈 서비스)
                case Constants.PUSH_PAYLOAD_TYPE_FIND_PASSWORD :
                    break;

                default:
                    Log.d(TAG, "Unknown push payload type !!!");
                    break;
            }

//
//            ComponentName componentName = new ComponentName(context.getPackageName(), HomeLauncherActivity.class.getName());
//            Intent i = new Intent();
//            i.setComponent(componentName);
//            i.setAction(action);
//            i.putExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA, intent.getStringExtra(PushConstants.EXTRA_PUSH_MESSAGE_DATA));
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(i);
        }
    }

    private void playNotificationAlarm(Context context, int textResId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSound(soundUri);
        notificationManager.notify(Constants.PUSH_NOTIFICATION_ALARM_ID, builder.build());

        Toast.makeText(context, textResId, Toast.LENGTH_SHORT).show();
    }
}
