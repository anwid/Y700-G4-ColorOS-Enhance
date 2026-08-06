package dev.lorana.y700.haptics;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.VibrationEffect;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final String SELF_PACKAGE = "dev.lorana.y700.haptics";
    private static final String DUAL_SERVICE = "vendor.qti.hardware.vibrator_dual.device.IVibratorDual/default";
    private static final String DUAL_DESCRIPTOR = "vendor.qti.hardware.vibrator_dual.device.IVibratorDual";
    private static volatile Context context;
    private static volatile IBinder dualBinder;
    private static final AtomicInteger dualGeneration = new AtomicInteger();

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) throws Throwable {
        if (!"android".equals(p.packageName)) return;
        try {
            Class<?> activityThread = XposedHelpers.findClass("android.app.ActivityThread", p.classLoader);
            Object thread = XposedHelpers.callStaticMethod(activityThread, "currentActivityThread");
            Object systemContext = XposedHelpers.callMethod(thread, "getSystemContext");
            if (systemContext instanceof Context) context = (Context) systemContext;
        } catch (Throwable t) { XposedBridge.log(t); }
        final Class<?> service = XposedHelpers.findClass("com.android.server.vibrator.VibratorManagerService", p.classLoader);
        XposedBridge.hookAllConstructors(service, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof Context) context = (Context) param.args[0];
            }
        });
        XposedBridge.hookAllMethods(service, "vibrateInternal", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (context == null || param.args == null || param.args.length < 5 || param.args[3] == null) return;
                    int usage = ((Integer) XposedHelpers.callMethod(param.args[4], "getUsage")).intValue();
                    String opPkg = param.args.length > 2 && param.args[2] instanceof String ? (String) param.args[2] : null;
                    String reason = param.args.length > 5 && param.args[5] instanceof String ? (String) param.args[5] : null;
                    if (SELF_PACKAGE.equals(opPkg)) {
                        String previewId = HapticConfig.getString(context.getContentResolver(), "preview", "id", "");
                        int previewIndex = indexOfId(previewId);
                        if (previewIndex < 0) return; // Laboratory effects are already fully built by the app.
                        int previewDuration = HapticConfig.get(context.getContentResolver(), previewId, "duration", HapticConfig.DEFAULT_DURATION[previewIndex]);
                        int previewAmplitude = HapticConfig.get(context.getContentResolver(), previewId, "amplitude", HapticConfig.DEFAULT_AMPLITUDE[previewIndex]);
                        int previewMode = HapticConfig.get(context.getContentResolver(), previewId, "mode", HapticConfig.DEFAULT_MODE[previewIndex]);
                        if (isDualMode(previewMode)) {
                            playDualPreset(previewMode, previewAmplitude);
                            param.setResult(null);
                            return;
                        }
                        String previewIds = HapticConfig.getString(context.getContentResolver(), previewId, "primitive_ids", HapticConfig.defaultPrimitiveIds(previewIndex));
                        String previewScales = HapticConfig.getString(context.getContentResolver(), previewId, "primitive_scales", HapticConfig.defaultPrimitiveScales(previewIndex));
                        String previewDelays = HapticConfig.getString(context.getContentResolver(), previewId, "primitive_delays", HapticConfig.defaultPrimitiveDelays(previewIndex));
                        String previewDurations = HapticConfig.getString(context.getContentResolver(), previewId, "segment_durations", HapticConfig.defaultSegmentDurations(previewIndex));
                        param.args[3] = transformCombined(param.args[3], previewDuration, previewAmplitude, previewMode, previewIds, previewScales, previewDelays, previewDurations);
                        return;
                    }
                    int index = HapticConfig.indexForRequest(context, usage, opPkg, reason);
                    String id = HapticConfig.IDS[index];
                    if (HapticConfig.get(context.getContentResolver(), id, "enabled", HapticConfig.DEFAULT_ENABLED[index]) == 0) return;
                    int duration = HapticConfig.get(context.getContentResolver(), id, "duration", HapticConfig.DEFAULT_DURATION[index]);
                    int amplitude = HapticConfig.get(context.getContentResolver(), id, "amplitude", HapticConfig.DEFAULT_AMPLITUDE[index]);
                    int mode = HapticConfig.get(context.getContentResolver(), id, "mode", HapticConfig.DEFAULT_MODE[index]);
                    if (isDualMode(mode)) {
                        playDualPreset(mode, amplitude);
                        param.setResult(null);
                        return;
                    }
                    String primitiveIds = HapticConfig.getString(context.getContentResolver(), id, "primitive_ids", HapticConfig.defaultPrimitiveIds(index));
                    String primitiveScales = HapticConfig.getString(context.getContentResolver(), id, "primitive_scales", HapticConfig.defaultPrimitiveScales(index));
                    String primitiveDelays = HapticConfig.getString(context.getContentResolver(), id, "primitive_delays", HapticConfig.defaultPrimitiveDelays(index));
                    String segmentDurations = HapticConfig.getString(context.getContentResolver(), id, "segment_durations", HapticConfig.defaultSegmentDurations(index));
                    param.args[3] = transformCombined(param.args[3], duration, amplitude, mode, primitiveIds, primitiveScales, primitiveDelays, segmentDurations);
                } catch (Throwable t) { XposedBridge.log(t); }
            }
        });
        XC_MethodHook cancelHook = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) { cancelDualPlayback(); }
        };
        XposedBridge.hookAllMethods(service, "cancelVibrate", cancelHook);
        XposedBridge.hookAllMethods(service, "cancelVibrateInternal", cancelHook);
        XposedBridge.hookAllMethods(service, "cancelVibrateInternalLocked", cancelHook);
        final Class<?> scaler = XposedHelpers.findClass("com.android.server.vibrator.VibrationScaler", p.classLoader);
        XposedBridge.hookAllMethods(scaler, "scale", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args != null && param.args.length >= 2 && param.args[0] instanceof VibrationEffect && param.args[1] instanceof Integer) {
                    int index = HapticConfig.indexForUsage(((Integer) param.args[1]).intValue());
                    if (context != null && HapticConfig.get(context.getContentResolver(), HapticConfig.IDS[index], "enabled", HapticConfig.DEFAULT_ENABLED[index]) != 0) param.setResult(param.args[0]);
                }
            }
        });
        XposedBridge.hookAllMethods(scaler, "getScaleFactor", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (context == null || param.args == null || param.args.length == 0) return;
                int usage = ((Integer) param.args[0]).intValue();
                int index = HapticConfig.indexForUsage(usage); String id = HapticConfig.IDS[index];
                if (HapticConfig.get(context.getContentResolver(), id, "enabled", HapticConfig.DEFAULT_ENABLED[index]) != 0) param.setResult(Float.valueOf(1.0f));
            }
        });
        XposedBridge.hookAllMethods(scaler, "getEffectStrength", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (isCustomUsage(param)) param.setResult(Integer.valueOf(1));
            }
        });
        XposedBridge.hookAllMethods(scaler, "getAdaptiveHapticsScale", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (isCustomUsage(param)) param.setResult(Float.valueOf(1.0f));
            }
        });
        final Class<?> halVibration = XposedHelpers.findClass("com.android.server.vibrator.HalVibration", p.classLoader);
        XposedBridge.hookAllMethods(halVibration, "scaleEffects", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Object callerInfo = XposedHelpers.getObjectField(param.thisObject, "callerInfo");
                    Object attrs = XposedHelpers.getObjectField(callerInfo, "attrs");
                    int usage = ((Integer) XposedHelpers.callMethod(attrs, "getUsage")).intValue();
                    String opPkg = null, reason = null;
                    try { opPkg = (String) XposedHelpers.getObjectField(callerInfo, "opPkg"); } catch (Throwable ignored) {}
                    try { reason = (String) XposedHelpers.getObjectField(callerInfo, "reason"); } catch (Throwable ignored) {}
                    if (SELF_PACKAGE.equals(opPkg)) { param.setResult(null); return; }
                    int index = HapticConfig.indexForRequest(context, usage, opPkg, reason);
                    if (context != null && HapticConfig.get(context.getContentResolver(), HapticConfig.IDS[index], "enabled", HapticConfig.DEFAULT_ENABLED[index]) != 0) param.setResult(null);
                } catch (Throwable t) { XposedBridge.log(t); }
            }
        });
        XposedBridge.log("Y700Haptics: hooked vibration pipeline, context=" + (context != null));
    }

    private static Object transformCombined(final Object combined, final int duration, final int amplitude, final int mode,
                                            final String primitiveIds, final String primitiveScales, final String primitiveDelays,
                                            final String segmentDurations) throws Throwable {
        if (mode == HapticConfig.MODE_CUSTOM_COMPOSITION) {
            return buildMixedSequence(primitiveIds, primitiveScales, primitiveDelays, segmentDurations, amplitude);
        }
        Class<?> transformation = Class.forName("android.os.VibrationEffect$Transformation");
        Object proxy = Proxy.newProxyInstance(transformation.getClassLoader(), new Class<?>[]{transformation}, new InvocationHandler() {
            @Override public Object invoke(Object p, Method method, Object[] args) throws Throwable {
                if (args != null && args.length > 0 && args[0] instanceof VibrationEffect) return transformEffect((VibrationEffect) args[0], duration, amplitude, mode, primitiveIds, primitiveScales, primitiveDelays);
                return args != null && args.length > 0 ? args[0] : null;
            }
        });
        Method transform = combined.getClass().getMethod("transform", transformation, Object.class);
        return transform.invoke(combined, proxy, null);
    }

    private static VibrationEffect transformEffect(VibrationEffect effect, int duration, int amplitude, int mode,
                                                   String primitiveIds, String primitiveScales, String primitiveDelays) {
        try {
            float scale = Math.max(0.01f, Math.min(1.0f, amplitude / 255.0f));
            if (mode >= 3 && mode <= 8) {
                VibrationEffect predefined = VibrationEffect.createPredefined(mode - 3);
                int strength = amplitude < 85 ? 0 : (amplitude < 170 ? 1 : 2);
                return mark((VibrationEffect) XposedHelpers.callMethod(predefined, "applyEffectStrength", Integer.valueOf(strength)));
            }
            if (mode == 9) {
                return mark(VibrationEffect.createWaveform(new long[]{50}, new int[]{0}, -1));
            }
            if (mode >= 10 && mode <= 17) {
                Object composition = XposedHelpers.callStaticMethod(VibrationEffect.class, "startComposition");
                XposedHelpers.callMethod(composition, "addPrimitive", Integer.valueOf(mode - 9), Float.valueOf(scale), Integer.valueOf(0));
                return mark((VibrationEffect) XposedHelpers.callMethod(composition, "compose"));
            }
            if (mode == HapticConfig.MODE_CUSTOM_COMPOSITION) {
                int[] ids = parseInts(primitiveIds);
                float[] scales = parseFloats(primitiveScales);
                int[] delays = parseInts(primitiveDelays);
                if (ids.length == 0 || ids.length != scales.length || ids.length != delays.length || ids.length > 256) throw new IllegalArgumentException("invalid primitive sequence length");
                Object composition = XposedHelpers.callStaticMethod(VibrationEffect.class, "startComposition");
                int pendingDelay = 0;
                boolean added = false;
                for (int i = 0; i < ids.length; i++) {
                    if (ids[i] < 0 || ids[i] > 8 || scales[i] < 0 || scales[i] > 1 || delays[i] < 0 || delays[i] > 1000) throw new IllegalArgumentException("primitive parameter out of range");
                    if (ids[i] == 0) {
                        pendingDelay = Math.min(1000, pendingDelay + delays[i] + 50);
                        continue;
                    }
                    int actualDelay = Math.min(1000, pendingDelay + delays[i]);
                    pendingDelay = 0;
                    XposedHelpers.callMethod(composition, "addPrimitive", Integer.valueOf(ids[i]), Float.valueOf(scales[i] * scale), Integer.valueOf(actualDelay));
                    added = true;
                }
                if (!added) return mark(VibrationEffect.createWaveform(new long[]{Math.max(1, pendingDelay)}, new int[]{0}, -1));
                return mark((VibrationEffect) XposedHelpers.callMethod(composition, "compose"));
            }
            if (mode >= HapticConfig.MODE_TACTILE_FIRST && mode <= HapticConfig.MODE_TACTILE_LAST) {
                return mark(buildTactilePreset(mode, amplitude));
            }
            long original = ((Long) XposedHelpers.callMethod(effect, "getDuration")).longValue();
            if (mode == 1 || (mode == 0 && original > duration && original < Long.MAX_VALUE)) return mark(VibrationEffect.createOneShot(Math.max(1, duration), Math.max(1, Math.min(255, amplitude))));
            return mark((VibrationEffect) XposedHelpers.callMethod(effect, "scale", Float.valueOf(scale)));
        } catch (Throwable t) {
            XposedBridge.log(t);
            return effect;
        }
    }

    private static Object buildMixedSequence(String typeText, String scaleText, String delayText,
                                             String durationText, int masterAmplitude) throws Throwable {
        int[] types = parseInts(typeText);
        float[] scales = parseFloats(scaleText);
        int[] delays = parseInts(delayText);
        int[] durations = parseInts(durationText);
        if (types.length == 0 || types.length != scales.length || types.length != delays.length
                || types.length != durations.length || types.length > 256) {
            throw new IllegalArgumentException("invalid mixed sequence length");
        }
        Class<?> combinedClass = Class.forName("android.os.CombinedVibration");
        Object sequence = XposedHelpers.callStaticMethod(combinedClass, "startSequential");
        float master = Math.max(1, Math.min(255, masterAmplitude)) / 255.0f;
        int pendingDelay = 0;
        boolean added = false;
        for (int i = 0; i < types.length; i++) {
            int type = types[i];
            if (!((type >= 0 && type <= 8) || (type >= 100 && type <= 106))
                    || scales[i] < 0 || scales[i] > 1 || delays[i] < 0 || delays[i] > 1000
                    || durations[i] < 1 || durations[i] > 300) {
                throw new IllegalArgumentException("mixed sequence parameter out of range");
            }
            if (type == 0 || scales[i] == 0) {
                pendingDelay = Math.min(5000, pendingDelay + delays[i] + (type == 0 ? 50 : 0));
                continue;
            }
            float actualScale = Math.max(0.01f, Math.min(1.0f, scales[i] * master));
            VibrationEffect effect;
            if (type == HapticConfig.SEGMENT_LIMIT_DURATION) {
                int amp = Math.max(1, Math.min(255, Math.round(actualScale * 255)));
                effect = VibrationEffect.createOneShot(durations[i], amp);
            } else if (type >= HapticConfig.SEGMENT_EFFECT_CLICK && type <= HapticConfig.SEGMENT_EFFECT_HEAVY_CLICK) {
                int[] effectIds = {0, 1, 2, 3, 4, 5};
                effect = VibrationEffect.createPredefined(effectIds[type - HapticConfig.SEGMENT_EFFECT_CLICK]);
                int strength = actualScale < (85f / 255f) ? 0 : (actualScale < (170f / 255f) ? 1 : 2);
                effect = (VibrationEffect) XposedHelpers.callMethod(effect, "applyEffectStrength", Integer.valueOf(strength));
            } else {
                Object composition = XposedHelpers.callStaticMethod(VibrationEffect.class, "startComposition");
                XposedHelpers.callMethod(composition, "addPrimitive", Integer.valueOf(type), Float.valueOf(actualScale), Integer.valueOf(0));
                effect = (VibrationEffect) XposedHelpers.callMethod(composition, "compose");
            }
            Object parallel = XposedHelpers.callStaticMethod(combinedClass, "createParallel", effect);
            int before = Math.min(5000, pendingDelay + delays[i]);
            pendingDelay = 0;
            XposedHelpers.callMethod(sequence, "addNext", parallel, Integer.valueOf(before));
            added = true;
        }
        if (!added) {
            VibrationEffect silence = VibrationEffect.createWaveform(new long[]{Math.max(1, pendingDelay)}, new int[]{0}, -1);
            return XposedHelpers.callStaticMethod(combinedClass, "createParallel", silence);
        }
        return XposedHelpers.callMethod(sequence, "combine");
    }

    private static VibrationEffect buildTactilePreset(int mode, int amplitude) {
        // On this ColorOS port, amplitude-controlled waveforms stay soft even at 255.
        // The PERFORM_CALLBACK path reaches the AW86937 firmware effects and their braking.
        int effectId;
        if (mode == 19) effectId = 0;       // CLICK
        else if (mode == 20) effectId = 1;  // DOUBLE_CLICK
        else if (mode == 21) effectId = 2;  // TICK
        else if (mode == 22) effectId = 5;  // HEAVY_CLICK
        else effectId = 4;                  // POP
        VibrationEffect effect = VibrationEffect.createPredefined(effectId);
        int strength = amplitude < 85 ? 0 : (amplitude < 170 ? 1 : 2);
        return (VibrationEffect) XposedHelpers.callMethod(effect, "applyEffectStrength", Integer.valueOf(strength));
    }

    private static boolean isDualMode(int mode) {
        return mode >= HapticConfig.MODE_DUAL_FIRST && mode <= HapticConfig.MODE_DUAL_LAST;
    }

    private static void playDualPreset(final int mode, final int masterAmplitude) {
        final int generation = dualGeneration.incrementAndGet();
        callDualCancel(2);
        Thread worker = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    switch (mode) {
                        case 24: // Stock ZUI unlock success: main motor, mapped hardware scene 1.
                            step(generation, 0, 0, 1, 1, 15, 15, 100, 100, masterAmplitude);
                            break;
                        case 25: // Stock ZUI switch.
                            step(generation, 0, 2, 31, 31, 15, 15, 100, 100, masterAmplitude);
                            break;
                        case 26: // Stock ZUI seek-bar tick.
                            step(generation, 0, 2, 31, 31, 11, 11, 70, 70, masterAmplitude);
                            break;
                        case 27: // Stock ZUI password key.
                            step(generation, 0, 2, 41, 41, 30, 30, 100, 100, masterAmplitude);
                            break;
                        case 28: // Stock ZUI alphabet/list tick.
                            step(generation, 0, 2, 29, 29, 10, 10, 50, 50, masterAmplitude);
                            break;
                        case 29: // Crisp: short RAM 2 on both motors.
                            step(generation, 0, 2, 31, 31, 9, 9, 100, 100, masterAmplitude);
                            break;
                        case 30: // Click-clack: RAM 2 attack, RAM 1 tail.
                            step(generation, 0, 2, 31, 31, 9, 9, 100, 100, masterAmplitude);
                            step(generation, 23, 2, 29, 29, 8, 8, 50, 50, masterAmplitude);
                            break;
                        case 31: // Firm confirmation: RAM 3 body, RAM 2 brake.
                            step(generation, 0, 2, 41, 41, 16, 16, 100, 100, masterAmplitude);
                            step(generation, 22, 2, 31, 31, 9, 9, 50, 50, masterAmplitude);
                            break;
                        case 32: // Horizontal relay: overlap left and right by four milliseconds.
                            step(generation, 0, 0, 31, 31, 11, 11, 100, 100, masterAmplitude);
                            step(generation, 7, 1, 31, 31, 11, 11, 100, 100, masterAmplitude);
                            break;
                        case 33: // Light dense tick.
                            step(generation, 0, 2, 29, 29, 8, 8, 50, 50, masterAmplitude);
                            break;
                        case 34: // Fast press and rebound.
                            step(generation, 0, 2, 31, 31, 8, 8, 100, 100, masterAmplitude);
                            step(generation, 18, 2, 31, 31, 7, 7, 50, 50, masterAmplitude);
                            break;
                        case 35: // Main/sub response with different RAM waves.
                            step(generation, 0, 0, 41, 41, 13, 13, 100, 100, masterAmplitude);
                            step(generation, 6, 1, 29, 29, 9, 9, 50, 50, masterAmplitude);
                            break;
                        default:
                            break;
                    }
                } catch (Throwable t) { XposedBridge.log(t); }
            }
        }, "Y700-DualHaptics");
        worker.setDaemon(true);
        worker.start();
    }

    private static void step(int generation, int delayMs, int devicePattern,
                             int leftId, int rightId, int leftDuration, int rightDuration,
                             int leftPercent, int rightPercent, int masterAmplitude) throws Throwable {
        if (delayMs > 0) Thread.sleep(delayMs);
        if (dualGeneration.get() != generation) return;
        int left = packEffect(leftId, scalePercent(leftPercent, masterAmplitude));
        int right = packEffect(rightId, scalePercent(rightPercent, masterAmplitude));
        callDual(devicePattern, left, right, leftDuration, rightDuration);
    }

    private static int scalePercent(int presetPercent, int masterAmplitude) {
        int master = Math.max(1, Math.min(255, masterAmplitude));
        return Math.max(1, Math.min(100, (presetPercent * master + 127) / 255));
    }

    private static int packEffect(int effectId, int percent) {
        return (effectId & 0xfff) | ((percent & 0xfff) << 12);
    }

    private static IBinder getDualBinder() throws Throwable {
        IBinder binder = dualBinder;
        if (binder != null && binder.isBinderAlive()) return binder;
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        binder = (IBinder) XposedHelpers.callStaticMethod(serviceManager, "getService", DUAL_SERVICE);
        dualBinder = binder;
        return binder;
    }

    private static void callDual(int devicePattern, int leftEffect, int rightEffect,
                                 int leftDuration, int rightDuration) throws Throwable {
        IBinder binder = getDualBinder();
        if (binder == null) throw new IllegalStateException("Y700 dual vibrator service unavailable");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DUAL_DESCRIPTOR);
            data.writeInt(4);
            data.writeInt(devicePattern);
            data.writeInt(leftEffect);
            data.writeInt(rightEffect);
            data.writeInt(leftDuration);
            data.writeInt(rightDuration);
            if (!binder.transact(2, data, reply, 0)) throw new IllegalStateException("Y700 onDual transaction rejected");
            reply.readException();
        } catch (Throwable t) {
            dualBinder = null;
            throw t;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static void cancelDualPlayback() {
        dualGeneration.incrementAndGet();
        callDualCancel(2);
    }

    private static void callDualCancel(int devicePattern) {
        Parcel data = null;
        Parcel reply = null;
        try {
            IBinder binder = getDualBinder();
            if (binder == null) return;
            data = Parcel.obtain();
            reply = Parcel.obtain();
            data.writeInterfaceToken(DUAL_DESCRIPTOR);
            data.writeInt(devicePattern);
            if (binder.transact(1, data, reply, 0)) reply.readException();
        } catch (Throwable t) {
            dualBinder = null;
            XposedBridge.log(t);
        } finally {
            if (reply != null) reply.recycle();
            if (data != null) data.recycle();
        }
    }

    private static int[] parseInts(String value) {
        String[] parts = value.trim().split("\\s*,\\s*"); int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Integer.parseInt(parts[i]);
        return result;
    }

    private static float[] parseFloats(String value) {
        String[] parts = value.trim().split("\\s*,\\s*"); float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Float.parseFloat(parts[i]);
        return result;
    }

    private static int indexOfId(String id) {
        for (int i = 0; i < HapticConfig.IDS.length; i++) if (HapticConfig.IDS[i].equals(id)) return i;
        return -1;
    }

    private static VibrationEffect mark(VibrationEffect effect) {
        return effect;
    }

    private static boolean isCustomUsage(XC_MethodHook.MethodHookParam param) {
        if (context == null || param.args == null || param.args.length == 0 || !(param.args[0] instanceof Integer)) return false;
        int index = HapticConfig.indexForUsage(((Integer) param.args[0]).intValue());
        return HapticConfig.get(context.getContentResolver(), HapticConfig.IDS[index], "enabled", HapticConfig.DEFAULT_ENABLED[index]) != 0;
    }
}
