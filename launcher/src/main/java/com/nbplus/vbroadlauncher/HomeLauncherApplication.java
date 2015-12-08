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

import android.app.Application;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.nbplus.iotapp.btcharacteristics.SmartSensor;
import com.nbplus.iotlib.IoTInterface;
import com.nbplus.iotlib.callback.IoTServiceStatusNotification;
import com.nbplus.iotlib.callback.SmartSensorNotification;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTServiceStatus;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.PushService;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
import com.nbplus.vbroadlauncher.service.InstalledApplicationTask;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.basdroid.common.StringUtils;

/**
 * Created by basagee on 2015. 6. 1..
 */
@ReportsCrashes(formUri = "https://collector.tracepot.com/eee1cc67")
public class HomeLauncherApplication extends Application  {
    private static final String TAG = HomeLauncherApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);

        // pre-load installed application list
        new InstalledApplicationTask(this).execute();

        // start push agent service
        VBroadcastServer serverInfo = LauncherSettings.getInstance(this).getServerInformation();
        if (serverInfo != null && StringUtils.isEmptyString(serverInfo.getPushInterfaceServer()) == false) {
            Log.d(TAG, "Application onCreate() : ACTION_START_SERVICE");
            Intent intent = new Intent(this, PushService.class);
            intent.setAction(PushConstants.ACTION_START_SERVICE);
            intent.putExtra(PushConstants.EXTRA_START_SERVICE_IFADDRESS, serverInfo.getPushInterfaceServer());
            startService(intent);
        }

        mCurrentOutdoorMode = LauncherSettings.getInstance(HomeLauncherApplication.this).isOutdoorMode();
        IoTInterface.getInstance().setSmartSensorNotificationCallback(HomeLauncherApplication.class.getSimpleName(), mSmartSensorNotificationCallback);
    }

    private boolean mCurrentOutdoorMode = false;
    private long mLastInOutdoorMotionReportTime = System.currentTimeMillis();
    // OutDoor 설정 후 1분이 지난 다음부터 체크한다.
    private static final int OUTDOOR_MOTION_CHECK_START_TERM = 10 * 1000;

    // Indoor 모드에서 하루이상 모션이 발생하지 않는 경우 리포트
    private static final int INDOOR_NO_MOTION_REPORT_TERM = 24 * 60 * 60 * 1000;

    SmartSensorNotification mSmartSensorNotificationCallback = new SmartSensorNotification() {
        @Override
        public void notifyMotionSensor(IoTDevice device, boolean isMotionActive, boolean isDoorOpened, boolean isDoorOpened2) {
            Log.d(TAG, "Smart Sensor id = " + device.getDeviceId() + ", motion detection = " + isMotionActive + ", door opened = " + isDoorOpened + ", door opened2 = " + isDoorOpened2);
            boolean isOutdoor = LauncherSettings.getInstance(HomeLauncherApplication.this).isOutdoorMode();
            long currTime = System.currentTimeMillis();
            if (mLastInOutdoorMotionReportTime == 0L) {
                mLastInOutdoorMotionReportTime = currTime;
            }
            
            if (isOutdoor && (isMotionActive || isDoorOpened || isDoorOpened2)) {
                // 외출모드 설정 중인데 모션이 감지되었다.
                // 외출모드는 마지막 보고시점을 별도로 기록하지 않고 움직임이 발생할때마다 전달한다.
                Log.d(TAG, "currTime - mLastInOutdoorMotionReportTime = " + (currTime - mLastInOutdoorMotionReportTime));
                if (currTime - mLastInOutdoorMotionReportTime > OUTDOOR_MOTION_CHECK_START_TERM) {
                    Log.d(TAG, "외출중 모션 감지");
                    mLastInOutdoorMotionReportTime = currTime;

                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();

                    if (isMotionActive) {
                        Toast toast = Toast.makeText(HomeLauncherApplication.this, R.string.outdoor_mode_motion, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    } else if (isDoorOpened || isDoorOpened2) {
                        Toast toast = Toast.makeText(HomeLauncherApplication.this, R.string.outdoor_mode_door_open, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    }
                }
            } else if (isOutdoor && (!isMotionActive && !isDoorOpened && !isDoorOpened2)) {
                // 일정시간이 지나면 false 가한번씩 올라와서... 여기선 아무것도하지말자.
                //if (currTime - mLastInOutdoorMotionReportTime < OUTDOOR_MOTION_CHECK_START_TERM) {
                    //mLastInOutdoorMotionReportTime = currTime;
                //}
            } else if (!isOutdoor && !isMotionActive) {
                // 외출모드가 아닌데 모션이 감지되지 않는다.
                // 모션이 감지되지 않는 보고시간은 마지막 보고시점이후에 INDOOR_NO_MOTION_REPORT_TERM에 정해진 시간 이상일 경우이다.
                if (currTime - mLastInOutdoorMotionReportTime > INDOOR_NO_MOTION_REPORT_TERM) {
                    Log.d(TAG, "집안인데 모션이 없음. 서버에 보고???");
                    mLastInOutdoorMotionReportTime = currTime;

                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();

                    Toast toast = Toast.makeText(HomeLauncherApplication.this, R.string.indoor_mode_no_motion, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            } else if (!isOutdoor && isMotionActive) {
                if (currTime - mLastInOutdoorMotionReportTime < INDOOR_NO_MOTION_REPORT_TERM) {
                    // 서버에 보고?????
                }
                mLastInOutdoorMotionReportTime = currTime;
            }
        }
    };

    public void outdoorModeChanged(boolean isOutdoor) {
        Log.d(TAG, "outdoorModechanged() called....");
        if (mCurrentOutdoorMode != isOutdoor) {
            mCurrentOutdoorMode = isOutdoor;

            // 인터페이스가 정상동작 중인지 확인하여 정상이면 현재시간.
            // 정상이 아니면 0으로 셋팅한다.
            if (IoTInterface.getInstance().isIoTServiceAvailable()) {
                mLastInOutdoorMotionReportTime = System.currentTimeMillis();
            } else {
                mLastInOutdoorMotionReportTime = 0L;
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
