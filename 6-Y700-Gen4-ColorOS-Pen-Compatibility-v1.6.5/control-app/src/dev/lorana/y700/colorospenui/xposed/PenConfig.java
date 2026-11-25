package dev.lorana.y700.colorospenui.xposed;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

final class PenConfig {
    static final String PREFS = "pen_config";
    static final String AUTO = "auto_fallback";
    static final String NORMAL_IME = "normal_ime";
    static final String HANDWRITING_IME = "handwriting_ime";
    static final String GLOBAL_AUTO = "y700_pen_ime_auto";
    static final String DEFAULT_NORMAL = "com.baidu.input/.ImeService";
    static final String DEFAULT_HANDWRITING = "com.sohu.inputmethod.sogouoem/.SogouIME";

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean auto(Context context) {
        if (Settings.Global.getString(context.getContentResolver(), GLOBAL_AUTO) == null) {
            boolean local = prefs(context).getBoolean(AUTO, true);
            try {
                Settings.Global.putInt(context.getContentResolver(), GLOBAL_AUTO, local ? 1 : 0);
            } catch (SecurityException ignored) {}
            return local;
        }
        return sharedAuto(context);
    }

    static boolean sharedAuto(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), GLOBAL_AUTO, 1) != 0;
    }
    static String normalIme(Context context) {
        return configuredIme(context, "y700_pen_module_normal_ime", NORMAL_IME, DEFAULT_NORMAL);
    }

    static String handwritingIme(Context context) {
        return configuredIme(context, "y700_pen_handwriting_ime", HANDWRITING_IME, DEFAULT_HANDWRITING);
    }

    private static String configuredIme(Context context, String sharedKey, String preference, String fallback) {
        String shared = Settings.Global.getString(context.getContentResolver(), sharedKey);
        String local = prefs(context).getString(preference, fallback);
        InputMethodManager manager = context.getSystemService(InputMethodManager.class);
        if (manager == null) return null;
        boolean localEnabled = false;
        for (InputMethodInfo info : manager.getEnabledInputMethodList()) {
            if (info.getId().equals(shared)) return shared;
            if (info.getId().equals(local)) localEnabled = true;
        }
        return localEnabled ? local : null;
    }
}
