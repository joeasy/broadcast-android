package com.nbplus.vbroadlauncher.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by basagee on 2015. 5. 7..
 */
public class AppViewPager extends ViewPager {
    private boolean swipeable = true;

    public AppViewPager(Context context) {
        super(context);
    }

    public AppViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Call this method in your motion events when you want to disable or enable
    // It should work as desired.
    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.swipeable ? super.onTouchEvent(event) : false;
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.swipeable ? super.onInterceptTouchEvent(event) : false;
    }

    public void moveNext() {
        //it doesn't matter if you're already in the last item
        setCurrentItem(getCurrentItem() + 1);
    }

    public void movePrevious() {
        //it doesn't matter if you're already in the first item
        setCurrentItem(getCurrentItem() - 1);
    }

    public Fragment getActiveFragment(FragmentManager fragmentManager, int position) {
        String name = makeFragmentName(getId(), position);
        final Fragment fragmentByTag = fragmentManager.findFragmentByTag(name);
        if (fragmentByTag == null) {
//            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            fragmentManager.dump("", null, new PrintWriter(outputStream, true), null);
//
//            final String s = new String(outputStream.toByteArray());
//            throw new IllegalStateException("Could not find fragment via hacky way.\n" +
//                    "We were looking for position: " + position + " name: " + name + "\n" +
//                    "Fragment at this position does not exists, or hack stopped working.\n" +
//                    "Current fragment manager dump is: " + s);
            return null;
        }
        return fragmentByTag;
    }

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }
}
