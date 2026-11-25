package dev.lorana.y700.colorospenui.xposed;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

final class RouletteConfig {
    static final String TOUCHNOTES_PACKAGE = "com.newskyer.draw";
    private static final String KEY = "y700_pen_roulette_blocked_packages";

    private RouletteConfig() {}

    static Set<String> read(Context context) {
        String value = Settings.Global.getString(context.getContentResolver(), KEY);
        Set<String> packages = new TreeSet<>();
        if (value == null) {
            packages.add(TOUCHNOTES_PACKAGE);
        } else if (!value.isEmpty()) {
            packages.addAll(Arrays.asList(value.split(",")));
        }
        return packages;
    }

    static boolean save(Context context, Set<String> packages) {
        // An explicit empty string means the user unchecked every app.
        return Settings.Global.putString(context.getContentResolver(), KEY, TextUtils.join(",", packages));
    }
}
