package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nbplus.vbroadlauncher.BroadcastWebViewActivity;
import com.nbplus.vbroadlauncher.HomeLauncherActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.ShowApplicationActivity;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.callback.OnFragmentInteractionListener;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
//import com.nbplus.vbroadlauncher.widget.IButton;

import com.nbplus.vbroadlauncher.widget.TextClock;
import com.nbplus.vbroadlauncher.widget.WeatherView;
import com.nbplus.vbroadlauncher.widget.YahooWeatherView;

import org.basdroid.common.DisplayUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LauncherFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LauncherFragment extends Fragment implements OnActivityInteractionListener, View.OnClickListener {
    private static final String TAG = LauncherFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private Button mServiceTreeMap;
    private Button mApplicationsView;
    private TextView mVillageName;
    private TextClock mTextClock;
    private WeatherView mWeatherView;
    private LinearLayout mMainViewLayout;
    private LinearLayout mMainViewLeftPanel;
    private LinearLayout mMainViewRightPanel;
    private GridLayout mMainShortcutGridLayout;
    private GridLayout mShorcutGridLayout;
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

        mMainViewLayout = (LinearLayout)v.findViewById(R.id.main_view_layout);

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
        mWeatherView = (WeatherView)v.findViewById(R.id.weather_view);
        mMainViewLeftPanel = (LinearLayout)v.findViewById(R.id.main_view_left_panel);
        mMainViewRightPanel = (LinearLayout)v.findViewById(R.id.main_view_right_panel);

        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // add main shortcut.
        ArrayList<ShortcutData> mainShortcutDatas = LauncherSettings.getInstance(getActivity()).getLauncherMainShortcuts();
        mMainShortcutGridLayout = (GridLayout)v.findViewById(R.id.main_shortcut_grid);
        float dp = getResources().getDimension(R.dimen.launcher_ic_menu_main_shortcut_width) / getResources().getDisplayMetrics().density;
        float widthPx = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = getResources().getDimension(R.dimen.launcher_ic_menu_main_shortcut_height) / getResources().getDisplayMetrics().density;
        float heightPx = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = getResources().getDimension(R.dimen.launcher_ic_menu_main_shortcut_font_size) / getResources().getDisplayMetrics().density;
        float mainShortcutFontPx = DisplayUtils.pxFromDp(getActivity(), dp);
        for (int i = 0; i < mMainShortcutGridLayout.getColumnCount(); i++) {
            /**
             * right shortcut panel
             */
            ShortcutData data = mainShortcutDatas.get(i);
            LinearLayout btnLayout = (LinearLayout)layoutInflater.inflate(R.layout.launcher_menu_item, mMainShortcutGridLayout, false);//new Button(getActivity());

            btnLayout.setBackgroundResource(data.getIconBackResId());

            GridLayout.LayoutParams lp = (GridLayout.LayoutParams)btnLayout.getLayoutParams();
            lp.width = (int)widthPx;
            lp.height = (int)heightPx;
            btnLayout.setLayoutParams(lp);

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
            mMainShortcutGridLayout.addView(btnLayout);
        }

        // add other shortcuts.
        mShorcutGridLayout = (GridLayout)v.findViewById(R.id.shortcut_grid);
        ArrayList<ShortcutData> shortcutDatas = LauncherSettings.getInstance(getActivity()).getLauncherShortcuts();
        int columnNum = mShorcutGridLayout.getColumnCount();
        final int MAX_ROW_NUM = 3;

        int shortcutNum = shortcutDatas.size() > (columnNum * MAX_ROW_NUM) ? (columnNum * MAX_ROW_NUM) : shortcutDatas.size();
        // draw shortcut button
        dp = getResources().getDimension(R.dimen.launcher_ic_menu_shortcut_size) / getResources().getDisplayMetrics().density;
        float btnSizePx = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = getResources().getDimension(R.dimen.ic_nav_btn_drawable_padding) / getResources().getDisplayMetrics().density;
        float drawablePadding = DisplayUtils.pxFromDp(getActivity(), dp);

        dp = getResources().getDimension(R.dimen.launcher_ic_menu_shortcut_font_size) / getResources().getDisplayMetrics().density;
        float btnFontPx = DisplayUtils.pxFromDp(getActivity(), dp);

        for (int i = 0; i < shortcutNum; i++) {
            /**
             * right shortcut panel
             */
            ShortcutData data = shortcutDatas.get(i);
            LinearLayout btnLayout = (LinearLayout)layoutInflater.inflate(R.layout.launcher_menu_item, mShorcutGridLayout, false);//new Button(getActivity());

            btnLayout.setBackgroundResource(data.getIconBackResId());

            GridLayout.LayoutParams lp = (GridLayout.LayoutParams)btnLayout.getLayoutParams();
            lp.width = (int)btnSizePx;
            lp.height = (int) btnSizePx;
            btnLayout.setLayoutParams(lp);

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
            mShorcutGridLayout.addView(btnLayout);
        }

        setContentViewByOrientation();
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            ((HomeLauncherActivity)activity).setOnActivityInteractionListener(this);
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
            marginDp = getResources().getDimension(R.dimen.launcher_panel_margin) / getResources().getDisplayMetrics().density;
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
            marginDp = getResources().getDimension(R.dimen.launcher_panel_margin) / getResources().getDisplayMetrics().density;
            marginPx = DisplayUtils.pxFromDp(getActivity(), marginDp);
            lp.setMargins(0, 0, 0, (int)marginPx);
            mMainViewLeftPanel.setLayoutParams(lp);
            lp = (LinearLayout.LayoutParams)mMainViewRightPanel.getLayoutParams();
            lp.weight = 1.0f;
            lp.setMargins(0, 0, 0, 0);
            mMainViewRightPanel.setLayoutParams(lp);
        }
        lp = (LinearLayout.LayoutParams)mMainViewLayout.getLayoutParams();

        float heightDp = getResources().getDimension(R.dimen.launcher_main_view_margin_top) / getResources().getDisplayMetrics().density;
        float px = DisplayUtils.pxFromDp(getActivity(), heightDp);
        float horizontalMarginDp = getResources().getDimension(R.dimen.launcher_main_view_margin_horizontal) / getResources().getDisplayMetrics().density;
        float horizontalMarginPx = DisplayUtils.pxFromDp(getActivity(), horizontalMarginDp);

        lp.setMargins((int)horizontalMarginPx, (int)px, (int)horizontalMarginPx, lp.bottomMargin);
        mMainViewLayout.setLayoutParams(lp);

        float dp = getResources().getDimension(R.dimen.launcher_clock_height) / getResources().getDisplayMetrics().density;
        px = DisplayUtils.pxFromDp(getActivity(), dp);
        mTextClock.setTextSize(px);
        mWeatherView.onConfigurationChanged(orientation);
    }

    /**
     * when click right panel shortcut button ...
     * @param view
     */
    @Override
    public void onClick(View view) {
        ShortcutData data = (ShortcutData) view.getTag();
        VBroadcastServer serverData = LauncherSettings.getInstance(getActivity()).getServerInformation();

        Log.d(TAG, ">>> Clicked = " + data.getName());
        Log.d(TAG, ">>> Open URL = " + serverData.getDocServer() + data.getPath());

        switch (data.getType()) {
            case Constants.SHORTCUT_TYPE_WEB_INTERFACE_SERVER:
                data.setDomain(serverData.getApiServer());
                break;
            case Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER:
                Intent intent = new Intent(getActivity(), BroadcastWebViewActivity.class);
                data.setDomain(serverData.getDocServer());
                intent.putExtra(Constants.EXTRA_NAME_SHORTCUT_DATA, data);
                startActivity(intent);
                break;
            case Constants.SHORTCUT_TYPE_NATIVE_INTERFACE:
                FragmentManager fm = getActivity().getSupportFragmentManager();
                RadioDialogFragment dialogFragment = new RadioDialogFragment();
                Bundle bundle = new Bundle();
                data.setDomain(serverData.getDocServer());
                bundle.putParcelable(Constants.EXTRA_NAME_SHORTCUT_DATA, data);
                dialogFragment.setArguments(bundle);
                dialogFragment.show(fm, "fragment_dialog_radio");
                break;
            default:
                Log.d(TAG, "Unknown shortcut type !!!");
        }
    }
}
