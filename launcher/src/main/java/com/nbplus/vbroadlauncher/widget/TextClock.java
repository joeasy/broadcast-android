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

package com.nbplus.vbroadlauncher.widget;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.data.Constants;

import org.basdroid.common.DisplayUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static android.view.ViewDebug.ExportedProperty;
import static android.widget.RemoteViews.RemoteView;

/**
 * <p><code>TextClock</code> can display the current date and/or time as
 * a formatted string.</p>
 *
 * <p>This view honors the 24-hour format system setting. As such, it is
 * possible and recommended to provide two different formatting patterns:
 * one to display the date/time in 24-hour mode and one to display the
 * date/time in 12-hour mode. Most callers will want to use the defaults,
 * though, which will be appropriate for the user's locale.</p>
 *
 * <p>It is possible to determine whether the system is currently in
 * 24-hour mode by calling {@link #is24HourModeEnabled()}.</p>
 *
 * <p>The rules used by this widget to decide how to format the date and
 * time are the following:</p>
 * <ul>
 *     <li>In 24-hour mode:
 *         <ul>
 *             <li>Use the value returned by {@link #getFormat24Hour()} when non-null</li>
 *             <li>Otherwise, use the value returned by {@link #getFormat12Hour()} when non-null</li>
 *             <li>Otherwise, use a default value appropriate for the user's locale, such as {@code h:mm a}</li>
 *         </ul>
 *     </li>
 *     <li>In 12-hour mode:
 *         <ul>
 *             <li>Use the value returned by {@link #getFormat12Hour()} when non-null</li>
 *             <li>Otherwise, use the value returned by {@link #getFormat24Hour()} when non-null</li>
 *             <li>Otherwise, use a default value appropriate for the user's locale, such as {@code HH:mm}</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p>The {@link CharSequence} instances used as formatting patterns when calling either
 * {@link #setFormat24Hour(CharSequence)} or {@link #setFormat12Hour(CharSequence)} can
 * contain styling information. To do so, use a {@link android.text.Spanned} object.
 * Note that if you customize these strings, it is your responsibility to supply strings
 * appropriate for formatting dates and/or times in the user's locale.</p>
 *
 * @attr ref android.R.styleable#TextClock_format12Hour
 * @attr ref android.R.styleable#TextClock_format24Hour
 * @attr ref android.R.styleable#TextClock_timeZone
 */
@RemoteView
public class TextClock extends TextView {
    private static final String TAG = TextClock.class.getSimpleName();
    /**
     * The default formatting pattern in 12-hour mode. This pattern is used
     * if {@link #setFormat12Hour(CharSequence)} is called with a null pattern
     * or if no pattern was specified when creating an instance of this class.
     *
     * This default pattern shows only the time, hours and minutes, and an am/pm
     * indicator.
     *
     * @see #setFormat12Hour(CharSequence)
     * @see #getFormat12Hour()
     *
     * @deprecated Let the system use locale-appropriate defaults instead.
     */
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";

    /**
     * The default formatting pattern in 24-hour mode. This pattern is used
     * if {@link #setFormat24Hour(CharSequence)} is called with a null pattern
     * or if no pattern was specified when creating an instance of this class.
     *
     * This default pattern shows only the time, hours and minutes.
     *
     * @see #setFormat24Hour(CharSequence)
     * @see #getFormat24Hour()
     *
     * @deprecated Let the system use locale-appropriate defaults instead.
     */
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";

    public  static final char    QUOTE                  =    '\'';
    public  static final char    SECONDS                =    's';

    private CharSequence mFormat12;
    private CharSequence mFormat24;

    @ExportedProperty
    private CharSequence mFormat;
    @ExportedProperty
    private boolean mHasSeconds;

    private boolean mAttached;
    private boolean mRegistered;
    private boolean mResumed;

    private Calendar mTime;
    private String mTimeZone;

