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

package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nbplus.iotlib.IoTInterface;
import com.nbplus.iotlib.data.IoTConstants;
import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.PushService;
import com.nbplus.vbroadlauncher.BaseActivity;
import com.nbplus.vbroadlauncher.BroadcastWebViewActivity;
import com.nbplus.vbroadlauncher.HomeLauncherActivity;
import com.nbplus.vbroadlauncher.HomeLauncherApplication;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.ShowApplicationActivity;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.data.BaseApiResult;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.PushPayloadData;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;

import com.nbplus.vbroadlauncher.service.BaseServerApiAsyncTask;
import com.nbplus.vbroadlauncher.widget.TextClock;
import com.nbplus.vbroadlauncher.widget.WeatherView;

import org.basdroid.common.DisplayUtils;
import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link LauncherFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LauncherFragment extends Fragment implements OnActivityInteractionListener, View.OnClickListener {
    private static final String TAG = LauncherFragment.class.getSimpleName();

    ProgressDialogFragment mProgressDialogFragment;
    private ImageView mPushServiceStatus;
    private LinearLayout mOutdoorMode;
    private TextView mOutdoorText;

    //부가데이터 동기화
    private LinearLayout mIoTDataSync;
    private TextView mIoTDataSyncText;
    private boolean mIsProcessingIoTDataSync;

    private LinearLayout mServiceTreeMap;
    private LinearLayout mApplicationsView;
    private TextView mVillageName;
    private TextClock mTextClock;
    private WeatherView mWeatherView;
    private LinearLayout mMainViewLayout;
    private LinearLayout mMainViewLeftPanel;
    private LinearLayout mMainViewRightPanel;
    private GridLayout mMainShortcutGridLayout;
    private GridLayout mShorcutGridLayout;

    boolean mLastNetworkStatus = false;

    private LauncherFragmentHandler mHandler;

    private static final int HANDLER_MESSAGE_CONNECTIVITY_CHANGED = 0x01;
    private static final int HANDLER_MESSAGE_LOCALE_CHANGED = 0x02;
    private static final int HANDLER_MESSAGE_SET_VILLAGE_NAME = 0x03;
    private static final int HANDLER_IOT_DATA_SYNC_COMPLETED = 0x04;
    private static final int HANDLER_IOT_SERVICE_STATUS_CHANGED = 0x05;

    private ArrayList<ShortcutData> mPushNotifiableShorcuts = new ArrayList<>();

    // 핸들러 객체 만들기
    private static class LauncherFragmentHandler extends Handler {
        private final WeakReference<LauncherFragment> mFragment;

        public LauncherFragmentHandler(LauncherFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            LauncherFragment fragment = mFragment.get();
            if (fragment != null) {
                fragment.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case HANDLER_MESSAGE_CONNECTIVITY_CHANGED:
                Log.d(TAG, "HANDLER_MESSAGE_CONNECTIVITY_CHANGED received !!!");
                final boolean networkStatus = NetworkUtils.isConnected(getActivity());
                if (mLastNetworkStatus == networkStatus) {
                    Log.d(TAG, ">> current and previous are same status. ignore it...");
                    return;
                }
                mLastNetworkStatus = networkStatus;

                mWeatherView.onNetworkConnected(mLastNetworkStatus);
                if (mLastNetworkStatus) {
                    // start push agent service
                    VBroadcastServer serverInfo = LauncherSettings.getInstance(getActivity()).getServerInformation();
                    if (serverInfo != null && StringUtils.isEmptyString(serverInfo.getPushInterfaceServer()) == false) {
                        Intent intent = new Intent(getActivity(), PushService.class);
                        intent.setAction(PushConstants.ACTION_START_SERVICE);
                        intent.putExtra(PushConstants.EXTRA_START_SERVICE_IFADDRESS, serverInfo.getPushInterfaceServer());
                        getActivity().startService(intent);
                    }
                }
                break;

            case Constants.HANDLER_MESSAGE_PUSH_STATUS_CHANGED :
                int status = msg.arg1;
                Log.d(TAG, "HANDLER_MESSAGE_PUSH_STATUS_CHANGED received. status = " + status);
                if (status == PushConstants.PUSH_STATUS_VALUE_CONNECTED) {
                    mPushServiceStatus.setImageResource(R.drawable.ic_nav_wifi_on);
                } else {
                    mPushServiceStatus.setImageResource(R.drawable.ic_nav_wifi_off);
                }

//                int what = msg.arg2;
//                if (what == PushConstants.PUSH_STATUS_WHAT_NETORSERVER) {
//                    new AlertDialog.Builder(getActivity()).setMessage("네트워크 상태 또는 서버에 의하여 푸시 에이전트 연결이 해제되었습니다.")
//                            //.setTitle(R.string.alert_network_title)
//                            .setCancelable(true)
//                            .setPositiveButton(R.string.alert_ok,
//                                    new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int whichButton) {
//                                            dialog.dismiss();
//                                        }
//                                    })
//                            .show();
//                }
                break;
            case Constants.HANDLER_MESSAGE_PUSH_MESAGE_RECEIVED :
                PushPayloadData payloadData = (PushPayloadData)msg.obj;
                if (payloadData == null) {
                    Log.d(TAG, "empty push message string !!");
                    return;
                }

                Log.d(TAG, "HANDLER_MESSAGE_PUSH_MESAGE_RECEIVED received = " + payloadData.getServiceType());
                String type = payloadData.getServiceType();
                switch (type) {
                    // 방송알림
                    case Constants.PUSH_PAYLOAD_TYPE_REALTIME_BROADCAST :
                    case Constants.PUSH_PAYLOAD_TYPE_NORMAL_BROADCAST :
                    case Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST : {
                        // 외출모드, 브라우저에서 방송서버접속중, 실시간 미지원 단말등인 경우..
                        // 여기로 들어온다.
                        View btnShortcut = null;
                        for (int i = 0; i < mPushNotifiableShorcuts.size(); i++) {
                            ShortcutData shortcut = mPushNotifiableShorcuts.get(i);
                            String[] pushType = shortcut.getPushType();
                            if (pushType != null && pushType.length > 0 && Arrays.asList(pushType).indexOf(type) >= 0) {
                                btnShortcut = shortcut.getLauncherButton();
                                break;
                            }
                        }
                        // push notification badge 를 보여준다.
                        if (btnShortcut != null) {
                            TextView badgeView = (TextView) btnShortcut.findViewById(R.id.launcher_menu_badge);
                            if (badgeView != null) {
                                badgeView.setVisibility(View.VISIBLE);
                            }
                        }

                        break;
                    }
                    // 긴급호출메시지
                    case Constants.PUSH_PAYLOAD_TYPE_EMERGENCY_CALL :
                        break;
                    // 주민투표
                    case Constants.PUSH_PAYLOAD_TYPE_INHABITANTS_POLL :
                    // 공동구매
                    case Constants.PUSH_PAYLOAD_TYPE_COOPERATIVE_BUYING : {
                        View btnShortcut = null;
                        for (int i = 0; i < mPushNotifiableShorcuts.size(); i++) {
                            ShortcutData shortcut = mPushNotifiableShorcuts.get(i);
                            String[] pushType = shortcut.getPushType();
                            if (pushType != null && pushType.length > 0 && Arrays.asList(pushType).indexOf(type) >= 0) {
                                btnShortcut = shortcut.getLauncherButton();
                                break;
                            }
                        }
                        // push notification badge 를 보여준다.
                        if (btnShortcut != null) {
                            TextView badgeView = (TextView) btnShortcut.findViewById(R.id.launcher_menu_badge);
                            if (badgeView != null) {
                                badgeView.setVisibility(View.VISIBLE);
                            }
                        }
                        break;
                    }
                    default:
                        Log.d(TAG, "Unknown push payload type !!!");
                        break;
                }
                break;

            case Constants.HANDLER_MESSAGE_SEND_EMERGENCY_CALL_COMPLETE_TASK: {
                BaseApiResult result = (BaseApiResult) msg.obj;
                Toast toast;
                dismissProgressDialog();
                if (result != null) {
                    Log.d(TAG, ">> EMERGENCY CALL result code = " + result.getResultCode() + ", message = " + result.getResultMessage());
                    if (Constants.RESULT_OK.equals(result.getResultCode())) {
                        toast = Toast.makeText(getActivity(), R.string.emergency_call_success_message, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {
                        toast = Toast.makeText(getActivity(), result.getResultMessage(), Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                } else {
                    toast = Toast.makeText(getActivity(), R.string.emergency_call_fail_message, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                break;
            }
            case HANDLER_MESSAGE_SET_VILLAGE_NAME :
                if (mVillageName != null) {
                    mVillageName.setText(LauncherSettings.getInstance(getActivity()).getVillageName());
                }
                if (mWeatherView != null) {
                    mWeatherView.onChangedVillageName();
                }
                break;

            // 부가데이터 동기화
            case HANDLER_IOT_DATA_SYNC_COMPLETED: {
                if (!mIsProcessingIoTDataSync) {
                    return;
                }
                mIsProcessingIoTDataSync = false;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismissProgressDialog();
                    }
                }, 1000);
                Toast toast;
                toast = Toast.makeText(getActivity(), R.string.toast_iot_data_sync, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                mIoTDataSyncText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cached_white, 0, 0, 0);
                mIoTDataSyncText.setTextColor(getResources().getColor(R.color.white));
                break;
            }

            case HANDLER_IOT_SERVICE_STATUS_CHANGED: {
                Bundle extras = msg.getData();
                if (extras == null) {
                    return;
                }

                boolean serviceStatus = extras.getBoolean(IoTConstants.EXTRA_SERVICE_STATUS);
                if (serviceStatus) {
                    mIoTDataSyncText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cached_white, 0, 0, 0);
                    mIoTDataSyncText.setTextColor(getResources().getColor(R.color.white));

                    mIoTDataSync.setOnClickListener(mIoTSyncClickListener);
                    mIoTDataSync.setClickable(true);
                    mIoTDataSync.setEnabled(true);
                } else {
                    mIoTDataSyncText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cached_grey600, 0, 0, 0);
                    mIoTDataSyncText.setTextColor(getResources().getColor(R.color.btn_color_absentia_off));

                    mIoTDataSync.setOnClickListener(null);
                    mIoTDataSync.setClickable(false);
                    mIoTDataSync.setEnabled(false);
                }
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Broadcast message received !!!");
            if (mHandler == null) {
                return;
            }
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(HANDLER_MESSAGE_CONNECTIVITY_CHANGED);
            } else if (Constants.ACTION_SET_VILLAGE_NAME.equals(action)) {
                mHandler.sendEmptyMessage(HANDLER_MESSAGE_SET_VILLAGE_NAME);
            } else if (PushConstants.ACTION_PUSH_STATUS_CHANGED.equals(action)) {
                Message msg = new Message();
                msg.what = Constants.HANDLER_MESSAGE_PUSH_STATUS_CHANGED;
                msg.arg1 = intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED);
                msg.arg2 = intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_WHAT, PushConstants.PUSH_STATUS_WHAT_NORMAL);
                mHandler.sendMessage(msg);
            } else if (PushConstants.ACTION_PUSH_MESSAGE_RECEIVED.equals(action)) {
                Message msg = new Message();
                msg.what = Constants.HANDLER_MESSAGE_PUSH_MESAGE_RECEIVED;
                msg.arg1 = intent.getIntExtra(PushConstants.EXTRA_PUSH_STATUS_VALUE, PushConstants.PUSH_STATUS_VALUE_DISCONNECTED);
                msg.obj = intent.getParcelableExtra(Constants.EXTRA_BROADCAST_PAYLOAD_DATA);
                mHandler.sendMessage(msg);
            } else if (IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED.equals(action)) {
                Message msg = new Message();
                msg.what = HANDLER_IOT_DATA_SYNC_COMPLETED;
                mHandler.sendMessage(msg);
            } else if (IoTConstants.ACTION_IOT_SERVICE_STATUS_CHANGED.equals(action)) {
                Message msg = new Message();
                msg.what = HANDLER_IOT_SERVICE_STATUS_CHANGED;
                Bundle data = new Bundle();
                data.putBoolean(IoTConstants.EXTRA_SERVICE_STATUS, intent.getBooleanExtra(IoTConstants.EXTRA_SERVICE_STATUS, false));
                msg.setData(data);

                mHandler.sendMessage(msg);
            }
        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LauncherFragment.
     */
    public static LauncherFragment newInstance(String param1, String param2) {
        LauncherFragment fragment = new LauncherFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public LauncherFragment() {
        // Required empty public constructor
    }

    private View.OnClickListener mIoTSyncClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mIsProcessingIoTDataSync = true;
            showProgressDialog();
            mIoTDataSyncText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cached_grey600, 0, 0, 0);
            mIoTDataSyncText.setTextColor(getResources().getColor(R.color.btn_color_absentia_off));

            IoTInterface.getInstance().forceDataSync();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getActivity().setTitle("LauncherFragment");
        mLastNetworkStatus = NetworkUtils.isConnected(getActivity());

        if (!LauncherSettings.getInstance(getActivity()).isCheckedTTSEngine()) {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            getActivity().startActivityForResult(checkIntent, Constants.START_ACTIVITY_REQUEST_CHECK_TTS_DATA);
        }

        Log.d(TAG, "PushConstants.ACTION_GET_STATUS send");
        // check push agent status
        Intent intent = new Intent(getActivity(), PushService.class);
        intent.setAction(PushConstants.ACTION_GET_STATUS);
        getActivity().startService(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_launcher, container, false);

        mMainViewLayout = (LinearLayout)v.findViewById(R.id.main_view_layout);

        // push agent 연결상태이다.
        mPushServiceStatus = (ImageView) v.findViewById(R.id.ic_nav_wifi);
        if (((BaseActivity)getActivity()).isPushServiceConnected()) {
            mPushServiceStatus.setImageResource(R.drawable.ic_nav_wifi_on);
        } else {
            mPushServiceStatus.setImageResource(R.drawable.ic_nav_wifi_off);
        }

        mVillageName = (TextView)v.findViewById(R.id.launcher_village_name);
        mVillageName.setText(LauncherSettings.getInstance(getActivity()).getVillageName());

        mApplicationsView = (LinearLayout) v.findViewById(R.id.ic_nav_apps);
        mApplicationsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ShowApplicationActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
        mServiceTreeMap = (LinearLayout)v.findViewById(R.id.ic_nav_show_map);
        mServiceTreeMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!NetworkUtils.isConnected(getActivity())) {
                    ((BaseActivity)getActivity()).showNetworkConnectionAlertDialog();
                    return;
                }
                Intent intent = new Intent(getActivity(), BroadcastWebViewActivity.class);

                ShortcutData data = new ShortcutData(Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER,
                        R.string.btn_show_map,
                        getActivity().getResources().getString(R.string.addr_show_map),
                        R.drawable.ic_menu_04,
                        R.drawable.ic_menu_shortcut_02_selector,
                        0,
                        null);

                VBroadcastServer serverInfo = LauncherSettings.getInstance(getActivity()).getServerInformation();
                data.setDomain(serverInfo.getDocServer());

                intent.putExtra(Constants.EXTRA_NAME_SHORTCUT_DATA, data);
                startActivity(intent);
            }
        });
        mOutdoorMode = (LinearLayout)v.findViewById(R.id.ic_nav_outdoor);
        mOutdoorText = (TextView) v.findViewById(R.id.tv_outdoor);
        if (LauncherSettings.getInstance(getActivity()).isOutdoorMode()) {
            mOutdoorText.setTextColor(getResources().getColor(R.color.btn_color_absentia_on));
            mOutdoorText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_nav_absentia_on, 0, 0, 0);
        } else {
            mOutdoorText.setTextColor(getResources().getColor(R.color.btn_color_absentia_off));
            mOutdoorText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_nav_absentia_off, 0, 0, 0);
        }
        mOutdoorMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast;

                boolean mode = false;
                if (LauncherSettings.getInstance(getActivity()).isOutdoorMode()) {
                    LauncherSettings.getInstance(getActivity()).setIsOutdoorMode(false);
                    mOutdoorText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_nav_absentia_off, 0, 0, 0);
                    mOutdoorText.setTextColor(getResources().getColor(R.color.btn_color_absentia_off));

                    toast = Toast.makeText(getActivity(), R.string.outdoor_mode_off, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                } else {
                    mode = true;
                    LauncherSettings.getInstance(getActivity()).setIsOutdoorMode(true);
                    mOutdoorText.setTextColor(getResources().getColor(R.color.btn_color_absentia_on));
                    mOutdoorText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_nav_absentia_on, 0, 0, 0);

                    toast = Toast.makeText(getActivity(), R.string.outdoor_mode_on, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }

                HomeLauncherApplication application = (HomeLauncherApplication)getActivity().getApplicationContext();
                if (application != null) {
                    application.outdoorModeChanged(mode);
                }
            }
        });
        // 부가데이터 동기화
        mIoTDataSync= (LinearLayout)v.findViewById(R.id.ic_iot_data_sync);
        mIoTDataSyncText = (TextView) v.findViewById(R.id.tv_iot_data_sync);
        mIoTDataSync.setOnClickListener(mIoTSyncClickListener);
        mIoTDataSync.setClickable(true);
        mIoTDataSync.setEnabled(true);

        mTextClock = (TextClock)v.findViewById(R.id.text_clock);
        if (mTextClock != null) {
            mTextClock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
                        startActivity(intent);
                        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                i.addCategory(Intent.CATEGORY_DEFAULT);
                                startActivity(i);
                            }
                        });
                        alert.setMessage(R.string.alert_calendar_not_found);
                        alert.show();
                    }
                }
            });
        }
        mWeatherView = (WeatherView)v.findViewById(R.id.weather_view);
        mMainViewLeftPanel = (LinearLayout)v.findViewById(R.id.main_view_left_panel);
        mMainViewRightPanel = (LinearLayout)v.findViewById(R.id.main_view_right_panel);

        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // add main shortcut.
        ArrayList<ShortcutData> mainShortcutDatas = LauncherSettings.getInstance(getActivity()).getLauncherMainShortcuts();
        mMainShortcutGridLayout = (GridLayout)v.findViewById(R.id.main_shortcut_grid);
        float dp;// = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_main_shortcut_width);
