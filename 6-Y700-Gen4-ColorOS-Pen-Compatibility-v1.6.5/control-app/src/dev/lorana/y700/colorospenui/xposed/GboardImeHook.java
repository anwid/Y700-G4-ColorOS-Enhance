package dev.lorana.y700.colorospenui.xposed;

import android.inputmethodservice.InputMethodService;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class GboardImeHook {
    static final String PACKAGE = "com.google.android.inputmethod.latin";
    private static final String TAG = "Y700Gboard";

    private GboardImeHook() {}

    static void install(ClassLoader loader) {
        try {
            Class<?> ime = XposedHelpers.findClass("com.android.inputmethod.latin.LatinIME", loader);
            Class<?> showRequest = owner(ime, "onShowInputRequested", int.class, boolean.class);
            Class<?> showWindow = owner(ime, "showWindow", boolean.class);
            XposedBridge.hookAllMethods(showRequest, "onShowInputRequested", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (ime.isInstance(param.thisObject)
                            && handoff((InputMethodService) param.thisObject, "show request")) {
                        param.setResult(false);
                    }
                }
            });
            XposedBridge.hookAllMethods(showWindow, "showWindow", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (ime.isInstance(param.thisObject) && param.args.length == 1
                            && Boolean.TRUE.equals(param.args[0])
                            && handoff((InputMethodService) param.thisObject, "show window")) {
                        param.setResult(null);
                    }
                }
            });
            Log.i(TAG, "Gboard virtual keyboard handoff hooks installed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            Log.w(TAG, "Gboard keyboard handoff unavailable", error);
        }
    }

    private static boolean handoff(InputMethodService service, String trigger) {
        try {
            if (!PenConfig.sharedAuto(service)) return false;
            EditorInfo editor = service.getCurrentInputEditorInfo();
            int tool = editor == null ? MotionEvent.TOOL_TYPE_UNKNOWN : editor.getInitialToolType();
            boolean penNear = Settings.Global.getInt(service.getContentResolver(), "y700_pen_near", 0) != 0;
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER
                    || (tool == MotionEvent.TOOL_TYPE_UNKNOWN && penNear)) {
                Log.d(TAG, "Kept Gboard for pen-triggered " + trigger + " (tool=" + tool + ")");
                return false;
            }
            String normal = PenConfig.normalIme(service);
            if (normal == null || normal.startsWith(PACKAGE + "/")) return false;
            String selected = Settings.Secure.getString(service.getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD);
            if (normal.equals(selected)) return true;
            if (selected == null || !selected.startsWith(PACKAGE + "/")) return false;
            service.switchInputMethod(normal);
            Log.i(TAG, "Requested normal IME before Gboard " + trigger);
            return true;
        } catch (RuntimeException error) {
            Log.w(TAG, "Keyboard handoff failed", error);
            return false;
        }
    }

    private static Class<?> owner(Class<?> root, String name, Class<?>... arguments)
            throws NoSuchMethodException {
        for (Class<?> type = root; type != null; type = type.getSuperclass()) {
            try {
                type.getDeclaredMethod(name, arguments);
                return type;
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(name);
    }
}
