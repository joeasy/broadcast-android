package com.nbplus.vbroadlauncher;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nbplus.vbroadlauncher.decorators.HighlightWeekendsDecorator;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateChangedListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class CalendarActivity extends AppCompatActivity implements OnDateChangedListener {
    private static final String TAG = CalendarActivity.class.getSimpleName();

    private static final DateFormat FORMATTER = SimpleDateFormat.getDateInstance();
    MaterialCalendarView mWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_calendar);

        mWidget = (MaterialCalendarView) findViewById(R.id.calendarView);
        mWidget.setOnDateChangedListener(this);

        Calendar calendar = Calendar.getInstance();
        mWidget.setSelectedDate(calendar.getTime());

        calendar.set(calendar.get(Calendar.YEAR), Calendar.JANUARY, 1);
        mWidget.setMinimumDate(calendar.getTime());

        calendar.set(calendar.get(Calendar.YEAR) + 2, Calendar.OCTOBER, 31);
        mWidget.setMaximumDate(calendar.getTime());

        mWidget.setShowOtherDates(true);
        mWidget.addDecorators(new HighlightWeekendsDecorator(CalendarActivity.this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_calendar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDateChanged(MaterialCalendarView widget, CalendarDay date) {
        //textView.setText(FORMATTER.format(date.getDate()));
        Log.d(TAG, "onDateChanged() = " + FORMATTER.format(date.getDate()));
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

}
