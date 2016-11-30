package com.pl.sphelper;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import java.util.HashSet;
import java.util.Set;

import static com.pl.sphelper.ConstantUtil.CONTENT_URI;
import static com.pl.sphelper.ConstantUtil.NULL_STRING;
import static com.pl.sphelper.ConstantUtil.SEPARATOR;
import static com.pl.sphelper.ConstantUtil.TYPE_BOOLEAN;
import static com.pl.sphelper.ConstantUtil.TYPE_CONTAIN;
import static com.pl.sphelper.ConstantUtil.TYPE_FLOAT;
import static com.pl.sphelper.ConstantUtil.TYPE_INT;
import static com.pl.sphelper.ConstantUtil.TYPE_LONG;
import static com.pl.sphelper.ConstantUtil.TYPE_STRING;
import static com.pl.sphelper.ConstantUtil.TYPE_STRING_SET;
import static com.pl.sphelper.ConstantUtil.VALUE;

public class SPHelper {
    public static final String COMMA_REPLACEMENT="__COMMA__";

    public static Context context;

    public static void checkContext() {
        if (context == null) {
            throw new IllegalStateException("context has not been initialed");
        }
    }

    public static void init(Application application) {
        context = application.getApplicationContext();
    }

    public synchronized static void save(String name, Boolean t) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_BOOLEAN + SEPARATOR + name);
        ContentValues cv = new ContentValues();
        cv.put(VALUE, t);
        cr.update(uri, cv, null, null);
    }

    public synchronized static void save(String name, String t) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_STRING + SEPARATOR + name);
        ContentValues cv = new ContentValues();
        cv.put(VALUE, t);
        cr.update(uri, cv, null, null);
    }

    public synchronized static void save(String name, Integer t) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_INT + SEPARATOR + name);
        ContentValues cv = new ContentValues();
        cv.put(VALUE, t);
        cr.update(uri, cv, null, null);
    }

    public synchronized static void save(String name, Long t) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_LONG + SEPARATOR + name);
        ContentValues cv = new ContentValues();
        cv.put(VALUE, t);
        cr.update(uri, cv, null, null);
    }

    public synchronized static void save(String name, Float t) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_BOOLEAN + SEPARATOR + name);
        ContentValues cv = new ContentValues();
        cv.put(VALUE, t);
        cr.update(uri, cv, null, null);
    }


    public synchronized static void save(String name, Set<String> t) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_STRING_SET + SEPARATOR + name);
        ContentValues cv = new ContentValues();
        Set<String> convert=new HashSet<>();
        for (String string:t){
            convert.add(string.replace(",",COMMA_REPLACEMENT));
        }
        cv.put(VALUE, convert.toString());
        cr.update(uri, cv, null, null);
    }

    public static String getString(String name, String defaultValue) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_STRING + SEPARATOR + name);
        String rtn = cr.getType(uri);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defaultValue;
        }
        return rtn;
    }

    public static int getInt(String name, int defaultValue) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_INT + SEPARATOR + name);
        String rtn = cr.getType(uri);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defaultValue;
        }
        return Integer.parseInt(rtn);
    }

    public static float getFloat(String name, float defaultValue) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_FLOAT + SEPARATOR + name);
        String rtn = cr.getType(uri);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defaultValue;
        }
        return Float.parseFloat(rtn);
    }

    public static boolean getBoolean(String name, boolean defaultValue) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_BOOLEAN + SEPARATOR + name);
        String rtn = cr.getType(uri);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(rtn);
    }

    public static long getLong(String name, long defaultValue) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_LONG + SEPARATOR + name);
        String rtn = cr.getType(uri);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defaultValue;
        }
        return Long.parseLong(rtn);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static Set<String> getStringSet(String name, Set<String> defaultValue) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_STRING_SET + SEPARATOR + name);
        String rtn = cr.getType(uri);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defaultValue;
        }
        if (!rtn.matches("\\[.*\\]")){
            return defaultValue;
        }
        String sub=rtn.substring(1,rtn.length()-1);
        String[] spl=sub.split(",");
        Set<String> returns=new HashSet<>();
        for (String t:spl){
            returns.add(t.replace(COMMA_REPLACEMENT,","));
        }
        return returns;
    }

    public static boolean contains(String name) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_CONTAIN + SEPARATOR + name);
        String rtn = cr.getType(uri);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return false;
        } else {
            return Boolean.parseBoolean(rtn);
        }
    }

    public static void remove(String name) {
        checkContext();
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse(CONTENT_URI + SEPARATOR + TYPE_LONG + SEPARATOR + name);
        cr.delete(uri, null, null);
    }
//    public static void clear(){
//        SharedPreferences sp=getSP();
//        SharedPreferences.Editor editor=sp.edit();
//        editor.clear();
//        editor.commit();
//    }
}