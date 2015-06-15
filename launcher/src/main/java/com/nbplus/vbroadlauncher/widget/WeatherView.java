package com.nbplus.vbroadlauncher.widget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.api.GsonRequest;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.ForecastItem;
import com.nbplus.vbroadlauncher.data.ForecastSpaceData;
import com.nbplus.vbroadlauncher.data.ForecastTimeData;
import com.nbplus.vbroadlauncher.data.LauncherSettings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by basagee on 2015. 6. 3..
 */
public class WeatherView extends LinearLayout {
    private static final String TAG = WeatherView.class.getSimpleName();

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

    private boolean mAttached;
    private boolean mLocationChangedReceived = false;
    RequestQueue mWeatherRequestQueue;
    private ArrayList<ForecastItem> mForecastSpaceDataItems;
    private ArrayList<ForecastItem> mForecastTimeDataItems;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (Constants.WEATHER_SERVICE_UPDATE_ACTION.equals(intent.getAction())) {
                    Log.d(TAG, "Weather service action received !!!");
                    releaseAlarm();

                    // update weather and set next alarm
                    //updateWeather();
                    setNextAlarm();
                } else if (Constants.LOCATION_CHANGED_ACTION.equals(intent.getAction())) {
                    Log.d(TAG, "LOCATION_CHANGED_ACTION received !!!");
                    if (!mLocationChangedReceived) {
                        mLocationChangedReceived = true;
                        releaseAlarm();

                        // update weather and set next alarm
                        updateWeather();
                        setNextAlarm();
                    }
                }
            }
        }
    };

    public WeatherView(Context context) {
        super(context);
        initializeWeatherView(context);
    }

    public WeatherView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeWeatherView(context);
    }

    public WeatherView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeWeatherView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WeatherView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeWeatherView(context);
    }

    private void initializeWeatherView(Context context) {
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = li.inflate(R.layout.weather_view, this, false);
        addView(v);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;

            registerReceiver();
            setAlarm(System.currentTimeMillis() + 5000);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached) {
            unregisterReceiver();
            mAttached = false;
        }
        Log.d(TAG, "weatherview onDetachedFromWindow!!!");
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();

        filter.addAction(Constants.WEATHER_SERVICE_UPDATE_ACTION);
        filter.addAction(Constants.LOCATION_CHANGED_ACTION);
        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }

    // 알람 등록
    private void setNextAlarm() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        Log.d(TAG, ">> Current getTimeInMillis = " + calendar.getTimeInMillis());
        Log.d(TAG, ">> Current hour = " + hour + ", minutes = " + minutes);

        // 동네예보 단기는 3시간간격으로 해당시간 + 3일치의 데이터가 내려온다. (02, 05, 08, 11, 14, 17, 20, 23)
        int remain = hour % 3;
        if (remain == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, hour + 3);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, hour + (2 - remain));
        }
        calendar.set(Calendar.MINUTE, 35);
        Log.d(TAG, ">> Next alarm hour = " + calendar.get(Calendar.HOUR_OF_DAY) + ", minutes = " + calendar.get(Calendar.MINUTE));

        calendar.getTimeInMillis();
        setAlarm(calendar.getTimeInMillis());
    }
    private void setAlarm(long milliseconds){
        Log.i(TAG, "setAlarm() = " + milliseconds);
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        Intent Intent = new Intent(Constants.WEATHER_SERVICE_UPDATE_ACTION);
        PendingIntent pIntent = PendingIntent.getBroadcast(getContext(), 0, Intent, 0);

        alarmManager.set(AlarmManager.RTC, milliseconds, pIntent);
    }

    // 알람 해제
    private void releaseAlarm() {
        Log.i(TAG, "releaseAlarm()");
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        Intent Intent = new Intent(Constants.WEATHER_SERVICE_UPDATE_ACTION);
        PendingIntent pIntent = PendingIntent.getBroadcast(getContext(), 0, Intent, 0);
        alarmManager.cancel(pIntent);

        // 주석을 풀면 먼저 실행되는 알람이 있을 경우, 제거하고
        // 새로 알람을 실행하게 된다. 상황에 따라 유용하게 사용 할 수 있다.
//      alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 3000, pIntent);
    }

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

    ///////////////////////////////////
    // for weather
    ///////////////////////////////////
    public void updateWeather() {
        // 초단기예보
        updateForecastTimeData(1);
        // 단기예보
        updateForecastSpaceData(1);
    }

    public void updateForecastTimeData(int pageNo) {
        if (mWeatherRequestQueue == null) {
            mWeatherRequestQueue = Volley.newRequestQueue(getContext());
        }
        String url = Constants.WEATHER_SERVER_PREFIX + Constants.WEATHER_SERVICE_TIMEDATA + "?";
        url += Constants.WEATHER_PARAM_TYPE + "&" + Constants.WEATHER_PARAM_SERVICE_KEY + Constants.WEATHER_OPEN_API_KEY;

        Location currLoc = LauncherSettings.getInstance(getContext()).getPreferredUserLocation();
        if (currLoc == null) {
            Log.d(TAG, ">> set default location");
            currLoc = new Location("stub");
            currLoc.setLongitude(126.929810);
            currLoc.setLatitude(37.488201);
        }

        Location gridLoc = convertLocation2Grid(currLoc);
        Log.d(TAG, ">> gridLoc X = " + gridLoc.getLongitude() + ", Y = " + gridLoc.getLatitude());

        url += "&" + "nx=" + (int)gridLoc.getLongitude();
        url += "&" + "ny=" + (int)gridLoc.getLatitude();


        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        // 동네예보 초단기는 매시간 30분에 발표된다.
        if (minutes < 30) {
            if ((hour - 1) < 0) {
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
            }
            int newHour = (hour - 1) < 0 ? 24 + (hour - 1) : (hour - 1);
            url += "&" + "base_time=" + newHour + "30";
        } else {
            url += "&" + "base_time=" + (hour) + "30";
        }

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyyMMdd");
        url += "&" + "base_date=" + sdf.format(calendar.getTime());
        url += "&" + "pageNo=" + pageNo;

        Log.d(TAG, ">> ReqURL = " + url);
        GsonRequest jsRequest = new GsonRequest(Request.Method.GET, url, ForecastTimeData.class, new Response.Listener<ForecastTimeData>() {

            @Override
            public void onResponse(ForecastTimeData response) {
                Log.d(TAG, ">>> volley success resultCode = " + response.getResultCode() + ", resultMessage = " + response.getResultMessage());
                if (Constants.WEATHER_RESULT_OK.equals(response.getResultCode())) {
                    // for log
                    Gson gson = new GsonBuilder().create();
                    Log.d(TAG, ">>> respLog = " + gson.toJson(response));
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, ">>> volley error");
            }
        });

        mWeatherRequestQueue.add(jsRequest);
    }
    public void updateForecastSpaceData(int pageNo) {
        if (mWeatherRequestQueue == null) {
            mWeatherRequestQueue = Volley.newRequestQueue(getContext());
        }
        String url = Constants.WEATHER_SERVER_PREFIX + Constants.WEATHER_SERVICE_SPACEDATA + "?";
        url += Constants.WEATHER_PARAM_TYPE + "&" + Constants.WEATHER_PARAM_SERVICE_KEY + Constants.WEATHER_OPEN_API_KEY;

        Location currLoc = LauncherSettings.getInstance(getContext()).getPreferredUserLocation();
        if (currLoc == null) {
            Log.d(TAG, ">> set default location");
            currLoc = new Location("stub");
            currLoc.setLongitude(126.929810);
            currLoc.setLatitude(37.488201);
        }

        Location gridLoc = convertLocation2Grid(currLoc);
        Log.d(TAG, ">> gridLoc X = " + gridLoc.getLongitude() + ", Y = " + gridLoc.getLatitude());

        url += "&" + "nx=" + (int)gridLoc.getLongitude();
        url += "&" + "ny=" + (int)gridLoc.getLatitude();


        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        // 동네예보 단기는 3시간간격으로 해당시간 + 3일치의 데이터가 내려온다. (02, 05, 08, 11, 14, 17, 20, 23)
        int remain = hour % 3;
        if (remain == 2) {
            url += "&" + "base_time=" + (hour) + "00";
        } else {
            if (hour - (remain + 1) < 0) {
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
            }
            int newHour = (hour - (remain + 1)) < 0 ? 24  - (remain + 1) : (hour - (remain + 1));
            url += "&" + "base_time=" + (newHour) + "00";
        }

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyyMMdd");
        url += "&" + "base_date=" + sdf.format(calendar.getTime());
        url += "&" + "pageNo=" + pageNo;//sdf.format(date);

        Log.d(TAG, ">> ReqURL = " + url);
        GsonRequest jsRequest = new GsonRequest(Request.Method.GET, url, ForecastSpaceData.class, new Response.Listener<ForecastSpaceData>() {

            @Override
            public void onResponse(ForecastSpaceData response) {
                Log.d(TAG, ">>> volley success resultCode = " + response.getResultCode() + ", resultMessage = " + response.getResultMessage());
                if (Constants.WEATHER_RESULT_OK.equals(response.getResultCode())) {
                    // for log
                    Gson gson = new GsonBuilder().create();
                    Log.d(TAG, ">>> respLog = " + gson.toJson(response));
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, ">>> volley error");
            }
        });

        mWeatherRequestQueue.add(jsRequest);
    }
}
