package dev.lorana.y700.colorospenui.xposed;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class DeviceSpacePenHook {
    static final String PACKAGE = "com.heytap.mydevices";
    static final String PEN_NAME = "Lenovo Tab Pen Plus";
    static final String BATTERY_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";
    static final String CARD_ID = "y700_lenovo_tab_pen_plus";
    static final int SUPPORTED_VERSION = 1704015;
    static final String ENTITIES = "com.oplus.mydevices.domain.entities.";
    private static final String TAG = "Y700DeviceSpace";

    static void install(ClassLoader loader) {
        DeviceSpacePenRepository.install(loader);
        try {
            Class<?> activityType = loader.loadClass(
                    "com.oplus.mydevices.deviceui.devicecard.DeviceCardHomeActivity");
            Method updateCards = activityType.getDeclaredMethod("B1", List.class,
                    boolean.class, boolean.class);
            WeakHashMap<Activity, CardRefresh> refreshers = new WeakHashMap<>();
            XposedBridge.hookAllMethods(activityType, "onDestroy", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    CardRefresh refresher = refreshers.remove(activity);
                    if (refresher != null) activity.unregisterReceiver(refresher);
                }
            });
            XposedBridge.hookAllMethods(activityType, updateCards.getName(), new XC_MethodHook() {
                private boolean failed;

                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (failed) return;
                    try {
                        Activity activity = (Activity) param.thisObject;
                        if (activity.getPackageManager().getPackageInfo(PACKAGE, 0)
                                .getLongVersionCode() != SUPPORTED_VERSION) return;
                        CardRefresh refresher = refreshers.get(activity);
                        if (refresher == null) {
                            refresher = new CardRefresh(activity, updateCards);
                            IntentFilter filter = new IntentFilter(BATTERY_CHANGED);
                            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                            // Bluetooth broadcasts originate in the privileged Bluetooth process.
                            activity.registerReceiver(refresher, filter, 2); // RECEIVER_EXPORTED
                            refreshers.put(activity, refresher);
                        }
                        refresher.originalArgs = param.args.clone();
                        Class<?> wrapper = loader.loadClass("com.oplus.wrapper.bluetooth.BluetoothDevice");
                        Object pen = connectedPen(wrapper);
                        if (pen == null) return;

                        Class<?> cardType = loader.loadClass(ENTITIES + "cards.QuickCardDeviceData");
                        Method getId = cardType.getMethod("getId");
                        Method getDeviceType = cardType.getMethod("getDeviceType");
                        Object pencilType = loader.loadClass(ENTITIES + "device.DeviceType")
                                .getField("PENCIL").get(null);
                        List<?> original = (List<?>) param.args[0];
                        List<Object> cards = new ArrayList<>(original.size() + 1);
                        for (Object card : original) {
                            if (!cardType.isInstance(card)) return;
                            if (pencilType.equals(getDeviceType.invoke(card))) return;
                            String id = (String) getId.invoke(card);
                            if (!"empty_id".equals(id) && !"network_lost_id".equals(id)) {
                                cards.add(card);
                            }
                        }

                        int battery = (Integer) wrapper.getMethod("getBatteryLevel")
                                .invoke(pen);
                        cards.add(makeCard(loader, cardType, pencilType, activity, battery));
                        param.args[0] = cards;
                        param.args[1] = false;
                    } catch (PackageManager.NameNotFoundException | ReflectiveOperationException
                            | RuntimeException | LinkageError error) {
                        failed = true;
                        Log.w(TAG, "Could not bind the native pencil card", error);
                    }
                }
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            Log.w(TAG, "Unsupported Device Space card entry point", error);
        }
    }

    private static final class CardRefresh extends BroadcastReceiver {
        private final WeakReference<Activity> activityRef;
        private final Method updateCards;
        private Object[] originalArgs;

        CardRefresh(Activity activity, Method updateCards) {
            this.activityRef = new WeakReference<>(activity);
            this.updateCards = updateCards;
        }

        @Override public void onReceive(Context context, Intent intent) {
            Activity activity = activityRef.get();
            if (activity == null || activity.isDestroyed() || originalArgs == null) return;
            try {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null || !PEN_NAME.equals(device.getName())) return;
                updateCards.invoke(activity, originalArgs);
            } catch (ReflectiveOperationException | RuntimeException error) {
                Log.w(TAG, "Could not refresh the pencil card", error);
            }
        }
    }

    private static Object connectedPen(Class<?> wrapper) throws ReflectiveOperationException {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) return null;
        Method isConnected = wrapper.getMethod("isConnected");
        Constructor<?> create = wrapper.getConstructor(BluetoothDevice.class);
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            if (!PEN_NAME.equals(device.getName())) continue;
            Object candidate = create.newInstance(device);
            if (Boolean.TRUE.equals(isConnected.invoke(candidate))) return candidate;
        }
        return null;
    }

    private static Object makeCard(ClassLoader loader, Class<?> cardType, Object pencilType,
            Activity activity, int level) throws ReflectiveOperationException {
        Class<?> batteryType = loader.loadClass(ENTITIES + "device.BatteryType");
        Class<?> batteryInfo = loader.loadClass(ENTITIES + "device.BatteryInfo");
        Class<?> deviceStatus = loader.loadClass(ENTITIES + "plugin.DeviceStatus");
        Class<?> abnormalState = loader.loadClass(ENTITIES + "cards.DeviceAbnormalState");
        Class<?> connectState = loader.loadClass(ENTITIES + "device.ConnectState");
        Class<?> displayState = loader.loadClass(ENTITIES + "cards.DeviceCardDisplayState");

        boolean hasBattery = level >= 0 && level <= 100;
        Object battery = hasBattery
                ? batteryInfo.getConstructor(batteryType, int.class, boolean.class)
                        .newInstance(batteryType.getField("SINGLE").get(null), level, false)
                : null;
        int iconId = activity.getResources().getIdentifier("icon_pencil", "drawable", PACKAGE);
        String icon = iconId == 0 ? "" : new Uri.Builder().scheme("android.resource")
                .authority(PACKAGE).appendPath(Integer.toString(iconId)).build().toString();
        Constructor<?> create = cardType.getConstructor(String.class, String.class, String.class,
                String.class, String.class, deviceStatus, deviceStatus, deviceStatus, String.class,
                List.class, String.class, boolean.class, boolean.class, boolean.class,
                batteryInfo, batteryInfo, batteryInfo, batteryInfo, Boolean.class, boolean.class,
                String.class, abnormalState, String.class, boolean.class, String.class);
        Object card = create.newInstance(CARD_ID, "OPPO Pencil", icon, "", "CONNECTED",
                null, null, null, "", Collections.emptyList(), "PENCIL", hasBattery, !hasBattery,
                true, null, null, battery, battery, hasBattery, false, "",
                abnormalState.getField("NONE").get(null), "", false, null);

        Class<?> deviceType = pencilType.getClass();
        cardType.getMethod("setDeviceType", deviceType).invoke(card, pencilType);
        Object companion = displayState.getField("Companion").get(null);
        Object state = companion.getClass().getMethod("fromDeviceData", deviceType, boolean.class,
                connectState, boolean.class).invoke(companion, pencilType, false,
                        connectState.getField("CONNECTED").get(null), hasBattery);
        cardType.getMethod("setDisplayState", displayState).invoke(card, state);
        cardType.getMethod("setOriginalIconUri", String.class).invoke(card, icon);
        return card;
    }
}
