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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
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

import com.nbplus.iotgateway.data.IoTDevice;
import com.nbplus.iotgateway.service.IoTService;
import com.nbplus.progress.ProgressDialogFragment;
import com.nbplus.vbroadlauncher.BaseActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.adapter.StickyGridHeadersIoTDevicesAdapter;
import com.nbplus.vbroadlauncher.data.Constants;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleArrayAdapter;

import org.basdroid.common.DisplayUtils;

import java.util.ArrayList;

import io.vov.vitamio.widget.CenterLayout;

/**
 * Created by basagee on 2015. 6. 23..
 */
public class LoadIoTDevicesDialogFragment extends DialogFragment implements DialogInterface.OnKeyListener {
    private static final String TAG = LoadIoTDevicesDialogFragment.class.getSimpleName();

    private ArrayList<IoTDevice> mIoTDevicesList = new ArrayList<>();

    // button control
    ImageButton mCloseButton;
    Button      mRefreshButton;
    Button      mSendButton;
    GridView    mGridView;
    StickyGridHeadersIoTDevicesAdapter mGridAdapter;

    Handler     mHandler = new Handler();
    ProgressDialogFragment mProgressDialogFragment;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = (intent == null) ? "" : intent.getAction();
            Log.d(TAG, ">> mBroadcastReceiver action received = " + action);
            // send handler message
            switch (action) {
                case com.nbplus.iotgateway.data.Constants.ACTION_IOT_DEVICE_LIST :
                    ArrayList<IoTDevice> iotDevicesList = intent.getParcelableArrayListExtra(com.nbplus.iotgateway.data.Constants.EXTRA_IOT_DEVICE_LIST);
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
                    break;
                default :
                    break;
            }
        }
    };

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

                Intent intent = new Intent(getActivity(), IoTService.class);
                intent.setAction(com.nbplus.iotgateway.data.Constants.ACTION_GET_IOT_DEVICE_LIST);
                getActivity().startService(intent);
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
        Point p = DisplayUtils.getScreenSize(getActivity());
        getDialog().getWindow().setLayout(p.x - 60, p.y - 30);
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
        Intent intent = new Intent(getActivity(), IoTService.class);
        intent.setAction(com.nbplus.iotgateway.data.Constants.ACTION_GET_IOT_DEVICE_LIST);
        getActivity().startService(intent);

        ((BaseActivity)getActivity()).showProgressDialog();

        IntentFilter filter = new IntentFilter();
        filter.addAction(com.nbplus.iotgateway.data.Constants.ACTION_IOT_DEVICE_LIST);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "dialog onDetach");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    public void showSyncAlertDialog() {
        new android.support.v7.app.AlertDialog.Builder(getActivity()).setMessage(R.string.iot_devices_send_alert)
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
                                sendIntent.putParcelableArrayListExtra(com.nbplus.iotgateway.data.Constants.EXTRA_IOT_DEVICE_LIST, mIoTDevicesList);
                                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(sendIntent);

                                dialog.dismiss();
                                dismissDialogFragment();
                            }
                        })
                .show();
    }

    private void dismissDialogFragment() {
        dismiss();
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
}
