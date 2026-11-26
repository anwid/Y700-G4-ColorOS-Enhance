package dev.lorana.y700.colorospenui.xposed;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class NativePenPanelHook {
    static final String PACKAGE = "com.oplus.ipemanager";
    static final String PANEL = PACKAGE + ".btadsorb.pencilPanel.activity.PencilPanelActivity";
    static final String ACTION = PACKAGE + ".btadsorb.PENCIL_PANEL";
    static final String MAC = "device_mac_info";
    static final String BRIDGE = "dev.lorana.y700.extra.NATIVE_PEN_PANEL";
    static final String BATTERY = "dev.lorana.y700.extra.PEN_BATTERY";
    static final String CHARGING = "dev.lorana.y700.extra.PEN_CHARGING";
    static final String CONNECTED = "dev.lorana.y700.extra.PEN_CONNECTED";

    private static final String IPE = PACKAGE + ".btadsorb.";
    private static volatile WeakReference<Activity> active = new WeakReference<>(null);

    static void install(ClassLoader loader) {
        try {
            Class<?> panel = loader.loadClass(PANEL);
            XposedBridge.hookAllMethods(panel, "onCreate", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    Intent intent = activity.getIntent();
                    if (intent != null && intent.getBooleanExtra(BRIDGE, false)
                            && BluetoothAdapter.checkBluetoothAddress(intent.getStringExtra(MAC))) {
                        active = new WeakReference<>(activity);
                    }
                }
            });
            XposedBridge.hookAllMethods(panel, "onDestroy", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (active.get() == param.thisObject) active.clear();
                }
            });

            Class<?> deviceCard = loader.loadClass(IPE + "d");
            Object pencil = loader.loadClass(IPE + "data.IpeDeviceType").getField("PENCIL").get(null);
            override(deviceCard, "getIpeDeviceType", intent -> pencil);
            // CoreService runs in :ble, so the panel normally reads through these Binder proxies.
            override(loader.loadClass("k3.a"), "getIpeDeviceType", intent -> pencil);

            for (String name : new String[] {IPE + "ble.f0", "k3.i"}) {
                Class<?> service = loader.loadClass(name);
                override(service, "getAlias", intent -> "OPPO Pencil");
                override(service, "getConnectState", intent -> intent.getBooleanExtra(CONNECTED, false) ? 2 : 0);
                override(service, "getBatteryLevel", intent -> intent.getIntExtra(BATTERY, -1));
                override(service, "getChargingState", intent -> intent.getBooleanExtra(CHARGING, false));
            }

            hidePanelControls(loader);
            hideOfficialSettingsSections(loader);

            Class<?> click = loader.loadClass(IPE + "pencilPanel.fragment.h0");
            XposedBridge.hookAllMethods(click, "onPreferenceClick", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = active.get();
                    if (activity == null || param.args.length != 1 || param.args[0] == null) return;
                    Object preference = param.args[0];
                    Method getKey = preference.getClass().getMethod("getKey");
                    if (!"more_settings".equals(getKey.invoke(preference))) return;
                    openControl(activity);
                    param.setResult(true);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            Log.w("Y700DeviceSpace", "Unsupported native pencil panel", error);
        }
    }

    private static void hidePanelControls(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> preferences = loader.loadClass(IPE + "pencilPanel.fragment.k1");
        XposedBridge.hookAllMethods(preferences, "onCreatePreferences", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object fragment = param.thisObject;
                if (param.hasThrowable() || !isBridgedPanel(fragment)) return;
                for (String key : new String[] {"double_click_pencil", "double_click_pencil2",
                        "double_click_jump", "double_click_jump_iceland", "disconnect_device"}) {
                    removePreference(findPreference(fragment, key));
                }
            }
        });

        Class<?> panel = loader.loadClass(IPE + "pencilPanel.fragment.g0");
        XposedBridge.hookAllMethods(panel, "d", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object fragment = param.thisObject;
                if (param.hasThrowable() || !isBridgedPanel(fragment)) return;
                Activity activity = active.get();
                Object toolbar = fragment.getClass().getMethod("getToolbar").invoke(fragment);
                if (activity == null || toolbar == null) return;
                int id = activity.getResources().getIdentifier("menu_delete_device", "id", PACKAGE);
                Menu menu = (Menu) toolbar.getClass().getMethod("getMenu").invoke(toolbar);
                if (id != 0) menu.removeItem(id);
            }
        });
    }

    private static boolean isBridgedPanel(Object fragment) throws ReflectiveOperationException {
        Activity activity = active.get();
        return activity != null && activity == fragment.getClass().getMethod("getActivity").invoke(fragment);
    }

    private static void hideOfficialSettingsSections(ClassLoader loader)
            throws ReflectiveOperationException {
        Class<?> settings = loader.loadClass(IPE + "setting.fragment.t0");
        XposedBridge.hookAllMethods(settings, "onCreatePreferences", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.hasThrowable()) return;
                Object fragment = param.thisObject;
                Object scribe = findPreference(fragment, "scribe");
                Object learn = findPreference(fragment, "learn_to_use_scribe");
                Object group = parentOf(scribe);
                if (group != null && group == parentOf(learn) && (Integer) group.getClass()
                        .getMethod("getPreferenceCount").invoke(group) == 2) {
                    removePreference(group);
                } else {
                    removePreference(scribe);
                    removePreference(learn);
                }
                removePreference(findPreference(fragment, "about"));
            }
        });
    }

    private static Object findPreference(Object fragment, String key) throws ReflectiveOperationException {
        return fragment.getClass().getMethod("findPreference", CharSequence.class).invoke(fragment, key);
    }

    private static Object parentOf(Object preference) throws ReflectiveOperationException {
        return preference == null ? null : preference.getClass().getMethod("getParent").invoke(preference);
    }

    private static void removePreference(Object preference) throws ReflectiveOperationException {
        Object parent = parentOf(preference);
        if (parent == null) return;
        Class<?> preferenceType = preference.getClass().getClassLoader()
                .loadClass("androidx.preference.Preference");
        parent.getClass().getMethod("removePreference", preferenceType).invoke(parent, preference);
    }

    private interface PanelValue {
        Object from(Intent intent);
    }

    private static void override(Class<?> type, String method, PanelValue value) {
        XposedBridge.hookAllMethods(type, method, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                Activity activity = active.get();
                if (activity == null || param.args.length != 1) return;
                Intent intent = activity.getIntent();
                String address = intent.getStringExtra(MAC);
                if (address != null && (param.args[0] instanceof String)
                        && address.equalsIgnoreCase((String) param.args[0])) {
                    param.setResult(value.from(intent));
                }
            }
        });
    }

    private static void openControl(Context context) {
        Intent settings = new Intent().setClassName("dev.lorana.y700.colorospenui.xposed",
                "dev.lorana.y700.colorospenui.xposed.SettingsActivity");
        context.startActivity(settings);
    }
}
