package com.ftsafe.iccd.ecard;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by takakiyo on 15/8/6.
 */
public class Config {

    public static final String APP_ID = "com.ftsafe.ecard";
    public static final String SERVER_URL = "http://192.168.9.202:8080/epload";
    public static final String CHARSET = "utf-8";

    public static final String KEY_STATUS = "status";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_DEPOSIT = "deposit";
    public static final String KEY_DRAW = "draw";
    public static final String KEY_CONSUME = "consume";
    public static final String KEY_RANDOM = "random";
    public static final String KEY_MSG = "msg";

    public static final int RESULT_STATUS_SUCCESS = 0;
    public static final int RESULT_STATUS_ERROR = 1;
    public static final int RESULT_STATUS_HALLOG = 2;

    public static final String REFUSE_SERVICE = "无法连接服务中心。";


    public static void cachedToken(Context context, String token) {
        SharedPreferences sp = context.getSharedPreferences(APP_ID, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_TOKEN, token);
        editor.commit();
    }
    public static String getCachedToken(Context context) {
        return context.getSharedPreferences(APP_ID, context.MODE_PRIVATE).getString(KEY_TOKEN, null);
    }

    public static void cachedPhone(Context context, String phone) {
        SharedPreferences.Editor ed = context.getSharedPreferences(APP_ID, context.MODE_PRIVATE).edit();
        ed.putString(KEY_PHONE, phone);
        ed.commit();
    }

    public static String getCachedPhone(Context context) {
        return context.getSharedPreferences(APP_ID, context.MODE_PRIVATE).getString(KEY_PHONE, null);
    }
}
