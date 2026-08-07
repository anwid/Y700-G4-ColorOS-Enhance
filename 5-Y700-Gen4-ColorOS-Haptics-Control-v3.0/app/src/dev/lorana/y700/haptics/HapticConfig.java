package dev.lorana.y700.haptics;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

public final class HapticConfig {
    public static final String PREFIX = "y700_haptic_";
    public static final String[] IDS = {"back", "multitask", "lockscreen", "touch", "ime", "hardware", "physical", "notification", "ringtone", "alarm", "media", "communication", "accessibility", "unknown"};
    public static final String[] NAMES = {
        "\u4fa7\u8fb9\u8fd4\u56de\u624b\u52bf\uff08SystemUI\uff09", "\u4e3b\u9875\u3001\u591a\u4efb\u52a1\u4e0e\u684c\u9762\u624b\u52bf\uff08Launcher\uff09",
        "\u9501\u5c4f\u5bc6\u7801\u4e0e\u9501\u5c4f\u754c\u9762", "\u5176\u4ed6\u5e94\u7528\u89e6\u6478\u53cd\u9988", "\u5f53\u524d\u8f93\u5165\u6cd5\u6309\u952e\u53cd\u9988",
        "\u786c\u4ef6\u6309\u952e\u53cd\u9988", "\u7269\u7406\u6a21\u62df\u53cd\u9988",
        "\u901a\u77e5", "\u6765\u7535", "\u95f9\u949f", "\u5a92\u4f53\u89e6\u611f",
        "\u901a\u4fe1\u8bf7\u6c42", "\u65e0\u969c\u788d\u53cd\u9988", "\u672a\u5206\u7c7b\u632f\u52a8"
    };
    // A fresh install always starts in the compatibility layer's ZUXOS-stock
    // path. "enabled" only means that the user wants to override that stock
    // mapping with the selected custom mode; it never disables compatibility.
    public static final int[] DEFAULT_ENABLED = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static final int[] DEFAULT_DURATION = {25, 10, 15, 15, 15, 15, 10, 80, 250, 500, 50, 80, 20, 50};
    public static final int[] DEFAULT_AMPLITUDE = {255, 255, 255, 255, 255, 255, 160, 255, 255, 255, 255, 255, 180, 255};
    public static final int[] DEFAULT_MODE = {30, 18, 20, 11, 15, 12, 11, 0, 0, 0, 2, 0, 0, 0};
    public static final String[] MODE_NAMES = {
        "\u9650\u5236\u6700\u957f\u65f6\u95f4",
        "\u5f3a\u5236\u56fa\u5b9a\u77ed\u8109\u51b2",
        "\u4fdd\u7559\u539f\u6ce2\u5f62\uff0c\u4ec5\u8c03\u5f3a\u5ea6",
        "\u6807\u51c6\u6548\u679c\uff1aCLICK", "\u6807\u51c6\u6548\u679c\uff1aDOUBLE_CLICK",
        "\u6807\u51c6\u6548\u679c\uff1aTICK", "\u6807\u51c6\u6548\u679c\uff1aTHUD",
        "\u6807\u51c6\u6548\u679c\uff1aPOP", "\u6807\u51c6\u6548\u679c\uff1aHEAVY_CLICK",
        "\u5408\u6210\u539f\u8bed\uff1aNOOP", "\u5408\u6210\u539f\u8bed\uff1aCLICK",
        "\u5408\u6210\u539f\u8bed\uff1aTHUD", "\u5408\u6210\u539f\u8bed\uff1aSPIN",
        "\u5408\u6210\u539f\u8bed\uff1aQUICK_RISE", "\u5408\u6210\u539f\u8bed\uff1aSLOW_RISE",
        "\u5408\u6210\u539f\u8bed\uff1aQUICK_FALL", "\u5408\u6210\u539f\u8bed\uff1aTICK",
        "\u5408\u6210\u539f\u8bed\uff1aLOW_TICK", "\u81ea\u5b9a\u4e49\u591a\u6bb5\u539f\u8bed\u5e8f\u5217",
        "ZUXOS \u539f\u5382 HAL\uff1aCLICK", "ZUXOS \u539f\u5382 HAL\uff1aDOUBLE_CLICK",
        "ZUXOS \u539f\u5382 HAL\uff1aTICK", "ZUXOS \u539f\u5382 HAL\uff1aHEAVY_CLICK", "ZUXOS \u539f\u5382 HAL\uff1aPOP",
        "Y700 \u539f\u5382\uff1a\u89e3\u9501\u4e3b\u9a6c\u8fbe", "Y700 \u539f\u5382\uff1a\u5f00\u5173\u53cc\u9a6c\u8fbe",
        "Y700 \u539f\u5382\uff1a\u6ed1\u6761\u523b\u5ea6", "Y700 \u539f\u5382\uff1a\u5bc6\u7801\u6309\u952e", "Y700 \u539f\u5382\uff1a\u5217\u8868\u523b\u5ea6",
        "Y700 \u8c03\u6821\uff1a\u6e05\u8106\u300c\u5494\u300d", "Y700 \u8c03\u6821\uff1a\u300c\u5494\u54d2\u300d\u53cc\u6bb5",
        "Y700 \u8c03\u6821\uff1a\u575a\u5b9e\u786e\u8ba4", "Y700 \u8c03\u6821\uff1a\u5de6\u53f3\u63a5\u529b",
        "Y700 \u8c03\u6821\uff1a\u8f7b\u523b\u5ea6", "Y700 \u8c03\u6821\uff1a\u53cc\u4fa7\u56de\u5f39", "Y700 \u8c03\u6821\uff1a\u4e3b\u6b21\u547c\u5e94",
        "ZUXOS \u539f\u5382 HAL\uff1aTHUD"
    };
    public static final String[] MODE_DESCRIPTIONS = {
        "\u4ec5\u628a\u8fc7\u957f\u632f\u52a8\u622a\u6210\u8bbe\u5b9a\u4e0a\u9650\uff0c\u77ed\u632f\u52a8\u4fdd\u7559\u539f\u8282\u594f\u3002",
        "\u5ffd\u7565\u539f\u8282\u594f\uff0c\u6bcf\u6b21\u90fd\u53d8\u6210\u53ef\u8c03\u65f6\u957f\u7684\u5355\u6b21\u77ed\u8109\u51b2\u3002",
        "\u4fdd\u7559 ROM \u539f\u59cb\u6ce2\u5f62\u548c\u65f6\u957f\uff0c\u53ea\u8c03\u6574\u6574\u4f53\u5f3a\u5ea6\u3002",
        "\u5e72\u8106\u7684\u5355\u6b21\u786e\u8ba4\u611f\uff0c\u9002\u5408\u8fd4\u56de\u3001\u6309\u952e\u548c\u666e\u901a\u70b9\u51fb\u3002",
        "\u4e24\u6b21\u8fde\u7eed\u89e6\u611f\uff0c\u9002\u5408\u91cd\u8981\u786e\u8ba4\u6216\u72b6\u6001\u5207\u6362\u3002",
        "\u8f7b\u800c\u5c16\u7684\u523b\u5ea6\u611f\uff0c\u9002\u5408\u6eda\u52a8\u5230\u4f4d\u6216\u5c0f\u578b\u9009\u62e9\u3002",
        "\u504f\u4f4e\u6c89\u7684\u5355\u6b21\u649e\u51fb\uff0c\u6bd4 CLICK \u66f4\u539a\u91cd\u3002",
        "\u5feb\u901f\u5f39\u51fa\u7684\u9897\u7c92\u611f\uff0c\u9002\u5408\u5f00\u5173\u6216\u5143\u7d20\u51fa\u73b0\u3002",
        "\u6bd4 CLICK \u66f4\u5f3a\u7684\u786e\u8ba4\u611f\uff0c\u9002\u5408\u5b8c\u6210\u6216\u91cd\u8981\u64cd\u4f5c\u3002",
        "\u771f\u6b63\u9759\u97f3\uff1a\u6a21\u5757\u4e0d\u8c03\u7528\u672c\u673a\u4f1a\u5f02\u5e38\u632f\u52a8\u7684 HAL NOOP\uff0c\u6539\u7528\u96f6\u632f\u5e45\u6ce2\u5f62\u3002",
        "\u539f\u8bed CLICK\uff1a\u77ed\u4fc3\u3001\u6e05\u6670\u7684\u70b9\u51fb\u3002",
        "\u539f\u8bed THUD\uff1a\u4f4e\u6c89\u3001\u504f\u91cd\u7684\u649e\u51fb\u3002",
        "\u539f\u8bed SPIN\uff1a\u5e26\u8d77\u4f0f\u53d8\u5316\u7684\u65cb\u8f6c\u8d28\u611f\u3002",
        "\u539f\u8bed QUICK_RISE\uff1a\u5f3a\u5ea6\u5feb\u901f\u4e0a\u5347\u3002",
        "\u539f\u8bed SLOW_RISE\uff1a\u5f3a\u5ea6\u8f83\u6162\u5730\u4e0a\u5347\u3002",
        "\u539f\u8bed QUICK_FALL\uff1a\u5f3a\u5ea6\u5feb\u901f\u8870\u51cf\u3002",
        "\u539f\u8bed TICK\uff1a\u8f7b\u5feb\u3001\u5c16\u9510\u7684\u523b\u5ea6\u611f\u3002",
        "\u539f\u8bed LOW_TICK\uff1a\u9891\u611f\u66f4\u4f4e\u3001\u66f4\u67d4\u548c\u7684\u523b\u5ea6\u611f\u3002",
        "\u6309\u987a\u5e8f\u7f16\u6392\u591a\u4e2a\u539f\u8bed\uff0c\u6bcf\u6bb5\u53ef\u8bbe\u5f3a\u5ea6\u548c\u524d\u7f6e\u5ef6\u8fdf\u3002",
        "\u8c03\u7528 ZUXOS \u539f\u5382 Hapticsconfig.xml \u4e2d\u7684 CLICK \u9884\u5236\u6548\u679c\uff1b\u4fdd\u7559\u539f\u5382\u5236\u52a8\u4e0e\u5f3a\u5ea6\u6863\u4f4d\u3002",
        "\u8c03\u7528 ZUXOS \u539f\u5382 DOUBLE_CLICK \u9884\u5236\u6548\u679c\uff1b\u5b9e\u9645\u5dee\u5f02\u7531\u8054\u60f3\u539f\u5382\u56fa\u4ef6\u51b3\u5b9a\u3002",
        "\u8c03\u7528 ZUXOS \u539f\u5382 TICK \u9884\u5236\u6548\u679c\uff0c\u9002\u5408\u952e\u76d8\u3001\u5217\u8868\u548c\u7ec6\u5bc6\u523b\u5ea6\u3002",
        "\u8c03\u7528 ZUXOS \u539f\u5382 HEAVY_CLICK \u9884\u5236\u6548\u679c\uff0c\u9002\u5408\u91cd\u8981\u786e\u8ba4\u6216\u957f\u6309\u3002",
        "\u8c03\u7528 ZUXOS \u539f\u5382 POP \u9884\u5236\u6548\u679c\uff0c\u9002\u5408\u5f00\u5173\u548c\u5143\u7d20\u5f39\u51fa\u3002",
        "\u590d\u73b0 ZUXOS \u89e3\u9501\u6210\u529f\u7684\u4e3b\u9a6c\u8fbe\u77ed\u8109\u51b2\uff1aRAM 1\u300115 ms\uff1b\u539f\u5382\u5c31\u504f\u8f7b\u3002",
        "\u590d\u73b0 ZUI \u5f00\u5173\u63a7\u4ef6\uff1a\u53cc\u9a6c\u8fbe\u3001\u573a\u666f 31\u300115 ms\u3001\u539f\u5382\u6700\u5927\u589e\u76ca\u3002",
        "\u590d\u73b0 ZUI \u6ed1\u6761\u523b\u5ea6\uff1a\u53cc\u9a6c\u8fbe\u3001\u573a\u666f 31\u300111 ms\uff0c\u8282\u594f\u66f4\u7d27\u3002",
        "\u590d\u73b0 ZUI \u5bc6\u7801\u952e\u76d8\uff1a\u53cc\u9a6c\u8fbe\u3001\u573a\u666f 41\u300130 ms\uff0c\u8f83\u539a\u7684 RAM 3 \u6ce2\u5f62\u3002",
        "\u590d\u73b0 ZUI \u5217\u8868\u523b\u5ea6\uff1a\u53cc\u9a6c\u8fbe\u3001\u573a\u666f 29\u300110 ms\u3001\u534a\u589e\u76ca\u3002",
        "\u9488\u5bf9\u7ea6 167-170 Hz \u7684\u53cc AW86937 \u8c03\u6574\uff1aRAM 2 \u53cc\u4fa7 9 ms\uff0c\u77ed\u4fc3\u5e76\u5feb\u901f\u5236\u52a8\u3002",
        "\u4e00\u6b21\u6e05\u8106 RAM 2 \u540e\u63a5\u534a\u5f3a RAM 1\uff0c\u7528\u4e24\u6bb5\u95f4\u9694\u505a\u51fa\u300c\u5494\u54d2\u300d\u800c\u975e\u62c9\u957f\u5355\u6bb5\u3002",
        "\u5148\u7528 RAM 3 \u7ed9\u51fa\u4e3b\u51b2\u51fb\uff0c\u518d\u4ee5 RAM 2 \u6536\u5c3e\uff0c\u9002\u5408\u591a\u4efb\u52a1\u548c\u91cd\u8981\u786e\u8ba4\u3002",
        "\u5de6\u9a6c\u8fbe\u5148\u8d77\u3001\u53f3\u9a6c\u8fbe\u76f8\u9694 7 ms \u8ddf\u8fdb\uff0c\u5f3a\u8c03 Y700 \u7684\u6a2a\u5411\u53cc\u9a6c\u8fbe\u7279\u6027\u3002",
        "\u4f4e\u589e\u76ca RAM 1 \u7684 8 ms \u53cc\u4fa7\u70b9\u6309\uff0c\u7528\u4e8e\u5bc6\u96c6\u952e\u76d8\u6216\u6ed1\u52a8\u523b\u5ea6\u3002",
        "\u5f3a\u77ed RAM 2 \u540e\u63a5\u534a\u5f3a\u540c\u6ce2\u5f62\uff0c\u505a\u51fa\u5feb\u901f\u538b\u4e0b\u4e0e\u56de\u5f39\u611f\u3002",
        "\u4e3b\u9a6c\u8fbe RAM 3 \u8d77\u624b\uff0c\u53f3\u9a6c\u8fbe RAM 1 \u77ed\u6536\u5c3e\uff0c\u6bd4\u540c\u6b65\u53cc\u632f\u66f4\u6709\u5c42\u6b21\u3002",
        "\u8c03\u7528 ZUXOS \u539f\u5382 Hapticsconfig.xml \u4e2d\u7684 THUD \u9884\u5236\u6548\u679c\uff0c\u4fdd\u7559\u539f\u5382\u5236\u52a8\u3001\u5f3a\u5ea6\u6863\u4f4d\u4e0e\u56de\u9000\u903b\u8f91\u3002"
    };

