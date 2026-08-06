package dev.lorana.y700.colorospenui.xposed;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class SettingsActivity extends Activity {
    private static final int BLUE = 0xff0b57d0;
    private final ArrayList<String> imeIds = new ArrayList<String>();
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void ensureAccessibilityEnabled() {
        try {
            String component = "dev.lorana.y700.colorospenui.xposed/dev.lorana.y700.colorospenui.xposed.PenAccessibilityService";
            String current = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (current == null || current.length() == 0) current = component;
            else if (!current.contains(component)) current = current + ":" + component;
            Settings.Secure.putString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, current);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (SecurityException ignored) {}
    }

    @Override public void onCreate(Bundle state) {
        int theme = getResources().getIdentifier("Theme.Material.Light.NoActionBar", "style", "android");
        if (theme != 0) setTheme(theme);
        super.onCreate(state);
        setTitle("Y700 手写笔控制");
        final SharedPreferences prefs = PenConfig.prefs(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xfff7f8fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        root.addView(text("Y700 ColorOS 手写笔控制", 25, 0xff202124));
        root.addView(text("Lenovo Tab Pen Plus compatibility bridge", 13, BLUE));
        TextView intro = text("普通打字保留常用输入法；笔进入屏幕悬停范围时提前切到支持 Android 随处写的输入法，移开后自动恢复。", 14, 0xff555555);
        intro.setPadding(0, dp(8), 0, dp(8)); root.addView(intro);

        LinearLayout testCard = card(root);
        testCard.addView(text("自动切换测试", 18, 0xff202124));
        EditText testInput = new EditText(this);
        testInput.setHint("先点这里，再把笔靠近并直接书写");
        testInput.setSingleLine(false);
        testInput.setMinLines(2);
        testCard.addView(testInput, new LinearLayout.LayoutParams(-1, -2));
        testCard.addView(text("键盘状态由下方配置决定；该输入框不保存内容。", 13, 0xff5f6368));

        LinearLayout autoCard = card(root);
        android.widget.Switch enabled = new android.widget.Switch(this);
        enabled.setText("手写时自动切换输入法"); enabled.setTextSize(17);
        enabled.setChecked(PenConfig.auto(this)); autoCard.addView(enabled);
        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean checked) { prefs.edit().putBoolean(PenConfig.AUTO, checked).apply(); }
        });

        final List<InputMethodInfo> methods = ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).getEnabledInputMethodList();
        ArrayList<String> labels = new ArrayList<String>();
        for (InputMethodInfo info : methods) {
            imeIds.add(info.getId());
            boolean stylus = supportsStylus(info);
            labels.add(String.valueOf(info.loadLabel(getPackageManager())) + (stylus ? "（支持随处写）" : "（仅键盘）"));
        }
        final Spinner normal = spinner(autoCard, "常用输入法", labels, indexOf(PenConfig.normalIme(this)));
        final Spinner handwriting = spinner(autoCard, "手写回退输入法", labels, indexOf(PenConfig.handwritingIme(this)));

        final TextView delayLabel = text("停止书写后恢复：" + (PenConfig.restoreMs(this) / 1000L) + " 秒", 14, 0xff333333);
        final SeekBar delay = new SeekBar(this); delay.setMax(145); delay.setProgress((int) Math.max(0, Math.min(145, PenConfig.restoreMs(this) / 100L - 5)));
        autoCard.addView(delayLabel); autoCard.addView(delay, new LinearLayout.LayoutParams(-1, -2));
        delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                long ms = (value + 5L) * 100L; prefs.edit().putLong(PenConfig.RESTORE_MS, ms).apply();
                delayLabel.setText("停止书写后恢复：" + String.format(java.util.Locale.ROOT, "%.1f", ms / 1000.0) + " 秒");
            }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        });

        Button save = new Button(this); save.setText("保存输入法选择"); stylePrimary(save); autoCard.addView(save, buttonParams());
        save.setOnClickListener(v -> {
            if (imeIds.isEmpty()) return;
            String n = imeIds.get(normal.getSelectedItemPosition()); String h = imeIds.get(handwriting.getSelectedItemPosition());
            if (n.equals(h)) { Toast.makeText(this, "常用与手写输入法相同，无需自动回退", Toast.LENGTH_LONG).show(); }
            prefs.edit().putString(PenConfig.NORMAL_IME, n).putString(PenConfig.HANDWRITING_IME, h).apply();
            putGlobalString("y700_pen_normal_ime", n);
            putGlobalString("y700_pen_handwriting_ime", h);
            if (getGlobalInt("y700_pen_near", 0) == 0) {
                Settings.Secure.putString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, n);
            }
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        });

        Button accessibility = new Button(this); accessibility.setText("打开无障碍设置（只需启用一次）"); styleSecondary(accessibility); autoCard.addView(accessibility, buttonParams());
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        boolean accessibilityGranted = isAccessibilityEnabled();
        autoCard.addView(text("无障碍授权状态：" + (accessibilityGranted ? "已启用（配套模块会自动维持服务）" : "未启用（安装并启用配套 KSU 模块后会自动恢复）") + "。本服务不读取文字内容，只检查当前焦点是否为可编辑控件并监听手写笔事件。", 13, accessibilityGranted ? 0xff137333 : 0xffb3261e));

        LinearLayout systemCard = card(root);
        systemCard.addView(text("ColorOS 系统笔功能", 18, 0xff202124));
        addSecureSwitch(systemCard, "随处写总开关", "stylus_handwriting_enabled", true);
        addSecureSwitch(systemCard, "允许应用显示笔悬停指针（ColorOS 空白处默认无小圆点）", "stylus_pointer_icon_enabled", true);
        addGlobalSwitch(systemCard, "补全全局悬停小圆点（需 LSPosed 系统框架作用域和重启）", "y700_pen_custom_hover_cursor", true);
        addGlobalSwitch(systemCard, "右上角下扫随手写", "ipe_pencil_swipe_function", true);
        Button official = new Button(this); official.setText("打开 ColorOS 官方手写笔设置"); styleSecondary(official); systemCard.addView(official, buttonParams());
        official.setOnClickListener(v -> {
            try {
                Intent i = new Intent(); i.setComponent(new ComponentName("com.oplus.ipemanager", "com.oplus.ipemanager.btadsorb.setting.activity.PencilSettingActivity")); startActivity(i);
            } catch (Throwable t) { Toast.makeText(this, "无法打开官方设置页", Toast.LENGTH_LONG).show(); }
        });

        LinearLayout statusCard = card(root);
        statusCard.addView(text("底层状态与调参边界", 18, 0xff202124));
        statusCard.addView(text("压感：NVT 驱动已报告 0–4095，Android 已归一化为 0–1；不在这里伪造压力曲线。\n低延迟：需要 Lenovo touchscreen HAL 在下次重启后启动并执行 Pen Mode；高报点率保持只读，以免覆盖你的触摸采样率超频。\nColorOS 笔记画笔：当前是 libSuniaEngine.so 的 EGL 原生崩溃，和输入法回退无关。", 14, 0xff555555));

        setContentView(scroll);
    }

    private boolean supportsStylus(InputMethodInfo info) {
        try { Method m = InputMethodInfo.class.getMethod("supportsStylusHandwriting"); return ((Boolean) m.invoke(info)).booleanValue(); }
        catch (Throwable ignored) { return PenConfig.DEFAULT_HANDWRITING.equals(info.getId()); }
    }
    private int indexOf(String id) { int i = imeIds.indexOf(id); return i < 0 ? 0 : i; }
    private Spinner spinner(LinearLayout card, String title, ArrayList<String> labels, int selection) {
        card.addView(text(title, 14, 0xff333333)); Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        if (!labels.isEmpty()) s.setSelection(Math.max(0, Math.min(labels.size() - 1, selection))); card.addView(s); return s;
    }
    private void addSecureSwitch(LinearLayout card, String title, final String key, boolean def) {
        android.widget.Switch s = new android.widget.Switch(this); s.setText(title); s.setTextSize(16);
        s.setChecked(Settings.Secure.getInt(getContentResolver(), key, def ? 1 : 0) != 0); card.addView(s);
        s.setOnCheckedChangeListener((b, value) -> Settings.Secure.putInt(getContentResolver(), key, value ? 1 : 0));
    }
    private void addGlobalSwitch(LinearLayout card, String title, final String key, boolean def) {
        android.widget.Switch s = new android.widget.Switch(this); s.setText(title); s.setTextSize(16);
        s.setChecked(getGlobalInt(key, def ? 1 : 0) != 0); card.addView(s);
        s.setOnCheckedChangeListener((b, value) -> putGlobalInt(key, value ? 1 : 0));
    }
    private int getGlobalInt(String key, int def) {
        try { Class<?> c = Class.forName("android.provider.Settings$Global"); Method m = c.getMethod("getInt", android.content.ContentResolver.class, String.class, Integer.TYPE); return ((Integer) m.invoke(null, getContentResolver(), key, Integer.valueOf(def))).intValue(); }
        catch (Throwable ignored) { return def; }
    }
    private void putGlobalInt(String key, int value) {
        try { Class<?> c = Class.forName("android.provider.Settings$Global"); Method m = c.getMethod("putInt", android.content.ContentResolver.class, String.class, Integer.TYPE); m.invoke(null, getContentResolver(), key, Integer.valueOf(value)); }
        catch (Throwable t) { Toast.makeText(this, "写入系统设置失败", Toast.LENGTH_LONG).show(); }
    }
    private void putGlobalString(String key, String value) {
        try { Class<?> c = Class.forName("android.provider.Settings$Global"); Method m = c.getMethod("putString", android.content.ContentResolver.class, String.class, String.class); m.invoke(null, getContentResolver(), key, value); }
        catch (Throwable t) { Toast.makeText(this, "写入输入法切换配置失败", Toast.LENGTH_LONG).show(); }
    }
    private boolean isAccessibilityEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null || enabled.length() == 0) return false;
        ComponentName expected = new ComponentName(this, PenAccessibilityService.class);
        String[] components = enabled.split(":");
        for (String flattened : components) {
            ComponentName actual = ComponentName.unflattenFromString(flattened);
            if (expected.equals(actual)) return true;
        }
        return false;
    }
    private TextView text(String value, int sp, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.START); v.setPadding(0, dp(4), 0, dp(4)); return v; }
    private LinearLayout card(LinearLayout root) { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(14), dp(12), dp(14), dp(12)); GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1), 0xffe0e3e7); c.setBackground(bg); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(9), 0, 0); root.addView(c, p); return c; }
    private LinearLayout.LayoutParams buttonParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(8), 0, 0); return p; }
    private void stylePrimary(Button b) { b.setTextColor(Color.WHITE); GradientDrawable bg = new GradientDrawable(); bg.setColor(BLUE); bg.setCornerRadius(dp(20)); b.setBackground(bg); }
    private void styleSecondary(Button b) { GradientDrawable bg = new GradientDrawable(); bg.setColor(0xffedf3ff); bg.setCornerRadius(dp(20)); bg.setStroke(dp(1), 0xff9bb7e8); b.setTextColor(0xff174ea6); b.setBackground(bg); }
}
