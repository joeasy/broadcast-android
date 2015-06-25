package com.nbplus.vbroadlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.nbplus.vbroadlauncher.adapter.NbplusViewPager;
import com.nbplus.vbroadlauncher.adapter.RadioPagerAdapter;
import com.nbplus.vbroadlauncher.callback.OnRadioActivityInteractionListener;
import com.nbplus.vbroadlauncher.callback.OnRadioFragmentInteractionListener;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.fragment.ProgressDialogFragment;
import com.nbplus.vbroadlauncher.fragment.RadioGridFragment;
import com.nbplus.vbroadlauncher.service.GetRadioChannelTask;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.nbplus.media.MusicRetriever;
import com.nbplus.media.MusicService;

/**
 * Created by basagee on 2015. 6. 22..
 */
public class RadioActivity extends BaseActivity implements OnRadioFragmentInteractionListener {
    private static final String TAG = RadioActivity.class.getSimpleName();

    private static final int HANDLER_MESSAGE_STREAM_MUSIC_VOLUME_CHANGE = 0x01;
    private static final int HANDLER_MESSAGE_MUSIC_SERVICE_ACTION = 0x02;
    private static final int HANDLER_MESSAGE_HIDE_PROGRESS_DIALOG = 0x03;

    private NbplusViewPager mViewPager;
    RadioPagerAdapter mRadioPagerAdapter;
    PageIndicator mIndicator;
    private ArrayList<RadioChannelInfo.RadioChannel> mRadioChannelItems = new ArrayList<>();
    ShortcutData mShortcutData;
    ProgressDialogFragment mProgressDialogFragment;
    SettingsContentObserver mSettingsContentObserver;

    RelativeLayout mActivityLayout;
    // title bar
    TextView    mRadioTitle;
    // media controller
    ImageButton mPlayToggle;
    ImageButton mPlayStop;
    ImageButton mSoundToggle;
    SeekBar     mSeekbar;
    int         mSoundTogglePreviousValue = -1;

    Bundle      mCurrentPlayingStatus = null;
    ArrayList<OnRadioActivityInteractionListener> mActivityInteractionListener = new ArrayList<>();

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
                Intent i = new Intent(this, MusicService.class);
                i.setAction(MusicService.ACTION_STOP);
                i.putExtra(MusicService.EXTRA_MUSIC_FORCE_STOP, true);
                startService(i);
                break;

            case HANDLER_MESSAGE_STREAM_MUSIC_VOLUME_CHANGE :
                int currentVolume = msg.arg1;
                if (currentVolume <= 0) {
                    mSoundToggle.setBackgroundResource(R.drawable.ic_button_radio_sound_off);
                } else {
                    mSoundToggle.setBackgroundResource(R.drawable.ic_button_radio_sound_on);
                }
                mSeekbar.setProgress(currentVolume);
                break;

            case HANDLER_MESSAGE_MUSIC_SERVICE_ACTION :
                Bundle b = msg.getData();
                if (b == null) {
                    Log.i(TAG, "HANDLER_MESSAGE_MUSIC_SERVICE_ACTION bundle is not found!!");
                    return;
                }
                String action = b.getString(MusicService.EXTRA_ACTION);
                MusicService.State state = (MusicService.State)b.getSerializable(MusicService.EXTRA_PLAYING_STATUS);
                MusicRetriever.Item item = b.getParcelable(MusicService.EXTRA_MUSIC_ITEM);

                if (state == MusicService.State.Playing) {
                    mPlayToggle.setBackgroundResource(R.drawable.ic_btn_radio_pause_selector);
                    mRadioTitle.setText(item.getTitle());
                } else if (state == MusicService.State.Paused) {
                    mPlayToggle.setBackgroundResource(R.drawable.ic_btn_radio_play_selector);
                    mRadioTitle.setText(item.getTitle());
                } else if (state == MusicService.State.Stopped) {
                    mPlayToggle.setBackgroundResource(R.drawable.ic_btn_radio_play_selector);
                    b.remove(MusicService.EXTRA_MUSIC_ITEM);
                    mRadioTitle.setText(R.string.activity_radio_default_title);
                } else {
                    mRadioTitle.setText(R.string.activity_radio_default_title);
                }
                mCurrentPlayingStatus = b;
                for (int idx = 0; idx < mActivityInteractionListener.size(); idx++) {
                    mActivityInteractionListener.get(idx).onPlayItemChanged(b);
                }

                mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_HIDE_PROGRESS_DIALOG, 1500);
                break;
            case HANDLER_MESSAGE_HIDE_PROGRESS_DIALOG :
                dismissProgressDialog();
                break;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.d(TAG, ">> mBroadcastReceiver action received = " + action);
            mHandler.removeMessages(Constants.HANDLER_MESSAGE_PLAY_RADIO_CHANNEL_TIMEOUT);
            // send handler message
            switch (action) {
                case MusicService.ACTION_PLAYED :
                case MusicService.ACTION_PAUSED :
                case MusicService.ACTION_COMPLETED :
                case MusicService.ACTION_ERROR :
                case MusicService.ACTION_STOPPED :
                case MusicService.ACTION_PLAYING_STATUS :
                    Message msg = new Message();
                    msg.what = HANDLER_MESSAGE_MUSIC_SERVICE_ACTION;
                    Bundle b = new Bundle();
                    b.putString(MusicService.EXTRA_ACTION, action);
                    b.putSerializable(MusicService.EXTRA_PLAYING_STATUS, intent.getSerializableExtra(MusicService.EXTRA_PLAYING_STATUS));
                    b.putParcelable(MusicService.EXTRA_MUSIC_ITEM, intent.getParcelableExtra(MusicService.EXTRA_MUSIC_ITEM));

                    msg.setData(b);
                    mHandler.sendMessage(msg);
                    break;
                default :
                    break;
            }
        }
    };

    private class SettingsContentObserver extends ContentObserver {
        int previousVolume;
        Context context;

        public SettingsContentObserver(Context c, Handler handler) {
            super(handler);
            context = c;

            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

            int delta = previousVolume - currentVolume;
            if (delta != 0) {
                previousVolume = currentVolume;

                Message msg = new Message();
                msg.what = HANDLER_MESSAGE_STREAM_MUSIC_VOLUME_CHANGE;
                msg.arg1 = previousVolume;
                mHandler.sendMessage(msg);
            }
        }
    }

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

        mSettingsContentObserver = new SettingsContentObserver(this,new Handler());
        getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver );

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
            finish();
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_PLAYED);
        filter.addAction(MusicService.ACTION_PAUSED);
        filter.addAction(MusicService.ACTION_STOPPED);
        filter.addAction(MusicService.ACTION_COMPLETED);
        filter.addAction(MusicService.ACTION_ERROR);
        filter.addAction(MusicService.ACTION_PLAYING_STATUS);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);
        // send playing status
        Intent queryStatus = new Intent(this, MusicService.class);
        queryStatus.setAction(MusicService.ACTION_PLAYING_STATUS);
        startService(queryStatus);


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

        mActivityLayout = (RelativeLayout) findViewById(R.id.radio_activity_background);
        int wallpagerId = LauncherSettings.getInstance(this).getWallpagerResourceId();
        mActivityLayout.setBackgroundResource(LauncherSettings.landWallpaperResource[wallpagerId]);

        // title.
        mRadioTitle = (TextView) findViewById(R.id.radio_activity_label);

        // media controller
        mPlayToggle = (ImageButton) findViewById(R.id.ic_media_control_play);
        mPlayToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentPlayingStatus == null) {
                    return;
                }
                MusicService.State state = (MusicService.State)mCurrentPlayingStatus.getSerializable(MusicService.EXTRA_PLAYING_STATUS);
                Intent i = new Intent(RadioActivity.this, MusicService.class);
                if (state == MusicService.State.Playing) {
                    i.setAction(MusicService.ACTION_PAUSE);
                } else if (state == MusicService.State.Paused) {
                    i.setAction(MusicService.ACTION_PLAY);
                }
                startService(i);
            }
        });
        mPlayStop = (ImageButton) findViewById(R.id.ic_media_control_stop);
        mPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentPlayingStatus == null) {
                    return;
                }
                MusicService.State state = (MusicService.State)mCurrentPlayingStatus.getSerializable(MusicService.EXTRA_PLAYING_STATUS);
                if (state == MusicService.State.Playing || state == MusicService.State.Paused) {
                    Intent i = new Intent(RadioActivity.this, MusicService.class);
                    i.setAction(MusicService.ACTION_STOP);
                    i.putExtra(MusicService.EXTRA_MUSIC_FORCE_STOP, false);
                    startService(i);
                }
            }
        });

        mSoundToggle = (ImageButton) findViewById(R.id.ic_media_control_volume_btn);
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (currentVolume <= 0) {
            mSoundToggle.setBackgroundResource(R.drawable.ic_button_radio_sound_off);
        } else {
            mSoundToggle.setBackgroundResource(R.drawable.ic_button_radio_sound_on);
        }
        mSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) <= 0) {
                    if (mSoundTogglePreviousValue > 0) {
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, mSoundTogglePreviousValue, AudioManager.FLAG_PLAY_SOUND);
                    } else {
                        mSoundTogglePreviousValue = 1;
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, mSoundTogglePreviousValue, AudioManager.FLAG_PLAY_SOUND);
                    }
                    mSoundToggle.setBackgroundResource(R.drawable.ic_button_radio_sound_on);
                    mSeekbar.setProgress(mSoundTogglePreviousValue);
                    mSoundTogglePreviousValue = -1;
                } else {
                    mSoundTogglePreviousValue = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
                    mSoundToggle.setBackgroundResource(R.drawable.ic_button_radio_sound_off);
                }
            }
        });

        mSeekbar = (SeekBar) findViewById(R.id.ic_media_control_volume_seek);
        mSeekbar.setMax(maxVolume);
        mSeekbar.setProgress(currentVolume);
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mSoundTogglePreviousValue = -1;
                    AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        new GetRadioChannelTask(this, mHandler, mShortcutData.getDomain() + mShortcutData.getPath()).execute();
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
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

    @Override
    public Bundle getMusicPlayingStatus() {
        return mCurrentPlayingStatus;
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

    public void setOnRadioActivityInteractionListener(OnRadioActivityInteractionListener l) {
        if (l != null) {
            mActivityInteractionListener.add(l);
        }
    }

    public void removeOnRadioActivityInteractionListener(OnRadioActivityInteractionListener l) {
        if (l != null) {
            mActivityInteractionListener.remove(l);
        }
    }
}