    public static final int MODE_CUSTOM_COMPOSITION = 18;
    public static final int MODE_TACTILE_FIRST = 19;
    public static final int MODE_TACTILE_LAST = 23;
    public static final int MODE_DUAL_FIRST = 24;
    public static final int MODE_DUAL_LAST = 35;
    public static final int MODE_STOCK_THUD = 36;
    public static final String DEFAULT_PRIMITIVE_IDS = "1,7";
    public static final String DEFAULT_PRIMITIVE_SCALES = "1.0,0.6";
    public static final String DEFAULT_PRIMITIVE_DELAYS = "0,40";
    public static final String DEFAULT_SEGMENT_DURATIONS = "25,25";
    public static final int SEGMENT_LIMIT_DURATION = 100;
    public static final int SEGMENT_EFFECT_CLICK = 101;
    public static final int SEGMENT_EFFECT_DOUBLE_CLICK = 102;
    public static final int SEGMENT_EFFECT_TICK = 103;
    public static final int SEGMENT_EFFECT_THUD = 104;
    public static final int SEGMENT_EFFECT_POP = 105;
    public static final int SEGMENT_EFFECT_HEAVY_CLICK = 106;
    public static final String[] DEFAULT_PRIMITIVE_IDS_BY_CATEGORY = {
        "101,100", "4,100", "1,7", "1,7", "1,7", "1,7", "1,7",
        "1,7", "1,7", "1,7", "1,7", "1,7", "1,7", "1,7"
    };
    public static final String[] DEFAULT_PRIMITIVE_SCALES_BY_CATEGORY = {
        "1.0,1.0", "1.0,0.6", "1.0,0.6", "1.0,0.6", "1.0,0.6", "1.0,0.6", "1.0,0.6",
        "1.0,0.6", "1.0,0.6", "1.0,0.6", "1.0,0.6", "1.0,0.6", "1.0,0.6", "1.0,0.6"
    };
    public static final String[] DEFAULT_PRIMITIVE_DELAYS_BY_CATEGORY = {
        "0,10", "0,7", "0,40", "0,40", "0,40", "0,40", "0,40",
        "0,40", "0,40", "0,40", "0,40", "0,40", "0,40", "0,40"
    };
    public static final String[] DEFAULT_SEGMENT_DURATIONS_BY_CATEGORY = {
        "25,53", "25,20", "25,25", "25,25", "25,25", "25,25", "25,25",
        "25,25", "25,25", "25,25", "25,25", "25,25", "25,25", "25,25"
    };

