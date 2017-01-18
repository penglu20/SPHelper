package com.pl.sphelper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

import static com.pl.sphelper.ConstantUtil.*;


public class SPContentProvider extends ContentProvider{

    @Override
    public boolean onCreate() {

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String[] path= uri.getPath().split(SEPARATOR);
        String type=path[1];
        if (type.equals(TYPE_GET_ALL)){
            Map<String, ?> all = SPHelperImpl.getAll(getContext());
            if (all!=null) {
                MatrixCursor cursor = new MatrixCursor(new String[]{CURSOR_COLUMN_NAME,CURSOR_COLUMN_TYPE,CURSOR_COLUMN_VALUE});
                Set<String> keySet = all.keySet();
                for (String key:keySet){
                    Object[] rows=new Object[3];
                    rows[0]=key;
                    rows[2]=all.get(key);
                    if (rows[2] instanceof Boolean) {
                        rows[1]=TYPE_BOOLEAN;
                    }else
                    if (rows[2] instanceof String) {
                        rows[1]=TYPE_STRING;
                    }else
                    if (rows[2] instanceof Integer) {
                        rows[1]=TYPE_INT;
                    }else
                    if (rows[2] instanceof Long) {
                        rows[1]=TYPE_LONG;
                    }else
                    if (rows[2] instanceof Float) {
                        rows[1]=TYPE_FLOAT;
                    }
                    cursor.addRow(rows);
                }
                return cursor;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        // 用这个来取数值
        String[] path= uri.getPath().split(SEPARATOR);
        String type=path[1];
        String key=path[2];
        if (type.equals(TYPE_CONTAIN)){
            return SPHelperImpl.contains(getContext(),key)+"";
        }
        return  ""+SPHelperImpl.get(getContext(),key,type);
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String[] path= uri.getPath().split(SEPARATOR);
        String type=path[1];
        String key=path[2];
        Object obj= (Object) values.get(VALUE);
        if (obj!=null)
            SPHelperImpl.save(getContext(),key,obj);
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String[] path= uri.getPath().split(SEPARATOR);
        String type=path[1];
        if (type.equals(TYPE_CLEAN)){
            SPHelperImpl.clear(getContext());
            return 0;
        }
        String key=path[2];
        if (SPHelperImpl.contains(getContext(),key)){
            SPHelperImpl.remove(getContext(),key);
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
       insert(uri,values);
       return 0;
    }
}