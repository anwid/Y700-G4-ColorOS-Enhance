package dev.lorana.y700.colorospenui.xposed;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class LenovoPenButtonHook {
    private static final String SINGLE = "com.oplus.ipemanager.action.PENCIL_SINGLE_CLICK";
    private static final String DOUBLE = "com.oplus.ipemanager.action.PENCIL_DOUBLE_CLICK";
    private static final String DOUBLE_PERMISSION = "com.oplus.ipemanager.permission.receiver.DOUBLE_CLICK";
    private static final String TAG = "Y700PenButton";

    static void install(ClassLoader loader, Supplier<Context> context) {
        try {
            Class<?> controller = loader.loadClass("com.android.server.am.BroadcastController");
            XposedBridge.hookAllMethods(controller, "broadcastIntentWithFeature", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length != 16 || Binder.getCallingUid() != 0
                            || param.args[0] != null || !(param.args[2] instanceof Intent)) return;
                    Intent original = (Intent) param.args[2];
                    if (!SINGLE.equals(original.getAction()) || original.getIntExtra("action", -1) != 4
                            || original.getComponent() != null) return;
                    try {
                        int caller = Binder.getCallingPid();
                        if (!isRomPenCommand(caller)) {
                            Log.d(TAG, "Ignored single-click broadcast from PID " + caller);
                            return;
                        }
                        Context system = context.get();
                        if (system == null) return;
                        String foreground = foregroundPackage(system);
                        if (foreground == null) return;
                        boolean passthrough = RouletteConfig.read(system).contains(foreground);
                        int mode = Settings.Global.getInt(system.getContentResolver(),
                                "ipe_pencil_double_click", 1);
                        if (passthrough || (mode >= 1 && mode <= 3)) {
                            Intent converted = new Intent(original).setAction(DOUBLE).setPackage(foreground);
                            converted.removeExtra("action");
                            param.args[2] = converted;
                            param.args[8] = new String[] {DOUBLE_PERMISSION};
                            if (passthrough) {
                                Log.i(TAG, "Sent Lenovo button as OPPO Pencil double-click to " + foreground);
                            } else {
                                Log.i(TAG, "Routed ROM Lenovo pen button to double-click mode " + mode);
                            }
                        } else if (mode == 0) {
                            // Complete the shell broadcast without sending a pen action.
                            param.args[2] = new Intent("dev.lorana.y700.action.PEN_BUTTON_DISABLED")
                                    .setPackage("dev.lorana.y700.colorospenui.xposed");
                            Log.i(TAG, "Suppressed ROM Lenovo pen button for disabled double-click mode");
                        }
                    } catch (IOException | RuntimeException error) {
                        Log.w(TAG, "Could not route the ROM pen button", error);
                    }
                }
            });
            Log.i(TAG, "ROM pen button routing hook installed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            Log.w(TAG, "Unsupported activity broadcast entry point", error);
        }
    }

    private static String foregroundPackage(Context context) {
        ActivityManager manager = context.getSystemService(ActivityManager.class);
        if (manager == null) return null;
        List<ActivityManager.RunningTaskInfo> tasks = manager.getRunningTasks(1);
        return tasks.isEmpty() || tasks.get(0).topActivity == null
                ? null : tasks.get(0).topActivity.getPackageName();
    }

    private static boolean isRomPenCommand(int pid) throws IOException {
        for (int depth = 0; depth < 5 && pid > 1; depth++) {
            try (FileInputStream input = new FileInputStream("/proc/" + pid + "/cmdline")) {
                byte[] bytes = new byte[128];
                int count = input.read(bytes);
                int length = 0;
                while (length < count && bytes[length] != 0) length++;
                String command = new String(bytes, 0, length, StandardCharsets.UTF_8);
                if ("/system_ext/bin/gugu_platform_hal".equals(command)) return true;
            }
            pid = parentPid(pid);
        }
        return false;
    }

    private static int parentPid(int pid) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/status"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("PPid:")) return Integer.parseInt(line.substring(5).trim());
            }
        }
        return -1;
    }
}
