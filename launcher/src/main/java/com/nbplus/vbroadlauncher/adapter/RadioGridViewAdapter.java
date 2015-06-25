package com.nbplus.vbroadlauncher.adapter;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nbplus.media.MusicRetriever;
import com.nbplus.media.MusicService;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

import org.basdroid.common.StringUtils;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 5. 21..
 */
public class RadioGridViewAdapter extends BaseAdapter {
    private static final String TAG = RadioGridViewAdapter.class.getSimpleName();

    private Context context;
    private ArrayList<RadioChannelInfo.RadioChannel> mRadioChannelList;
    private View.OnClickListener mClickListener;
    private long mPlayingItemIdx = -1;

    public static class RadioViewHolder {
        public Button channelButton;
        public RadioChannelInfo.RadioChannel radioChannel;
    }

    public RadioGridViewAdapter(Context context, ArrayList<RadioChannelInfo.RadioChannel> channelList, View.OnClickListener listener) {
        this.context = context;
        this.mRadioChannelList = channelList;
        this.mClickListener = listener;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        RadioViewHolder viewHolder;

        if (convertView == null) {
            // get layout from mobile.xml
            convertView = inflater.inflate(R.layout.radio_grid_view, null);
            viewHolder = new RadioViewHolder();
            convertView.setTag(viewHolder);
            viewHolder.channelButton = (Button)convertView.findViewById(R.id.btn_radio_channel);
        } else {
            viewHolder = (RadioViewHolder) convertView.getTag();
        }

        RadioChannelInfo.RadioChannel radioChannel = mRadioChannelList.get(position);
        if (radioChannel != null) {
            viewHolder.radioChannel = radioChannel;

            if (StringUtils.isEmptyString(radioChannel.channelName)) {
                viewHolder.channelButton.setText("xxxxx");
            } else {
                viewHolder.channelButton.setText(radioChannel.channelName);
            }
            if (radioChannel.index == mPlayingItemIdx) {
                viewHolder.channelButton.setBackgroundResource(R.drawable.ic_radio_channel_pressed);
                viewHolder.channelButton.setClickable(false);
                //convertView.setClickable();

                viewHolder.channelButton.setOnClickListener(null);
            } else {
                viewHolder.channelButton.setBackgroundResource(R.drawable.ic_radio_channel_selector);
                viewHolder.channelButton.setClickable(true);

                viewHolder.channelButton.setOnClickListener(this.mClickListener);
            }
        } else {
            Log.d(TAG, ">> invalid radioChannel...");
        }
        return convertView;
    }
    @Override
    public int getCount() {
        return mRadioChannelList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void setPlayingItems(Bundle b) {
        if (b != null && b.getSerializable(MusicService.EXTRA_PLAYING_STATUS) != null) {
            MusicService.State state = (MusicService.State)b.getSerializable(MusicService.EXTRA_PLAYING_STATUS);
            if (state == MusicService.State.Paused || state == MusicService.State.Playing) {
                MusicRetriever.Item item = b.getParcelable(MusicService.EXTRA_MUSIC_ITEM);
                if (item != null) {
                    mPlayingItemIdx = item.getId();
                } else {
                    mPlayingItemIdx = -1;
                }
            } else {
                mPlayingItemIdx = -1;
            }
        }
    }
}
