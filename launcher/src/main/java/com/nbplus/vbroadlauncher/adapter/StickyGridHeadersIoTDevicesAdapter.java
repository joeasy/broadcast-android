/*
 Copyright 2013 Tonic Artos

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.nbplus.vbroadlauncher.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nbplus.iotgateway.data.IoTDevice;
import com.nbplus.vbroadlauncher.R;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleAdapter;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersSimpleArrayAdapter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Tonic Artos
 */
public class StickyGridHeadersIoTDevicesAdapter extends BaseAdapter implements
        StickyGridHeadersSimpleAdapter {
    protected static final String TAG = StickyGridHeadersSimpleArrayAdapter.class.getSimpleName();

    private int mHeaderResId;

    private LayoutInflater mInflater;

    private int mItemResId;
    String[] deviceTypeStringArray;

    private ArrayList<IoTDevice> mItems = new ArrayList<>();

    public StickyGridHeadersIoTDevicesAdapter(Context context, ArrayList<IoTDevice> items, int headerResId,
                                              int itemResId) {
        init(context, items, headerResId, itemResId);
    }

    public StickyGridHeadersIoTDevicesAdapter(Context context, IoTDevice[] items, int headerResId,
                                              int itemResId) {
        init(context, new ArrayList<IoTDevice>(Arrays.asList(items)), headerResId, itemResId);
    }

    public void setItems(ArrayList<IoTDevice> items) {
        if (items != null) {
            mItems = items;
        } else {
            mItems = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public long getHeaderId(int position) {
        IoTDevice item = getItem(position);
        return item.getDeviceTypeId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(mHeaderResId, parent, false);
            holder = new HeaderViewHolder();
            holder.textView = (TextView)convertView.findViewById(R.id.header_title);
            holder.imageView = (ImageView)convertView.findViewById(R.id.header_icon);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder)convertView.getTag();
        }

        IoTDevice item = getItem(position);

        // set header text as first char in string
        String deviceTypeText = deviceTypeStringArray[item.getDeviceTypeId()];
        holder.textView.setText(deviceTypeText);
        switch (item.getDeviceTypeId()) {
            case IoTDevice.DEVICE_TYPE_ID_BT :
                holder.imageView.setImageResource(R.drawable.iot_bt);
                break;
            case IoTDevice.DEVICE_TYPE_ID_ZW :
                holder.imageView.setImageResource(R.drawable.iot_zwave);
                break;
            case IoTDevice.DEVICE_TYPE_ID_IR :
                holder.imageView.setImageResource(R.drawable.iot_ir);
                break;
            default:
            case IoTDevice.DEVICE_TYPE_ID_NONE :
                holder.imageView.setImageResource(android.R.drawable.ic_menu_help);
                break;
        }


        return convertView;
    }

    @Override
    public IoTDevice getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    @SuppressWarnings("unchecked")
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(mItemResId, parent, false);
            holder = new ViewHolder();
            holder.textView = (TextView)convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        IoTDevice item = getItem(position);
        holder.textView.setText(item.getDeviceName());

        convertView.setClickable(false);
        return convertView;
    }

    private void init(Context context, ArrayList<IoTDevice> items, int headerResId, int itemResId) {
        if (items != null) {
            this.mItems = items;
        } else {
            this.mItems = new ArrayList<>();
        }
        this.mHeaderResId = headerResId;
        this.mItemResId = itemResId;
        mInflater = LayoutInflater.from(context);
        Resources res = context.getResources();
        deviceTypeStringArray = res.getStringArray(R.array.iot_device_type);
    }

    protected class HeaderViewHolder {
        public ImageView imageView;
        public TextView textView;
    }

    protected class ViewHolder {
        public TextView textView;
    }
}
