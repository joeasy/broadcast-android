package com.nbplus.vbroadlauncher.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nbplus.vbroadlauncher.R;

import org.basdroid.common.StringUtils;

import java.util.ArrayList;

/**
 * Created by basagee on 2015. 5. 21..
 */
public class GridViewAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<ApplicationInfo> mAppList;

    public static class AppViewHolder {
        public ImageView icon;
        public TextView name;
        public ApplicationInfo appInfo;
    }

    public GridViewAdapter(Context context, ArrayList<ApplicationInfo> appList) {
        this.context = context;
        this.mAppList = appList;
    }

    public void setApplicationList(ArrayList<ApplicationInfo> appList) {
        this.mAppList = appList;
        this.notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        AppViewHolder viewHolder;

        if (convertView == null) {
            // get layout from mobile.xml
            convertView = inflater.inflate(R.layout.gridview, null);
            viewHolder = new AppViewHolder();
            // set value into textview
            viewHolder.name = (TextView) convertView.findViewById(R.id.grid_item_label);
            // set image based on selected text
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.grid_item_image);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AppViewHolder) convertView.getTag();
        }

        ApplicationInfo appInfo = mAppList.get(position);
        if (appInfo != null) {
            String label = appInfo.loadLabel(context.getPackageManager()).toString();
            if (StringUtils.isEmptyString(label)) {
                viewHolder.name.setText("xxxxx");
            } else {
                viewHolder.name.setText(label);
            }
            viewHolder.icon.setImageDrawable(appInfo.loadIcon(context.getPackageManager()));
            viewHolder.appInfo = appInfo;
        }

        return convertView;
    }
    @Override
    public int getCount() {
        return mAppList.size();
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
