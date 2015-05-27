package com.prolificinteractive.materialcalendarview;

import android.graphics.drawable.Drawable;
import android.os.Build;

/**
 * Abstraction layer to help in decorating Day views
 */
public final class DayViewFacade {

    private DayView dayView;
    private CharSequence initialText;

    public DayViewFacade() {
    }

    /**
     * Set the drawable to use in the 'normal' state of the Day view
     *
     * @param drawable Drawable to use, null for default
     */
    public void setBackgroundUnselected(Drawable drawable) {
        dayView.setCustomBackground(drawable);
    }

    /**
     * Set the entire background drawable of the Day view
     *
     * @param drawable the drawable for the Day view
     */
    public void setBackground(Drawable drawable) {
        if(Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            dayView.setBackgroundDrawable(drawable);
        } else {
            dayView.setBackground(drawable);
        }
    }

    public void setSelectionColor(int color) {
        dayView.setSelectionColor(color);
    }

    public boolean isChecked() {
        return dayView.isChecked();
    }
    public boolean isEnabled() {
        return dayView.isEnabled();
    }

    public void setTextColor(int color) {
        dayView.setTextColor(color);
    }

    /**
     * @return the text to be decorated. This will always be the same
     * even if after you call {@linkplain #setText(CharSequence)}
     */
    public CharSequence getText() {
        return initialText;
    }

    /**
     * Set the text on the Day view
     * @param text Text to display
     */
    public void setText(CharSequence text) {
        dayView.setText(text);
    }

    /**
     * @return The {@linkplain CalendarDay} for the Day view being decorated
     */
    public CalendarDay getDate() {
        return dayView.getDate();
    }

    protected void setDayView(DayView dayView) {
        this.dayView = dayView;
        this.initialText = dayView.getText();
    }
}
