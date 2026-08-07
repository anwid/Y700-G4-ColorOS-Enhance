package android.provider;

import android.content.ContentResolver;

public final class Settings {
    public static final class Global {
        public static int getInt(ContentResolver resolver, String name, int fallback) { return fallback; }
        public static boolean putInt(ContentResolver resolver, String name, int value) { return false; }
        public static String getString(ContentResolver resolver, String name) { return null; }
        public static boolean putString(ContentResolver resolver, String name, String value) { return false; }
    }
    public static final class Secure {
        public static final String DEFAULT_INPUT_METHOD = "default_input_method";
        public static String getString(ContentResolver resolver, String name) { return null; }
    }
}
