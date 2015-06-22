package com.nbplus.vbroadlauncher.widget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.location.Location;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.api.GsonRequest;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.ForecastGrib;
import com.nbplus.vbroadlauncher.data.ForecastItem;
import com.nbplus.vbroadlauncher.data.ForecastSpaceData;
import com.nbplus.vbroadlauncher.data.ForecastTimeData;
import com.nbplus.vbroadlauncher.data.LauncherSettings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

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
    RequestQueue mWeatherRequestQueue;
    private ArrayList<ForecastItem> mForecastSpaceDataItems = new ArrayList<ForecastItem>();
    private ArrayList<ForecastItem> mForecastTimeDataItems = new ArrayList<ForecastItem>();
    private ArrayList<ForecastItem> mForecastGribItems = new ArrayList<ForecastItem>();
    private int mForecastSpaceDataNumRowsPerPage = 0;
    private int mForecastSpaceDataTotalCount = 0;
    private int mForecastSpaceDataRequestPage = 0;
    private int mForecastSpaceDataRetry = 0;
    private int mForecastTimeDataRetry = 0;
    private int mForecastGribRetry = 0;

    // layout
    private LinearLayout mThreeDaysLayout;
    private LinearLayout mTodayLayout;
    private LinearLayout mTomorrowLayout;
    private LinearLayout mDayAfterTomorrowLayout;

    /**
     * weather views
     */
    // current
    private TextView mCurrentTitle;
    private TextView mCurrentCelsius;
    private TextView mCurrentSkyStatus;

    // today
    private TextView mTodayTitle;
    private ImageView mTodaySkyStatus;
    private TextView mTodayMaxCelsius;
    // 오후 2시 이후에 발표하는 데이터에는 오늘의 최고기온이 없다.
    private String mLastMaxTodayCelsius;
    // tomorrow
    private TextView mTomorrowTitle;
    private ImageView mTomorrowSkyStatus;
    private TextView mTomorrowMaxCelsius;
    // day after tomorrow
    private TextView mDayAfterTomorrowTitle;
    private ImageView mDayAfterTomorrowSkyStatus;
    private TextView mDayAfterTomorrowMaxCelsius;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (Constants.WEATHER_SERVICE_GRIB_UPDATE_ACTION.equals(intent.getAction())) {
                    Log.d(TAG, "Weather service action received !!!");
                    releaseAlarm(Constants.WEATHER_SERVICE_GRIB_UPDATE_ACTION);

                    // update weather and set next alarm
                    updateForecastGrib(1);
                    setNextForecastGribAlarm();
                } else if (Constants.WEATHER_SERVICE_SPACE_UPDATE_ACTION.equals(intent.getAction())) {
                    Log.d(TAG, "Weather service action received !!!");
                    releaseAlarm(Constants.WEATHER_SERVICE_SPACE_UPDATE_ACTION);

                    // update weather and set next alarm
                    updateForecastSpaceData(1);
                    setNextForecastSpaceDataAlarm();
                } else if (Constants.LOCATION_CHANGED_ACTION.equals(intent.getAction()) ||
                        Constants.WEATHER_SERVICE_DEFAULT_TIMER.equals(intent.getAction())) {
                    Log.d(TAG, "LOCATION_CHANGED_ACTION received !!!");
                    releaseAlarm(Constants.WEATHER_SERVICE_DEFAULT_TIMER);

                    // update weather and set next alarm
                    // 실황예보
                    updateForecastGrib(1);
                    // 단기예보
                    updateForecastSpaceData(1);
                    setNextForecastGribAlarm();
                    setNextForecastSpaceDataAlarm();
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

        mThreeDaysLayout = (LinearLayout) v.findViewById(R.id.ll_three_days);
        mTodayLayout = (LinearLayout) v.findViewById(R.id.ll_todays);
        mTomorrowLayout = (LinearLayout) v.findViewById(R.id.ll_tomorrow);
        mDayAfterTomorrowLayout = (LinearLayout) v.findViewById(R.id.ll_day_after_tomorrow);

        /**
         * weather views
         */
        // current
        mCurrentTitle = (TextView) v.findViewById(R.id.village_name);
        mCurrentTitle.setText(getContext().getString(R.string.weather_title,
                LauncherSettings.getInstance(getContext()).getVillageName()));

        mCurrentCelsius = (TextView) v.findViewById(R.id.current_celsius);
        mCurrentSkyStatus = (TextView) v.findViewById(R.id.current_sky_status);

        // today
        mTodayTitle = (TextView) v.findViewById(R.id.today);
        mTodaySkyStatus = (ImageView) v.findViewById(R.id.today_sky_status_icon);
        mTodayMaxCelsius = (TextView) v.findViewById(R.id.today_max_celsius);
        // tomorrow
        mTomorrowTitle = (TextView) v.findViewById(R.id.tomorrow);
        mTomorrowSkyStatus = (ImageView) v.findViewById(R.id.tomorrow_sky_status_icon);
        mTomorrowMaxCelsius = (TextView) v.findViewById(R.id.tomorrow_max_celsius);
        // day after tomorrow
        mDayAfterTomorrowTitle = (TextView) v.findViewById(R.id.day_after_tomorrow);
        mDayAfterTomorrowSkyStatus = (ImageView) v.findViewById(R.id.day_after_tomorrow_sky_status_icon);
        mDayAfterTomorrowMaxCelsius = (TextView) v.findViewById(R.id.day_after_tomorrow_max_celsius);

        Calendar calendar = Calendar.getInstance();
        Resources res = getResources();
        String[] weekStringArray = res.getStringArray(R.array.week_day);

        // today
        mTodayTitle.setText(getContext().getString(R.string.weather_day,
                calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));

        calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
        mTomorrowTitle.setText(getContext().getString(R.string.weather_day,
                calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));
        calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
        mDayAfterTomorrowTitle.setText(getContext().getString(R.string.weather_day,
                calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;

            registerReceiver();
            if (LauncherSettings.getInstance(getContext()).getPreferredUserLocation() != null) {
                Intent intent = new Intent(Constants.WEATHER_SERVICE_DEFAULT_TIMER);
                getContext().sendBroadcast(intent);
            } else {
                setAlarm(System.currentTimeMillis() + 5000, Constants.WEATHER_SERVICE_DEFAULT_TIMER);
            }
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

        filter.addAction(Constants.WEATHER_SERVICE_DEFAULT_TIMER);
        filter.addAction(Constants.WEATHER_SERVICE_GRIB_UPDATE_ACTION);
        filter.addAction(Constants.WEATHER_SERVICE_TIME_UPDATE_ACTION);
        filter.addAction(Constants.WEATHER_SERVICE_SPACE_UPDATE_ACTION);
        filter.addAction(Constants.LOCATION_CHANGED_ACTION);
        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }

    // 알람 등록
    private void setNextForecastGribAlarm() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        // 실황예보는 1시간 간격으로
        if (minutes >= 30) {
            calendar.set(Calendar.HOUR_OF_DAY, hour + 1);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
        }
        calendar.set(Calendar.MINUTE, 35);
        Log.d(TAG, ">> setNextForecastGribAlarm = " + calendar.get(Calendar.HOUR_OF_DAY) + ", minutes = " + calendar.get(Calendar.MINUTE));

        calendar.getTimeInMillis();
        setAlarm(calendar.getTimeInMillis(), Constants.WEATHER_SERVICE_GRIB_UPDATE_ACTION);
    }
    private void setNextForecastSpaceDataAlarm() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        // 동네예보 단기는 3시간간격으로 해당시간 + 3일치의 데이터가 내려온다. (02, 05, 08, 11, 14, 17, 20, 23)
        int remain = hour % 3;
        if (remain == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, hour + 3);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, hour + (2 - remain));
        }
        calendar.set(Calendar.MINUTE, 35);
        Log.d(TAG, ">> setNextForecastSpaceDataAlarm = " + calendar.get(Calendar.HOUR_OF_DAY) + ", minutes = " + calendar.get(Calendar.MINUTE));

        calendar.getTimeInMillis();
        setAlarm(calendar.getTimeInMillis(), Constants.WEATHER_SERVICE_SPACE_UPDATE_ACTION);
    }
    private void setAlarm(long milliseconds, String action){
        Log.i(TAG, "setAlarm() = " + milliseconds);
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        if (action == null) {
            action = Constants.WEATHER_SERVICE_DEFAULT_TIMER;
        }
        Intent Intent = new Intent(action);
        PendingIntent pIntent = PendingIntent.getBroadcast(getContext(), 0, Intent, 0);

        alarmManager.set(AlarmManager.RTC, milliseconds, pIntent);
    }

    // 알람 해제
    private void releaseAlarm(String action) {
        Log.i(TAG, "releaseAlarm()");
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        Intent Intent = new Intent(action);
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
        //Log.d(TAG, ">>> sn = " + sn);
        //Log.d(TAG, ">>> Math.log(Math.cos(slat1) / Math.cos(slat2)) = " + Math.log(Math.cos(slat1) / Math.cos(slat2)));
        //Log.d(TAG, ">>> Math.log(sn) = " + Math.log(sn));
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
    /**
     * 실황데이터
     * @param pageNo
     */
    public void updateForecastGrib(int pageNo) {
        if (mWeatherRequestQueue == null) {
            mWeatherRequestQueue = Volley.newRequestQueue(getContext());
        }
        String url = Constants.WEATHER_SERVER_PREFIX + Constants.WEATHER_SERVICE_GRIB + "?";
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

        // 실황은 매시간 30분에 해당시간자료가 발표
        if (minutes < 30) {
            if ((hour - 1) < 0) {
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
            }
            int newHour = (hour - 1) < 0 ? 24 + (hour - 1) : (hour - 1);
            url += "&" + "base_time=" + ((newHour) < 10 ? "0" + newHour : newHour) + "00";
        } else {
            url += "&" + "base_time=" + ((hour) < 10 ? "0" + hour : hour) + "00";
        }

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyyMMdd");
        url += "&" + "base_date=" + sdf.format(calendar.getTime());
        url += "&" + "pageNo=" + pageNo;

        if (pageNo == 1) {
            Log.d(TAG, ">> ReqURL = " + url);
        }
        GsonRequest jsRequest = new GsonRequest(Request.Method.GET, url, ForecastGrib.class, new Response.Listener<ForecastGrib>() {

            @Override
            public void onResponse(ForecastGrib response) {
                Log.d(TAG, ">>> updateForecastGrib success resultCode = " + response.getResultCode() + ", resultMessage = " + response.getResultMessage());
                mForecastGribRetry = 0;
                if (Constants.RESULT_OK.equals(response.getResultCode())) {
                    mForecastGribItems.clear();
                    mForecastGribItems = response.getWeatherItems();

                    updateForecastGribWeatherView();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, ">>> updateForecastGrib error");
                mForecastGribRetry++;
                if (mForecastGribRetry > 3) {     // retry 3 times
                    mForecastGribRetry = 0;
                    Log.d(TAG, ">> 3 회재시도 실패.... 더이상조회할게없다. !!!");
                    updateForecastGribWeatherView();
                } else {                // retry
                    Log.d(TAG, ">> 재시도를한다.  retry count = " + mForecastSpaceDataRetry);
                    updateForecastGrib(1);
                }
            }
        });

        mWeatherRequestQueue.add(jsRequest);
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
            url += "&" + "base_time=" + ((newHour) < 10 ? "0" + newHour : newHour) + "30";
        } else {
            url += "&" + "base_time=" + ((hour) < 10 ? "0" + hour : hour) + "30";
        }

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyyMMdd");
        url += "&" + "base_date=" + sdf.format(calendar.getTime());
        url += "&" + "pageNo=" + pageNo;

        if (pageNo == 1) {
            Log.d(TAG, ">> ReqURL = " + url);
        }
        GsonRequest jsRequest = new GsonRequest(Request.Method.GET, url, ForecastTimeData.class, new Response.Listener<ForecastTimeData>() {

            @Override
            public void onResponse(ForecastTimeData response) {
                Log.d(TAG, ">>> updateForecastTimeData success resultCode = " + response.getResultCode() + ", resultMessage = " + response.getResultMessage());
                mForecastTimeDataRetry = 0;
                if (Constants.RESULT_OK.equals(response.getResultCode())) {
                    mForecastTimeDataItems.clear();
                    mForecastTimeDataItems = response.getWeatherItems();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, ">>> updateForecastTimeData error");
                mForecastSpaceDataRetry++;
                if (mForecastTimeDataRetry > 3) {     // retry 3 times
                    mForecastTimeDataRetry = 0;
                    Log.d(TAG, ">> 3 회재시도 실패.... 더이상조회할게없다. !!!");
                } else {                // retry
                    Log.d(TAG, ">> 재시도를한다.  retry count = " + mForecastSpaceDataRetry);
                    updateForecastTimeData(1);
                }
            }
        });

        mWeatherRequestQueue.add(jsRequest);
    }

    /**
     * 단기 예보
     * @param pageNo
     */
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
            // 해당시간대에도 30분 이전에는 이전시간을 사용한다.
            if (minutes < 30) {
                if (hour - (remain + 1) < 0) {
                    calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
                }
                int newHour = (hour - 3) < 0 ? 24  - (3 - hour) : (hour - 3);
                url += "&" + "base_time=" + ((newHour) < 10 ? "0" + newHour : newHour) + "00";
            } else {
                url += "&" + "base_time=" + ((hour) < 10 ? "0" + hour : hour) + "00";
            }
        } else {
            if (hour - (remain + 1) < 0) {
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
            }
            int newHour = (hour - (remain + 1)) < 0 ? 24  - (remain + 1) : (hour - (remain + 1));
            url += "&" + "base_time=" + ((newHour) < 10 ? "0" + newHour : newHour) + "00";
        }

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyyMMdd");
        url += "&" + "base_date=" + sdf.format(calendar.getTime());
        url += "&" + "pageNo=" + pageNo;//sdf.format(date);
        mForecastSpaceDataRequestPage = pageNo;

        if (pageNo == 1) {
            Log.d(TAG, ">> ReqURL = " + url);
        }
        GsonRequest jsRequest = new GsonRequest(Request.Method.GET, url, ForecastSpaceData.class, new Response.Listener<ForecastSpaceData>() {

            @Override
            public void onResponse(ForecastSpaceData response) {
                Log.d(TAG, ">>> updateForecastSpaceData success resultCode = " + response.getResultCode() + ", resultMessage = " + response.getResultMessage());
                Log.d(TAG, ">>> ForecastSpaceData get page = " + response.getNumOfPage() + ", rows = " + response.getRowsPerPage());

                mForecastSpaceDataRetry = 0;
                if (Constants.RESULT_OK.equals(response.getResultCode())) {
                    if (response.getNumOfPage() == 1) {
                        mForecastSpaceDataItems.clear();
                        mForecastSpaceDataItems = response.getWeatherItems();

                        if (response.getRowsPerPage() < response.getTotalCount()) {
                            updateForecastSpaceData(response.getNumOfPage() + 1);
                            mForecastSpaceDataNumRowsPerPage = response.getRowsPerPage();
                            mForecastSpaceDataTotalCount = response.getTotalCount();
                        } else {
                            // end of update all space data
                            updateForecastSpaceDataWeatherView();
                        }
                    } else {
                        mForecastSpaceDataItems.addAll(mForecastSpaceDataItems.size(), response.getWeatherItems());
                        if (((response.getNumOfPage() - 1) * mForecastSpaceDataNumRowsPerPage + response.getRowsPerPage()) < response.getTotalCount()) {
                            updateForecastSpaceData(response.getNumOfPage() + 1);
                        } else {
                            // end of update all space data
                            updateForecastSpaceDataWeatherView();
                        }
                    }
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, ">>> updateForecastSpaceData error... retry = " + error.toString());
                int page = mForecastSpaceDataRetry & 0xff00 >> 8;
                if (page == 0) {        // retry 이력이없음.
                    mForecastSpaceDataRetry = (mForecastSpaceDataRequestPage << 8);
                    mForecastSpaceDataRetry++;
                    Log.d(TAG, ">> RETRY HISTORY = NONE");
                    updateForecastSpaceData(mForecastSpaceDataRequestPage);
                } else {
                    int retryCnt = mForecastSpaceDataRetry & 0x00ff;
                    if (retryCnt > 3) {     // retry 3 times
                        // try next page
                        if (mForecastSpaceDataRequestPage * mForecastSpaceDataNumRowsPerPage < mForecastSpaceDataTotalCount) {
                            mForecastSpaceDataRequestPage ++;
                            mForecastSpaceDataRetry = (mForecastSpaceDataRequestPage << 8);
                            mForecastSpaceDataRetry++;
                            Log.d(TAG, ">> 3 회재시도 실패.... 다음페이지 = " + mForecastSpaceDataRequestPage);
                            updateForecastSpaceData(mForecastSpaceDataRequestPage);
                        } else {
                            // end of update all space data
                            Log.d(TAG, ">> 3 회재시도 실패.... 더이상조회할게없다. !!!");
                            mForecastSpaceDataRetry = 0;
                            updateForecastSpaceDataWeatherView();
                        }
                    } else {                // retry
                        Log.d(TAG, ">> 재시도를한다.  page = " + page + ", retry count = " + (mForecastSpaceDataRetry & 0x00ff));
                        mForecastSpaceDataRetry++;
                        updateForecastSpaceData(mForecastSpaceDataRequestPage);
                    }
                }
            }
        });

        mWeatherRequestQueue.add(jsRequest);
    }

    /**
     * 기상청에서 받아온 날씨데이터를 화면에 업데이트
     */
    private ForecastItem getForecastItemCategory(ArrayList<ForecastItem> items, String category) {
        ForecastItem item = null;

        if (category == null) {
            return null;
        }

        for (int i = 0; i < items.size(); i++) {
            ForecastItem tmp = items.get(i);
            if (category.equals(tmp.category)) {
                item = tmp;
                break;
            }
        }

        return item;
    }

    private class ForecastOfDay {
        public ForecastItem maxCelsius;
        public ForecastItem skyStatus;
    }

    /**
     * 특정 날짜의 최고기온과 하늘상태를 가져온다.
     * @param items
     * @param forecastDate
     * @return
     */
    private ForecastOfDay getMaxCelsiusAndSkyStatusOfDay(ArrayList<ForecastItem> items, String forecastDate) {
        if (forecastDate == null) {
            return null;
        }

        ForecastOfDay result = new ForecastOfDay();
        int searchDate = Integer.parseInt(forecastDate);
        int baseTime = 1500;

        for (int i = 0; i < items.size(); i++) {
            ForecastItem tmp = items.get(i);

            int fcstDate;
            try {
                fcstDate = Integer.parseInt(tmp.fcstDate);
            } catch (NumberFormatException e) {
                continue;
            }

            if (fcstDate > searchDate) {
                break;
            }
            if (fcstDate == searchDate) {
                if (ForecastItem.TMX.equals(tmp.category)) {
                    if (result.maxCelsius == null) {
                        result.maxCelsius = tmp;
                    } else {
                        // 시간을비교해서 15:00를최적으로한다.
                        int fcstTime = Integer.parseInt(tmp.fcstTime);
                        int resultTime = Integer.parseInt(result.maxCelsius.fcstTime);
                        if (fcstTime > resultTime && fcstTime <= baseTime) {
                            result.maxCelsius = tmp;
                        }
                    }
                }
                if (ForecastItem.SKY.equals(tmp.category)) {
                    if (result.skyStatus == null) {
                        result.skyStatus = tmp;
                    } else {
                        // 시간을비교해서 14:00를최적으로한다.
                        int fcstTime = Integer.parseInt(tmp.fcstTime);
                        int resultTime = Integer.parseInt(result.maxCelsius.fcstTime);
                        if (fcstTime > resultTime && fcstTime <= baseTime) {
                            result.skyStatus = tmp;
                        }
                    }
                }
            } else {
                continue;
            }
        }

        return result;
    }
    private void updateForecastGribWeatherView() {
        Log.d(TAG, ">> updateForecastGribWeatherViewWeatherView().......");

        ForecastItem item = null;
        int skyStatusValue = 1;

        item = getForecastItemCategory(mForecastGribItems, ForecastItem.SKY);
        if (item != null) {

            /**
             * 하늘상태 단위 - OpenAPI 활용 가이드(기상청_동네예보정보조회서비스)_v1.1.docx
             * ----------------------------------------------------------------
             *    하늘상태(SKY)         |                코드값
             * ----------------------------------------------------------------
             *        맑음             |                  1
             *       구름조금           |                  2
             *       구름많음           |                  3
             *        흐림             |                  4
             * ----------------------------------------------------------------
             */

            Resources res = getResources();
            String[] skyStatusArray = res.getStringArray(R.array.sky_status);
            skyStatusValue = Integer.parseInt(item.obsrValue);

            String skyStatus = skyStatusArray[0];
            if (skyStatusValue >= 1 && skyStatusValue <= 4) {       // 없는값이올수있을까??
                skyStatus = skyStatusArray[skyStatusValue - 1];
            }

            if (mCurrentSkyStatus != null) {
                mCurrentSkyStatus.setText(getContext().getString(R.string.sky_status, skyStatus, "무엇을?"));
            }
        }
        item = getForecastItemCategory(mForecastGribItems, ForecastItem.T1H);
        if (item != null) {
            if (mCurrentCelsius != null) {
                try {
                    float currCelsiusVal = Float.parseFloat(item.obsrValue);
                    float lastMaxCelsius = 0f;
                    if (mLastMaxTodayCelsius != null) {
                        lastMaxCelsius = Float.parseFloat(mLastMaxTodayCelsius);
                    }
                    if (lastMaxCelsius < currCelsiusVal) {
                        mLastMaxTodayCelsius = item.obsrValue;
                        mTodayMaxCelsius.setText(getContext().getString(R.string.celsius, item.obsrValue));
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                mCurrentCelsius.setTag(item);
                mCurrentCelsius.setText(getContext().getString(R.string.celsius, item.obsrValue));
                TypedArray skyStatusDrawable = getResources().obtainTypedArray(R.array.sky_status_drawable);
                mCurrentCelsius.setCompoundDrawablesWithIntrinsicBounds(0, 0, skyStatusDrawable.getResourceId(skyStatusValue - 1, 0), 0);
            }
        }
    }

    private void updateForecastSpaceDataWeatherView() {
        Log.d(TAG, ">> updateForecastSpaceDataWeatherView().......");
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyyMMdd");
        String date;

        Resources res = getResources();
        String[] weekStringArray = res.getStringArray(R.array.week_day);

        TypedArray skyStatusDrawable = getResources().obtainTypedArray(R.array.sky_status_drawable);

        // today
        mTodayTitle.setText(getContext().getString(R.string.weather_day,
                calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));
        date = sdf.format(calendar.getTime());
        ForecastOfDay data = getMaxCelsiusAndSkyStatusOfDay(mForecastSpaceDataItems, date);
        if (data != null) {
            if (data.maxCelsius != null) {
                if ("-50".equals(data.maxCelsius.fcstValue)) {          // missing value
                    Log.d(TAG, "단기정보에서 " + date + "에 해당하는 최고기온 정보가 missing value(-50) 이다.");
                }
                try {
                    float celsiusVal = Float.parseFloat(data.maxCelsius.fcstValue);
                    float lastMaxCelsius = 0f;
                    if (mLastMaxTodayCelsius != null) {
                        lastMaxCelsius = Float.parseFloat(mLastMaxTodayCelsius);
                    }
                    if (lastMaxCelsius < celsiusVal) {
                        mLastMaxTodayCelsius = data.maxCelsius.fcstValue;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                mTodayMaxCelsius.setText(getContext().getString(R.string.celsius, mLastMaxTodayCelsius));
            } else {
                Log.d(TAG, "단기정보에서 " + date + "에 해당하는 최고기온 정보를 가져올 수 없다.");
            }
            if (data.skyStatus != null) {
                int skyStaus = Integer.parseInt(data.skyStatus.fcstValue);
                if (skyStaus < 1 || skyStaus > 4) {       // 없는값이올수있을까??
                    skyStaus = 1;
                }

                mTodaySkyStatus.setImageResource(skyStatusDrawable.getResourceId(skyStaus - 1, 0));
                Log.d(TAG, ">> Today sky status = " + skyStaus);
            } else {
                Log.d(TAG, "단기정보에서 " + date + "에 해당하는 하늘상태 정보를 가져올 수 없다.");
            }
        } else {
            Log.d(TAG, "단기정보에서 " + date + "에 해당하는 정보를 가져올 수 없다.");
        }

        // tomorrow
        String tomorrowCelsiusValue = null;
        calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
        mTomorrowTitle.setText(getContext().getString(R.string.weather_day,
                calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));
        date = sdf.format(calendar.getTime());
        data = getMaxCelsiusAndSkyStatusOfDay(mForecastSpaceDataItems, date);
        if (data != null) {
            if (data.maxCelsius != null) {
                if ("-50".equals(data.maxCelsius.fcstValue)) {          // missing value
                    Log.d(TAG, "단기정보에서 " + date + "에 해당하는 최고기온 정보가 missing value(-50) 이다.");
                }
                tomorrowCelsiusValue = data.maxCelsius.fcstValue;
                mTomorrowMaxCelsius.setText(getContext().getString(R.string.celsius, data.maxCelsius.fcstValue));
            } else {
                Log.d(TAG, "단기정보에서 " + date + "에 해당하는 최고기온 정보를 가져올 수 없다.");
            }
            if (data.skyStatus != null) {
                int skyStaus = Integer.parseInt(data.skyStatus.fcstValue);
                if (skyStaus < 1 || skyStaus > 4) {       // 없는값이올수있을까??
                    skyStaus = 1;
                }

                mTomorrowSkyStatus.setImageResource(skyStatusDrawable.getResourceId(skyStaus - 1, 0));
                Log.d(TAG, ">> Tomorrow sky status = " + skyStaus);
            } else {
                Log.d(TAG, "단기정보에서 " + date + "에 해당하는 하늘상태 정보를 가져올 수 없다.");
            }
        } else {
            Log.d(TAG, "단기정보에서 " + date + "에 해당하는 정보를 가져올 수 없다.");
        }

        // day after tomorrow
        calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
        mDayAfterTomorrowTitle.setText(getContext().getString(R.string.weather_day,
                calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));
        date = sdf.format(calendar.getTime());
        data = getMaxCelsiusAndSkyStatusOfDay(mForecastSpaceDataItems, date);
        if (data != null) {
            if (data.maxCelsius != null) {
                int celsiusValue = Integer.parseInt(data.maxCelsius.fcstValue);
                if (celsiusValue == -50 || celsiusValue == 0) {          // missing value
                    Log.d(TAG, "단기정보에서 " + date + "에 해당하는 최고기온 정보가 missing value(-50) 이거나 0이다.");
                    // 이틀후 최고기온이 0으로 오기도 한다. 이럴경우 약간의 오류가 나올 수도 있지만 전날 최고기온으로 대체하자.
                    mDayAfterTomorrowMaxCelsius.setText(getContext().getString(R.string.celsius, tomorrowCelsiusValue));
                } else {
                    mDayAfterTomorrowMaxCelsius.setText(getContext().getString(R.string.celsius, data.maxCelsius.fcstValue));
                }

            } else {
                Log.d(TAG, "단기정보에서 " + date + "에 해당하는 최고기온 정보를 가져올 수 없다.");
            }
            if (data.skyStatus != null) {
                int skyStatus = Integer.parseInt(data.skyStatus.fcstValue);
                if (skyStatus < 1 || skyStatus > 4) {       // 없는값이올수있을까??
                    skyStatus = 1;
                }

                Log.d(TAG, ">> DayAfterTomorrow sky status = " + skyStatus);
                mDayAfterTomorrowSkyStatus.setImageResource(skyStatusDrawable.getResourceId(skyStatus - 1, 0));
            } else {
                Log.d(TAG, "단기정보에서 " + date + "에 해당하는 하늘상태 정보를 가져올 수 없다.");
            }
        } else {
            Log.d(TAG, "단기정보에서 " + date + "에 해당하는 정보를 가져올 수 없다.");
        }
    }

    public void onConfigurationChanged(int orientation) {
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            mThreeDaysLayout.setOrientation(LinearLayout.VERTICAL);
            mTodayLayout.setOrientation(LinearLayout.HORIZONTAL);
            mTomorrowLayout.setOrientation(LinearLayout.HORIZONTAL);
            mDayAfterTomorrowLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            mThreeDaysLayout.setOrientation(LinearLayout.HORIZONTAL);
            mTodayLayout.setOrientation(LinearLayout.VERTICAL);
            mTomorrowLayout.setOrientation(LinearLayout.VERTICAL);
            mDayAfterTomorrowLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    public void onChangedVillageName() {
        mCurrentTitle.setText(getContext().getString(R.string.weather_title,
                LauncherSettings.getInstance(getContext()).getVillageName()));

    }
}