    private static final int[] MODE_DISPLAY_ORDER = buildModeDisplayOrder();

    private HapticConfig() {}

    public static int indexOf(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return 0;
    }

    public static String defaultPrimitiveIds(int index) { return DEFAULT_PRIMITIVE_IDS_BY_CATEGORY[index]; }
    public static String defaultPrimitiveScales(int index) { return DEFAULT_PRIMITIVE_SCALES_BY_CATEGORY[index]; }
    public static String defaultPrimitiveDelays(int index) { return DEFAULT_PRIMITIVE_DELAYS_BY_CATEGORY[index]; }
    public static String defaultSegmentDurations(int index) { return DEFAULT_SEGMENT_DURATIONS_BY_CATEGORY[index]; }

    private static int[] buildModeDisplayOrder() {
        int[] order = new int[MODE_NAMES.length];
        order[0] = MODE_CUSTOM_COMPOSITION;
        int out = 1;
        // Keep the visual sequence editor first, then make every verified
        // ZUXOS-stock HAL/dual-motor choice easy to find.
        int[] stock = {19, 20, 21, MODE_STOCK_THUD, 22, 23, 24, 25, 26, 27, 28};
        for (int mode : stock) order[out++] = mode;
        for (int mode = 0; mode < MODE_NAMES.length; mode++) {
            if (mode == MODE_CUSTOM_COMPOSITION || contains(stock, mode)) continue;
            order[out++] = mode;
        }
        return order;
    }

