package org.basdroid.common;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by basagee on 2015. 4. 30..
 */
public class DisplayUtils {
    /**
     * Checks if the device is a tablet or a phone
     * 아래것은 해상도를 너무 엄격하게 따진다. 일단 사용하지 말자.
     *
     * @param activityContext
     *            The Activity Context.
     * @return Returns true if the device is a Tablet
     */
    /*
    @Deprecated
    public static boolean isTabletDevice(Context activityContext) {
        // Verifies if the Generalized Size of the device is XLARGE to be
        // considered a Tablet
        boolean xlarge = ((activityContext.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) ==
                Configuration.SCREENLAYOUT_SIZE_XLARGE);

        // If XLarge, checks if the Generalized Density is at least MDPI
        // (160dpi)
        if (xlarge) {
            DisplayMetrics metrics = new DisplayMetrics();
            Activity activity = (Activity) activityContext;
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            // MDPI=160, DEFAULT=160, DENSITY_HIGH=240, DENSITY_MEDIUM=160,
            // DENSITY_TV=213, DENSITY_XHIGH=320
            if (metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT
                    || metrics.densityDpi == DisplayMetrics.DENSITY_HIGH
                    || metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM
                    || metrics.densityDpi == DisplayMetrics.DENSITY_TV
                    || metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH) {

                // Yes, this is a tablet!
                return true;
            }
        }

        // No, this is not a tablet!
        return false;
    }
    */

    /**
     * Checks if the device is a tablet or a phone
     *
     * @param activityContext
     *            The Activity Context.
     * @return Returns true if the device is a Tablet
     */
    public static boolean isTabletDevice(Context activityContext) {
        boolean xlarge = ((activityContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
        boolean large = ((activityContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    /**
     * Checks if the device is a tablet or a phone
     *
     * @param activityContext
     *            The Activity Context.
     * @return Returns true if the device is a Tablet
     */
    /*
    public static boolean isTabletDevice(Context activityContext) {
        DisplayMetrics metrics = activityContext.getResources().getDisplayMetrics();

        int portrait_width_pixel = Math.min(metrics.widthPixels, metrics.heightPixels);
        int dots_per_virtual_inch = metrics.densityDpi;
        float virutal_width_inch = portrait_width_pixel / dots_per_virtual_inch;

        return virutal_width_inch > 2;
    }
    */

    /**
     * get screen size
     *
     * @param activityContext
     * @return Point
     */
    public static Point getScreenSize(Context activityContext) {
        Point size = new Point();
        Activity activity = (Activity) activityContext;

        Display display = activity.getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            size.set(display.getWidth(), display.getHeight());
        } else {
            display.getSize(size);
        }

        return size;
    }

    /**
     * get screen size
     *
     * @param activityContext
     * @return Point
     */
    public static Point getRealScreenSize(Context activityContext) {
        Point size = new Point();
        Activity activity = (Activity) activityContext;

        Display display = activity.getWindowManager().getDefaultDisplay();
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Method mGetRawH = null, mGetRawW = null;
                int width = 0, height = 0;

                mGetRawH = Display.class.getMethod("getRawHeight");
                mGetRawW = Display.class.getMethod("getRawWidth");

                try {
                    width = (Integer) mGetRawW.invoke(display);
                    height = (Integer) mGetRawH.invoke(display);
                    size.set(width, height);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    size.set(0, 0);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    size.set(0, 0);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    size.set(0, 0);
                }
            } else {
                display.getRealSize(size);
            }
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
            size.set(0, 0);
        }

        return size;
    }

    /**
     * get screen size
     *
     * @param activityContext
     * @return Point
     */
    public static Point getScreenDp(Context activityContext) {
        Point size = DisplayUtils.getRealScreenSize(activityContext);
        Activity activity = (Activity) activityContext;

        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        return new Point(size.x / (metrics.densityDpi / 160), size.y / (metrics.densityDpi / 160));
    }

    public static void setWallPaperResource(Context activityContext, int resId) {
        WallpaperManager myWallpaperManager = WallpaperManager.getInstance(activityContext);
        try {
            myWallpaperManager.setResource(resId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static float dpFromPx(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }


    public static float pxFromDp(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public static int getScreenOrientation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e("DisplayUtils", "Unknown screen orientation. Defaulting to " +
                            "portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e("DisplayUtils", "Unknown screen orientation. Defaulting to " +
                            "landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }
}
