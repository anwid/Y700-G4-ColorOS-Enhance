package dev.lorana.coloros.fraudwhitelist;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class WhitelistActivity extends Activity {
    private EditText packages;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("ColorOS 反诈告警白名单");

        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("仅忽略白名单应用触发的无障碍权限变更告警");
        title.setTextSize(20);
        title.setTextColor(Color.rgb(25, 25, 28));
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("每行一个包名。未列出的应用仍由 ColorOS 正常检测。\n修改后无需重装，但首次启用模块需要重启 system_server。\n\n笔控制应用始终内置放行：\n" + HookEntry.DEFAULT_PACKAGE);
        hint.setTextSize(14);
        hint.setPadding(0, dp(12), 0, dp(16));
        root.addView(hint);

        packages = new EditText(this);
        packages.setHint("例如：\ncom.example.accessibilitytool");
        packages.setMinLines(8);
        packages.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        packages.setText(Settings.Secure.getString(getContentResolver(), HookEntry.SETTINGS_KEY));
        root.addView(packages, new LinearLayout.LayoutParams(-1, -2));

        Button save = new Button(this);
        save.setText("保存白名单");
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { save(); }
        });
        root.addView(save, new LinearLayout.LayoutParams(-1, -2));

        Button clear = new Button(this);
        clear.setText("清空自定义项（保留内置笔应用）");
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                packages.setText("");
                save();
            }
        });
        root.addView(clear, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void save() {
        String raw = packages.getText().toString().trim();
        try {
            boolean ok;
            if (raw.isEmpty()) ok = Settings.Secure.putString(getContentResolver(), HookEntry.SETTINGS_KEY, null);
            else ok = Settings.Secure.putString(getContentResolver(), HookEntry.SETTINGS_KEY, raw);
            Toast.makeText(this, ok ? "已保存" : "保存失败", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "缺少 WRITE_SECURE_SETTINGS，请通过 root 授权", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
