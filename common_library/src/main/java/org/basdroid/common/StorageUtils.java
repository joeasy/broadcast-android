package org.basdroid.common;

import android.os.Environment;

/**
 * Created by basagee on 2015. 7. 19..
 */
public class StorageUtils {

    /**
     * @return True if the external storage is available. False otherwise.
     */
    public static boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * @return True if the external storage is writable. False otherwise.
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;

    }


}
