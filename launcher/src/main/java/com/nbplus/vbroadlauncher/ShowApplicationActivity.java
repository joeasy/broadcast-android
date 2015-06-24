package com.nbplus.vbroadlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.callback.OnFragmentInteractionListener;
import com.nbplus.vbroadlauncher.adapter.AppPagerAdapter;
import com.nbplus.vbroadlauncher.adapter.NbplusViewPager;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.service.InstalledApplicationTask;
import com.viewpagerindicator.LinePageIndicator;
import com.viewpagerindicator.PageIndicator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class ShowApplicationActivity extends BaseActivity implements OnFragmentInteractionListener {
    private static final String TAG = ShowApplicationActivity.class.getSimpleName();

    private NbplusViewPager mViewPager;
    AppPagerAdapter mAppPagerAdapter;
    PageIndicator mIndicator;
    InstalledApplicationTask mLoadAsyncTask;

    private ArrayList<OnActivityInteractionListener> mActivityInteractionListener = new ArrayList<OnActivityInteractionListener>();

    private final InstalledListHandler mHandler = new InstalledListHandler(this);

    // 핸들러 객체 만들기
    private static class InstalledListHandler extends Handler {
        private final WeakReference<ShowApplicationActivity> mActivity;

        public InstalledListHandler(ShowApplicationActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ShowApplicationActivity activity = mActivity.get();
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
            case Constants.HANDLER_MESSAGE_INSTALLED_APPLIST_TASK :
                Log.d(TAG, "HANDLER_MESSAGE_INSTALLED_APPLIST_TASK received !!!");
                mAppPagerAdapter.notifyDataSetChanged();
                mIndicator.notifyDataSetChanged();

                if (mActivityInteractionListener != null) {
                    for (OnActivityInteractionListener listener : mActivityInteractionListener) {
                        listener.onDataChanged();
                    }
                }
                break;
        }
    }

    private BroadcastReceiver mPackageInstallReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String packageName = intent.getData().getSchemeSpecificPart();
            Log.d(TAG, packageName + " application install/uninstall");

            if (mLoadAsyncTask != null) {
                if (mLoadAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                    mLoadAsyncTask.cancel(true);
                }
                mLoadAsyncTask = new InstalledApplicationTask(ShowApplicationActivity.this, mHandler);
            } else {
                mLoadAsyncTask = new InstalledApplicationTask(ShowApplicationActivity.this, mHandler);
            }
            mLoadAsyncTask.execute();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_show_application);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");

        registerReceiver(mPackageInstallReceiver, filter);
        mLoadAsyncTask = new InstalledApplicationTask(this, mHandler);
        mLoadAsyncTask.execute();

        // ViewPager 화면.
        // 사용자등록이나 어플리케이션 설치등을수행하는 프래그먼트들이 불린다.
        mViewPager = (NbplusViewPager) findViewById(R.id.viewPager);
        mIndicator = (LinePageIndicator)findViewById(R.id.indicator);

        mAppPagerAdapter = new AppPagerAdapter(this, getSupportFragmentManager());
        mViewPager.setAdapter(mAppPagerAdapter);
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

    @Override
    public void onFragmentInteraction(Uri uri) {

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

        if (mAppPagerAdapter != null) {
            mAppPagerAdapter.notifyDataSetChanged();
        }
        if (mIndicator != null) {
            mIndicator.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPackageInstallReceiver);
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
