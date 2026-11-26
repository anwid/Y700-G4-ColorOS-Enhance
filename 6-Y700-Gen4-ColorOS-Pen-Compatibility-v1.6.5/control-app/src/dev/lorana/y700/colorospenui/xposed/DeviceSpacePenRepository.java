package dev.lorana.y700.colorospenui.xposed;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class DeviceSpacePenRepository {
    private static final String TAG = "Y700DeviceSpace";
    private static final String DEVICE = DeviceSpacePenHook.ENTITIES + "device.";
    private static boolean registered;
    private static volatile Context applicationContext;
    private static volatile Object nativeDeviceObserver;
    private static volatile Object sharedDao;

    static void install(ClassLoader loader) {
        try {
            Class<?> cardAction = loader.loadClass("com.oplus.mydevices.quickapp.action.QuickAppCardAction");
            cardAction.getDeclaredMethod("v", String.class);
            XposedBridge.hookAllMethods(cardAction, "v", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    Context context = applicationContext;
                    if (context == null || param.args.length != 1
                            || !DeviceSpacePenHook.CARD_ID.equals(param.args[0])) return;
                    openNativePanel(context);
                    param.setResult(new Bundle());
                }
            });
            Class<?> detail = loader.loadClass("com.oplus.mydevices.bluetooth.BlueToothDetailActivity");
            XC_MethodHook redirectDetail = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    Intent intent = param.args[0] instanceof Intent
                            ? (Intent) param.args[0] : activity.getIntent();
                    if (intent != null && DeviceSpacePenHook.CARD_ID.equals(
                            intent.getStringExtra("device_id"))) {
                        openNativePanel(activity);
                        activity.finish();
                    }
                }
            };
            XposedBridge.hookAllMethods(detail, "onCreate", redirectDetail);
            XposedBridge.hookAllMethods(detail, "onNewIntent", redirectDetail);

            Class<?> manager = loader.loadClass("com.heytap.mydevices.core.device.DevicesManager");
            Method observer = manager.getMethod("r0");
            XposedBridge.hookAllMethods(manager, "create", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!param.hasThrowable()) nativeDeviceObserver = observer.invoke(param.thisObject);
                }
            });
            Class<?> daoType = loader.loadClass("com.heytap.mydevices.core.persist.DevicesDao");
            XposedBridge.hookAllConstructors(daoType, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.hasThrowable() || param.args.length != 1
                            || !(param.args[0] instanceof Context)) return;
                    Context context = (Context) param.args[0];
                    register(context, loader, daoType, param.thisObject);
                }
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            Log.w(TAG, "Unsupported shared device repository", error);
        }
    }

    private static void openNativePanel(Context context) {
        try {
            Object dao = sharedDao;
            Object device = dao == null ? null : dao.getClass().getMethod("c", String.class)
                    .invoke(dao, DeviceSpacePenHook.CARD_ID);
            if (device == null) throw new IllegalStateException("Pencil record is unavailable");
            Class<?> type = device.getClass();
            String address = (String) type.getMethod("getMac").invoke(device);
            if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                throw new IllegalStateException("Pencil address is unavailable");
            }
            int level = -1;
            boolean charging = false;
            List<?> batteries = (List<?>) type.getMethod("getBatteryList").invoke(device);
            if (!batteries.isEmpty()) {
                Object battery = batteries.get(0);
                level = (Integer) battery.getClass().getMethod("getValue").invoke(battery);
                charging = (Boolean) battery.getClass().getMethod("getCharge").invoke(battery);
            }
            boolean connected = (Boolean) type.getMethod("isConnected").invoke(device);
            Intent panel = new Intent(NativePenPanelHook.ACTION)
                    .setClassName(NativePenPanelHook.PACKAGE, NativePenPanelHook.PANEL)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(NativePenPanelHook.BRIDGE, true)
                    .putExtra(NativePenPanelHook.MAC, address)
                    .putExtra(NativePenPanelHook.BATTERY, level)
                    .putExtra(NativePenPanelHook.CHARGING, charging)
                    .putExtra(NativePenPanelHook.CONNECTED, connected);
            context.startActivity(panel);
        } catch (ReflectiveOperationException | RuntimeException error) {
            Log.w(TAG, "Could not open the native pencil panel", error);
            new Handler(context.getMainLooper()).post(() ->
                    Toast.makeText(context, "暂时无法打开原生手写笔卡片", Toast.LENGTH_SHORT).show());
        }
    }

    private static synchronized void register(Context context, ClassLoader loader,
            Class<?> daoType, Object dao) {
        if (registered) return;
        try {
            if (context.getPackageManager().getPackageInfo(DeviceSpacePenHook.PACKAGE, 0)
                    .getLongVersionCode() != DeviceSpacePenHook.SUPPORTED_VERSION) return;
            applicationContext = context.getApplicationContext();
            sharedDao = dao;
            if (!DeviceSpacePenHook.PACKAGE.equals(Application.getProcessName())) {
                registered = true;
                return;
            }
            PenRecord record = new PenRecord(loader, daoType, dao);
            HandlerThread worker = new HandlerThread("Y700PenRepository");
            worker.start();
            Handler handler = new Handler(worker.getLooper());
            record.chargeMonitor = new PenChargeMonitor(applicationContext, handler, record::sync);
            IntentFilter filter = new IntentFilter(DeviceSpacePenHook.BATTERY_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            try {
                context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
                    @Override public void onReceive(Context receiverContext, Intent intent) {
                        try {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())
                                    || (device != null && DeviceSpacePenHook.PEN_NAME.equals(device.getName()))) {
                                record.sync();
                            }
                        } catch (RuntimeException error) {
                            Log.w(TAG, "Could not read Bluetooth change", error);
                        }
                    }
                }, filter, null, handler, 2); // RECEIVER_EXPORTED
            } catch (RuntimeException error) {
                worker.quitSafely();
                throw error;
            }
            registered = true;
            handler.post(record::sync);
        } catch (Exception | LinkageError error) {
            Log.w(TAG, "Could not initialize the shared pencil record", error);
        }
    }

    private static final class PenRecord {
        private PenChargeMonitor chargeMonitor;
        private Object lastPublished;
        private final Object dao;
        private final Class<?> infoType;
        private final Class<?> statusType;
        private final Constructor<?> createDevice;
        private final Constructor<?> createBattery;
        private final Constructor<?> wrapDevice;
        private final Method isConnected;
        private final Method getBatteryLevel;
        private final Method find;
        private final Method insert;
        private final Method update;
        private final Object connected;
        private final Object disconnected;
        private final Object pencil;
        private final Object source;
        private final Object singleBattery;

        PenRecord(ClassLoader loader, Class<?> daoType, Object dao)
                throws ReflectiveOperationException {
            this.dao = dao;
            infoType = loader.loadClass(DEVICE + "DeviceInfo");
            statusType = loader.loadClass(DEVICE + "DeviceInfo$DeviceStatusData");
            Class<?> connectType = loader.loadClass(DEVICE + "ConnectState");
            Class<?> deviceType = loader.loadClass(DEVICE + "DeviceType");
            Class<?> sourceType = loader.loadClass(DEVICE + "SourceType");
            Class<?> batteryType = loader.loadClass(DEVICE + "BatteryType");
            Class<?> batteryInfo = loader.loadClass(DEVICE + "BatteryInfo");
            Class<?> wrapper = loader.loadClass("com.oplus.wrapper.bluetooth.BluetoothDevice");
            Class<?> daoDevice = daoType.getMethod("c", String.class).getReturnType();
            find = daoType.getMethod("c", String.class);
            insert = daoType.getMethod("f", daoDevice);
            update = daoType.getMethod("h", daoDevice);
            wrapDevice = wrapper.getConstructor(BluetoothDevice.class);
            isConnected = wrapper.getMethod("isConnected");
            getBatteryLevel = wrapper.getMethod("getBatteryLevel");
            createBattery = batteryInfo.getConstructor(batteryType, int.class, boolean.class);
            connected = connectType.getField("CONNECTED").get(null);
            disconnected = connectType.getField("DISCONNECTED").get(null);
            pencil = deviceType.getField("PENCIL").get(null);
            source = sourceType.getField("BLUETOOTH").get(null);
            singleBattery = batteryType.getField("SINGLE").get(null);
            createDevice = infoType.getConstructor(
                    String.class, String.class, String.class, String.class, String.class,
                    String.class, String.class, String.class, String.class, String.class,
                    boolean.class, boolean.class, connectType, long.class, long.class, boolean.class,
                    String.class, String.class, String.class, String.class, String.class, String.class,
                    String.class, int.class, List.class, loader.loadClass(DEVICE + "EventMessage"),
                    Map.class, List.class, statusType, Integer.class, int.class, int.class, int.class,
                    boolean.class, int.class, deviceType, sourceType,
                    loader.loadClass(DEVICE + "DeviceInfo$CardData"), Integer.class,
                    loader.loadClass(DEVICE + "DeviceEventMessage"), boolean.class, boolean.class);
        }

        void sync() {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice pen = null;
                if (adapter != null && adapter.isEnabled()) {
                    for (BluetoothDevice device : adapter.getBondedDevices()) {
                        if (DeviceSpacePenHook.PEN_NAME.equals(device.getName())) {
                            pen = device;
                            break;
                        }
                    }
                }
                Object previous = find.invoke(dao, DeviceSpacePenHook.CARD_ID);
                Object wrapper = pen == null ? null : wrapDevice.newInstance(pen);
                boolean online = wrapper != null && Boolean.TRUE.equals(isConnected.invoke(wrapper));
                chargeMonitor.watch(online ? pen : null);
                if (previous == null && !online) return;

                int level = online ? (Integer) getBatteryLevel.invoke(wrapper) : -1;
                Object status = statusType.getConstructor().newInstance();
                List<?> batteries = level >= 0 && level <= 100
                        ? Collections.singletonList(createBattery.newInstance(singleBattery, level, chargeMonitor.isCharging()))
                        : Collections.emptyList();
                statusType.getMethod("setBatteryList", List.class).invoke(status, batteries);
                long created = previous == null ? System.currentTimeMillis()
                        : (Long) infoType.getMethod("getCreateTime").invoke(previous);
                String address = pen == null ? (String) infoType.getMethod("getMac").invoke(previous)
                        : pen.getAddress();
                Object device = createDevice.newInstance(
                        DeviceSpacePenHook.CARD_ID, "OPPO Pencil", "", null, "", "",
                        null, address, null, "pencil", online, true, online ? connected : disconnected,
                        created, created, false, null, DeviceSpacePenHook.PACKAGE, null, null,
                        null, null, null, 7, Collections.emptyList(), null, null,
                        Collections.emptyList(), status, null, 0, 0, 0, false, 0,
                        pencil, source, null, null, null, false, false);
                boolean changed = Boolean.TRUE.equals(
                        (previous == null ? insert : update).invoke(dao, device));
                Object observer = nativeDeviceObserver;
                if (observer != null && !device.equals(lastPublished)) {
                    // Reuse the platform device update callback to update its in-memory panel cache.
                    observer.getClass().getMethod("b", infoType).invoke(observer, device);
                    lastPublished = device;
                }
                if (changed) {
                    applicationContext.sendBroadcast(new Intent(NativePenPanelHook.STATE_CHANGED)
                            .setPackage(NativePenPanelHook.PACKAGE)
                            .putExtra(NativePenPanelHook.MAC, address)
                            .putExtra(NativePenPanelHook.BATTERY, level)
                            .putExtra(NativePenPanelHook.CONNECTED, online)
                            .putExtra(NativePenPanelHook.CHARGING, chargeMonitor.isCharging()),
                            NativePenPanelHook.STATE_PERMISSION);
                    Log.i(TAG, "Published shared pencil record (connected=" + online + ", battery=" + level + ")");
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                Log.w(TAG, "Could not publish shared pencil record", error);
            }
        }
    }
}
