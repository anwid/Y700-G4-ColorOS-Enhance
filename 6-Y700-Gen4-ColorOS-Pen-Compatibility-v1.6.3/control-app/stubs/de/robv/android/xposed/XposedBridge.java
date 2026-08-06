package de.robv.android.xposed;
public final class XposedBridge {
    public static java.util.Set<?> hookAllMethods(Class<?> clazz, String name, XC_MethodHook callback) { return null; }
    public static java.util.Set<?> hookAllConstructors(Class<?> clazz, XC_MethodHook callback) { return null; }
    public static void log(String text) {}
    public static void log(Throwable throwable) {}
}