//        float widthPx = DisplayUtils.pxFromDp(getActivity(), dp);
//
//        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_main_shortcut_height);
//        float heightPx = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_main_shortcut_font_size);
        float mainShortcutFontPx = DisplayUtils.pxFromDp(getActivity(), dp);
        for (int i = 0; i < mMainShortcutGridLayout.getColumnCount(); i++) {
            /**
             * right shortcut panel
             */
            ShortcutData data = mainShortcutDatas.get(i);
            FrameLayout btnLayout = (FrameLayout)layoutInflater.inflate(R.layout.launcher_menu_top_item, mMainShortcutGridLayout, false);//new Button(getActivity());
            mMainShortcutGridLayout.addView(btnLayout);
            if (data.getPushType() != null && data.getPushType().length > 0) {
                data.setLauncherButton(btnLayout);
                mPushNotifiableShorcuts.add(data);
            }

            btnLayout.setBackgroundResource(data.getIconBackResId());

//            GridLayout.LayoutParams lp = (GridLayout.LayoutParams)btnLayout.getLayoutParams();
//            lp.width = (int)widthPx;
//            lp.height = (int)heightPx;
//            btnLayout.setLayoutParams(lp);

            TextView label = (TextView)btnLayout.findViewById(R.id.menu_item_label);
            label.setText(data.getName());
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, mainShortcutFontPx);
            label.setTextColor(getResources().getColor(R.color.white));
            label.setTypeface(null, Typeface.BOLD);
            label.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

            ImageView icon = (ImageView)btnLayout.findViewById(R.id.menu_item_image);
            icon.setImageResource(data.getIconResId());

            btnLayout.setTag(data);
            btnLayout.setOnClickListener(this);
        }

        // add other shortcuts.
        mShorcutGridLayout = (GridLayout)v.findViewById(R.id.shortcut_grid);
        ArrayList<ShortcutData> shortcutDatas = LauncherSettings.getInstance(getActivity()).getLauncherShortcuts();
        int columnNum = mShorcutGridLayout.getColumnCount();
        final int MAX_ROW_NUM = 3;

        int shortcutNum = shortcutDatas.size() > (columnNum * MAX_ROW_NUM) ? (columnNum * MAX_ROW_NUM) : shortcutDatas.size();
        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_shortcut_font_size);
        float btnFontPx = DisplayUtils.pxFromDp(getActivity(), dp);

        for (int i = 0; i < shortcutNum; i++) {
            /**
             * right shortcut panel
             */
            ShortcutData data = shortcutDatas.get(i);
            FrameLayout btnLayout = (FrameLayout)layoutInflater.inflate(R.layout.launcher_menu_item, mShorcutGridLayout, false);//new Button(getActivity());
            mShorcutGridLayout.addView(btnLayout);
            if (data.getPushType() != null && data.getPushType().length > 0) {
                data.setLauncherButton(btnLayout);
                mPushNotifiableShorcuts.add(data);
            }

            btnLayout.setBackgroundResource(data.getIconBackResId());

            TextView label = (TextView)btnLayout.findViewById(R.id.menu_item_label);
            label.setText(data.getName());
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, btnFontPx);
            label.setTextColor(getResources().getColor(R.color.white));
            label.setTypeface(null, Typeface.BOLD);
            label.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

            ImageView icon = (ImageView)btnLayout.findViewById(R.id.menu_item_image);
            icon.setImageResource(data.getIconResId());

            btnLayout.setTag(data);
            btnLayout.setOnClickListener(this);
        }

        setContentViewByOrientation();

        return v;
    }

    @Override
    public boolean onPushReceived(Message message) {
        Message msg = new Message();
        msg.what = message.what;
        msg.arg1 = message.arg1;
        msg.arg2 = message.arg2;
        msg.obj = message.obj;
        mHandler.sendMessage(msg);
        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            Log.d(TAG, "LauncherFragment onAttach()");
            ((HomeLauncherActivity)getActivity()).registerActivityInteractionListener(this);

            // check network status
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

            intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.ACTION_SET_VILLAGE_NAME);
            intentFilter.addAction(IoTConstants.ACTION_IOT_DATA_SYNC_COMPLETED);
            intentFilter.addAction(IoTConstants.ACTION_IOT_SERVICE_STATUS_CHANGED);
            intentFilter.addAction(PushConstants.ACTION_PUSH_STATUS_CHANGED);
            intentFilter.addAction(PushConstants.ACTION_PUSH_MESSAGE_RECEIVED);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, intentFilter);

            mHandler = new LauncherFragmentHandler(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        getActivity().unregisterReceiver(mBroadcastReceiver);
        ((HomeLauncherActivity)getActivity()).unRegisterActivityInteractionListener(this);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);

        Log.d(TAG, "LauncherFragment onDetach()");
        mHandler = null;
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onDataChanged() {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentViewByOrientation();
    }

    private void setContentViewByOrientation() {
        int orientation = DisplayUtils.getScreenOrientation(getActivity());
        LinearLayout.LayoutParams lp;
        float marginDp = 0f;
        float marginPx = 0f;

        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            mMainViewLayout.setOrientation(LinearLayout.HORIZONTAL);

            lp = (LinearLayout.LayoutParams)mMainViewLeftPanel.getLayoutParams();
            lp.weight = 0.6f;
            marginDp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_panel_margin);
            marginPx = DisplayUtils.pxFromDp(getActivity(), marginDp);
            lp.setMargins(0, 0, (int)marginPx, 0);
            mMainViewLeftPanel.setLayoutParams(lp);
            lp = (LinearLayout.LayoutParams)mMainViewRightPanel.getLayoutParams();
            lp.weight = 0.4f;
            lp.setMargins(0, 0, 0, 0);
            mMainViewRightPanel.setLayoutParams(lp);
        } else {
            mMainViewLayout.setOrientation(LinearLayout.VERTICAL);

            lp = (LinearLayout.LayoutParams)mMainViewLeftPanel.getLayoutParams();
            lp.weight = 1.0f;
            marginDp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_panel_margin);
            marginPx = DisplayUtils.pxFromDp(getActivity(), marginDp);
            lp.setMargins(0, 0, 0, (int)marginPx);
            mMainViewLeftPanel.setLayoutParams(lp);
            lp = (LinearLayout.LayoutParams)mMainViewRightPanel.getLayoutParams();
            lp.weight = 1.0f;
            lp.setMargins(0, 0, 0, 0);
            mMainViewRightPanel.setLayoutParams(lp);
        }
        lp = (LinearLayout.LayoutParams)mMainViewLayout.getLayoutParams();

        float heightDp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_main_view_margin_top);
        float px = DisplayUtils.pxFromDp(getActivity(), heightDp);
        float horizontalMarginDp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_main_view_margin_horizontal);
        float horizontalMarginPx = DisplayUtils.pxFromDp(getActivity(), horizontalMarginDp);

        lp.setMargins((int)horizontalMarginPx, (int)px, (int)horizontalMarginPx, lp.bottomMargin);
        mMainViewLayout.setLayoutParams(lp);

        float dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_clock_height);
        px = DisplayUtils.pxFromDp(getActivity(), dp);
        mTextClock.setTextSize(px);
        mWeatherView.onConfigurationChanged(orientation);

        // right shortcut menu
        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_main_shortcut_width);
        float widthPx = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_main_shortcut_height);
        float heightPx = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_main_shortcut_font_size);
        float mainShortcutFontPx = DisplayUtils.pxFromDp(getActivity(), dp);

        GridLayout.LayoutParams childlp;
        View child;
        for (int i = 0; i < mMainShortcutGridLayout.getChildCount(); i++) {
            /**
             * right shortcut panel
             */
            child = mMainShortcutGridLayout.getChildAt(i);

            childlp = (GridLayout.LayoutParams)child.getLayoutParams();
            childlp.width = (int)widthPx;
            childlp.height = (int)heightPx;
            child.setLayoutParams(childlp);
        }

        // add other shortcuts.
        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_shortcut_width);
        float btnWidthPx = DisplayUtils.pxFromDp(getActivity(), dp);
        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_shortcut_height);
        float btnHeightPx = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = DisplayUtils.getDimension(getActivity(), R.dimen.ic_nav_btn_drawable_padding);
        float drawablePadding = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = DisplayUtils.getDimension(getActivity(), R.dimen.launcher_ic_menu_shortcut_font_size);
        float btnFontPx = DisplayUtils.pxFromDp(getActivity(), dp);

        for (int i = 0; i < mShorcutGridLayout.getChildCount(); i++) {
            /**
             * right shortcut panel
             */
            child = mShorcutGridLayout.getChildAt(i);

            childlp = (GridLayout.LayoutParams)child.getLayoutParams();
            childlp.width = (int)btnWidthPx;
            childlp.height = (int) btnHeightPx;
            child.setLayoutParams(childlp);
        }
    }

    /**
     * when click right panel shortcdddut button ...
     * @param view
     */
    @Override
    public void onClick(View view) {
        ShortcutData data = (ShortcutData) view.getTag();
        VBroadcastServer serverData = LauncherSettings.getInstance(getActivity()).getServerInformation();

        Intent intent;

        if (!NetworkUtils.isConnected(getActivity())) {
            ((BaseActivity)getActivity()).showNetworkConnectionAlertDialog();
            return;
        }

        // push notification badge 를 제거한다.
        if (data.getPushType() != null) {
            TextView badgeView = (TextView)view.findViewById(R.id.launcher_menu_badge);
            if (badgeView != null) {
                badgeView.setVisibility(View.GONE);
            }
        }

        switch (data.getType()) {
            case Constants.SHORTCUT_TYPE_WEB_INTERFACE_SERVER:
                BaseServerApiAsyncTask task;
                if (StringUtils.isEmptyString(serverData.getApiServer())) {
                    showIncorrectServerInformation();
                    break;
                }
                data.setDomain(serverData.getApiServer());
                try {
                    task = (BaseServerApiAsyncTask)data.getNativeClass().newInstance();
                    if (task != null) {
                        showProgressDialog();
                        task.setBroadcastApiData(getActivity(), mHandler, data.getDomain() + data.getPath());
                        task.execute();
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (java.lang.InstantiationException e) {
                    e.printStackTrace();
                }
                break;
            case Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER:
                if (StringUtils.isEmptyString(serverData.getDocServer())) {
                    showIncorrectServerInformation();
                    break;
                }
                intent = new Intent(getActivity(), BroadcastWebViewActivity.class);
                data.setDomain(serverData.getDocServer());
                intent.putExtra(Constants.EXTRA_NAME_SHORTCUT_DATA, data);
                startActivity(intent);
                break;
            case Constants.SHORTCUT_TYPE_NATIVE_INTERFACE:
                if (StringUtils.isEmptyString(serverData.getDocServer())) {
                    showIncorrectServerInformation();
                    break;
                }
                switch (data.getNativeType()) {
                    case 0 :            // activity
                        intent = new Intent(getActivity(), data.getNativeClass());
                        data.setDomain(serverData.getDocServer());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra(Constants.EXTRA_NAME_SHORTCUT_DATA, data);
                        startActivity(intent);
                        break;
                    case 1 :            // fragment
                        break;
                    case 2 :            // dialog fragment
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        //no paramater
                        //Class noparams[] = {};
                        //String parameter
                        Class[] param = new Class[1];
                        param[0] = ShortcutData.class;
                        DialogFragment fragment = null;

                        try {
                            Object obj =  data.getNativeClass().newInstance();
                            Method method = data.getNativeClass().getDeclaredMethod("newInstance", param);

                            data.setDomain(serverData.getDocServer());
                            fragment = (DialogFragment)method.invoke(obj, data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (fragment != null) {
                            fragment.show(fm, "fragment_dialog_radio");
                        }
                        break;
                }
                break;
            default:
                Log.d(TAG, "Unknown shortcut type !!!");
        }
    }

    private void showProgressDialog() {
        dismissProgressDialog();
        mProgressDialogFragment = ProgressDialogFragment.newInstance();
        mProgressDialogFragment.show(getActivity().getSupportFragmentManager(), "launcher_progress_dialog");
    }
    private void dismissProgressDialog() {
        try {
            if (mProgressDialogFragment != null) {
                mProgressDialogFragment.dismiss();
                mProgressDialogFragment = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mTextClock != null) {
            mTextClock.onResumed();
        }
        if (mWeatherView != null) {
            mWeatherView.onResumed();
        }
        if (IoTInterface.getInstance().isIoTServiceAvailable()) {
            mIoTDataSyncText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cached_white, 0, 0, 0);
            mIoTDataSyncText.setTextColor(getResources().getColor(R.color.white));

            mIoTDataSync.setOnClickListener(mIoTSyncClickListener);
            mIoTDataSync.setClickable(true);
            mIoTDataSync.setEnabled(true);
        } else {
            mIoTDataSyncText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cached_grey600, 0, 0, 0);
            mIoTDataSyncText.setTextColor(getResources().getColor(R.color.btn_color_absentia_off));

            mIoTDataSync.setOnClickListener(null);
            mIoTDataSync.setClickable(false);
            mIoTDataSync.setEnabled(false);
        }
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mTextClock != null) {
            mTextClock.onPaused();
        }
        if (mWeatherView != null) {
            mWeatherView.onPaused();
        }
    }

    public void showIncorrectServerInformation() {
        new AlertDialog.Builder(getActivity()).setMessage(R.string.alert_server_info_message)
                //.setTitle(R.string.alert_network_title)
                .setCancelable(true)
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }

}
