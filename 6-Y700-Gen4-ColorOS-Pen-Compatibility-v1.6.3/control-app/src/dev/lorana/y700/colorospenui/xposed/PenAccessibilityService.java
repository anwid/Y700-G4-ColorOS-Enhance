package dev.lorana.y700.colorospenui.xposed;

import android.accessibilityservice.AccessibilityService;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.reflect.Method;

public final class PenAccessibilityService extends AccessibilityService {
    private static final String TAG = "Y700PenAccess";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean editableFocused;
    private boolean switchedByUs;
    private boolean penNear;
    private ContentObserver penObserver;
    private final Runnable restore = new Runnable() {
        @Override public void run() { restoreNormalIme(); }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        editableFocused = findEditableFocus();
        penNear = getGlobalInt("y700_pen_near", 0) != 0;
        penObserver = new ContentObserver(handler) {
            @Override public void onChange(boolean selfChange) { updatePenNear(); }
            @Override public void onChange(boolean selfChange, Uri uri) { updatePenNear(); }
        };
        try {
            Class<?> global = Class.forName("android.provider.Settings$Global");
            Method getUri = global.getMethod("getUriFor", String.class);
            Uri uri = (Uri) getUri.invoke(null, "y700_pen_near");
            getContentResolver().registerContentObserver(uri, false, penObserver);
            Log.i(TAG, "watching KSU pen proximity state; near=" + penNear);
        } catch (Throwable error) { Log.e(TAG, "cannot watch KSU pen proximity state", error); }
        if (penNear && PenConfig.auto(this)) switchToHandwritingIme();
        else if (!penNear) restoreNormalIme();
    }

    private int getGlobalInt(String key, int def) {
        try {
            Class<?> global = Class.forName("android.provider.Settings$Global");
            Method get = global.getMethod("getInt", android.content.ContentResolver.class, String.class, Integer.TYPE);
            return ((Integer) get.invoke(null, getContentResolver(), key, Integer.valueOf(def))).intValue();
        } catch (Throwable ignored) { return def; }
    }

    private void updatePenNear() {
        penNear = getGlobalInt("y700_pen_near", 0) != 0;
        Log.d(TAG, "KSU pen proximity near=" + penNear + " editable=" + editableFocused);
        if (penNear && PenConfig.auto(this)) switchToHandwritingIme();
        else if (!penNear) scheduleRestore();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_VIEW_FOCUSED || type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            AccessibilityNodeInfo source = event.getSource();
            editableFocused = source != null && isEditable(source);
            if (editableFocused && penNear) switchToHandwritingIme();
            if (!editableFocused && switchedByUs) scheduleRestore();
        } else if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            editableFocused = findEditableFocus();
            if (editableFocused && penNear) switchToHandwritingIme();
            if (!editableFocused && switchedByUs) scheduleRestore();
        }
    }

    @Override public void onInterrupt() { restoreNormalIme(); }
    @Override public boolean onUnbind(android.content.Intent intent) {
        if (penObserver != null) getContentResolver().unregisterContentObserver(penObserver);
        restoreNormalIme();
        return super.onUnbind(intent);
    }

    private boolean findEditableFocus() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        boolean result = focus != null && isEditable(focus) && focus.isFocused();
        editableFocused = result;
        return result;
    }

    private boolean isEditable(AccessibilityNodeInfo node) {
        try {
            Method method = AccessibilityNodeInfo.class.getMethod("isEditable");
            return ((Boolean) method.invoke(node)).booleanValue();
        } catch (Throwable ignored) { return false; }
    }

    private void switchToHandwritingIme() {
        String fallback = PenConfig.handwritingIme(this);
        if (fallback == null || fallback.length() == 0) return;
        String current = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (fallback.equals(current)) {
            return;
        }
        String normal = PenConfig.normalIme(this);
        if (normal == null || normal.length() == 0 || fallback.equals(normal)) {
            PenConfig.prefs(this).edit().putString(PenConfig.NORMAL_IME, current).apply();
        }
        if (Settings.Secure.putString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, fallback)) {
            switchedByUs = true;
            Log.i(TAG, "switched to handwriting IME: " + fallback);
        }
    }

    private void scheduleRestore() {
        handler.removeCallbacks(restore);
        handler.postDelayed(restore, Math.max(500L, PenConfig.restoreMs(this)));
    }

    private void restoreNormalIme() {
        handler.removeCallbacks(restore);
        String normal = PenConfig.normalIme(this);
        if (normal != null && normal.length() > 0) {
            Settings.Secure.putString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, normal);
            Log.i(TAG, "restored normal IME: " + normal);
        }
        switchedByUs = false;
    }
}
