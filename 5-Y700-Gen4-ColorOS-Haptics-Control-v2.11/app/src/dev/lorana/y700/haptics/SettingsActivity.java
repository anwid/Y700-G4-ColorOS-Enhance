package dev.lorana.y700.haptics;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.json.JSONObject;

public final class SettingsActivity extends Activity {
    private static final int REQUEST_EXPORT = 7001;
    private static final int REQUEST_IMPORT = 7002;
    private static final int BLUE = 0xff0b57d0;
    private static final int PAGE = 0xfff7f8fa;
    private static final String[] SEGMENT_NAMES = {
        "\u9650\u5236\u6700\u957f\u65f6\u95f4\uff08\u53ef\u8c03\u65f6\u957f\u8109\u51b2\uff09",
        "\u6807\u51c6\u6548\u679c\uff1aCLICK", "\u6807\u51c6\u6548\u679c\uff1aDOUBLE_CLICK", "\u6807\u51c6\u6548\u679c\uff1aTICK",
        "\u6807\u51c6\u6548\u679c\uff1aTHUD", "\u6807\u51c6\u6548\u679c\uff1aPOP", "\u6807\u51c6\u6548\u679c\uff1aHEAVY_CLICK",
        "\u5408\u6210\u539f\u8bed\uff1aCLICK", "\u5408\u6210\u539f\u8bed\uff1aTHUD", "\u5408\u6210\u539f\u8bed\uff1aSPIN",
        "\u5408\u6210\u539f\u8bed\uff1aQUICK_RISE", "\u5408\u6210\u539f\u8bed\uff1aSLOW_RISE", "\u5408\u6210\u539f\u8bed\uff1aQUICK_FALL",
        "\u5408\u6210\u539f\u8bed\uff1aTICK", "\u5408\u6210\u539f\u8bed\uff1aLOW_TICK", "\u7eaf\u505c\u987f"
    };
    private static final int[] SEGMENT_TYPES = {100, 101, 102, 103, 104, 105, 106, 1, 2, 3, 4, 5, 6, 7, 8, 0};
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private Vibrator vibrator() { return (Vibrator) getSystemService(VIBRATOR_SERVICE); }

    @Override public void onCreate(Bundle state) {
        int theme = getResources().getIdentifier("Theme.Material.Light.NoActionBar", "style", "android");
        if (theme != 0) setTheme(theme);
        super.onCreate(state);
        migrateSettings();
        setTitle("Y700 \u89e6\u611f\u63a7\u5236");
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAGE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("Y700 ColorOS \u89e6\u611f\u63a7\u5236", 26, 0xff202124);
        title.setPadding(0, dp(6), 0, 0); root.addView(title);
        TextView subtitle = text("AOSP Haptics Controller", 13, BLUE); root.addView(subtitle);
        TextView hint = text("\u6309 Android \u7528\u9014\u548c\u5b9e\u9645\u8c03\u7528\u573a\u666f\u5206\u4e3a 14 \u7c7b\u72ec\u7acb\u63a7\u5236\u3002\u5173\u95ed\u67d0\u9879\u8868\u793a\u5b8c\u5168\u6cbf\u7528 ROM\uff1b\u5f00\u542f\u540e\u53ef\u8bbe\u7f6e\u5904\u7406\u6a21\u5f0f\u3001\u65f6\u957f\u548c\u6700\u7ec8\u76ee\u6807\u5f3a\u5ea6\u3002", 14, 0xff555555);
        hint.setPadding(0, dp(8), 0, dp(12));
        root.addView(hint);

        for (int i = 0; i < HapticConfig.IDS.length; i++) addCategory(root, i);
        addWaveformLab(root);
        addConfigTransfer(root);

        Button stop = new Button(this);
        stop.setText("\u505c\u6b62\u6240\u6709\u6d4b\u8bd5\u632f\u52a8");
        stop.setOnClickListener(v -> { Vibrator vib = vibrator(); if (vib != null) vib.cancel(); });
        styleSecondary(stop);
        root.addView(stop, fullButtonParams());

        Button defaults = new Button(this);
        defaults.setText("\u6062\u590d\u9ed8\u8ba4\u8bbe\u7f6e");
        defaults.setOnClickListener(v -> {
            ContentResolver cr = getContentResolver();
            for (int i = 0; i < HapticConfig.IDS.length; i++) {
                HapticConfig.put(cr, HapticConfig.IDS[i], "enabled", HapticConfig.DEFAULT_ENABLED[i]);
                HapticConfig.put(cr, HapticConfig.IDS[i], "duration", HapticConfig.DEFAULT_DURATION[i]);
                HapticConfig.put(cr, HapticConfig.IDS[i], "amplitude", HapticConfig.DEFAULT_AMPLITUDE[i]);
                HapticConfig.put(cr, HapticConfig.IDS[i], "mode", HapticConfig.DEFAULT_MODE[i]);
                HapticConfig.putString(cr, HapticConfig.IDS[i], "primitive_ids", HapticConfig.defaultPrimitiveIds(i));
                HapticConfig.putString(cr, HapticConfig.IDS[i], "primitive_scales", HapticConfig.defaultPrimitiveScales(i));
                HapticConfig.putString(cr, HapticConfig.IDS[i], "primitive_delays", HapticConfig.defaultPrimitiveDelays(i));
                HapticConfig.putString(cr, HapticConfig.IDS[i], "segment_durations", HapticConfig.defaultSegmentDurations(i));
            }
            recreate();
        });
        styleSecondary(defaults);
        root.addView(defaults, fullButtonParams());

        TextView footer = text("\u8bbe\u7f6e\u5b9e\u65f6\u4fdd\u5b58\u3002Vector \u7684 system_server \u5185\u90e8\u4f5c\u7528\u57df\u4e3a system/0\uff1bLSPosed \u4e2d\u9009\u201c\u7cfb\u7edf\u6846\u67b6\u201d\u3002Y700 \u539f\u5382/\u8c03\u6821\u9884\u8bbe\u4f1a\u76f4\u63a5\u4f7f\u7528 ROM \u4fdd\u7559\u7684 IVibratorDual \u670d\u52a1\uff0c\u53ef\u5206\u522b\u9a71\u52a8\u5de6\u53f3 AW86937\uff1b\u4e0d\u4fee\u6539\u9a71\u52a8\u7535\u538b\u4e0e\u539f\u5382\u589e\u76ca\u4e0a\u9650\u3002", 13, 0xff777777);
        footer.setPadding(0, dp(16), 0, 0);
        root.addView(footer);
        setContentView(scroll);
    }