    private static boolean contains(int[] values, int target) {
        for (int value : values) if (value == target) return true;
        return false;
    }

    public static String[] displayModeNames() {
        String[] names = new String[MODE_DISPLAY_ORDER.length];
        for (int i = 0; i < names.length; i++) names[i] = MODE_NAMES[MODE_DISPLAY_ORDER[i]];
        return names;
    }

    public static int modeForDisplayPosition(int position) {
        return MODE_DISPLAY_ORDER[Math.max(0, Math.min(MODE_DISPLAY_ORDER.length - 1, position))];
    }

    public static int displayPositionForMode(int mode) {
        for (int i = 0; i < MODE_DISPLAY_ORDER.length; i++) if (MODE_DISPLAY_ORDER[i] == mode) return i;
        return 0;
    }

    public static int get(ContentResolver cr, String id, String suffix, int fallback) {
        try { return Settings.Global.getInt(cr, PREFIX + id + "_" + suffix, fallback); }
        catch (Throwable ignored) { return fallback; }
    }

    public static void put(ContentResolver cr, String id, String suffix, int value) {
        Settings.Global.putInt(cr, PREFIX + id + "_" + suffix, value);
    }

    public static String getString(ContentResolver cr, String id, String suffix, String fallback) {
        try {
            String value = Settings.Global.getString(cr, PREFIX + id + "_" + suffix);
            return value == null || value.length() == 0 ? fallback : value;
        } catch (Throwable ignored) { return fallback; }
    }

