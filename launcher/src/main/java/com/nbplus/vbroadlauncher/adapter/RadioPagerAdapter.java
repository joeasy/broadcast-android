package com.nbplus.vbroadlauncher.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.nbplus.vbroadlauncher.data.ShowAllLaunchAppsInfo;
import com.nbplus.vbroadlauncher.fragment.AppGridFragment;
import com.nbplus.vbroadlauncher.fragment.RadioGridFragment;

import java.lang.reflect.Method;

/**
 * Created by basagee on 2015. 5. 7..
 */
public class RadioPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = RadioPagerAdapter.class.getSimpleName();
    private Context mContext;

    // for page count calculate
    private int mPageCount;

    public RadioPagerAdapter(Context context, FragmentManager fm, int pageCount) {
        super(fm);
        mContext = context;

        mPageCount = pageCount;
    }

    @Override
    public Fragment getItem(int pos) {
        //String parameter
        Class[] paramInteger = new Class[1];
        paramInteger[0] = Integer.class;
        Fragment fragment = null;

        try {
            if (pos >= 0 && pos < getCount()) {
                Object obj =  RadioGridFragment.class.newInstance();
                Method method = RadioGridFragment.class.getDeclaredMethod("newInstance", paramInteger);
                fragment = (Fragment)method.invoke(obj, new Integer(pos));
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fragment;
    }

    @Override
    public int getCount() {
        return mPageCount;
    }

}
