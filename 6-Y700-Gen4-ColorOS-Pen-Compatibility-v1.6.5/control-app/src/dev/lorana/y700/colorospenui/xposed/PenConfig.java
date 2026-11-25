package dev.lorana.y700.colorospenui.xposed;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

final class PenConfig {
    static final String PREFS = "pen_config";
    static final String AUTO = "auto_fallback";
    static final String NORMAL_IME = "normal_ime";
    static final String HANDWRITING_IME = "handwriting_ime";
    private static final String RESTORE_MS = "restore_ms";
    private static final String GLOBAL_RESTORE_MS = "y700_pen_ime_restore_ms";
    private static final int DEFAULT_RESTORE_MS = 800;
    static final String DEFAULT_NORMAL = "com.baidu.input/.ImeService";
    static final String DEFAULT_HANDWRITING = "com.sohu.inputmethod.sogouoem/.SogouIME";

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean auto(Context context) { return prefs(context).getBoolean(AUTO, true); }
    static String normalIme(Context context) { return prefs(context).getString(NORMAL_IME, DEFAULT_NORMAL); }
    static String handwritingIme(Context context) { return prefs(context).getString(HANDWRITING_IME, DEFAULT_HANDWRITING); }
    static int restoreMs(Context context) {
        int local = clampRestoreMs(prefs(context).getLong(RESTORE_MS, DEFAULT_RESTORE_MS));
        int shared = Settings.Global.getInt(context.getContentResolver(), GLOBAL_RESTORE_MS, -1);
        return shared == -1 ? local : clampRestoreMs(shared);
    }

    static void migrateRestoreMs(Context context) {
        if (Settings.Global.getString(context.getContentResolver(), GLOBAL_RESTORE_MS) == null) {
            setRestoreMs(context, restoreMs(context));
        }
    }

    static boolean setRestoreMs(Context context, int ms) {
        int delay = clampRestoreMs(ms);
        try {
            return Settings.Global.putInt(context.getContentResolver(), GLOBAL_RESTORE_MS, delay);
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private static int clampRestoreMs(long ms) {
        return (int) Math.max(500, Math.min(15000, ms));
    }
}
