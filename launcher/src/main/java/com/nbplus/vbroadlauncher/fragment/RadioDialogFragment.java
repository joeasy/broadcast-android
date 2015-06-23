package com.nbplus.vbroadlauncher.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.adapter.AppGridViewAdapter;
import com.nbplus.vbroadlauncher.adapter.RadioGridViewAdapter;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;
import com.nbplus.vbroadlauncher.data.ShortcutData;
import com.nbplus.vbroadlauncher.data.ShowAllLaunchAppsInfo;
import com.nbplus.vbroadlauncher.service.GetRadioChannelTask;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.vov.vitamio.MediaPlayer;

/**
 * Created by basagee on 2015. 6. 16..
 */
public class RadioDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener {
    private static final String TAG = RadioDialogFragment.class.getSimpleName();
    // 각종 뷰 변수 선언
    ShortcutData mShortcutData;
    private ArrayList<RadioChannelInfo.RadioChannel> mRadioChannelItems = new ArrayList<>();
    private GridView mGridLayout;
    private RadioGridViewAdapter mAdapter;

    public RadioDialogFragment() {}

    public static RadioDialogFragment newInstance(ShortcutData data) {
        RadioDialogFragment fragment = new RadioDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.EXTRA_NAME_SHORTCUT_DATA, data);
        fragment.setArguments(bundle);
        return fragment;
    }

    private final RadioChannelListHandler mHandler = new RadioChannelListHandler(this);

    // 핸들러 객체 만들기
    private static class RadioChannelListHandler extends Handler {
        private final WeakReference<RadioDialogFragment> mFragment;

        public RadioChannelListHandler(RadioDialogFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            RadioDialogFragment fragment = mFragment.get();
            if (fragment != null) {
                fragment.handleMessage(msg);
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

                updateRadioChannelView();
                break;
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShortcutData = getArguments().getParcelable(Constants.EXTRA_NAME_SHORTCUT_DATA);
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     * <p/>
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radio, container);

        // remove dialog title
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        // remove dialog background
        getDialog().getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        // 레이아웃 XML과 뷰 변수 연결
        mGridLayout = (GridView)view.findViewById(R.id.grid_layout);

        new GetRadioChannelTask(getActivity(), mHandler, mShortcutData.getDomain() + mShortcutData.getPath()).execute();
        return view;
    }

    public void updateRadioChannelView() {
        /**
         * 새롭게 갱신해주지 않으면 swipe 시에 화면이 제대로 갱신되지 않는다.
         * mAdapter.setApplicationList(mAppsList)에서 notifyDataSetChanged()를 호출해주지만..
         * 제대로 되지 않는듯하다.
         */
        //if (mAdapter == null) {
        mAdapter = new RadioGridViewAdapter(getActivity(), mRadioChannelItems);
        mGridLayout.setAdapter(mAdapter);
        mGridLayout.setOnItemClickListener(this);
        //} else {
        //    mAdapter.setApplicationList(mAppsList);
        //}
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        RadioGridViewAdapter.RadioViewHolder viewHolder = (RadioGridViewAdapter.RadioViewHolder)view.getTag();

        String url = viewHolder.url;
        Log.d(TAG, ">> Play Radio name = " + viewHolder.name + ", url = " + viewHolder.url);
    }

    private void playAudio(Integer media) {
//        try {
//            switch (media) {
//                case LOCAL_AUDIO:
//                    /**
//                     * TODO: Set the path variable to a local audio file path.
//                     */
//                    path = "";
//                    if (path == "") {
//                        // Tell the user to provide an audio file URL.
//                        Toast.makeText(MediaPlayerDemo_Audio.this, "Please edit MediaPlayer_Audio Activity, " + "and set the path variable to your audio file path." + " Your audio file must be stored on sdcard.", Toast.LENGTH_LONG).show();
//                        return;
//                    }
//                    mMediaPlayer = new MediaPlayer(this);
//                    mMediaPlayer.setDataSource(path);
//                    mMediaPlayer.prepare();
//                    mMediaPlayer.start();
//                    break;
//                case RESOURCES_AUDIO:
//                    /**
//                     * TODO: Upload a audio file to res/raw folder and provide its resid in
//                     * MediaPlayer.create() method.
//                     */
//                    //Bug need fixed
//                    mMediaPlayer = createMediaPlayer(this, R.raw.test_cbr);
//                    mMediaPlayer.start();
//
//            }
//            tx.setText("Playing audio...");
//
//        } catch (Exception e) {
//            Log.e(TAG, "error: " + e.getMessage(), e);
//        }

    }

    public MediaPlayer createMediaPlayer(Context context, int resid) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            MediaPlayer mp = new MediaPlayer(context);
            mp.setDataSource(afd.getFileDescriptor());
            afd.close();
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (SecurityException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        }
        return null;
    }

}