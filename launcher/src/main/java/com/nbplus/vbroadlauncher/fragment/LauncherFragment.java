package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.nbplus.vbroadlauncher.BroadcastWebViewActivity;
import com.nbplus.vbroadlauncher.HomeLauncherActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.ShowApplicationActivity;
import com.nbplus.vbroadlauncher.api.GsonRequest;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.callback.OnFragmentInteractionListener;
import com.nbplus.vbroadlauncher.data.ForecastSpaceData;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.VBroadcastServer;
import com.nbplus.vbroadlauncher.widget.IButton;

import com.nbplus.vbroadlauncher.widget.TextClock;

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
public class LauncherFragment extends Fragment implements OnActivityInteractionListener {
    private static final String TAG = LauncherFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private Button mServiceTreeMap;
    private Button mApplicationsView;
    private TextView mVillageName;
    private TextClock mTextClock;
    private LinearLayout mMainViewLayout;
    private Handler mHandler = new Handler();

    // TODO : for test
    private TextView mLocationData;

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

        // test...
        mLocationData = (TextView)v.findViewById(R.id.location_update);

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
                        case Constants.SHORTCUT_TYPE_WEB_INTERFACE_SERVER :
                            data.setDomain(serverData.getApiServer());
                            break;
                        case Constants.SHORTCUT_TYPE_WEB_DOCUMENT_SERVER :
                            Intent intent = new Intent(getActivity(), BroadcastWebViewActivity.class);
                            data.setDomain(serverData.getDocServer());
                            intent.putExtra(Constants.EXTRA_NAME_SHORTCUT_DATA, data);
                            startActivity(intent);
                            break;
                        case Constants.SHORTCUT_TYPE_NATIVE_INTERFACE :
                            break;
                        default :
                            Log.d(TAG, "Unknown shortcut type !!!");
                    }
                }
            });
        }

        updateWeather();

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

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mMainViewLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            mMainViewLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    @Override
    public void onLocationDataChanged(Location location) {
        long currTimems = System.currentTimeMillis();

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("H:mm ss");
        Date date = new Date(currTimems);
        String dateStr = sdf.format(date);
        mLocationData.setText(dateStr + " updated : lat = " + location.getLatitude() + ", lon = " + location.getLongitude());
    }



    ///////////////////////////////////
    // for weather
    ///////////////////////////////////
    public void updateWeather() {
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = Constants.WEATHER_SERVER_PREFIX + Constants.WEATHER_SERVICE_SPACEDATA + "?";
        url += Constants.WEATHER_PARAM_TYPE + "&" + Constants.WEATHER_PARAM_SERVICE_KEY + Constants.WEATHER_OPEN_API_KEY;

        Location currLoc = LauncherSettings.getInstance(getActivity()).getPreferredUserLocation();
        currLoc.setLongitude(126.929810);
        currLoc.setLatitude(37.488201);
        Location gridLoc = convertLocation2Grid(currLoc);
        Log.d(TAG, ">> gridLoc X = " + gridLoc.getLongitude() + ", Y = " + gridLoc.getLatitude());

        url += "&" + "nx=" + (int)gridLoc.getLongitude();
        url += "&" + "ny=" + (int)gridLoc.getLatitude();

        Date date = new Date(System.currentTimeMillis());

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyyMMdd");
        url += "&" + "base_date=" + sdf.format(date);
        sdf = new SimpleDateFormat("HHmm");

        // 동네예보 단기는 3시간간격으로 해당시간 + 3일치의 데이터가 내려온다. (02, 05, 08, 11, 14, 17, 20, 23)
        if (date.getTime() > 14) {
            url += "&" + "base_time=" + "1400";//sdf.format(date);
        } else {
            int remain = (int)date.getTime() % 3;
            if (remain % 3 == 2) {
                url += "&" + "base_time=" + date.getTime() + "00";//sdf.format(date);
            } else {
                url += "&" + "base_time=" + (date.getTime() - (remain + 1)) + "00";//sdf.format(date);
            }
        }
        url += "&" + "pageNo=" + 9;//sdf.format(date);

        GsonRequest jsRequest = new GsonRequest(Request.Method.GET, url, ForecastSpaceData.class, new Response.Listener<ForecastSpaceData>() {

            @Override
            public void onResponse(ForecastSpaceData response) {
                Log.d(TAG, ">>> volley success resultCode = " + response.getResultCode() + ", resultMessage = " + response.getResultMessage());
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, ">>> volley error");
            }
        });

        queue.add(jsRequest);
    }

    private class LamcParameter {
        public double mapRadius = Constants.DEFAULT_MAP_RADIUS;
        public double gridDistance = Constants.DEFAULT_GRID_DISTANCE;
        public double startLatitude = Constants.DEFAULT_STANDARD_LATITUDE_1;
        public double endLatitude = Constants.DEFAULT_STANDARD_LATITUDE_2;
        public double datumLatitude = Constants.DEFAULT_LATITUDE;
        public double datumLongitude = Constants.DEFAULT_LONGITITUDE;
        public double datumGridX = Constants.DEFAULT_GRID_X;
        public double datumGridY = Constants.DEFAULT_GRID_Y;
    }
    private final LamcParameter mLamcParameter = new LamcParameter();

    private Location convertLocation2Grid(Location location) {
        //  위경도 -> (X, Y)
        Location resLoc = lamcProjection(location, 0, mLamcParameter);
        resLoc.setLongitude(resLoc.getLongitude() + 1.5);
        resLoc.setLatitude(resLoc.getLatitude() + 1.5);
        return resLoc;
    }

    /**
     * Lambert Conformal Conic Projection
     * @param location 위경도 또는 격자값
     * @param convertType 0 : (위경도 -> 격자), 1 : (격자 -> 위경도)
     * @param mapParam
     * @return
     */
    private Location lamcProjection(Location location, int convertType, LamcParameter mapParam) {
        Location resLocation = new Location(location);

        double pi = Math.asin(1.0) * 2.0;
        double degrad = pi / 180.0;
        double raddeg = 180.0 / pi;

        double re = mapParam.mapRadius / mapParam.gridDistance;
        double slat1 = mapParam.startLatitude * degrad;
        double slat2 = mapParam.endLatitude * degrad;
        double olat = mapParam.datumLatitude * degrad;
        double olon = mapParam.datumLongitude * degrad;

        double sn = Math.tan(pi * 0.25 + slat2 * 0.5) / Math.tan(pi * 0.25 + slat1 * 0.5);
        Log.d(TAG, ">>> sn = " + sn);
        Log.d(TAG, ">>> Math.log(Math.cos(slat1) / Math.cos(slat2)) = " + Math.log(Math.cos(slat1) / Math.cos(slat2)));
        Log.d(TAG, ">>> Math.log(sn) = " + Math.log(sn));
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(pi * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(pi * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = 0;
        double theta = 0;
        double xn = 0, yn = 0, alat = 0, alon = 0;
        if (convertType == 0) {
            ra = Math.tan(pi * 0.25 + location.getLatitude() * degrad * 0.5);
            ra = re * sf / Math.pow(ra, sn);

            theta = location.getLongitude() * degrad - olon;
            if (theta > pi) {
                theta -= 2.0 * pi;
            }
            if (theta < -pi) {
                theta += 2.0 * pi;
            }

            theta *= sn;
            resLocation.setLongitude(ra * Math.sin(theta) + mapParam.datumGridX);
            resLocation.setLatitude(ro - ra * Math.cos(theta) + mapParam.datumGridY);
        } else {
            xn = location.getLongitude() - mapParam.datumGridX;
            yn = ro - location.getLatitude() + mapParam.datumGridY;

            ra = Math.sqrt(xn * xn + yn * yn);
            if (sn < 0.0) {
                ra = -ra;
            }

            alat = Math.pow(re * sf / ra, 1.0 / sn);
            alat = 2.0 * Math.atan(alat) - pi * 0.5;
            if (Math.abs(xn) <= 0.0) {
                theta = 0.0;
            } else {
                if (Math.abs(yn) <= 0.0) {
                    theta = pi * 0.5;
                    if (xn < 0.0) {
                        theta = -theta;
                    } else {
                        theta = Math.atan2(xn, yn);
                    }

                    alon = theta / sn + olon;
                    resLocation.setLatitude(alat * raddeg);
                    resLocation.setLongitude(alon * raddeg);
                }
            }
        }

        return resLocation;
    }
}
