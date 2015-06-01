package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nbplus.vbroadlauncher.BroadcastWebViewActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.ShowApplicationActivity;
import com.nbplus.vbroadlauncher.callback.OnFragmentInteractionListener;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.data.Types;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
import com.nbplus.vbroadlauncher.widget.IButton;

import org.basdroid.widget.TextClock;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LauncherFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LauncherFragment extends Fragment {
    private static final String TAG = LauncherFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private Button mServiceTreeMap;
    private Button mApplicationsView;
    private TextView mVillageName;
    private TextClock mTextClock;
    private Handler mHandler = new Handler();

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getActivity().setTitle("LauncherFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_launcher, container, false);

        mVillageName = (TextView)v.findViewById(R.id.launcher_village_name);
        mVillageName.setText(LauncherSettings.getInstance(getActivity()).getVillageName());

        mServiceTreeMap = (Button)v.findViewById(R.id.btn_show_map);
        mApplicationsView = (Button)v.findViewById(R.id.btn_show_apps);
        mApplicationsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ShowApplicationActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        mTextClock = (TextClock)v.findViewById(R.id.text_clock);
        if (mTextClock != null) {
            mTextClock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    Intent intent = new Intent(getActivity(), CalendarActivity.class);
                    try {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
                        startActivity(intent);
                        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setPositiveButton(R.string.alert_phone_finish_ok, new DialogInterface.OnClickListener() {
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

        GridLayout gridLayout = (GridLayout)v.findViewById(R.id.shortcut_grid);
        ArrayList<ShortcutData> shortcutDatas = LauncherSettings.getInstance(getActivity()).getLauncherShortcuts();
        int columnNum = gridLayout.getColumnCount();
        final int MAX_ROW_NUM = 3;

        int shortcutNum = shortcutDatas.size() > (columnNum * MAX_ROW_NUM) ? (columnNum * MAX_ROW_NUM) : shortcutDatas.size();
        // draw shortcut button
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (int i = 0; i < shortcutNum; i++) {
            /**
             * right shortcut panel
             */
            final IButton btn = (IButton)layoutInflater.inflate(R.layout.ibutton, gridLayout, false);
            gridLayout.addView(btn);

            ShortcutData data = shortcutDatas.get(i);
            btn.setText(data.getName());
            btn.setDrawable(data.getIconResId());
            btn.setBackground(data.getIconBackResId());
            btn.setTag(data);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ShortcutData data = (ShortcutData)view.getTag();
                    VBroadcastServer serverData = LauncherSettings.getInstance(getActivity()).getServerInformation();

                    String docServer = serverData.getDocServer();

                    Log.d(TAG, ">>> Clicked = " + data.getName());
                    Log.d(TAG, ">>> Open URL = " + docServer + data.getPath());

                    switch (data.getType()) {
                        case Types.SHORTCUT_TYPE_WEB_INTERFACE_SERVER :
                            data.setDomain(serverData.getApiServer());
                            break;
                        case Types.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER :
                            Intent intent = new Intent(getActivity(), BroadcastWebViewActivity.class);
                            data.setDomain(serverData.getDocServer());
                            intent.putExtra(Types.EXTRA_NAME_SHORTCUT_DATA, data);
                            startActivity(intent);
                            break;
                        case Types.SHORTCUT_TYPE_NATIVE_INTERFACE :
                            break;
                        default :
                            Log.d(TAG, "Unknown shortcut type !!!");
                    }
                }
            });
        }

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
