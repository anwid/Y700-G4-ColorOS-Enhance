package dev.lorana.y700.colorospenui.xposed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.icu.text.Transliterator;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class RouletteAppPicker {
    private final Activity activity;
    private final PackageManager manager;
    private final Set<String> checked;
    private final List<App> installed = new ArrayList<>();
    private final List<App> visible = new ArrayList<>();
    private ArrayAdapter<App> adapter;
    private boolean loading = true;

    private RouletteAppPicker(Activity activity) {
        this.activity = activity;
        manager = activity.getPackageManager();
        checked = RouletteConfig.read(activity);
    }

    private List<App> loadApps(Set<String> selected) {
        List<App> apps = new ArrayList<>();
        Transliterator pinyin = Build.VERSION.SDK_INT >= 29
                ? Transliterator.getInstance("Han-Latin; Latin-ASCII") : null;
        Set<String> installedPackages = new HashSet<>();
        for (ApplicationInfo info : manager.getInstalledApplications(0)) {
            apps.add(new App(info.packageName, info.loadLabel(manager).toString(), info, pinyin));
            installedPackages.add(info.packageName);
        }
        for (String packageName : selected) {
            if (!installedPackages.contains(packageName)) apps.add(new App(packageName, packageName, null, pinyin));
        }
        Collator names = Collator.getInstance();
        apps.sort((a, b) -> {
            int checkedOrder = Boolean.compare(selected.contains(b.packageName), selected.contains(a.packageName));
            if (checkedOrder != 0) return checkedOrder;
            int labelOrder = names.compare(a.label, b.label);
            return labelOrder != 0 ? labelOrder : a.packageName.compareTo(b.packageName);
        });
        return apps;
    }

    static void show(Activity activity, Runnable onSaved) {
        new RouletteAppPicker(activity).open(onSaved);
    }

    private void open(Runnable onSaved) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText search = new EditText(activity);
        search.setSingleLine(true);
        search.setHint("搜索应用名、包名或拼音");
        layout.addView(search, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout filters = new LinearLayout(activity);
        filters.setPadding(dp(12), 0, dp(12), 0);
        Spinner appType = new Spinner(activity);
        ArrayAdapter<String> types = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item,
                new String[]{"全部应用", "用户应用", "系统应用"});
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appType.setAdapter(types);
        filters.addView(appType, new LinearLayout.LayoutParams(0, -2, 1));
        layout.addView(filters, new LinearLayout.LayoutParams(-1, -2));

        TextView status = new TextView(activity);
        status.setPadding(dp(16), dp(6), dp(16), dp(6));
        layout.addView(status, new LinearLayout.LayoutParams(-1, -2));

        ListView list = new ListView(activity);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        adapter = new ArrayAdapter<App>(activity, android.R.layout.simple_list_item_multiple_choice, visible) {
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                CheckedTextView row = (CheckedTextView) super.getView(position, recycled, parent);
                App app = getItem(position);
                row.setText(app.label + "\n" + app.packageName);
                Drawable icon = app.info == null ? manager.getDefaultActivityIcon() : app.info.loadIcon(manager);
                icon.setBounds(0, 0, dp(32), dp(32));
                row.setCompoundDrawablePadding(dp(12));
                row.setCompoundDrawablesRelative(icon, null, null, null);
                return row;
            }
        };
        list.setAdapter(adapter);
        layout.addView(list, new LinearLayout.LayoutParams(-1, dp(360)));
        list.setOnItemClickListener((parent, row, position, id) -> {
            String packageName = visible.get(position).packageName;
            if (list.isItemChecked(position)) checked.add(packageName);
            else checked.remove(packageName);
            updateStatus(status);
        });
        Runnable filter = () -> {
            String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            int type = appType.getSelectedItemPosition();
            visible.clear();
            for (App app : installed) {
                if ((type == 1 && app.system) || (type == 2 && !app.system)) continue;
                if (matches(app.packageName, query) || matches(app.label, query)
                        || matches(app.pinyin, query)) {
                    visible.add(app);
                }
            }
            list.clearChoices();
            adapter.notifyDataSetChanged();
            restoreChecks(list);
            updateStatus(status);
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter.run(); }
            @Override public void afterTextChanged(Editable value) {}
        });
        appType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        updateStatus(status);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("选择不弹轮盘的应用")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.show();
        Set<String> selected = new HashSet<>(checked);
        new Thread(() -> {
            List<App> apps;
            try {
                apps = loadApps(selected);
            } catch (RuntimeException error) {
                android.util.Log.w("Y700PenUI", "Could not load apps for the roulette picker", error);
                activity.runOnUiThread(() -> {
                    if (dialog.isShowing()) status.setText("无法读取已安装应用");
                });
                return;
            }
            activity.runOnUiThread(() -> {
                if (!dialog.isShowing()) return;
                installed.addAll(apps);
                loading = false;
                filter.run();
            });
        }, "roulette-app-picker").start();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                if (RouletteConfig.save(activity, checked)) {
                    onSaved.run();
                    dialog.dismiss();
                    return;
                }
            } catch (SecurityException ignored) {}
            Toast.makeText(activity, "无法保存轮盘设置，请检查笔控模块权限", Toast.LENGTH_LONG).show();
        });
    }

    private void restoreChecks(ListView list) {
        for (int i = 0; i < visible.size(); i++) {
            if (checked.contains(visible.get(i).packageName)) list.setItemChecked(i, true);
        }
    }

    private void updateStatus(TextView status) {
        String prefix = loading ? "正在读取应用 · " : visible.isEmpty() ? "没有匹配的应用 · " : "";
        status.setText(prefix + "已选 " + checked.size() + " 个应用");
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static boolean matches(String value, String query) {
        String lower = value.toLowerCase(Locale.ROOT);
        int offset = 0;
        for (int i = 0; i < query.length(); i++) {
            int found = lower.indexOf(query.charAt(i), offset);
            if (found < 0) return false;
            offset = found + 1;
        }
        return true;
    }

    private static final class App {
        final String packageName;
        final String label;
        final ApplicationInfo info;
        final boolean system;
        final String pinyin;

        App(String packageName, String label, ApplicationInfo info, Transliterator transliterator) {
            this.packageName = packageName;
            this.label = label;
            this.info = info;
            system = info != null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            pinyin = transliterator == null ? label : transliterator.transliterate(label);
        }
    }
}
