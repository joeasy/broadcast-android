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

package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.RadioActivity;
import com.nbplus.vbroadlauncher.adapter.RadioGridViewAdapter;
import com.nbplus.vbroadlauncher.callback.OnRadioActivityInteractionListener;
import com.nbplus.vbroadlauncher.callback.OnRadioFragmentInteractionListener;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

import java.util.ArrayList;

/**
 * 라디오 재생...
 * 1. 시작시에 뮤직서비스의 플레이 상태를 알아보기 위하여 messenger를 이용하여 조회한다.
 * 2. 그 외에 실행중 뮤직서비스 상태변화는 LocalBroadcastManager 를 이용한다.
 */
public class RadioGridFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener, OnRadioActivityInteractionListener {

    private static final String TAG = RadioGridFragment.class.getSimpleName();

    private static final String ARGS_KEY_PAGE_POSITION = "key_page_position";
    private static final String ARGS_KEY_RADIO_CHANNEL_LIST = "key_radio_channel_list";

    private GridView mGridLayout;
    RadioGridViewAdapter mAdapter;
    ArrayList<RadioChannelInfo.RadioChannel> mRadioChannelList;
    int mViewPagePosition = -1;
    Activity mActivity;

    private OnRadioFragmentInteractionListener mListener;

    int mMaxIconView;
    boolean mCreated = false;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param page view pager position.
     * @return A new instance of fragment AppGridFragment.
     */
    public static RadioGridFragment newInstance(Integer page, ArrayList<RadioChannelInfo.RadioChannel> channels) {
        RadioGridFragment fragment = new RadioGridFragment();
        Bundle args = new Bundle();
        args.putInt(ARGS_KEY_PAGE_POSITION, page);
        args.putSerializable(ARGS_KEY_RADIO_CHANNEL_LIST, channels);
        fragment.setArguments(args);
        return fragment;
    }

    public RadioGridFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mViewPagePosition = getArguments().getInt(ARGS_KEY_PAGE_POSITION);
            mRadioChannelList = (ArrayList<RadioChannelInfo.RadioChannel>)getArguments().getSerializable(ARGS_KEY_RADIO_CHANNEL_LIST);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_radio_grid, container, false);
        mGridLayout = (GridView)v.findViewById(R.id.grid_layout);

        Bundle b = null;
        if (mListener != null) {
            b = mListener.getMusicPlayingStatus();
        }

        mAdapter = new RadioGridViewAdapter(getActivity(), mRadioChannelList, this);
        mGridLayout.setEmptyView(v.findViewById(R.id.empty_list_view));
        mAdapter.setPlayingItems(b);
        mGridLayout.setAdapter(mAdapter);
        mGridLayout.setOnItemClickListener(this);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = activity;
        try {
            mListener = (OnRadioFragmentInteractionListener) activity;
            ((RadioActivity)mActivity).setOnRadioActivityInteractionListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        ((RadioActivity)mActivity).removeOnRadioActivityInteractionListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Log.d(TAG, "Radio channel onItemClick()");
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        RadioGridViewAdapter.RadioViewHolder viewHolder = (RadioGridViewAdapter.RadioViewHolder) view.getTag();
        RadioChannelInfo.RadioChannel channel = (RadioChannelInfo.RadioChannel)viewHolder.radioChannel;

        viewHolder.channelButton.setSelected(true);
        Log.d(TAG, ">> clicked radio channel name = " + channel.channelName + ", url = " + channel.channelUrl);

        if (mListener != null) {
            mListener.onPlayRadioRequest(channel);
        }
    }

    @Override
    public void onPlayItemChanged(Bundle b) {
        mAdapter.setPlayingItems(b);
        mAdapter.notifyDataSetChanged();
    }
}
