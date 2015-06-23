package com.nbplus.vbroadlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nbplus.vbroadlauncher.adapter.AppPagerAdapter;
import com.nbplus.vbroadlauncher.adapter.AppViewPager;
import com.nbplus.vbroadlauncher.adapter.RadioPagerAdapter;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.service.GetRadioChannelTask;
import com.nbplus.vbroadlauncher.service.InstalledApplicationTask;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.LinePageIndicator;
import com.viewpagerindicator.PageIndicator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by basagee on 2015. 6. 22..
 */
public class RadioActivity extends AppCompatActivity {
    private static final String TAG = RadioActivity.class.getSimpleName();

    private AppViewPager mViewPager;
    RadioPagerAdapter mRadioPagerAdapter;
    PageIndicator mIndicator;
    private ArrayList<RadioChannelInfo.RadioChannel> mRadioChannelItems = new ArrayList<>();
    ShortcutData mShortcutData;

    private ArrayList<OnActivityInteractionListener> mActivityInteractionListener = new ArrayList<OnActivityInteractionListener>();

    private final RadioListHandler mHandler = new RadioListHandler(this);

    // 핸들러 객체 만들기
    private static class RadioListHandler extends Handler {
        private final WeakReference<RadioActivity> mActivity;

        public RadioListHandler(RadioActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            RadioActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case Constants.HANDLER_MESSAGE_GET_RADIO_CHANNEL_TASK :
                Log.d(TAG, "HANDLER_MESSAGE_GET_RADIO_CHANNEL_TASK received !!!");
                RadioChannelInfo data = (RadioChannelInfo)msg.obj;

                if (Constants.RESULT_OK.equals(data.getResultCode())) {
                    mRadioChannelItems = data.getRadioChannelList();
                    if (mRadioChannelItems == null) {
                        mRadioChannelItems = new ArrayList<>();
                    }
                }

                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_radio);

        Intent i = getIntent();
        mShortcutData = i.getParcelableExtra(Constants.EXTRA_NAME_SHORTCUT_DATA);
        if (mShortcutData == null) {
            Log.e(TAG, "mShortcutData is not found..");
            return;
        }
        new GetRadioChannelTask(this, mHandler, mShortcutData.getDomain() + mShortcutData.getPath()).execute();

        // ViewPager 화면.
        // 사용자등록이나 어플리케이션 설치등을수행하는 프래그먼트들이 불린다.
        mViewPager = (AppViewPager) findViewById(R.id.viewPager);
        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);

        mRadioPagerAdapter = new RadioPagerAdapter(this, getSupportFragmentManager(), 0);
        mViewPager.setAdapter(mRadioPagerAdapter);
        mViewPager.setSwipeable(true);
        mIndicator.setViewPager(mViewPager);
        mIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, ">> page selected position = " + position);
//                AppGridFragment fragment = (AppGridFragment)mViewPager.getActiveFragment(getSupportFragmentManager(), position);
//                if (fragment != null) {
//                    fragment.updateGridLayout();
//                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_application, menu);
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

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Activity에서 일어나는 Interaction lister 이다.
     * 여러개의 프래그먼트에서 동시에 처리하지 않도록 하나만 유지된다.
     * @param listener
     */
    public void setOnActivityInteractionListener(OnActivityInteractionListener listener) {
        this.mActivityInteractionListener.add(listener);
    }
    public void removeOnActivityInteractionListener(OnActivityInteractionListener listener) {
        this.mActivityInteractionListener.remove(listener);
    }

}
