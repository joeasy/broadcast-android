package com.nbplus.vbroadlauncher.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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

    public static class RadioViewHolder {
        public ImageView icon;
        public TextView name;
        public String url;
    }

    public RadioGridViewAdapter(Context context, ArrayList<RadioChannelInfo.RadioChannel> channelList) {
        this.context = context;
        this.mRadioChannelList = channelList;
    }

    public void setApplicationList(ArrayList<RadioChannelInfo.RadioChannel> appList) {
        this.mRadioChannelList = appList;
        this.notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        RadioViewHolder viewHolder;

        if (convertView == null) {
            // get layout from mobile.xml
            convertView = inflater.inflate(R.layout.apps_grid_view, null);
            viewHolder = new RadioViewHolder();
            // set value into textview
            viewHolder.name = (TextView) convertView.findViewById(R.id.grid_item_label);
            // set image based on selected text
            //viewHolder.icon = (ImageView) convertView.findViewById(R.id.grid_item_image);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (RadioViewHolder) convertView.getTag();
        }

        RadioChannelInfo.RadioChannel radioChannel = mRadioChannelList.get(position);
        if (radioChannel != null) {
            if (StringUtils.isEmptyString(radioChannel.channelName)) {
                viewHolder.name.setText("xxxxx");
            } else {
                viewHolder.name.setText(radioChannel.channelName);
            }
            //viewHolder.icon.setImageDrawable(appInfo.loadIcon(context.getPackageManager()));
            viewHolder.url = radioChannel.channelUrl;
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
}