    public static void putString(ContentResolver cr, String id, String suffix, String value) {
        Settings.Global.putString(cr, PREFIX + id + "_" + suffix, value);
    }

    /** Returns the original View haptic-feedback constant embedded by system_server. */
    public static int feedbackConstant(String reason) {
        if (reason == null) return -1;
        final String marker = "performHapticFeedback(constant=";
        int start = reason.indexOf(marker);
        if (start < 0) return -1;
        start += marker.length();
        int end = reason.indexOf(')', start);
        if (end <= start) return -1;
        try { return Integer.parseInt(reason.substring(start, end)); }
        catch (Throwable ignored) { return -1; }
    }

    /** ColorOS private feedback constants accepted by COUI but absent from AOSP/ZUXOS. */
    public static boolean isOplusFeedbackConstant(int constant) {
        return constant >= 300 && constant <= 308;
    }

    /**
     * Maps COUI-only semantics to feedback constants implemented by the current
     * ZUXOS/AOSP HapticFeedbackVibrationProvider. The provider then selects the
     * stock predefined effect/composition and the stock vendor HAL calibration.
     */
    public static int stockFeedbackConstantForOplus(int constant) {
        switch (constant) {
            case 300: return 0;  // LONG_VIBRATE -> LONG_PRESS / HEAVY_CLICK
            case 301: return 3;  // KEYBOARD_TOUCH_FEEDBACK -> KEYBOARD_TAP
            case 302: return 27; // GRANULAR_SHORT_VIBRATE -> SEGMENT_FREQUENT_TICK
            case 303: return 1;  // SHORT_VIBRATE -> VIRTUAL_KEY / CLICK
            case 304: return 17; // SHORT_TRIPLE_VIBRATE -> REJECT / DOUBLE_CLICK
            case 305: return 21; // STEPPING_SHORT_VIBRATE -> TOGGLE_ON composition
            case 306: return 12; // EDGE_SHORT_VIBRATE -> GESTURE_START / CLICK
            case 307: return 26; // EDGE_LIST_VIBRATE -> SEGMENT_TICK
            case 308: return 27; // GRANULAR_SHORT_VIBRATE_SYNC -> SEGMENT_FREQUENT_TICK
            default: return constant;
        }
    }

