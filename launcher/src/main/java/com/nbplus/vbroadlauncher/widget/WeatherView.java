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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Date;

/**
 * Created by basagee on 2015. 6. 3..
 */
public class WeatherView extends LinearLayout {
    private static final String TAG = WeatherView.class.getSimpleName();

    private boolean mAttached;
    private boolean mLocationChangedReceived = false;
    RequestQueue mWeatherRequestQueue;
    private ArrayList<ForecastItem> mForecastSpaceDataItems = new ArrayList<ForecastItem>();
    private ArrayList<ForecastItem> mForecastTimeDataItems = new ArrayList<ForecastItem>();
    private ArrayList<ForecastItem> mForecastGribItems = new ArrayList<ForecastItem>();
    private int mForecastRetry = 0;

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
                if (Constants.WEATHER_SERVICE_UPDATE_ACTION.equals(intent.getAction())) {
                    Log.d(TAG, "Weather service action received !!!");
                    releaseAlarm(Constants.WEATHER_SERVICE_GRIB_UPDATE_ACTION);

                    // update weather and set next alarm
                    updateForecast();
                    setNextAlarm();
                } else if (Constants.LOCATION_CHANGED_ACTION.equals(intent.getAction())) {
                    Log.d(TAG, "LOCATION_CHANGED_ACTION received !!!");
                    if (!mLocationChangedReceived) {
                        mLocationChangedReceived = true;
                        releaseAlarm(Constants.WEATHER_SERVICE_DEFAULT_TIMER);

                        // update geocode
                        updateGeocode();
                    }
                } else if (Constants.WEATHER_SERVICE_DEFAULT_TIMER.equals(intent.getAction())) {
                    Log.d(TAG, "LOCATION_CHANGED_ACTION received !!!");
                    if (!mLocationChangedReceived) {
                        mLocationChangedReceived = false;
                        releaseAlarm(Constants.WEATHER_SERVICE_DEFAULT_TIMER);

                        // update weather and set next alarm
                        if (LauncherSettings.getInstance(getContext()).getGeocode() == null) {
                            updateGeocode();
                        } else {
                            updateForecast();
                        }
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
            setAlarm(System.currentTimeMillis() + 5000, null);
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
        calendar.set(Calendar.MINUTE, 00);
        Log.d(TAG, ">> setNextForecastAlarm = " + calendar.get(Calendar.HOUR_OF_DAY) + ", minutes = " + calendar.get(Calendar.MINUTE));

        setAlarm(calendar.getTimeInMillis(), Constants.WEATHER_SERVICE_UPDATE_ACTION);
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


    ///////////////////////////////////
    // for weather
    ///////////////////////////////////
    /**
     * 실황데이터
     * @param pageNo
     */
    public void updateGeocode() {
        if (mWeatherRequestQueue == null) {
            mWeatherRequestQueue = Volley.newRequestQueue(getContext());
        }
        String url = Constants.WEATHER_SERVER_PREFIX + Constants.WEATHER_SERVICE_GRIB + "?";
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
    private class ForecastOfDay {
        public ForecastItem maxCelsius;
        public ForecastItem skyStatus;
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
