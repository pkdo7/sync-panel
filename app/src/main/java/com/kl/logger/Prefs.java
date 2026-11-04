package com.kl.logger;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private Prefs() {}
    private static final String NAME = "sp";
    private static final String KEY_TOKEN = "tok";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static String token(Context c) {
        return sp(c).getString(KEY_TOKEN, "");
    }

    static void setToken(Context c, String v) {
        sp(c).edit().putString(KEY_TOKEN, v).apply();
    }
}
