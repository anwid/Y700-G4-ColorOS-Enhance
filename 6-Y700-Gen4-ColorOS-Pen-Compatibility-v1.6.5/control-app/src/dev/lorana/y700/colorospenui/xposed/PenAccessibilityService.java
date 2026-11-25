package dev.lorana.y700.colorospenui.xposed;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.AccessibilityService;

public final class PenAccessibilityService extends AccessibilityService {
    private static final String TAG = "Y700PenAccess";
    private static final String PEN_NEAR = "y700_pen_near";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean penNear;
    private ContentObserver penObserver;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        if (penObserver == null) {
            penObserver = new ContentObserver(handler) {
                @Override public void onChange(boolean selfChange) { updatePenNear(); }
            };
            getContentResolver().registerContentObserver(Settings.Global.getUriFor(PEN_NEAR), false, penObserver);
        }
        updatePenNear();
    }

    private void updatePenNear() {
        boolean near = Settings.Global.getInt(getContentResolver(), PEN_NEAR, 0) != 0;
        if (near == penNear) return;
        penNear = near;
        if (!near || !PenConfig.auto(this)) return;

        String handwriting = PenConfig.handwritingIme(this);
        if (handwriting == null) return;
        String current = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (!handwriting.equals(current)
                && Settings.Secure.putString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, handwriting)) {
            Log.i(TAG, "Switched to handwriting IME on pen approach");
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}

    @Override public boolean onUnbind(android.content.Intent intent) {
        if (penObserver != null) {
            getContentResolver().unregisterContentObserver(penObserver);
            penObserver = null;
        }
        penNear = false;
        return super.onUnbind(intent);
    }
}