    private void migrateSettings() {
        ContentResolver cr = getContentResolver();
        int schema = Settings.Global.getInt(cr, HapticConfig.PREFIX + "schema", 0);
        if (schema == 0) {
            for (int i = 0; i < HapticConfig.IDS.length; i++) {
                String id = HapticConfig.IDS[i];
                HapticConfig.put(cr, id, "enabled", HapticConfig.DEFAULT_ENABLED[i]);
                HapticConfig.put(cr, id, "duration", HapticConfig.DEFAULT_DURATION[i]);
                HapticConfig.put(cr, id, "amplitude", HapticConfig.DEFAULT_AMPLITUDE[i]);
                HapticConfig.put(cr, id, "mode", HapticConfig.DEFAULT_MODE[i]);
                HapticConfig.putString(cr, id, "primitive_ids", HapticConfig.defaultPrimitiveIds(i));
                HapticConfig.putString(cr, id, "primitive_scales", HapticConfig.defaultPrimitiveScales(i));
                HapticConfig.putString(cr, id, "primitive_delays", HapticConfig.defaultPrimitiveDelays(i));
                HapticConfig.putString(cr, id, "segment_durations", HapticConfig.defaultSegmentDurations(i));
            }
            Settings.Global.putInt(cr, HapticConfig.PREFIX + "schema", 3);
            return;
        }
        if (schema < 2) {
            copyCategory(cr, "touch", "gesture", 3);
            copyCategory(cr, "touch", "lockscreen", 3);
            Settings.Global.putInt(cr, HapticConfig.PREFIX + "schema", 2);
            schema = 2;
        }
        if (schema < 3) {
            copyCategory(cr, "gesture", "back", 0);
            copyCategory(cr, "gesture", "multitask", 1);
            Settings.Global.putInt(cr, HapticConfig.PREFIX + "schema", 3);
        }
    }

    private void copyCategory(ContentResolver cr, String source, String target, int sourceIndex) {
        HapticConfig.put(cr, target, "enabled", HapticConfig.get(cr, source, "enabled", HapticConfig.DEFAULT_ENABLED[sourceIndex]));
        HapticConfig.put(cr, target, "duration", HapticConfig.get(cr, source, "duration", HapticConfig.DEFAULT_DURATION[sourceIndex]));
        HapticConfig.put(cr, target, "amplitude", HapticConfig.get(cr, source, "amplitude", HapticConfig.DEFAULT_AMPLITUDE[sourceIndex]));
        HapticConfig.put(cr, target, "mode", HapticConfig.get(cr, source, "mode", HapticConfig.DEFAULT_MODE[sourceIndex]));
        HapticConfig.putString(cr, target, "primitive_ids", HapticConfig.getString(cr, source, "primitive_ids", HapticConfig.defaultPrimitiveIds(sourceIndex)));
        HapticConfig.putString(cr, target, "primitive_scales", HapticConfig.getString(cr, source, "primitive_scales", HapticConfig.defaultPrimitiveScales(sourceIndex)));
        HapticConfig.putString(cr, target, "primitive_delays", HapticConfig.getString(cr, source, "primitive_delays", HapticConfig.defaultPrimitiveDelays(sourceIndex)));
        HapticConfig.putString(cr, target, "segment_durations", HapticConfig.getString(cr, source, "segment_durations", HapticConfig.defaultSegmentDurations(sourceIndex)));
    }

