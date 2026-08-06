package dev.lorana.y700.colorospenui.xposed;

import android.content.Context;
import android.content.SharedPreferences;

final class PenConfig {
    static final String PREFS = "pen_config";
    static final String AUTO = "auto_fallback";
    static final String NORMAL_IME = "normal_ime";
    static final String HANDWRITING_IME = "handwriting_ime";
    static final String RESTORE_MS = "restore_ms";
    static final String DEFAULT_NORMAL = "com.baidu.input/.ImeService";
    static final String DEFAULT_HANDWRITING = "com.sohu.inputmethod.sogouoem/.SogouIME";

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean auto(Context context) { return prefs(context).getBoolean(AUTO, true); }
    static String normalIme(Context context) { return prefs(context).getString(NORMAL_IME, DEFAULT_NORMAL); }
    static String handwritingIme(Context context) { return prefs(context).getString(HANDWRITING_IME, DEFAULT_HANDWRITING); }
    static long restoreMs(Context context) { return prefs(context).getLong(RESTORE_MS, 800L); }
}
