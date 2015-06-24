package com.nbplus.vbroadlauncher.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.nbplus.vbroadlauncher.data.ShowAllLaunchAppsInfo;
import com.nbplus.vbroadlauncher.fragment.AppGridFragment;
import com.nbplus.vbroadlauncher.fragment.RadioGridFragment;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by basagee on 2015. 5. 7..
 */
public class RadioPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = RadioPagerAdapter.class.getSimpleName();
    private Context mContext;

    private ArrayList<RadioGridFragment> mFragments = new ArrayList<>();

    public RadioPagerAdapter(Context context, FragmentManager fm, ArrayList<RadioGridFragment> fragments) {
        super(fm);
        mContext = context;

        if (fragments != null) {
            this.mFragments = fragments;
        }
    }

    @Override
    public Fragment getItem(int pos) {
        return this.mFragments.get(pos);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

}