    private LinearLayout.LayoutParams fullButtonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(12), 0, 0); return p;
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.START);
        return v;
    }

    private LinearLayout card(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(8), 0, 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE); background.setCornerRadius(dp(16));
        background.setStroke(dp(1), 0xffe0e3e7); card.setBackground(background);
        root.addView(card, cp); return card;
    }

    private void addCategory(LinearLayout root, final int index) {
        final String id = HapticConfig.IDS[index];
        final ContentResolver cr = getContentResolver();
        LinearLayout card = card(root);

        Switch enabled = new Switch(this);
        enabled.setText(HapticConfig.NAMES[index] + "\uff1a\u542f\u7528\u81ea\u5b9a\u4e49");
        enabled.setTextSize(17);
        enabled.setChecked(HapticConfig.get(cr, id, "enabled", HapticConfig.DEFAULT_ENABLED[index]) != 0);
        card.addView(enabled);

        final Spinner mode = new Spinner(this);
        mode.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, HapticConfig.displayModeNames()));
        mode.setSelection(HapticConfig.displayPositionForMode(HapticConfig.get(cr, id, "mode", HapticConfig.DEFAULT_MODE[index])));
        card.addView(text("\u5904\u7406\u6a21\u5f0f\uff1a", 14, 0xff333333)); card.addView(mode, new LinearLayout.LayoutParams(-1, -2));
        final TextView modeDescription = text("", 13, 0xff5f6368);
        modeDescription.setPadding(dp(4), 0, dp(4), dp(8)); card.addView(modeDescription);

        final TextView durationLabel = text("", 14, 0xff333333);
        final SeekBar duration = new SeekBar(this);
        duration.setMax(("ringtone".equals(id) || "alarm".equals(id)) ? 1000 : 300);
        duration.setProgress(HapticConfig.get(cr, id, "duration", HapticConfig.DEFAULT_DURATION[index]));
        card.addView(durationLabel); card.addView(duration, new LinearLayout.LayoutParams(-1, -2));

        final TextView amplitudeLabel = text("", 14, 0xff333333);
        final SeekBar amplitude = new SeekBar(this);
        amplitude.setMax(255);
        amplitude.setProgress(HapticConfig.get(cr, id, "amplitude", HapticConfig.DEFAULT_AMPLITUDE[index]));
        card.addView(amplitudeLabel); card.addView(amplitude, new LinearLayout.LayoutParams(-1, -2));
        updateModeUi(HapticConfig.modeForDisplayPosition(mode.getSelectedItemPosition()), modeDescription, durationLabel, amplitudeLabel, duration, amplitude);

        final LinearLayout compositionEditor = addCompositionEditor(card, id, cr);
        compositionEditor.setVisibility(HapticConfig.modeForDisplayPosition(mode.getSelectedItemPosition()) == HapticConfig.MODE_CUSTOM_COMPOSITION ? View.VISIBLE : View.GONE);

        Button test = new Button(this);
        test.setText("\u6d4b\u8bd5\u5f53\u524d\u6548\u679c");
        test.setOnClickListener(v -> vibratePreview(id, HapticConfig.modeForDisplayPosition(mode.getSelectedItemPosition()), duration.getProgress(), amplitude.getProgress()));
        styleSecondary(test);
        card.addView(test, new LinearLayout.LayoutParams(-1, -2));

        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton button, boolean checked) { HapticConfig.put(cr, id, "enabled", checked ? 1 : 0); }
        });
        mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long rowId) {
                int selectedMode = HapticConfig.modeForDisplayPosition(position);
                HapticConfig.put(cr, id, "mode", selectedMode);
                compositionEditor.setVisibility(selectedMode == HapticConfig.MODE_CUSTOM_COMPOSITION ? View.VISIBLE : View.GONE);
                updateModeUi(selectedMode, modeDescription, durationLabel, amplitudeLabel, duration, amplitude);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                int safe = Math.max(1, value); if (safe != value) bar.setProgress(safe);
                HapticConfig.put(cr, id, "duration", safe); updateModeUi(HapticConfig.modeForDisplayPosition(mode.getSelectedItemPosition()), modeDescription, durationLabel, amplitudeLabel, duration, amplitude);
            }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        });
        amplitude.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                int safe = Math.max(1, value); if (safe != value) bar.setProgress(safe);
                HapticConfig.put(cr, id, "amplitude", safe); updateModeUi(HapticConfig.modeForDisplayPosition(mode.getSelectedItemPosition()), modeDescription, durationLabel, amplitudeLabel, duration, amplitude);
            }
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
        });
    }

    private void vibratePreview(String id, int mode, int duration, int amplitude) {
        ContentResolver previewResolver = getContentResolver();
        HapticConfig.putString(previewResolver, "preview", "id", id);
        try {
            VibrationEffect effect;
            if (mode >= 3 && mode <= 8) {
                effect = applyEffectStrength(VibrationEffect.createPredefined(mode - 3), amplitude);
            }
            else if (mode >= 9 && mode <= 17) effect = compose(new int[]{mode - 9}, new float[]{amplitude / 255.0f}, new int[]{0});
            else if (mode == HapticConfig.MODE_CUSTOM_COMPOSITION) {
                // system_server replaces this harmless marker with the mixed sequence.
                effect = VibrationEffect.createOneShot(1, 1);
            }
            else if (mode >= HapticConfig.MODE_TACTILE_FIRST && mode <= HapticConfig.MODE_TACTILE_LAST) {
                effect = buildTactilePreset(mode, amplitude);
            }
            else effect = VibrationEffect.createOneShot(Math.max(1, duration), Math.max(1, amplitude));
            Vibrator vib = vibrator(); if (vib != null) vib.vibrate(effect);
        } catch (Throwable t) { showError(t); }
        finally { HapticConfig.putString(previewResolver, "preview", "id", ""); }
    }

    private VibrationEffect buildTactilePreset(int mode, int amplitude) {
        int effectId;
        if (mode == 19) effectId = 0;
        else if (mode == 20) effectId = 1;
        else if (mode == 21) effectId = 2;
        else if (mode == 22) effectId = 5;
        else effectId = 4;
        return applyEffectStrength(VibrationEffect.createPredefined(effectId), amplitude);
    }

    private VibrationEffect applyEffectStrength(VibrationEffect effect, int amplitude) {
        try {
            int strength = amplitude < 85 ? 0 : (amplitude < 170 ? 1 : 2);
            return (VibrationEffect) effect.getClass().getMethod("applyEffectStrength", Integer.TYPE).invoke(effect, strength);
        } catch (Throwable ignored) { return effect; }
    }

    private static final class Segment {
        int type; int scale; int delay; int duration;
        Segment(int type, int scale, int delay, int duration) { this.type = type; this.scale = scale; this.delay = delay; this.duration = duration; }
    }

    private LinearLayout addCompositionEditor(LinearLayout parent, String id, ContentResolver cr) {
        LinearLayout editor = new LinearLayout(this); editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(dp(8), dp(10), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(0xfff4f7fc); bg.setCornerRadius(dp(12));
        editor.setBackground(bg);
        editor.addView(text("\u591a\u6bb5\u89e6\u611f\u7f16\u6392", 16, 0xff202124));
        editor.addView(text("\u6bcf\u6bb5\u53ef\u9009\u6807\u51c6\u6548\u679c\u3001\u5408\u6210\u539f\u8bed\u3001\u7eaf\u505c\u987f\u6216\u53ef\u8c03\u65f6\u957f\u7684\u2018\u9650\u5236\u6700\u957f\u65f6\u95f4\u2019\u8109\u51b2\u3002\u6bcf\u6bb5\u90fd\u53ef\u8bbe\u5f3a\u5ea6\u4e0e\u524d\u7f6e\u5ef6\u8fdf\uff0c\u4e0a\u65b9\u603b\u5f3a\u5ea6\u4f1a\u6309\u6bd4\u4f8b\u4f5c\u7528\u4e8e\u6240\u6709\u6bb5\u3002", 13, 0xff5f6368));
        LinearLayout rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); editor.addView(rows);
        final ArrayList<Segment> segments = loadSegments(cr, id);
        renderSegmentRows(rows, segments, id, cr);
        Button add = new Button(this); add.setText("\uff0b \u6dfb\u52a0\u4e00\u6bb5"); styleSecondary(add);
        add.setOnClickListener(v -> {
            if (segments.size() >= 32) { Toast.makeText(this, "\u754c\u9762\u6700\u591a\u7f16\u6392 32 \u6bb5", Toast.LENGTH_SHORT).show(); return; }
            segments.add(new Segment(HapticConfig.SEGMENT_LIMIT_DURATION, 100, 25, 25)); saveSegments(cr, id, segments); renderSegmentRows(rows, segments, id, cr);
        });
        editor.addView(add, fullButtonParams());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(10), 0, dp(2));
        parent.addView(editor, p); return editor;
    }

    private ArrayList<Segment> loadSegments(ContentResolver cr, String id) {
        ArrayList<Segment> result = new ArrayList<Segment>();
        try {
            int defaultIndex = HapticConfig.indexOf(id);
            int[] ids = parseInts(HapticConfig.getString(cr, id, "primitive_ids", HapticConfig.defaultPrimitiveIds(defaultIndex)));
            float[] scales = parseFloats(HapticConfig.getString(cr, id, "primitive_scales", HapticConfig.defaultPrimitiveScales(defaultIndex)));
            int[] delays = parseInts(HapticConfig.getString(cr, id, "primitive_delays", HapticConfig.defaultPrimitiveDelays(defaultIndex)));
            int[] durations = parseInts(HapticConfig.getString(cr, id, "segment_durations", HapticConfig.defaultSegmentDurations(defaultIndex)));
            if (ids.length == 0 || ids.length != scales.length || ids.length != delays.length || ids.length != durations.length) throw new IllegalArgumentException();
            for (int i = 0; i < ids.length; i++) result.add(new Segment(ids[i], Math.round(scales[i] * 100), delays[i], durations[i]));
        } catch (Throwable ignored) {
            result.add(new Segment(HapticConfig.SEGMENT_EFFECT_CLICK, 100, 0, 25));
            result.add(new Segment(HapticConfig.SEGMENT_LIMIT_DURATION, 100, 25, 25));
        }
        return result;
    }

    private void renderSegmentRows(final LinearLayout rows, final ArrayList<Segment> segments, final String id, final ContentResolver cr) {
        rows.removeAllViews();
        for (int i = 0; i < segments.size(); i++) {
            final int position = i; final Segment segment = segments.get(i);
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(8), 0, dp(6));
            LinearLayout heading = new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = text("\u7b2c " + (i + 1) + " \u6bb5", 14, 0xff3c4043);
            heading.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            Button remove = new Button(this); remove.setText("\u5220\u9664"); styleSecondary(remove);
            remove.setEnabled(segments.size() > 1);
            remove.setOnClickListener(v -> { if (segments.size() > 1) { segments.remove(position); saveSegments(cr, id, segments); renderSegmentRows(rows, segments, id, cr); } });
            heading.addView(remove, new LinearLayout.LayoutParams(-2, -2)); row.addView(heading);
            Spinner primitive = new Spinner(this);
            primitive.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, SEGMENT_NAMES));
            primitive.setSelection(segmentPosition(segment.type)); row.addView(primitive, new LinearLayout.LayoutParams(-1, -2));
            final TextView scaleLabel = text("\u6bb5\u5f3a\u5ea6\uff1a" + segment.scale + "%", 13, 0xff3c4043); row.addView(scaleLabel);
            SeekBar scale = new SeekBar(this); scale.setMax(100); scale.setProgress(segment.scale); row.addView(scale);
            final TextView segmentDurationLabel = text("\u8109\u51b2\u65f6\u957f\uff1a" + segment.duration + " ms", 13, 0xff3c4043); row.addView(segmentDurationLabel);
            SeekBar segmentDuration = new SeekBar(this); segmentDuration.setMax(300); segmentDuration.setProgress(segment.duration); row.addView(segmentDuration);
            boolean durationEnabled = segment.type == HapticConfig.SEGMENT_LIMIT_DURATION;
            segmentDurationLabel.setVisibility(durationEnabled ? View.VISIBLE : View.GONE);
            segmentDuration.setVisibility(durationEnabled ? View.VISIBLE : View.GONE);
            final TextView delayLabel = text("\u524d\u7f6e\u5ef6\u8fdf\uff1a" + segment.delay + " ms", 13, 0xff3c4043); row.addView(delayLabel);
            SeekBar delay = new SeekBar(this); delay.setMax(1000); delay.setProgress(segment.delay); row.addView(delay);
            primitive.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> p, View v, int selected, long itemId) {
                    segment.type = SEGMENT_TYPES[selected];
                    boolean showDuration = segment.type == HapticConfig.SEGMENT_LIMIT_DURATION;
                    segmentDurationLabel.setVisibility(showDuration ? View.VISIBLE : View.GONE);
                    segmentDuration.setVisibility(showDuration ? View.VISIBLE : View.GONE);
                    saveSegments(cr, id, segments);
                }
                public void onNothingSelected(AdapterView<?> p) {}
            });
            scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar b, int value, boolean fromUser) { segment.scale = value; scaleLabel.setText("\u6bb5\u5f3a\u5ea6\uff1a" + value + "%"); saveSegments(cr, id, segments); }
                public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
            });
            segmentDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar b, int value, boolean fromUser) {
                    int safe = Math.max(1, value); if (safe != value) b.setProgress(safe);
                    segment.duration = safe; segmentDurationLabel.setText("\u8109\u51b2\u65f6\u957f\uff1a" + safe + " ms"); saveSegments(cr, id, segments);
                }
                public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
            });
            delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar b, int value, boolean fromUser) { segment.delay = value; delayLabel.setText("\u524d\u7f6e\u5ef6\u8fdf\uff1a" + value + " ms"); saveSegments(cr, id, segments); }
                public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
            });
            if (i > 0) { View divider = new View(this); divider.setBackgroundColor(0xffdfe3ea); LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(-1, dp(1)); d.setMargins(0, dp(4), 0, 0); rows.addView(divider, d); }
            rows.addView(row);
        }
    }

    private void saveSegments(ContentResolver cr, String id, ArrayList<Segment> segments) {
        StringBuilder ids = new StringBuilder(), scales = new StringBuilder(), delays = new StringBuilder(), durations = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) { ids.append(','); scales.append(','); delays.append(','); durations.append(','); }
            Segment s = segments.get(i); ids.append(s.type); scales.append(s.scale / 100.0f); delays.append(s.delay); durations.append(s.duration);
        }
        HapticConfig.putString(cr, id, "primitive_ids", ids.toString());
        HapticConfig.putString(cr, id, "primitive_scales", scales.toString());
        HapticConfig.putString(cr, id, "primitive_delays", delays.toString());
        HapticConfig.putString(cr, id, "segment_durations", durations.toString());
    }

    private int segmentPosition(int type) {
        for (int i = 0; i < SEGMENT_TYPES.length; i++) if (SEGMENT_TYPES[i] == type) return i;
        return 7;
    }

    private void addCompositionLab(LinearLayout root) {
        LinearLayout box = card(root);
        box.addView(text("\u5408\u6210\u539f\u8bed\u5b9e\u9a8c\u5ba4", 20, Color.BLACK));
        box.addView(text("ID: 0 NOOP, 1 CLICK, 2 THUD, 3 SPIN, 4 QUICK_RISE, 5 SLOW_RISE, 6 QUICK_FALL, 7 TICK, 8 LOW_TICK\u3002\u6700\u591a 256 \u6bb5\uff0c\u5ef6\u8fdf\u4e0a\u9650 1000 ms\u3002", 13, 0xff555555));
        final EditText ids = input("1,7"); final EditText scales = input("1.0,0.6"); final EditText delays = input("0,40");
        box.addView(text("\u539f\u8bed ID\uff08\u9017\u53f7\u5206\u9694\uff09", 13, 0xff333333)); box.addView(ids);
        box.addView(text("\u6bcf\u6bb5\u5f3a\u5ea6 0.0-1.0", 13, 0xff333333)); box.addView(scales);
        box.addView(text("\u6bcf\u6bb5\u524d\u5ef6\u8fdf ms", 13, 0xff333333)); box.addView(delays);
        Button run = new Button(this); run.setText("\u6d4b\u8bd5\u5408\u6210\u539f\u8bed\u5e8f\u5217");
        run.setOnClickListener(v -> {
            try { Vibrator vib = vibrator(); if (vib != null) vib.vibrate(compose(parseInts(ids.getText().toString()), parseFloats(scales.getText().toString()), parseInts(delays.getText().toString()))); }
            catch (Throwable t) { showError(t); }
        }); styleSecondary(run); box.addView(run);
    }

    private void addWaveformLab(LinearLayout root) {
        LinearLayout box = card(root);
        box.addView(text("\u81ea\u5b9a\u4e49\u6ce2\u5f62\u5b9e\u9a8c\u5ba4", 20, Color.BLACK));
        box.addView(text("\u65f6\u5e8f\u4e0e\u632f\u5e45\u6570\u91cf\u5fc5\u987b\u4e00\u81f4\u3002\u632f\u5e45 0-255\uff1b\u91cd\u590d\u70b9 -1 \u8868\u793a\u4e0d\u91cd\u590d\u3002", 13, 0xff555555));
        final EditText timings = input("0,10,20,10"); final EditText amplitudes = input("0,180,0,255"); final EditText repeat = input("-1");
        box.addView(text("\u65f6\u5e8f ms", 13, 0xff333333)); box.addView(timings);
        box.addView(text("\u632f\u5e45", 13, 0xff333333)); box.addView(amplitudes);
        box.addView(text("\u91cd\u590d\u8d77\u59cb\u7d22\u5f15", 13, 0xff333333)); box.addView(repeat);
        Button run = new Button(this); run.setText("\u6d4b\u8bd5\u81ea\u5b9a\u4e49\u6ce2\u5f62");
        run.setOnClickListener(v -> {
            try {
                int[] ti = parseInts(timings.getText().toString()), amp = parseInts(amplitudes.getText().toString());
                if (ti.length != amp.length || ti.length == 0 || ti.length > 256) throw new IllegalArgumentException("\u6bb5\u6570\u5fc5\u987b\u76f8\u540c\u4e14\u4e3a 1-256");
                long[] times = new long[ti.length]; long total = 0;
                for (int i = 0; i < ti.length; i++) { if (ti[i] < 0 || amp[i] < 0 || amp[i] > 255) throw new IllegalArgumentException("\u65f6\u95f4\u6216\u632f\u5e45\u8d8a\u754c"); times[i] = ti[i]; total += ti[i]; }
                if (total > 5000) throw new IllegalArgumentException("\u5355\u8f6e\u6ce2\u5f62\u4e0d\u80fd\u8d85\u8fc7 5000 ms");
                Vibrator vib = vibrator(); if (vib != null) vib.vibrate(VibrationEffect.createWaveform(times, amp, Integer.parseInt(repeat.getText().toString().trim())));
            } catch (Throwable t) { showError(t); }
        }); styleSecondary(run); box.addView(run);
    }

    private void addConfigTransfer(LinearLayout root) {
        LinearLayout box = card(root);
        box.addView(text("\u914d\u7f6e\u5907\u4efd", 20, 0xff202124));
        box.addView(text("\u901a\u8fc7 Android \u7cfb\u7edf\u6587\u4ef6\u9009\u62e9\u5668\u5bfc\u51fa/\u5bfc\u5165 JSON\uff0c\u4e0d\u9700\u8981\u5b58\u50a8\u6743\u9650\u3002", 13, 0xff5f6368));
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button save = new Button(this); save.setText("\u4fdd\u5b58\u914d\u7f6e\u6587\u4ef6"); stylePrimary(save);
        save.setOnClickListener(v -> {
            Intent i = new Intent("android.intent.action.CREATE_DOCUMENT");
            i.addCategory("android.intent.category.OPENABLE"); i.setType("application/json");
            i.putExtra("android.intent.extra.TITLE", "Y700-Haptics-Config.json"); startActivityForResult(i, REQUEST_EXPORT);
        });
        Button load = new Button(this); load.setText("\u5bfc\u5165\u914d\u7f6e\u6587\u4ef6"); styleSecondary(load);
        load.setOnClickListener(v -> {
            Intent i = new Intent("android.intent.action.OPEN_DOCUMENT");
            i.addCategory("android.intent.category.OPENABLE"); i.setType("application/json"); startActivityForResult(i, REQUEST_IMPORT);
        });
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1); half.setMargins(dp(4), dp(8), dp(4), 0);
        row.addView(save, half); row.addView(load, half); box.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == REQUEST_EXPORT) exportConfig(data.getData());
            else if (requestCode == REQUEST_IMPORT) { importConfig(data.getData()); recreate(); }
        } catch (Throwable t) { showError(t); }
    }

    private void exportConfig(Uri uri) throws Exception {
        JSONObject root = new JSONObject(); root.put("format", "y700-haptics-config"); root.put("version", 2);
        JSONObject categories = new JSONObject(); ContentResolver cr = getContentResolver();
        for (int i = 0; i < HapticConfig.IDS.length; i++) {
            String id = HapticConfig.IDS[i]; JSONObject item = new JSONObject();
            item.put("enabled", HapticConfig.get(cr, id, "enabled", HapticConfig.DEFAULT_ENABLED[i]));
            item.put("mode", HapticConfig.get(cr, id, "mode", HapticConfig.DEFAULT_MODE[i]));
            item.put("duration", HapticConfig.get(cr, id, "duration", HapticConfig.DEFAULT_DURATION[i]));
            item.put("amplitude", HapticConfig.get(cr, id, "amplitude", HapticConfig.DEFAULT_AMPLITUDE[i]));
            item.put("primitiveIds", HapticConfig.getString(cr, id, "primitive_ids", HapticConfig.defaultPrimitiveIds(i)));
            item.put("primitiveScales", HapticConfig.getString(cr, id, "primitive_scales", HapticConfig.defaultPrimitiveScales(i)));
            item.put("primitiveDelays", HapticConfig.getString(cr, id, "primitive_delays", HapticConfig.defaultPrimitiveDelays(i)));
            item.put("segmentDurations", HapticConfig.getString(cr, id, "segment_durations", HapticConfig.defaultSegmentDurations(i)));
            categories.put(id, item);
        }
        root.put("categories", categories);
        OutputStream stream = getContentResolver().openOutputStream(uri);
        if (stream == null) throw new IllegalStateException("openOutputStream returned null");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
        writer.write(root.toString(2)); writer.close(); Toast.makeText(this, "\u914d\u7f6e\u5df2\u4fdd\u5b58", Toast.LENGTH_SHORT).show();
    }

    private void importConfig(Uri uri) throws Exception {
        InputStream stream = getContentResolver().openInputStream(uri);
        if (stream == null) throw new IllegalStateException("openInputStream returned null");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8")); StringBuilder text = new StringBuilder();
        String line; while ((line = reader.readLine()) != null) { if (text.length() > 262144) throw new IllegalArgumentException("\u914d\u7f6e\u6587\u4ef6\u8fc7\u5927"); text.append(line); }
        reader.close(); JSONObject root = new JSONObject(text.toString());
        if (!"y700-haptics-config".equals(root.optString("format"))) throw new IllegalArgumentException("\u4e0d\u652f\u6301\u7684\u914d\u7f6e\u683c\u5f0f");
        JSONObject categories = root.getJSONObject("categories"); ContentResolver cr = getContentResolver();
        for (int i = 0; i < HapticConfig.IDS.length; i++) {
            String id = HapticConfig.IDS[i]; JSONObject item = categories.optJSONObject(id); if (item == null) continue;
            int enabled = item.optInt("enabled", HapticConfig.DEFAULT_ENABLED[i]);
            int mode = item.optInt("mode", HapticConfig.DEFAULT_MODE[i]);
            int duration = item.optInt("duration", HapticConfig.DEFAULT_DURATION[i]);
            int amplitude = item.optInt("amplitude", HapticConfig.DEFAULT_AMPLITUDE[i]);
            String primitiveIds = item.optString("primitiveIds"); if (primitiveIds.length() == 0) primitiveIds = HapticConfig.defaultPrimitiveIds(i);
            String primitiveScales = item.optString("primitiveScales"); if (primitiveScales.length() == 0) primitiveScales = HapticConfig.defaultPrimitiveScales(i);
            String primitiveDelays = item.optString("primitiveDelays"); if (primitiveDelays.length() == 0) primitiveDelays = HapticConfig.defaultPrimitiveDelays(i);
            String segmentDurations = item.optString("segmentDurations"); if (segmentDurations.length() == 0) segmentDurations = HapticConfig.defaultSegmentDurations(i);
            if ((enabled != 0 && enabled != 1) || mode < 0 || mode >= HapticConfig.MODE_NAMES.length || duration < 1 || duration > 1000 || amplitude < 1 || amplitude > 255) throw new IllegalArgumentException(id + " \u53c2\u6570\u8d8a\u754c");
            validateComposition(primitiveIds, primitiveScales, primitiveDelays, segmentDurations);
            HapticConfig.put(cr, id, "enabled", enabled); HapticConfig.put(cr, id, "mode", mode);
            HapticConfig.put(cr, id, "duration", duration); HapticConfig.put(cr, id, "amplitude", amplitude);
            HapticConfig.putString(cr, id, "primitive_ids", primitiveIds);
            HapticConfig.putString(cr, id, "primitive_scales", primitiveScales);
            HapticConfig.putString(cr, id, "primitive_delays", primitiveDelays);
            HapticConfig.putString(cr, id, "segment_durations", segmentDurations);
        }
        Toast.makeText(this, "\u914d\u7f6e\u5df2\u5bfc\u5165\u5e76\u751f\u6548", Toast.LENGTH_SHORT).show();
    }

    private void stylePrimary(Button b) {
        b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable d = new GradientDrawable(); d.setColor(BLUE); d.setCornerRadius(dp(20)); b.setBackground(d);
    }

    private void styleSecondary(Button b) {
        b.setTextColor(BLUE); b.setAllCaps(false); b.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable d = new GradientDrawable(); d.setColor(0xffedf3ff); d.setCornerRadius(dp(20)); d.setStroke(dp(1), 0xffc4d7f8); b.setBackground(d);
    }

    private EditText input(String value) { EditText e = new EditText(this); e.setText(value); e.setSingleLine(true); return e; }
    private int[] parseInts(String value) { String[] p = value.trim().split("\\s*,\\s*"); int[] out = new int[p.length]; for (int i = 0; i < p.length; i++) out[i] = Integer.parseInt(p[i]); return out; }
    private float[] parseFloats(String value) { String[] p = value.trim().split("\\s*,\\s*"); float[] out = new float[p.length]; for (int i = 0; i < p.length; i++) out[i] = Float.parseFloat(p[i]); return out; }

    private void validateComposition(String idText, String scaleText, String delayText, String durationText) {
        int[] ids = parseInts(idText), delays = parseInts(delayText), durations = parseInts(durationText); float[] scales = parseFloats(scaleText);
        if (ids.length == 0 || ids.length != scales.length || ids.length != delays.length || ids.length != durations.length || ids.length > 256) throw new IllegalArgumentException("\u591a\u6bb5\u5e8f\u5217\u6bb5\u6570\u65e0\u6548");
        for (int i = 0; i < ids.length; i++) {
            boolean validType = (ids[i] >= 0 && ids[i] <= 8) || (ids[i] >= 100 && ids[i] <= 106);
            if (!validType || scales[i] < 0 || scales[i] > 1 || delays[i] < 0 || delays[i] > 1000 || durations[i] < 1 || durations[i] > 300) throw new IllegalArgumentException("\u591a\u6bb5\u5e8f\u5217\u53c2\u6570\u8d8a\u754c");
        }
    }

    private VibrationEffect compose(int[] ids, float[] scales, int[] delays) throws Throwable {
        if (ids.length == 0 || ids.length != scales.length || ids.length != delays.length || ids.length > 256) throw new IllegalArgumentException("\u4e09\u7ec4\u53c2\u6570\u6570\u91cf\u5fc5\u987b\u76f8\u540c\uff0c\u4e14\u4e0d\u8d85\u8fc7 256");
        Method start = VibrationEffect.class.getMethod("startComposition"); Object c = start.invoke(null);
        Method add = c.getClass().getMethod("addPrimitive", Integer.TYPE, Float.TYPE, Integer.TYPE);
        int pendingDelay = 0; boolean added = false;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] < 0 || ids[i] > 8 || scales[i] < 0 || scales[i] > 1 || delays[i] < 0 || delays[i] > 1000) throw new IllegalArgumentException("\u539f\u8bed\u53c2\u6570\u8d8a\u754c");
            if (ids[i] == 0) { pendingDelay += delays[i] + 50; continue; }
            add.invoke(c, ids[i], scales[i], Math.min(1000, pendingDelay + delays[i]));
            pendingDelay = 0; added = true;
        }
        if (!added) return VibrationEffect.createWaveform(new long[]{Math.max(1, pendingDelay)}, new int[]{0}, -1);
        return (VibrationEffect) c.getClass().getMethod("compose").invoke(c);
    }

    private void showError(Throwable t) { Toast.makeText(this, "\u53c2\u6570\u9519\u8bef\uff1a" + String.valueOf(t.getMessage()), Toast.LENGTH_LONG).show(); }
    private void updateModeUi(int mode, TextView description, TextView durationLabel, TextView amplitudeLabel, SeekBar duration, SeekBar amplitude) {
        int safeMode = Math.max(0, Math.min(HapticConfig.MODE_DESCRIPTIONS.length - 1, mode));
        description.setText(HapticConfig.MODE_DESCRIPTIONS[safeMode]);
        boolean durationUsed = mode == 0 || mode == 1;
        duration.setEnabled(durationUsed);
        if (durationUsed) durationLabel.setText("\u65f6\u957f/\u4e0a\u9650\uff1a" + duration.getProgress() + " ms");
        else if (mode == 2) durationLabel.setText("\u65f6\u957f\uff1a\u4fdd\u7559\u539f\u6ce2\u5f62\uff08\u6b64\u6a21\u5f0f\u4e0d\u4f7f\u7528\u6ed1\u6746\uff09");
        else if (mode >= HapticConfig.MODE_DUAL_FIRST) durationLabel.setText("\u65f6\u957f/\u5de6\u53f3\u65f6\u5e8f\uff1a\u7531 Y700 \u53cc\u9a6c\u8fbe\u9884\u8bbe\u56fa\u5b9a\uff08\u6b64\u6a21\u5f0f\u4e0d\u4f7f\u7528\u6ed1\u6746\uff09");
        else if (mode >= HapticConfig.MODE_TACTILE_FIRST) durationLabel.setText("\u65f6\u957f\uff1a\u7531 Android \u7cfb\u7edf\u81ea\u5e26\u6548\u679c\u56fa\u5b9a\uff08\u6b64\u6a21\u5f0f\u4e0d\u4f7f\u7528\u6ed1\u6746\uff09");
        else durationLabel.setText("\u65f6\u957f\uff1a\u7531 HAL \u6548\u679c/\u539f\u8bed\u56fa\u5b9a\uff08\u6b64\u6a21\u5f0f\u4e0d\u4f7f\u7528\u6ed1\u6746\uff09");

        int amp = amplitude.getProgress();
        amplitude.setEnabled(mode != 9);
        if (mode >= 3 && mode <= 8) {
            String tier = amp < 85 ? "LIGHT" : (amp < 170 ? "MEDIUM" : "STRONG");
            amplitudeLabel.setText("\u6807\u51c6\u6548\u679c\u5f3a\u5ea6\uff1a" + tier + "\uff08\u4e09\u6863\uff1b1-84 / 85-169 / 170-255\uff09");
        } else if (mode == 9) {
            amplitudeLabel.setText("\u5f3a\u5ea6\uff1aNOOP \u4e0d\u9a71\u52a8\u9a6c\u8fbe\uff08\u4e0d\u4f7f\u7528\u6ed1\u6746\uff09");
        } else if (mode >= HapticConfig.MODE_DUAL_FIRST) {
            amplitudeLabel.setText("Y700 \u53cc\u9a6c\u8fbe\u603b\u5f3a\u5ea6\uff1a" + amp + " / 255\uff08\u6309\u6bd4\u4f8b\u7f29\u653e\u9884\u8bbe\uff1b\u4e0d\u8d85\u8fc7\u539f\u5382 0x80 \u589e\u76ca\uff09");
        } else if (mode >= HapticConfig.MODE_TACTILE_FIRST) {
            String tier = amp < 85 ? "LIGHT" : (amp < 170 ? "MEDIUM" : "STRONG");
            amplitudeLabel.setText("\u7cfb\u7edf\u81ea\u5e26\u6548\u679c\u5f3a\u5ea6\uff1a" + tier + "\uff08\u4e09\u6863\uff09");
        } else if (mode >= 10) {
            amplitudeLabel.setText("\u539f\u8bed/\u7ec4\u5408\u603b\u5f3a\u5ea6\uff1a" + amp + " / 255");
        } else {
            amplitudeLabel.setText("\u6700\u7ec8\u76ee\u6807\u5f3a\u5ea6\uff1a" + amp + " / 255\uff08\u5df2\u8865\u507f ColorOS \u7f29\u653e\uff09");
        }
    }
}
