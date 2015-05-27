package org.basdroid.common;

import android.content.res.Resources;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by basagee on 2015. 4. 30..
 */
public class StringUtils {
    private static final String TAG = StringUtils.class.getSimpleName();

    public final static String SHA1_ALGORITHM = "SHA-1";
    public final static String ISO_8859_1_CHAR_SET = "iso-8859-1";
    public final static String UTF_8_CHAR_SET = "UTF-8";

    private final static Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    // Cause android.util.Patterns.EMAIL_ADDRESS exists since API Level 8 only
    public final static boolean isEmailValid(String email) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            return !TextUtils.isEmpty(email) && EMAIL_ADDRESS_PATTERN.matcher(email).matches();
        } else {
            return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }
    }

    /**
     * Convert byte array to hex string
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for(int idx=0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10) sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Get utf8 byte array.
     * @param str
     * @return  array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str) {
        try { return str.getBytes(UTF_8_CHAR_SET); } catch (Exception ex) { return null; }
    }

    /**
     * Get utf8 byte array.
     * @param str
     * @return  array of NULL if error was found
     */
    public static byte[] getISO8859Bytes(String str) {
        try { return str.getBytes(ISO_8859_1_CHAR_SET); } catch (Exception ex) { return null; }
    }

    public static String sha1hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA1_ALGORITHM);
            md.update(getUTF8Bytes(text), 0, text.length());
            byte[] sha1hash = md.digest();
            return bytesToHex(sha1hash);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return null;
        }
    }

    public static boolean isEmptyString(String str) {
        return !(str != null && str.trim().length() > 0);
    }

    public static String loadRawResourceString(Resources res, int resourceId) throws IOException {
        InputStream is = res.openRawResource(resourceId);
        return loadString(is);
    }

    public static String loadAssetString(Resources res, String filename) throws IOException {
        InputStream is = res.getAssets().open(filename);
        return loadString(is);
    }

    public static String loadString(InputStream is) throws IOException {
        StringBuilder builder = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(is);
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            builder.append(buf, 0, numRead);
        }
        return builder.toString();
    }

    public static String[] union(String[] a, String[] b) {
        LinkedList<String> retVal = new LinkedList<String>();
        for (int i = 0; i < a.length; i++) {
            retVal.add(a[i]);
        }

        for (int i = 0; i < b.length; i++) {
            if (!retVal.contains(b[i])) {
                retVal.add(b[i]);
            }
        }

        String[] retArray = new String[retVal.size()];
        retVal.toArray(retArray);
        return retArray;

    }

    public static String[] intersection(String[] a, String[] b){
        List<String> bList = Arrays.asList(b);
        LinkedList<String> retVal = new LinkedList<String>();

        for(int i = 0; i < a.length; i++) {
            if(bList.contains(a[i])){
                retVal.add(a[i]);
            }
        }

        String[] retArray = new String[retVal.size()];
        retVal.toArray(retArray);
        return retArray;
    }

    public abstract static class ClickSpan extends ClickableSpan {

    }
    public static void clickify(TextView view, final String clickableText,
                                final ClickSpan span) {

        CharSequence text = view.getText();
        String string = text.toString();

        int start = string.indexOf(clickableText);
        int end = start + clickableText.length();
        if (start == -1) return;

        if (text instanceof Spannable) {
            ((Spannable)text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            SpannableString s = SpannableString.valueOf(text);
            s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            view.setText(s);
        }

        MovementMethod m = view.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    /**
     * Base64 인코딩
     */
    public static String getBase64encode(String content){
        return getBase64encode(content.getBytes(), 0, content.length(), 0);
    }

    public static String getBase64encode(byte[] input){
        return getBase64encode(input, 0, input.length, 0);
    }

    public static String getBase64encode(byte[] input, int offset, int len, int flags){
        return Base64.encodeToString(input, offset, len, flags);
    }

    /**
     * Base64 디코딩
     */
    public static byte[] getBase64decode(String content){
        return getBase64decode(content.getBytes(), 0, content.length(), 0);
    }

    public static byte[] getBase64decode(byte[]input){
        return getBase64decode(input, 0, input.length, 0);
    }

    public static byte[] getBase64decode(byte[] input, int offset, int len, int flags){
        return Base64.decode(input, offset, len, flags);
    }
    /**
     * getURLEncode
     */
    public static String getURLEncode(String content){
        try {
          return URLEncoder.encode(content, UTF_8_CHAR_SET);   // UTF-8
//          return URLEncoder.encode(content, "euc-kr");  // EUC-KR
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * getURLDecode
     */
    public static String getURLDecode(String content){
        try {
          return URLDecoder.decode(content, UTF_8_CHAR_SET);   // UTF-8
//          return URLDecoder.decode(content, "euc-kr");  // EUC-KR
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
