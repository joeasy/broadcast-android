package com.nbplus.vbroadlauncher.widget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.nbplus.vbroadlauncher.data.ForecastData;
import com.nbplus.vbroadlauncher.data.GeocodeData;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.YahooQueryForecastResult;
import com.nbplus.vbroadlauncher.data.YahooQueryGeocodeResult;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Created by basagee on 2015. 6. 3..
 */
public class YahooWeatherView extends LinearLayout {
    private static final String TAG = YahooWeatherView.class.getSimpleName();

    private boolean mAttached;
    RequestQueue mWeatherRequestQueue;
    ForecastData mForecastData;
    private int mForecastRetry = 0;

    // 아후 API에 접근하는것을 단말마다 랜덤하게하도록 한다.
    private int mRandAlarmSeconds = -1;

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
                if (Constants.WEATHER_SERVICE_UPDATE_ACTION.equals(intent.getAction())) {
                    Log.d(TAG, "Weather service update action received !!!");
                    releaseAlarm(Constants.WEATHER_SERVICE_GRIB_UPDATE_ACTION);

                    // update weather and set next alarm
                    updateForecast();
                    setNextForecastAlarm();
                } else if (Constants.LOCATION_CHANGED_ACTION.equals(intent.getAction())) {
                    Log.d(TAG, "LOCATION_CHANGED_ACTION received !!!");
                    releaseAlarm(Constants.WEATHER_SERVICE_DEFAULT_TIMER);

                    // update geocode
                    updateGeocode();
                } else if (Constants.WEATHER_SERVICE_DEFAULT_TIMER.equals(intent.getAction())) {
                    Log.d(TAG, "LOCATION_CHANGED_ACTION received !!!");
                    releaseAlarm(Constants.WEATHER_SERVICE_DEFAULT_TIMER);

                    // update weather and set next alarm
                    if (LauncherSettings.getInstance(getContext()).getGeocodeData() == null) {
                        updateGeocode();
                    } else {
                        updateForecast();
                    }
                    setNextForecastAlarm();
                }
            }
        }
    };

    public YahooWeatherView(Context context) {
        super(context);
        initializeWeatherView(context);
    }

    public YahooWeatherView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeWeatherView(context);
    }

    public YahooWeatherView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeWeatherView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public YahooWeatherView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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

            if (LauncherSettings.getInstance(getContext()).getGeocodeData() != null) {
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
        filter.addAction(Constants.WEATHER_SERVICE_UPDATE_ACTION);
        filter.addAction(Constants.LOCATION_CHANGED_ACTION);
        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }

    // 알람 등록
    private void setNextForecastAlarm() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        // 1시간 간격으로
        calendar.set(Calendar.HOUR_OF_DAY, hour + 1);

        if (mRandAlarmSeconds == -1) {
            Random oRandom = new Random();
            mRandAlarmSeconds = oRandom.nextInt(60);
        }
        calendar.set(Calendar.MINUTE, 00);
        calendar.set(Calendar.SECOND, mRandAlarmSeconds);
        Log.d(TAG, ">> setNextForecastAlarm = " + calendar.get(Calendar.HOUR_OF_DAY) + ", minutes = " + calendar.get(Calendar.MINUTE));

        setAlarm(calendar.getTimeInMillis(), Constants.WEATHER_SERVICE_UPDATE_ACTION);
    }

    private void setAlarm(long milliseconds, String action){
        Log.i(TAG, "setAlarm() = " + action);
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
        Log.i(TAG, "releaseAlarm() = " + action);
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        Intent Intent = new Intent(action);
        PendingIntent pIntent = PendingIntent.getBroadcast(getContext(), 0, Intent, 0);
        alarmManager.cancel(pIntent);

        // 주석을 풀면 먼저 실행되는 알람이 있을 경우, 제거하고
        // 새로 알람을 실행하게 된다. 상황에 따라 유용하게 사용 할 수 있다.
//      alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 3000, pIntent);
    }


    ///////////////////////////////////
    // for weather
    ///////////////////////////////////
    /**
     *  Yahoo! woeid  검색
     */
    public void updateGeocode() {
        if (mWeatherRequestQueue == null) {
            mWeatherRequestQueue = Volley.newRequestQueue(getContext());
        }

        Location location = LauncherSettings.getInstance(getContext()).getPreferredUserLocation();
        String yql = String.format(Constants.YAHOO_WOEID_QUERY, location.getLatitude(), location.getLongitude());
        try {
            yql = URLEncoder.encode(yql, "utf-8");
            yql = yql.replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = String.format(Constants.YAHOO_WEATHER_API, yql);
        Log.d(TAG, ">> updateGeocode URL = " + url);

        GsonRequest jsRequest = new GsonRequest(Request.Method.GET, url, YahooQueryGeocodeResult.class, new Response.Listener<YahooQueryGeocodeResult>() {

            @Override
            public void onResponse(YahooQueryGeocodeResult response) {
                Log.d(TAG, ">>> updateGeocode success !!!");
                mForecastRetry = 0;

                GeocodeData data = response.getGeocodeData();
                if (data != null) {
                    LauncherSettings.getInstance(getContext()).setGeocodeData(data);
                    updateForecast();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, ">>> updateGeocode error");
                mForecastRetry++;
                if (mForecastRetry > 3) {     // retry 3 times
                    mForecastRetry = 0;
                    Log.d(TAG, ">> 3 회재시도 실패.... 더이상조회할게없다. !!!");
                } else {                // retry
                    Log.d(TAG, ">> 재시도를한다.  retry count = " + mForecastRetry);
                    updateGeocode();
                }
            }
        });

        mWeatherRequestQueue.add(jsRequest);
    }

    /**
     *  Yahoo! woeid  검색
     */
    public void updateForecast() {
        if (mWeatherRequestQueue == null) {
            mWeatherRequestQueue = Volley.newRequestQueue(getContext());
        }

        GeocodeData geoCode = LauncherSettings.getInstance(getContext()).getGeocodeData();
        String yql = String.format(Constants.YAHOO_WEATHER_QUERY, geoCode.woeid);
        try {
            yql = URLEncoder.encode(yql, "utf-8");
            yql = yql.replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = String.format(Constants.YAHOO_WEATHER_API, yql);
        Log.d(TAG, ">> updatForecast URL = " + url);

        GsonRequest jsRequest = new GsonRequest(Request.Method.GET, url, YahooQueryForecastResult.class, new Response.Listener<YahooQueryForecastResult>() {

            @Override
            public void onResponse(YahooQueryForecastResult response) {
                Log.d(TAG, ">>> updatForecast success !!!");
                mForecastRetry = 0;

                mForecastData = response.getForecastData();
                if (mForecastData != null) {
                    updateForecastView();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, ">>> updatForecast error");
                mForecastRetry++;
                if (mForecastRetry > 3) {     // retry 3 times
                    mForecastRetry = 0;
                    Log.d(TAG, ">> 3 회재시도 실패.... 더이상조회할게없다. !!!");
                } else {                // retry
                    Log.d(TAG, ">> 재시도를한다.  retry count = " + mForecastRetry);
                    updateForecast();
                }
            }
        });

        mWeatherRequestQueue.add(jsRequest);
    }

    /**
     * 기상청에서 받아온 날씨데이터를 화면에 업데이트
     */
    private int conditonCodeToSkyStatus(String conditionCode) {
        /**
         https://developer.yahoo.com/weather/documentation.html#codes
         // 비
         Code	Description
         0	tornado
         1	tropical storm
         2	hurricane
         3	severe thunderstorms
         4	thunderstorms
         5	mixed rain and snow
         6	mixed rain and sleet
         11	showers
         12	showers
         37	isolated thunderstorms
         38	scattered thunderstorms
         39	scattered thunderstorms
         40	scattered showers
         45	thundershowers

         // 눈
         7	mixed snow and sleet
         8	freezing drizzle    // 눈
         9	drizzle
         10	freezing rain       //
         13	snow flurries
         14	light snow showers
         15	blowing snow
         16	snow
         17	hail
         18	sleet
         35	mixed rain and hail
         41	heavy snow
         42	scattered snow showers
         43	heavy snow
         46	snow showers
         47	isolated thundershowers

         // 바람
         // 조금 흐림 + 안개등
         19	dust
         20	foggy
         21	haze
         22	smoky
         23	blustery
         24	windy
         25	cold
         29	partly cloudy (night)
         30	partly cloudy (day)
         44	partly cloudy

         // 많이 흐림
         26	cloudy
         27	mostly cloudy (night)
         28	mostly cloudy (day)

         // 맑음
         31	clear (night)
         32	sunny
         33	fair (night)
         34	fair (day)
         36	hot

         3200	not available
         */
        final List<String> sunny = Arrays.asList("31", "32", "33", "34", "36");
        final List<String> littleCloudy = Arrays.asList("19", "20", "21", "22", "23", "24", "25", "29", "30", "44");
        final List<String> mostlyCloudy = Arrays.asList("26", "27", "28");
        final List<String> rainy = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "11", "12", "37", "38", "39", "40", "45");
        final List<String> snow = Arrays.asList("7", "8", "9", "10", "13", "14", "15", "16", "17", "18", "35", "41", "42", "43", "46", "47");

        if (sunny.indexOf(conditionCode) != -1) {
            return 0;
        } else if (littleCloudy.indexOf(conditionCode) != -1) {
            return 1;
        } else if (mostlyCloudy.indexOf(conditionCode) != -1) {
            return 2;
        } else if (rainy.indexOf(conditionCode) != -1) {
            return 3;
        } else if (snow.indexOf(conditionCode) != -1) {
            return 4;
        }

        return 0;
    }

    private void updateForecastView() {
        Log.d(TAG, ">> updateForecastView().......");

        if (mForecastData == null || mForecastData.item == null) {
            return;
        }

        Resources res = getResources();
        String[] skyStatusArray = res.getStringArray(R.array.sky_status);
        TypedArray skyStatusDrawable = getResources().obtainTypedArray(R.array.sky_status_drawable);
        String[] weekStringArray = res.getStringArray(R.array.week_day);

        int skyStatusValue = 0;

        /**
         * TODO : =야후 API가 약3도정도 오차가 있다. 약간은 보정해 줘야하나?
         */

        // condition 과 humidity 를 보여준다.
        if (mForecastData.atmosphere != null && mForecastData.item.currentCondition != null) {
            //ForecastData.Forecast data = mForecastData.item.weekCondition.get(0);
            //if (data != null) {
            //    skyStatusValue = conditonCodeToSkyStatus(data.conditionCode);
            //} else {
                skyStatusValue = conditonCodeToSkyStatus(mForecastData.item.currentCondition.conditionCode);
            //}
            String skyStatusString = skyStatusArray[skyStatusValue];

            if (mCurrentSkyStatus != null) {
                mCurrentSkyStatus.setText(getContext().getString(R.string.sky_status,
                        skyStatusString, "습도 " + mForecastData.atmosphere.humidity + "%"));
            }

            //보정
            float temp = Float.parseFloat(mForecastData.item.currentCondition.temperature);
            temp += 3;

            mCurrentCelsius.setText(getContext().getString(R.string.celsius, "" + ((int)temp)));
            mCurrentCelsius.setCompoundDrawablesWithIntrinsicBounds(0, 0, skyStatusDrawable.getResourceId(skyStatusValue, 0), 0);
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf;

        // today
        ForecastData.Forecast data = mForecastData.item.weekCondition.get(0);
        if (data != null) {
            //sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm z", Locale.US);
            sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            try {
                calendar.setTime(sdf.parse(data.date));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            mTodayTitle.setText(getContext().getString(R.string.weather_day,
                    calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));

            try {
                float currentCelsius = Float.parseFloat(mForecastData.item.currentCondition.temperature);
                float maxCelsius = Float.parseFloat(data.high);
                float minCelsius = Float.parseFloat(data.low);

                if (maxCelsius < currentCelsius) {
                    currentCelsius += 3;
                    mTodayMaxCelsius.setText(getContext().getString(R.string.celsius, "" + ((int)currentCelsius)));
                } else {
                    maxCelsius += 3;
                    mTodayMaxCelsius.setText(getContext().getString(R.string.celsius,  "" + ((int)maxCelsius)));
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            // TODO : 야후는 현재시간이후 값을준다. 비가안와도오는결로 표시될 수 있음
            //skyStatusValue = conditonCodeToSkyStatus(data.conditionCode);
            mTodaySkyStatus.setImageResource(skyStatusDrawable.getResourceId(skyStatusValue, 0));
            Log.d(TAG, ">> Today sky status = " + skyStatusValue);
        }

        // tomorrow
        data = mForecastData.item.weekCondition.get(1);
        if (data != null) {
            //sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm z", Locale.US);
            sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            try {
                calendar.setTime(sdf.parse(data.date));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            mTomorrowTitle.setText(getContext().getString(R.string.weather_day,
                    calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));

            //보정
            float temp = Float.parseFloat(data.high);
            temp += 3;

            mTomorrowMaxCelsius.setText(getContext().getString(R.string.celsius, "" + ((int)temp)));

            skyStatusValue = conditonCodeToSkyStatus(data.conditionCode);
            mTomorrowSkyStatus.setImageResource(skyStatusDrawable.getResourceId(skyStatusValue, 0));
            Log.d(TAG, ">> Today sky status = " + skyStatusValue);
        }

        // the day after tomorrow
        data = mForecastData.item.weekCondition.get(2);
        if (data != null) {
            //sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm z", Locale.US);
            sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            try {
                calendar.setTime(sdf.parse(data.date));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            mDayAfterTomorrowTitle.setText(getContext().getString(R.string.weather_day,
                    calendar.get(Calendar.DAY_OF_MONTH), weekStringArray[calendar.get(Calendar.DAY_OF_WEEK) - 1]));

            //보정
            float temp = Float.parseFloat(data.high);
            temp += 3;

            mDayAfterTomorrowMaxCelsius.setText(getContext().getString(R.string.celsius, "" + ((int)temp)));

            skyStatusValue = conditonCodeToSkyStatus(data.conditionCode);
            mDayAfterTomorrowSkyStatus.setImageResource(skyStatusDrawable.getResourceId(skyStatusValue, 0));
            Log.d(TAG, ">> Today sky status = " + skyStatusValue);
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
