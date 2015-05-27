package com.nbplus.vbroadlauncher.decorators;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.Log;

import com.nbplus.vbroadlauncher.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Calendar;

/**
 * Highlight Saturdays and Sundays with a background
 */
public class HighlightWeekendsDecorator implements DayViewDecorator {

    private Context mContext;

    public HighlightWeekendsDecorator(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        int weekDay = day.getCalendar().get(Calendar.DAY_OF_WEEK);
        return weekDay == Calendar.SATURDAY || weekDay == Calendar.SUNDAY;
    }

    @Override
    public void decorate(DayViewFacade view) {
        if (!view.isChecked() && view.isEnabled()) {
            view.setTextColor(mContext.getResources().getColor(R.color.red));
        } else {
            if (view.isChecked()) {
                view.setTextColor(mContext.getResources().getColor(R.color.white));
                view.setSelectionColor(mContext.getResources().getColor(R.color.mcv_text_date_red));
            }
        }
    }
}
