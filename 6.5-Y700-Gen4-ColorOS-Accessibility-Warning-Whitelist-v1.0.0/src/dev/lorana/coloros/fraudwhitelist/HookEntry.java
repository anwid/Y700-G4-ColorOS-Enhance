package dev.lorana.coloros.fraudwhitelist;

import android.content.Context;
import android.provider.Settings;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    static final String SETTINGS_KEY = "lorana_fraud_accessibility_whitelist";
    static final String DEFAULT_PACKAGE = "dev.lorana.y700.colorospenui.xposed";

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (!"android".equals(p.packageName)) return;
        try {
            Class<?> runnable = XposedHelpers.findClass(
                    "com.android.server.am.FraudBehaviorDetectManager$GlobalCloseGuideRunnable",
                    p.classLoader);
            XposedBridge.hookAllMethods(runnable, "run", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        String pkg = (String) XposedHelpers.getObjectField(param.thisObject, "pkgName");
                        if (pkg == null || !isWhitelisted(param.thisObject, pkg)) return;
                        XposedBridge.log("ColorOSFraudWhitelist: suppressed accessibility warning for " + pkg);
                        param.setResult(null);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            });
            XposedBridge.log("ColorOSFraudWhitelist: last-mile accessibility warning hook armed");
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean isWhitelisted(Object runnable, String pkg) {
        if (DEFAULT_PACKAGE.equals(pkg)) return true;
        Context context = getSystemContext(runnable);
        if (context == null) return false;
        String raw = Settings.Secure.getString(context.getContentResolver(), SETTINGS_KEY);
        if (raw == null || raw.trim().isEmpty()) return false;
        String[] entries = raw.split("[,;\\s]+");
        for (String entry : entries) {
            if (pkg.equals(entry.trim())) return true;
        }
        return false;
    }

    private static Context getSystemContext(Object runnable) {
        try {
            Object manager = XposedHelpers.getObjectField(runnable, "this$0");
            Object ams = XposedHelpers.getObjectField(manager, "mAms");
            return (Context) XposedHelpers.getObjectField(ams, "mContext");
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }
}
