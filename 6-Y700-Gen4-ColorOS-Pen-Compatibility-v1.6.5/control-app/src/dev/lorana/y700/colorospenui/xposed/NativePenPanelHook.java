package dev.lorana.y700.colorospenui.xposed;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
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
    static final String STATE_CHANGED = "dev.lorana.y700.action.PEN_STATE_CHANGED";
    static final String STATE_PERMISSION = "android.permission.BLUETOOTH_PRIVILEGED";
    static final String CONNECTED = "dev.lorana.y700.extra.PEN_CONNECTED";

    private static final String IPE = PACKAGE + ".btadsorb.";
    private static volatile WeakReference<Activity> active = new WeakReference<>(null);

    static void install(ClassLoader loader) {
        try {
            Class<?> panel = loader.loadClass(PANEL);
            PanelUpdates updates = new PanelUpdates(loader);
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
                    if (active.get() == param.thisObject) {
                        updates.detach((Activity) param.thisObject);
                        active.clear();
                    }
                }
            });

            Class<?> panelFragment = loader.loadClass(IPE + "pencilPanel.fragment.g0");
            Field viewModel = panelFragment.getDeclaredField("mViewModel");
            viewModel.setAccessible(true);
            XposedBridge.hookAllMethods(panelFragment, "onCreate", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = active.get();
                    if (param.hasThrowable() || activity == null || !isBridgedPanel(param.thisObject)) return;
                    updates.attach(activity, viewModel.get(param.thisObject));
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
                removePreference(findPreference(fragment, "disconnect_device"));
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

    private static final class PanelUpdates extends BroadcastReceiver {
        private final Field levelData;
        private final Field connectionData;
        private final Method postValue;
        private final Method setCharging;
        private WeakReference<Activity> owner = new WeakReference<>(null);
        private Object model;

        PanelUpdates(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> modelType = loader.loadClass("b3.f");
            levelData = modelType.getField("i");
            connectionData = modelType.getField("j");
            postValue = levelData.getType().getMethod("postValue", Object.class);
            setCharging = modelType.getMethod("k", boolean.class);
        }

        void attach(Activity activity, Object viewModel) {
            Activity previous = owner.get();
            if (previous != activity) {
                if (previous != null) detach(previous);
                activity.registerReceiver(this, new IntentFilter(STATE_CHANGED), STATE_PERMISSION,
                        null, 2); // RECEIVER_EXPORTED
                owner = new WeakReference<>(activity);
            }
            model = viewModel;
        }

        void detach(Activity activity) {
            if (owner.get() != activity) return;
            activity.unregisterReceiver(this);
            owner.clear();
            model = null;
        }

        @Override public void onReceive(Context context, Intent state) {
            Activity activity = owner.get();
            if (activity == null || model == null) return;
            Intent snapshot = activity.getIntent();
            String expected = snapshot.getStringExtra(MAC);
            if (expected == null || !expected.equalsIgnoreCase(state.getStringExtra(MAC))) return;
            int level = state.getIntExtra(BATTERY, -1);
            boolean connected = state.getBooleanExtra(CONNECTED, false);
            boolean charging = state.getBooleanExtra(CHARGING, false);
            snapshot.putExtra(BATTERY, level).putExtra(CONNECTED, connected).putExtra(CHARGING, charging);
            try {
                postValue.invoke(levelData.get(model), level);
                postValue.invoke(connectionData.get(model), connected ? 2 : 0);
                setCharging.invoke(model, charging);
            } catch (ReflectiveOperationException | RuntimeException error) {
                Log.w("Y700DeviceSpace", "Could not refresh the native pencil panel", error);
            }
        }
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
