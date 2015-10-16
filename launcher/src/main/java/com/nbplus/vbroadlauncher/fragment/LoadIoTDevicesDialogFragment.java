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
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;

import com.nbplus.iotlib.IoTInterface;
import com.nbplus.iotlib.callback.IoTServiceResponse;
import com.nbplus.iotlib.data.DeviceTypes;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTServiceCommand;
import com.nbplus.iotlib.data.IoTServiceStatus;
import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.vbroadlauncher.BaseActivity;
import com.nbplus.vbroadlauncher.BroadcastWebViewActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.adapter.StickyGridHeadersIoTDevicesAdapter;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleArrayAdapter;

import org.basdroid.common.DisplayUtils;

import java.util.ArrayList;

import io.vov.vitamio.widget.CenterLayout;

/**
 * Created by basagee on 2015. 6. 23..
 */
public class LoadIoTDevicesDialogFragment extends DialogFragment implements DialogInterface.OnKeyListener, IoTServiceResponse {
    private static final String TAG = LoadIoTDevicesDialogFragment.class.getSimpleName();

    private ArrayList<IoTDevice> mIoTDevicesList = new ArrayList<>();

    // button control
    ImageButton mCloseButton;
    Button      mRefreshButton;
    Button      mSendButton;
    GridView    mGridView;
    StickyGridHeadersIoTDevicesAdapter mGridAdapter;

    Handler     mHandler = new Handler();

//    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = (intent == null) ? "" : intent.getAction();
//            Log.d(TAG, ">> mBroadcastReceiver action received = " + action);
//            // send handler message
//            switch (action) {
//                case com.nbplus.iotlib.data.Constants.ACTION_IOT_DEVICE_LIST :
//                    ArrayList<IoTDevice> iotDevicesList = intent.getParcelableArrayListExtra(com.nbplus.iotlib.data.Constants.EXTRA_IOT_DEVICE_LIST);
//                    if (iotDevicesList != null) {
//                        mIoTDevicesList = iotDevicesList;
//                    } else {
//                        mIoTDevicesList = new ArrayList<>();
//                    }
//
//                    if (mGridAdapter == null) {
//                        mGridAdapter = new StickyGridHeadersIoTDevicesAdapter(getActivity(),
//                                mIoTDevicesList,
//                                R.layout.grid_iot_devices_header,
//                                R.layout.grid_iot_devices_item);
//
//                        mGridView.setAdapter(mGridAdapter);
//                    } else {
//                        mGridAdapter.setItems(mIoTDevicesList);
//                    }
//                    if (mGridAdapter.isEmpty()) {
//                        mSendButton.setEnabled(false);
//                        mSendButton.setClickable(false);
//                    } else {
//                        mSendButton.setEnabled(true);
//                        mSendButton.setClickable(true);
//                    }
//
//
//                    mHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            ((BaseActivity)getActivity()).dismissProgressDialog();
//                        }
//                    }, 2000);
//                    break;
//                default :
//                    break;
//            }
//        }
//    };

    public static LoadIoTDevicesDialogFragment newInstance(Bundle b) {
        LoadIoTDevicesDialogFragment frag = new LoadIoTDevicesDialogFragment();
        if (b != null) {
            frag.setArguments(b);
        }
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity()/*new ContextThemeWrapper(getActivity(), R.style.FullScreenDialog)*/);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // fullscreen without statusbar
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.setCancelable(false);
        this.setCancelable(false);

        // disable back key
        dialog.setOnKeyListener(this);

