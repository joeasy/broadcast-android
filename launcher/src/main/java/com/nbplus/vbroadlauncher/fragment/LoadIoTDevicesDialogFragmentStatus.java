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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;

import com.nbplus.iotlib.IoTInterface;
import com.nbplus.iotlib.callback.IoTServiceStatusNotification;
import com.nbplus.iotlib.data.DeviceTypes;
import com.nbplus.iotlib.data.IoTDevice;
import com.nbplus.iotlib.data.IoTResultCodes;
import com.nbplus.iotlib.data.IoTServiceCommand;
import com.nbplus.iotlib.data.IoTServiceStatus;
import com.nbplus.vbroadlauncher.BaseActivity;
import com.nbplus.vbroadlauncher.BroadcastWebViewActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.adapter.StickyGridHeadersIoTDevicesAdapter;
import com.nbplus.vbroadlauncher.data.Constants;

import org.basdroid.common.DisplayUtils;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 6. 23..
 */
public class LoadIoTDevicesDialogFragmentStatus extends DialogFragment implements DialogInterface.OnKeyListener, IoTServiceStatusNotification, AdapterView.OnItemClickListener {
    private static final String TAG = LoadIoTDevicesDialogFragmentStatus.class.getSimpleName();

    private ArrayList<IoTDevice> mIoTDevicesList = new ArrayList<>();
    private ArrayList<IoTDevice> mDisabledDeviceList = new ArrayList<>();

    // button control
    ImageButton mCloseButton;
    Button mRefreshButton;
    Button mSendButton;
    GridView mGridView;
    StickyGridHeadersIoTDevicesAdapter mGridAdapter;
    int originalOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;

    Handler mHandler = new Handler();

    public static LoadIoTDevicesDialogFragmentStatus newInstance(Bundle b) {
        LoadIoTDevicesDialogFragmentStatus frag = new LoadIoTDevicesDialogFragmentStatus();
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
        setRetainInstance(true);
        originalOrientation = getActivity().getRequestedOrientation();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

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
        mGridView = (GridView) v.findViewById(R.id.iot_devices_grid);
        mGridView.setEmptyView(v.findViewById(android.R.id.empty));

        // set button control
        mCloseButton = (ImageButton) v.findViewById(R.id.btn_close);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick btnClose..");
                ((BaseActivity) getActivity()).dismissProgressDialog();
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
                ((BaseActivity) getActivity()).showProgressDialog();

                IoTInterface.getInstance().getDevicesList(DeviceTypes.ALL, LoadIoTDevicesDialogFragmentStatus.this, true);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((BaseActivity) getActivity()).dismissProgressDialog();
                    }
                }, 6000);
            }
        });

        mSendButton = (Button) v.findViewById(R.id.btn_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick btnSend..");
                ((BaseActivity) getActivity()).dismissProgressDialog();
                showSyncAlertDialog();
            }
        });

        mGridView.setOnItemClickListener(this);

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
        IoTInterface.getInstance().getDevicesList(DeviceTypes.ALL, this, true);

        ((BaseActivity) getActivity()).showProgressDialog();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((BaseActivity) getActivity()).dismissProgressDialog();
            }
        }, 6000);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "dialog onDetach");
//        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        getActivity().setRequestedOrientation(originalOrientation);
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
        ((BroadcastWebViewActivity) getActivity()).dismissUpdateIoTDevicesDialog();
    }

    /**
     * 버그라고 할 건아니지만...
     * => Caused by: java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
     * <p/>
     * There are two DialogFragment show() methods
     * - show(FragmentManager manager, String tag) and show(FragmentTransaction transaction, String tag).
     * <p/>
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
    public void onResult(final int cmd, final IoTServiceStatus serviceStatus, final IoTResultCodes serviceStatusCode, final Bundle b) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handleIoTReuslt(cmd, serviceStatus, serviceStatusCode, b);
            }
        });
    }

    private void handleIoTReuslt(int cmd, final IoTServiceStatus serviceStatus, final IoTResultCodes serviceStatusCode, Bundle b) {
        Log.d(TAG, "IoTServiceResponse onResult...serviceStatus = " + serviceStatus + ", statusCode = " + serviceStatusCode);
        switch (cmd) {
            case IoTServiceCommand.GET_DEVICE_LIST:
                if (serviceStatus == null || serviceStatusCode == null) {
                    Log.e(TAG, "Unknown service status...");
                    break;
                }

                if (!serviceStatus.equals(IoTServiceStatus.RUNNING)) {
                    ((BaseActivity) getActivity()).dismissProgressDialog();

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
        if (b == null) {
            return;
        }
        ArrayList<IoTDevice> iotDevicesList = b.getParcelableArrayList(IoTServiceCommand.KEY_DATA);
        if (iotDevicesList != null) {
            mIoTDevicesList = iotDevicesList;
        } else {
            mIoTDevicesList = new ArrayList<>();
        }
        mDisabledDeviceList.clear();

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
                ((BaseActivity) getActivity()).dismissProgressDialog();
            }
        }, 500);
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
                    IoTInterface.getInstance().getDevicesList(DeviceTypes.ALL, LoadIoTDevicesDialogFragmentStatus.this);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ((BaseActivity) getActivity()).dismissProgressDialog();
                        }
                    }, 6000);
                }
                break;
        }
    }

    /**
     * Remove dialog.
     */
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemClick postion = " + position);
        StickyGridHeadersIoTDevicesAdapter.ViewHolder viewHolder = (StickyGridHeadersIoTDevicesAdapter.ViewHolder)view.getTag();
        if (viewHolder instanceof StickyGridHeadersIoTDevicesAdapter.ViewHolder) {
            if (viewHolder.isChecked) {
                viewHolder.isChecked = false;
                if (mIoTDevicesList.contains(viewHolder.device)) {
                    mIoTDevicesList.remove(viewHolder.device);
                    Log.d(TAG, "remove from mIoTDevicesList and add mDisabledDeviceList");
                }
                mDisabledDeviceList.add(viewHolder.device);
                viewHolder.textView.setBackgroundColor(getResources().getColor(R.color.iot_devices_unselected));

                if (mIoTDevicesList.size() == 0) {
                    mSendButton.setEnabled(false);
                }
            } else {
                viewHolder.isChecked = true;
                if (mDisabledDeviceList.contains(viewHolder.device)) {
                    mDisabledDeviceList.remove(viewHolder.device);
                    Log.d(TAG, "remove from mDisabledDeviceList and add mIoTDevicesList");
                }
                mIoTDevicesList.add(viewHolder.device);
                viewHolder.textView.setBackgroundColor(getResources().getColor(R.color.iot_devices_selected));

                if (mIoTDevicesList.size() > 0) {
                    mSendButton.setEnabled(true);
                }
            }
        } else {
            Log.d(TAG, "is not a view holder....");
        }
    }


}