    private final ContentObserver mFormatChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            chooseFormat();
            if (mResumed) {
                onTimeChanged();
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            chooseFormat();
            if (mResumed) {
                onTimeChanged();
            }
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String timeZone = intent.getStringExtra("time-zone");
                createTime(timeZone);
            } else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
                chooseFormat();
            }
            if (mResumed) {
                onTimeChanged();
            }
        }
    };

    private final Runnable mTicker = new Runnable() {
        public void run() {
            onTimeChanged();

            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);

            getHandler().postAtTime(mTicker, next);
        }
    };
    private boolean mTickerIsRunning = false;

    /**
     * Creates a new clock using the default patterns for the current locale.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    @SuppressWarnings("UnusedDeclaration")
    public TextClock(Context context) {
        super(context);
        init();
        onTimeChanged();
    }

    /**
     * Creates a new clock inflated from XML. This object's properties are
     * intialized from the attributes specified in XML.
     *
     * This constructor uses a default style of 0, so the only attribute values
     * applied are those in the Context's Theme and the given AttributeSet.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view
     */
    @SuppressWarnings("UnusedDeclaration")
    public TextClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Creates a new clock inflated from XML. This object's properties are
     * intialized from the attributes specified in XML.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     */
    public TextClock(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TextClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleRes);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.TextClock, defStyleAttr, defStyleRes);
        try {
            mFormat12 = a.getText(R.styleable.TextClock_format12Hour);
            mFormat24 = a.getText(R.styleable.TextClock_format24Hour);
            mTimeZone = a.getString(R.styleable.TextClock_timeZone);
        } finally {
            a.recycle();
        }

        init();
        onTimeChanged();
    }

    private void init() {
        if (mFormat12 == null || mFormat24 == null) {
            if (mFormat12 == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mFormat12 = DateFormat.getBestDateTimePattern(getContext().getResources().getConfiguration().locale, "hh:mm a");
                } else {
                    mFormat12 = "hh:mm a";
                }
            }
            if (mFormat24 == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mFormat24 = DateFormat.getBestDateTimePattern(getContext().getResources().getConfiguration().locale, "HH:mm");
                } else {
                    mFormat24 = "HH:mm";
                }
            }
        }

        createTime(mTimeZone);
        // Wait until onAttachedToWindow() to handle the ticker
        chooseFormat(false);
    }

    private void createTime(String timeZone) {
        if (timeZone != null) {
            mTime = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        } else {
            mTime = Calendar.getInstance();
        }
    }

    /**
     * Returns the formatting pattern used to display the date and/or time
     * in 12-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     *
     * @return A {@link CharSequence} or null.
     *
     * @see #setFormat12Hour(CharSequence)
     * @see #is24HourModeEnabled()
     */
    @ExportedProperty
    public CharSequence getFormat12Hour() {
        return mFormat12;
    }

    /**
     * <p>Specifies the formatting pattern used to display the date and/or time
     * in 12-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.</p>
     *
     * <p>If this pattern is set to null, {@link #getFormat24Hour()} will be used
     * even in 12-hour mode. If both 24-hour and 12-hour formatting patterns
     * are set to null, the default pattern for the current locale will be used
     * instead.</p>
     *
     * <p><strong>Note:</strong> if styling is not needed, it is highly recommended
     * you supply a format string generated by
     * {@link DateFormat#getBestDateTimePattern(java.util.Locale, String)}. This method
     * takes care of generating a format string adapted to the desired locale.</p>
     *
     *
     * @param format A date/time formatting pattern as described in {@link DateFormat}
     *
     * @see #getFormat12Hour()
     * @see #is24HourModeEnabled()
     * @see DateFormat#getBestDateTimePattern(java.util.Locale, String)
     * @see DateFormat
     *
     * @attr ref android.R.styleable#TextClock_format12Hour
     */
    public void setFormat12Hour(CharSequence format) {
        mFormat12 = format;

        chooseFormat();
        onTimeChanged();
    }

    /**
     * Returns the formatting pattern used to display the date and/or time
     * in 24-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     *
     * @return A {@link CharSequence} or null.
     *
     * @see #setFormat24Hour(CharSequence)
     * @see #is24HourModeEnabled()
     */
    @ExportedProperty
    public CharSequence getFormat24Hour() {
        return mFormat24;
    }

    /**
     * <p>Specifies the formatting pattern used to display the date and/or time
     * in 24-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.</p>
     *
     * <p>If this pattern is set to null, {@link #getFormat24Hour()} will be used
     * even in 12-hour mode. If both 24-hour and 12-hour formatting patterns
     * are set to null, the default pattern for the current locale will be used
     * instead.</p>
     *
     * <p><strong>Note:</strong> if styling is not needed, it is highly recommended
     * you supply a format string generated by
     * {@link DateFormat#getBestDateTimePattern(java.util.Locale, String)}. This method
     * takes care of generating a format string adapted to the desired locale.</p>
     *
     * @param format A date/time formatting pattern as described in {@link DateFormat}
     *
     * @see #getFormat24Hour()
     * @see #is24HourModeEnabled()
     * @see DateFormat#getBestDateTimePattern(java.util.Locale, String)
     * @see DateFormat
     *
     * @attr ref android.R.styleable#TextClock_format24Hour
     */
    public void setFormat24Hour(CharSequence format) {
        mFormat24 = format;

        chooseFormat();
        onTimeChanged();
    }

    /**
     * Sets whether this clock should always track the current user and not the user of the
     * current process. This is used for single instance processes like the systemUI who need
     * to display time for different users.
     *
     * @hide
     */
    public void setShowCurrentUserTime(boolean showCurrentUserTime) {
        chooseFormat();
        onTimeChanged();
        unregisterObserver();
        registerObserver();
    }

    /**
     * Indicates whether the system is currently using the 24-hour mode.
     *
     * When the system is in 24-hour mode, this view will use the pattern
     * returned by {@link #getFormat24Hour()}. In 12-hour mode, the pattern
     * returned by {@link #getFormat12Hour()} is used instead.
     *
     * If either one of the formats is null, the other format is used. If
     * both formats are null, the default formats for the current locale are used.
     *
     * @return true if time should be displayed in 24-hour format, false if it
     *         should be displayed in 12-hour format.
     *
     * @see #setFormat12Hour(CharSequence)
     * @see #getFormat12Hour()
     * @see #setFormat24Hour(CharSequence)
     * @see #getFormat24Hour()
     */
    public boolean is24HourModeEnabled() {
        return DateFormat.is24HourFormat(getContext());
    }

    /**
     * Indicates which time zone is currently used by this view.
     *
     * @return The ID of the current time zone or null if the default time zone,
     *         as set by the user, must be used
     *
     * @see TimeZone
     * @see java.util.TimeZone#getAvailableIDs()
     * @see #setTimeZone(String)
     */
    public String getTimeZone() {
        return mTimeZone;
    }

    /**
     * Sets the specified time zone to use in this clock. When the time zone
     * is set through this method, system time zone changes (when the user
     * sets the time zone in settings for instance) will be ignored.
     *
     * @param timeZone The desired time zone's ID as specified in {@link TimeZone}
     *                 or null to user the time zone specified by the user
     *                 (system time zone)
     *
     * @see #getTimeZone()
     * @see java.util.TimeZone#getAvailableIDs()
     * @see TimeZone#getTimeZone(String)
     *
     * @attr ref android.R.styleable#TextClock_timeZone
     */
    public void setTimeZone(String timeZone) {
        mTimeZone = timeZone;

        createTime(timeZone);
        onTimeChanged();
    }

    /**
     * Selects either one of {@link #getFormat12Hour()} or {@link #getFormat24Hour()}
     * depending on whether the user has selected 24-hour format.
     *
     * Calling this method does not schedule or unschedule the time ticker.
     */
    private void chooseFormat() {
        chooseFormat(true);
    }

    /**
     * Returns the current format string. Always valid after constructor has
     * finished, and will never be {@code null}.
     *
     * @hide
     */
    public CharSequence getFormat() {
        return mFormat;
    }

    /**
     * Selects either one of {@link #getFormat12Hour()} or {@link #getFormat24Hour()}
     * depending on whether the user has selected 24-hour format.
     *
     * @param handleTicker true if calling this method should schedule/unschedule the
     *                     time ticker, false otherwise
     */
    private void chooseFormat(boolean handleTicker) {
        final boolean format24Requested = is24HourModeEnabled();

        if (format24Requested) {
            mFormat = abc(mFormat24, mFormat12, "HH:mm");
        } else {
            mFormat = abc(mFormat12, mFormat24, "hh:mm a");
        }

        boolean hadSeconds = mHasSeconds;
        mHasSeconds = hasSeconds(mFormat);

        if (handleTicker && mAttached && hadSeconds != mHasSeconds) {
            if (hadSeconds) {
                if (mTickerIsRunning) {
                    getHandler().removeCallbacks(mTicker);
                    mTickerIsRunning = false;
                }
            }
            else {
                if (!mTickerIsRunning) {
                    mTicker.run();
                    mTickerIsRunning = true;
                }
            }
        }
    }

    /**
     * Indicates whether the specified format string contains seconds.
     *
     * Always returns false if the input format is null.
     *
     * @param inFormat the format string, as described in {@link android.text.format.DateFormat}
     *
     * @return true if the format string contains {@link #SECONDS}, false otherwise
     *
     * @hide
     */
    public boolean hasSeconds(CharSequence inFormat) {
        return hasDesignator(inFormat, SECONDS);
    }

    /**
     * Test if a format string contains the given designator. Always returns
     * {@code false} if the input format is {@code null}.
     *
     * @hide
     */
    public boolean hasDesignator(CharSequence inFormat, char designator) {
        if (inFormat == null) return false;

        final int length = inFormat.length();

        int c;
        int count;

        for (int i = 0; i < length; i += count) {
            count = 1;
            c = inFormat.charAt(i);

            if (c == QUOTE) {
                count = skipQuotedText(inFormat, i, length);
            } else if (c == designator) {
                return true;
            }
        }

        return false;
    }

    private int skipQuotedText(CharSequence s, int i, int len) {
        if (i + 1 < len && s.charAt(i + 1) == QUOTE) {
            return 2;
        }

        int count = 1;
        // skip leading quote
        i++;

        while (i < len) {
            char c = s.charAt(i);

            if (c == QUOTE) {
                count++;
                //  QUOTEQUOTE -> QUOTE
                if (i + 1 < len && s.charAt(i + 1) == QUOTE) {
                    i++;
                } else {
                    break;
                }
            } else {
                i++;
                count++;
            }
        }

        return count;
    }

    /**
     * Returns a if not null, else return b if not null, else return c.
     */
    private static CharSequence abc(CharSequence a, CharSequence b, CharSequence c) {
        return a == null ? (b == null ? c : b) : a;
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d("TextClock", "onAttachedToWindow()");
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;

            if (!mRegistered) {
                registerReceiver(true);
                registerObserver();
                mRegistered = true;
            }

            mResumed = true;

            createTime(mTimeZone);

            if (mHasSeconds) {
                if (!mTickerIsRunning) {
                    mTicker.run();
                    mTickerIsRunning = true;
                }
            } else {
                onTimeChanged();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.d("TextClock", "onDetachedFromWindow()");
        super.onDetachedFromWindow();

        if (mAttached) {
            if (mRegistered) {
                unregisterReceiver();
                unregisterObserver();
                mRegistered = false;
            }

            if (mTickerIsRunning) {
                getHandler().removeCallbacks(mTicker);
                mTickerIsRunning = false;
            }

            mAttached = false;
        }
    }

    private void registerReceiver(boolean withTimeTick) {
        unregisterReceiver();
        final IntentFilter filter = new IntentFilter();

        if (withTimeTick) {
            filter.addAction(Intent.ACTION_TIME_TICK);
        }
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        try {
            getContext().registerReceiver(mIntentReceiver, filter/*, null, getHandler()*/);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerObserver() {
        if (!mRegistered) {
            final ContentResolver resolver = getContext().getContentResolver();
            resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        }
    }

    private void unregisterReceiver() {
        try {
            getContext().unregisterReceiver(mIntentReceiver);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void unregisterObserver() {
        try {
            final ContentResolver resolver = getContext().getContentResolver();
            resolver.unregisterContentObserver(mFormatChangeObserver);
        } catch (Exception e) {

        }
    }

    public void onTimeChanged() {
        long currTimeMs = System.currentTimeMillis();
        mTime.setTimeInMillis(currTimeMs);

        Locale locale = getContext().getResources().getConfiguration().locale;
        SimpleDateFormat sdf;
        if (Locale.KOREA.toString().equals(locale.toString())) {
            sdf = new SimpleDateFormat("MM월 dd일 E요일");
        } else {
            sdf = new SimpleDateFormat("EEE, MMM dd");
        }
        Date date = new Date(currTimeMs);
        String dateStr = sdf.format(date);

        CharSequence timeString = DateFormat.format(mFormat, mTime);

        Spannable span = new SpannableString(timeString + "\n" +  dateStr);
        float sizeDp = DisplayUtils.getDimension(getContext(), R.dimen.launcher_clock_time_font_size);
        float sizePx = DisplayUtils.pxFromDp(getContext(), sizeDp);

        if (mFormat.equals(mFormat12)) {
            if (Locale.KOREA.toString().equals(locale.toString())) {
                // 앞쪽에있다.
                span.setSpan(new AbsoluteSizeSpan((int)sizePx), 3, timeString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sizeDp = DisplayUtils.getDimension(getContext(), R.dimen.launcher_clock_time_ampm_font_size);
                sizePx = DisplayUtils.pxFromDp(getContext(), sizeDp);
                span.setSpan(new AbsoluteSizeSpan((int)sizePx), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                span.setSpan(new AbsoluteSizeSpan((int)sizePx), 0, timeString.length() - 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sizeDp = DisplayUtils.getDimension(getContext(), R.dimen.launcher_clock_time_ampm_font_size);
                sizePx = DisplayUtils.pxFromDp(getContext(), sizeDp);
                span.setSpan(new AbsoluteSizeSpan((int)sizePx), timeString.length() - 2, timeString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            span.setSpan(new AbsoluteSizeSpan((int)sizePx), 0, timeString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        sizeDp = DisplayUtils.getDimension(getContext(), R.dimen.launcher_clock_date_font_size);
        sizePx = DisplayUtils.pxFromDp(getContext(), sizeDp);
        span.setSpan(new AbsoluteSizeSpan((int)sizePx), timeString.length(), span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(Color.BLACK), timeString.length(), span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                            0, span.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(span);
    }

    /**
     * TODO ::
     * 액티비티가 pause/resume 되는것에 따라서 broadcast receiver 를 register/unregister 하는경우에
     * "앱스" 액티비티나 타앱을 실행중인상태에서 홈키를누르면 런처앱이 재실행 되는 경우가 있다.
     * 이때 register 이전에 항상 unregister를하도록 했지만... 무슨 이유에서인지
     * IntentRecieverLeakedException 이 발생하면서 비정상 종료되는 케이스가발생한다.
     *
     * 왜일까?????????
     * 원인을 알기전까지는 onAttach / onDetach 에서만 처리하고 resume 여부에따라서 onTimeChanged()를 호출하게 하자.
     */
    public void onResumed() {
        Log.d("TextClock", "onResumed()");
        if (mAttached) {
            mResumed = true;
//            if (!mRegistered) {
////                registerReceiver(true);
//                registerObserver();
//
//                mRegistered = true;
//            }
            if (mHasSeconds) {
                if (!mTickerIsRunning) {
                    mTicker.run();
                    mTickerIsRunning = true;
                }
            } else {
                onTimeChanged();
            }
        }
    }
    public void onPaused() {
        Log.d("TextClock", "onPaused() mAttched = " + mAttached + ", mRegistered = " + mRegistered);
        if (mAttached) {
            mResumed = false;
//            if (mRegistered) {
//                //registerReceiver(false);
//                unregisterObserver();
//                mRegistered = false;
//            }
            if (mHasSeconds) {
                if (mTickerIsRunning) {
                    getHandler().removeCallbacks(mTicker);
                    mTickerIsRunning = false;
                }
            }
        }
    }
}
