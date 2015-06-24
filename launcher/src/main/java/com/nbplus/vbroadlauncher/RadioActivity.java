package com.nbplus.vbroadlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.nbplus.media.MusicRetriever;
import com.nbplus.media.MusicService;
import com.nbplus.vbroadlauncher.adapter.NbplusViewPager;
import com.nbplus.vbroadlauncher.adapter.RadioPagerAdapter;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.callback.OnRadioFragmentInteractionListener;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.fragment.ProgressDialogFragment;
import com.nbplus.vbroadlauncher.fragment.RadioGridFragment;
import com.nbplus.vbroadlauncher.service.GetRadioChannelTask;
import com.nbplus.vbroadlauncher.service.InstalledApplicationTask;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by basagee on 2015. 6. 22..
 */
public class RadioActivity extends BaseActivity implements OnRadioFragmentInteractionListener {
    private static final String TAG = RadioActivity.class.getSimpleName();

    private NbplusViewPager mViewPager;
    RadioPagerAdapter mRadioPagerAdapter;
    PageIndicator mIndicator;
    private ArrayList<RadioChannelInfo.RadioChannel> mRadioChannelItems = new ArrayList<>();
    ShortcutData mShortcutData;
    ProgressDialogFragment mProgressDialogFragment;

    private final RadioActivityHandler mHandler = new RadioActivityHandler(this);

    // 핸들러 객체 만들기
    private static class RadioActivityHandler extends Handler {
        private final WeakReference<RadioActivity> mActivity;

        public RadioActivityHandler(RadioActivity activity) {
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

                setupRadioChannelPager();
                dismissProgressDialog();
                break;
            case Constants.HANDLER_MESSAGE_PLAY_RADIO_CHANNEL_TIMEOUT :
                Log.d(TAG, "HANDLER_MESSAGE_PLAY_RADIO_CHANNEL_TIMEOUT received !!!");
                Intent i = new Intent(this, MusicService.class);
                i.setAction(MusicService.ACTION_STOP);
                i.putExtra(MusicService.EXTRA_MUSIC_FORCE_STOP, true);
                startService(i);
                break;

        }
    }

    private BroadcastReceiver musicServiceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.d(TAG, ">> musicServiceReceiver action received = " + action);
            mHandler.removeMessages(Constants.HANDLER_MESSAGE_PLAY_RADIO_CHANNEL_TIMEOUT);
            // send handler message
            dismissProgressDialog();
        }
    };

    private void setupRadioChannelPager() {
        if (mRadioChannelItems.size() > 0) {
            ArrayList<RadioGridFragment> viewPagerFragments = new ArrayList<>();

            int pages = mRadioChannelItems.size() / Constants.RADIO_CHANNEL_GRIDVIEW_SIZE;
            int remains = mRadioChannelItems.size() % Constants.RADIO_CHANNEL_GRIDVIEW_SIZE;

            if (remains > 0) {
                pages++;
            }

            // create fragment
            for (int i = 0; i < pages; i++) {
                int start = i * Constants.RADIO_CHANNEL_GRIDVIEW_SIZE;
                int end = start + Constants.RADIO_CHANNEL_GRIDVIEW_SIZE;
                if (end > mRadioChannelItems.size()) {
                    end = mRadioChannelItems.size();
                }
                ArrayList<RadioChannelInfo.RadioChannel> subList = new ArrayList<>(mRadioChannelItems.subList(start, end));

                viewPagerFragments.add(RadioGridFragment.newInstance(i, subList));
            }

            mRadioPagerAdapter = new RadioPagerAdapter(this, getSupportFragmentManager(), viewPagerFragments);
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
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
        } else {
            // show relaod or close
            Log.e(TAG, ">> Radio channel list update failed. show error message !!!");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showProgressDialog();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.activity_radio_background));
        }

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_radio);

        Intent i = getIntent();
        mShortcutData = i.getParcelableExtra(Constants.EXTRA_NAME_SHORTCUT_DATA);
        if (mShortcutData == null) {
            Log.e(TAG, "mShortcutData is not found..");
            return;
        }
        new GetRadioChannelTask(this, mHandler, mShortcutData.getDomain() + mShortcutData.getPath()).execute();


        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_PLAYED);
        filter.addAction(MusicService.ACTION_PAUSED);
        filter.addAction(MusicService.ACTION_STOPPED);
        filter.addAction(MusicService.ACTION_ERROR);

        LocalBroadcastManager.getInstance(this).registerReceiver(musicServiceReceiver, filter);

        // ViewPager 화면.
        // 사용자등록이나 어플리케이션 설치등을수행하는 프래그먼트들이 불린다.
        mViewPager = (NbplusViewPager) findViewById(R.id.viewPager);
        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);

        // close button
        ImageButton closeButton = (ImageButton) findViewById(R.id.btn_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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
    protected void onPause() {
        super.onPause();
        dismissProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(musicServiceReceiver);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onPlayRadioRequest(RadioChannelInfo.RadioChannel channel) {
        Log.d(TAG, "> User select radio channel... show progrss and ....");

        showProgressDialog();

        if (channel != null) {
            Intent i = new Intent(this, MusicService.class);
            i.setAction(MusicService.ACTION_URL);
            MusicRetriever.Item item = new MusicRetriever.Item(0, channel.index, null, channel.channelName, null, channel.channelUrl, 0);
            i.putExtra(MusicService.EXTRA_MUSIC_ITEM, item);
            startService(i);

            mHandler.sendEmptyMessageDelayed(Constants.HANDLER_MESSAGE_PLAY_RADIO_CHANNEL_TIMEOUT, 10000);
        }
    }

    private void showProgressDialog() {
        dismissProgressDialog();
        mProgressDialogFragment = ProgressDialogFragment.newInstance();
        mProgressDialogFragment.show(getSupportFragmentManager(), "radio_progress_dialog");
    }
    private void dismissProgressDialog() {
        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }
}