        // set content view
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_iot_devices, null, false);
        dialog.setContentView(v);

        // grid view
        mGridView = (GridView)v.findViewById(R.id.iot_devices_grid);
        mGridView.setEmptyView(v.findViewById(android.R.id.empty));

        // set button control
        mCloseButton = (ImageButton) v.findViewById(R.id.btn_close);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick btnClose..");
                ((BaseActivity)getActivity()).dismissProgressDialog();
                Intent sendIntent = new Intent();
                sendIntent.setAction(Constants.ACTION_IOT_DEVICE_LIST);
                sendIntent.putExtra(Constants.EXTRA_IOT_DEVICE_CANCELED, true);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

                dismiss();
            }
        });

        mRefreshButton = (Button) v.findViewById(R.id.btn_refresh);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick btnRefresh..");
                ((BaseActivity)getActivity()).showProgressDialog();

                IoTInterface.getInstance().getDevicesList(DeviceTypes.ALL, LoadIoTDevicesDialogFragment.this);
            }
        });

        mSendButton = (Button) v.findViewById(R.id.btn_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick btnSend..");
                ((BaseActivity)getActivity()).dismissProgressDialog();
                showSyncAlertDialog();
            }
        });

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
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

        try {
            Point p = DisplayUtils.getScreenSize(getActivity());

            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(p.x - 60, p.y - 30);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Disable back button
     * implements DialogInterface.OnKeyListener
     */
    @Override
    public boolean onKey(DialogInterface dialog, int keyCode,
                         KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "dialog onAttach");
        IoTInterface.getInstance().getDevicesList(DeviceTypes.ALL, this);

        ((BaseActivity)getActivity()).showProgressDialog();

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(com.nbplus.iotlib.data.Constants.ACTION_IOT_DEVICE_LIST);
//        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "dialog onDetach");
//        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    public void showSyncAlertDialog() {
        new AlertDialog.Builder(getActivity()).setMessage(R.string.iot_devices_send_alert)
                //.setTitle(R.string.alert_network_title)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Constants.ACTION_IOT_DEVICE_LIST);
                                sendIntent.putExtra(Constants.EXTRA_IOT_DEVICE_CANCELED, false);
                                sendIntent.putParcelableArrayListExtra(Constants.EXTRA_DATA, mIoTDevicesList);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

                                dialog.dismiss();
                                dismissDialogFragment();
                            }
                        })
                .show();
    }

    private void dismissDialogFragment() {
        ((BroadcastWebViewActivity)getActivity()).dismissUpdateIoTDevicesDialog();
    }

    /**
     * 버그라고 할 건아니지만...
     * => Caused by: java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
     *
     * There are two DialogFragment show() methods
     *    - show(FragmentManager manager, String tag) and show(FragmentTransaction transaction, String tag).
     *
     * If you want to use the FragmentManager version of the method (as in the original question),
     * an easy solution is to override this method and use commitAllowingStateLoss:
     */
    @Override
    public void show(FragmentManager manager, String tag) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    /**
     * @param cmd
     * @param serviceStatus
     * @param serviceStatusCode
     * @param b
     */
    @Override
    public void onResult(int cmd, IoTServiceStatus serviceStatus, IoTResultCodes serviceStatusCode, Bundle b) {
        Log.d(TAG, "IoTServiceResponse onResult...serviceStatus = " + serviceStatus + ", statusCode = " + serviceStatusCode);
        switch (cmd) {
            case IoTServiceCommand.GET_DEVICE_LIST:
                if (serviceStatus == null || serviceStatusCode == null) {
                    Log.e(TAG, "Unknown service status...");
                    break;
                }

                if (!serviceStatus.equals(IoTServiceStatus.RUNNING)) {
                    ((BaseActivity)getActivity()).dismissProgressDialog();

                    if (serviceStatusCode.equals(IoTResultCodes.BLE_NOT_SUPPORTED) ||
                            serviceStatusCode.equals(IoTResultCodes.BLUETOOTH_NOT_SUPPORTED) ||
                            serviceStatusCode.equals(IoTResultCodes.BIND_SERVICE_FAILED)) {
                        Log.d(TAG, ">> Can't use service :: " + serviceStatusCode);
                        new AlertDialog.Builder(getActivity()).setMessage(R.string.error_bluetooth_not_supported)
                                //.setTitle(R.string.alert_network_title)
                                .setCancelable(true)
                                .setPositiveButton(R.string.alert_ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                Intent sendIntent = new Intent();
                                                sendIntent.setAction(Constants.ACTION_IOT_DEVICE_LIST);
                                                sendIntent.putExtra(Constants.EXTRA_IOT_DEVICE_CANCELED, true);
                                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

                                                dialog.dismiss();
                                                dismissDialogFragment();
                                            }
                                        })
                                .show();
                        break;
                    } else if (serviceStatusCode.equals(IoTResultCodes.BLUETOOTH_NOT_ENABLED)) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        getActivity().startActivityForResult(enableBtIntent, Constants.START_ACTIVITY_REQUEST_ENABLE_BT);
                    } else {
                        new AlertDialog.Builder(getActivity()).setMessage(R.string.error_bluetooth_not_supported)
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
                } else {
                    if (serviceStatusCode.equals(IoTResultCodes.SUCCESS)) {
                        handleDeviceList(b);
                    }
                }
                break;
            default:
                Log.d(TAG, "Unknown command ");
        }
    }

    private void handleDeviceList(Bundle b) {
        ArrayList<IoTDevice> iotDevicesList = b.getParcelableArrayList(IoTServiceCommand.KEY_DATA);
        if (iotDevicesList != null) {
            mIoTDevicesList = iotDevicesList;
        } else {
            mIoTDevicesList = new ArrayList<>();
        }

        if (mGridAdapter == null) {
            mGridAdapter = new StickyGridHeadersIoTDevicesAdapter(getActivity(),
                    mIoTDevicesList,
                    R.layout.grid_iot_devices_header,
                    R.layout.grid_iot_devices_item);

            mGridView.setAdapter(mGridAdapter);
        } else {
            mGridAdapter.setItems(mIoTDevicesList);
        }
        if (mGridAdapter.isEmpty()) {
            mSendButton.setEnabled(false);
            mSendButton.setClickable(false);
        } else {
            mSendButton.setEnabled(true);
            mSendButton.setClickable(true);
        }


        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((BaseActivity)getActivity()).dismissProgressDialog();
            }
        }, 2000);
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}.  This follows the
     * related Activity API as described there in
     * {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, ">>> onActivityResult...");
        switch (requestCode) {
            case Constants.START_ACTIVITY_REQUEST_ENABLE_BT:
                // User chose not to enable Bluetooth.
                if (resultCode == Activity.RESULT_CANCELED) {
                    dismissDialogFragment();

                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Constants.ACTION_IOT_DEVICE_LIST);
                    sendIntent.putExtra(Constants.EXTRA_IOT_DEVICE_CANCELED, true);
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);
                } else {
                    ((BaseActivity) getActivity()).showProgressDialog();
                    //IoTInterface.getInstance().getDevicesList(DeviceTypes.ALL, LoadIoTDevicesDialogFragment.this);
                }
                break;
        }
    }
}
