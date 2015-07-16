package com.nbplus.vbroadlauncher.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.nbplus.vbroadlauncher.data.ShowAllLaunchAppsInfo;
import com.nbplus.vbroadlauncher.fragment.AppGridFragment;

import java.lang.reflect.Method;

/**
 * Created by basagee on 2015. 5. 7..
 */
public class AppPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = AppPagerAdapter.class.getSimpleName();
    private Context mContext;

    // for page count calculate
    private int mAppSize;
    private int mAppPerPage;
    private int mQuotient;
    private int mRemainder;
    private int mPageCount;

    public AppPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;

        int appSize = ShowAllLaunchAppsInfo.getInstance().getCount();
        int appPerPage = ShowAllLaunchAppsInfo.getMaxPageItemSize(mContext);

        int quotient = appSize / appPerPage;
        int remainder = appSize % appPerPage;

        mPageCount = remainder > 0 ? quotient + 1 : quotient;
    }

    @Override
    public Fragment getItem(int pos) {
        //String parameter
        Class[] paramInteger = new Class[1];
        paramInteger[0] = Integer.class;
        Fragment fragment = null;

        try {
            if (pos >= 0 && pos < getCount()) {
                Object obj =  AppGridFragment.class.newInstance();
                Method method = AppGridFragment.class.getDeclaredMethod("newInstance", paramInteger);
                fragment = (Fragment)method.invoke(obj, new Integer(pos));
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fragment;
    }

    /**
     * This method should be called by the application if the data backing this adapter has changed
     * and associated views should update.
     */
    @Override
    public void notifyDataSetChanged() {

        int appSize = ShowAllLaunchAppsInfo.getInstance().getCount();
        int appPerPage = ShowAllLaunchAppsInfo.getMaxPageItemSize(mContext);

        int quotient = appSize / appPerPage;
        int remainder = appSize % appPerPage;

        mPageCount = remainder > 0 ? quotient + 1 : quotient;
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mPageCount;
    }

}