    /** Closest ZUXOS stock predefined effect for each COUI-only semantic. */
    public static int stockEffectIdForOplusFeedback(int constant) {
        switch (constant) {
            case 300: return 5; // LONG_VIBRATE -> HEAVY_CLICK
            case 301: return 0; // KEYBOARD_TOUCH_FEEDBACK -> CLICK
            case 302: return 2; // GRANULAR_SHORT_VIBRATE -> TICK
            case 303: return 0; // SHORT_VIBRATE -> CLICK
            case 304: return 1; // SHORT_TRIPLE_VIBRATE -> DOUBLE_CLICK (closest stock multi-pulse)
            case 305: return 2; // STEPPING_SHORT_VIBRATE -> TICK
            case 306: return 4; // EDGE_SHORT_VIBRATE -> POP
            case 307: return 2; // EDGE_LIST_VIBRATE -> TICK
            case 308: return 2; // GRANULAR_SHORT_VIBRATE_SYNC -> TICK
            default: return -1;
        }
    }

    public static boolean isStockHalMode(int mode) {
        return (mode >= MODE_TACTILE_FIRST && mode <= MODE_TACTILE_LAST) || mode == MODE_STOCK_THUD;
    }

    public static int indexForRequest(Context context, int usage, String opPkg, String reason) {
        String name = usageName(usage);
        if (name.indexOf("IME") >= 0) return 4;
        if (name.indexOf("TOUCH") >= 0) {
            if (isCurrentIme(context, opPkg)) return 4;
            if ("com.android.systemui".equals(opPkg)) return isKeyguardLocked(context) ? 2 : 0;
            if (opPkg != null && ("com.android.launcher".equals(opPkg) || opPkg.indexOf(".launcher") >= 0)) return 1;
            return 3;
        }
        if (name.indexOf("HARDWARE") >= 0) return 5;
        if (name.indexOf("PHYSICAL") >= 0) return 6;
        if (name.indexOf("NOTIFICATION") >= 0) return 7;
        if (name.indexOf("RINGTONE") >= 0) return 8;
        if (name.indexOf("ALARM") >= 0) return 9;
        if (name.indexOf("MEDIA") >= 0) return 10;
        if (name.indexOf("COMMUNICATION") >= 0) return 11;
        if (name.indexOf("ACCESSIBILITY") >= 0) return 12;
        return 13;
    }

    public static int indexForUsage(int usage) { return indexForRequest(null, usage, null, null); }

    private static boolean isCurrentIme(Context context, String opPkg) {
        if (context == null || opPkg == null) return false;
        try {
            String ime = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (ime == null) return false; int slash = ime.indexOf('/');
            return opPkg.equals(slash < 0 ? ime : ime.substring(0, slash));
        } catch (Throwable ignored) { return false; }
    }

    private static boolean isKeyguardLocked(Context context) {
        if (context == null) return false;
        try {
            Object keyguard = context.getSystemService("keyguard");
            return keyguard != null && ((Boolean) keyguard.getClass().getMethod("isKeyguardLocked").invoke(keyguard)).booleanValue();
        } catch (Throwable ignored) { return false; }
    }

    private static String usageName(int usage) {
        try {
            Class<?> c = Class.forName("android.os.VibrationAttributes");
            Object value = c.getMethod("usageToString", Integer.TYPE).invoke(null, Integer.valueOf(usage));
            return String.valueOf(value);
        } catch (Throwable ignored) { return String.valueOf(usage); }
    }
}
