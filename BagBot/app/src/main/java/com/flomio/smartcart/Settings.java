package com.flomio.smartcart;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private Context context;

    public Settings(Context context) {
        this.context = context;
    }

    private static final String PREFS_NAME = "smartcart";
    private static final String PREF_KEY_DEVICE_NAME = "deviceName";

    public void setString(String prefKey, String url) {
        SharedPreferences prefs = getPrefs(context);
        SharedPreferences.Editor edit = prefs.edit();
        if (url != null) {
            edit.putString(prefKey, url);
        } else  {
            edit.remove(prefKey);
        }
        edit.apply();
    }
    public String getString(String prefKey) {
        final SharedPreferences prefs = getPrefs(context);
        return prefs.getString(prefKey, null);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
