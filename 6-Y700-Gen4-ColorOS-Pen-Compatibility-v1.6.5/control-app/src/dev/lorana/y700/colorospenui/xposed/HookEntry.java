package dev.lorana.y700.colorospenui.xposed;

import android.app.Application;
import android.database.ContentObserver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Method;
import java.util.Set;

public final class HookEntry implements IXposedHookLoadPackage {
    private static volatile Context systemContext;
    private static volatile boolean cursorLogged;
    private static volatile boolean gamePenHookArmed;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if ("android".equals(p.packageName)) {
            hookSystemStylusCursor(p.classLoader);
            return;
        }
        if (!"com.oplus.ipemanager".equals(p.packageName)) return;
        try {
            Method hook = XposedHelpers.class.getMethod("findAndHookMethod", String.class, ClassLoader.class, String.class, Object[].class);
            Object[] args = new Object[]{ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    try { hookSettingPage(((Application) param.getResult()).getClassLoader()); }
                    catch (Throwable t) { XposedBridge.log(t); }
                }
            }};
            hook.invoke(null, "android.app.Instrumentation", null, "newApplication", args);
        } catch (Throwable t) { XposedBridge.log(t); }
    }

    private static void hookSystemStylusCursor(ClassLoader loader) {
        try {
            final Class<?> service = XposedHelpers.findClass("com.android.server.input.InputManagerService", loader);
            XposedBridge.hookAllConstructors(service, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args == null) return;
                    for (Object arg : param.args) {
                        if (arg instanceof Context) { systemContext = (Context) arg; break; }
                    }
                    armGamePenPolicy();
                }
            });
            XposedBridge.hookAllMethods(service, "setPointerIcon", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (systemContext == null || param.args == null || param.args.length < 4) return;
                        if (getGlobalInt("y700_pen_custom_hover_cursor", 1) == 0) return;
                        Object oldIcon = param.args[0];
                        if (oldIcon != null) {
                            Method getType = oldIcon.getClass().getMethod("getType");
                            int type = ((Integer) getType.invoke(oldIcon)).intValue();
                            if (type != 0 && type != 1) return;
                        }
                        int deviceId = ((Integer) param.args[2]).intValue();
                        Class<?> inputDevice = Class.forName("android.view.InputDevice");
                        Object device = inputDevice.getMethod("getDevice", Integer.TYPE).invoke(null, Integer.valueOf(deviceId));
                        if (device == null) return;
                        boolean stylus = ((Boolean) inputDevice.getMethod("supportsSource", Integer.TYPE)
                                .invoke(device, Integer.valueOf(0x4002))).booleanValue();
                        if (!stylus) return;
                        Resources resources = systemContext.getResources();
                        int id = resources.getIdentifier("pointer_spot_hover_icon", "drawable", "android");
                        if (id == 0) return;
                        Class<?> pointerIcon = Class.forName("android.view.PointerIcon");
                        Object dot = pointerIcon.getMethod("load", Resources.class, Integer.TYPE)
                                .invoke(null, resources, Integer.valueOf(id));
                        if (dot != null) {
                            param.args[0] = dot;
                            if (!cursorLogged) {
                                cursorLogged = true;
                                XposedBridge.log("Y700PenUI: installed ColorOS pointer_spot_hover for stylus device " + deviceId);
                            }
                        }
                    } catch (Throwable error) { XposedBridge.log(error); }
                }
            });
            final Class<?> fraud = XposedHelpers.findClass("com.android.server.am.FraudBehaviorDetectManager", loader);
            XposedBridge.hookAllMethods(fraud, "readySendAccessibilityChangedEvent", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 2) return;
                    String pkg = "dev.lorana.y700.colorospenui.xposed";
                    Set<?> added = (Set<?>) param.args[0];
                    Set<?> removed = (Set<?>) param.args[1];
                    if (added != null) added.remove(pkg);
                    if (removed != null) removed.remove(pkg);
                    if ((added == null || added.isEmpty()) && (removed == null || removed.isEmpty())) {
                        param.setResult(null);
                    }
                }
            });
            XposedBridge.log("Y700PenUI: scoped ColorOS accessibility-warning filter armed");
            XposedBridge.log("Y700PenUI: system stylus cursor hook armed");
        } catch (Throwable error) { XposedBridge.log(error); }
    }

    private static int getGlobalInt(String key, int def) {
        try {
            Class<?> global = Class.forName("android.provider.Settings$Global");
            Method method = global.getMethod("getInt", android.content.ContentResolver.class, String.class, Integer.TYPE);
            return ((Integer) method.invoke(null, systemContext.getContentResolver(), key, Integer.valueOf(def))).intValue();
        } catch (Throwable ignored) { return def; }
    }

    private static synchronized void armGamePenPolicy() {
        if (gamePenHookArmed || systemContext == null) return;
        try {
            final Handler handler = new Handler(Looper.getMainLooper());
            ContentObserver observer = new ContentObserver(handler) {
                @Override public void onChange(boolean selfChange) {
                    applyGamePenPolicy("setting-change");
                    handler.postDelayed(() -> applyGamePenPolicy("post-event-settle"), 120L);
                }
            };
            systemContext.getContentResolver().registerContentObserver(
                    Uri.parse("content://settings/global/debug_gamemode_value"), false, observer);
            systemContext.getContentResolver().registerContentObserver(
                    Uri.parse("content://settings/global/y700_pen_in_game"), false, observer);
            gamePenHookArmed = true;
            applyGamePenPolicy("hook-init");
            XposedBridge.log("Y700PenUI: ColorOS game-mode pen policy hook armed");
        } catch (Throwable error) { XposedBridge.log(error); }
    }

    private static boolean desiredPenState() {
        boolean inGame = getGlobalInt("debug_gamemode_value", 0) == 1;
        boolean allowInGame = getGlobalInt("y700_pen_in_game", 0) == 1;
        return !inGame || allowInGame;
    }

    private static void applyGamePenPolicy(String reason) {
        writePenState(desiredPenState(), reason);
    }

    private static void writePenState(boolean enabled, String reason) {
        String event = "desired=" + (enabled ? 1 : 0) + " reason=" + reason;
        Log.i("Y700PenGame", event);
        XposedBridge.log("Y700PenUI: game policy event " + event);
    }

    private static void hookSettingPage(ClassLoader loader) {
        try {
            Class<?> activity = XposedHelpers.findClass("com.oplus.ipemanager.btadsorb.setting.activity.PencilSettingActivity", loader);
            Method hook = XposedHelpers.class.getMethod("findAndHookMethod", Class.class, String.class, Object[].class);
            Object[] args = new Object[]{activity, Integer.class, String.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    param.args[1] = Integer.valueOf(2);
                }
            }};
            hook.invoke(null, activity, "c", args);
            XposedBridge.log("Y700PenUI: settings UI bridge active");
        } catch (Throwable t) { XposedBridge.log(t); }
    }
}
