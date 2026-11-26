package dev.lorana.y700.colorospenui.xposed;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class IpePenConnectionHook {
    private static final String STATE = "ipe_pencil_connect_state";
    private static final String OWNED = "owns_connection_state";
    private static final String TAG = "Y700IpeConnection";

    static void install(ClassLoader loader) {
        try {
            Class<?> service = loader.loadClass("com.oplus.ipemanager.btadsorb.CoreService");
            Method isConnected = loader.loadClass("h3.t").getMethod("K", BluetoothDevice.class);
            Method write = loader.loadClass("e2.b").getMethod("a", int.class, String.class);
            WeakHashMap<Service, ConnectionObserver> observers = new WeakHashMap<>();

            XposedBridge.hookAllMethods(service, "onCreate", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.hasThrowable()) return;
                    Service instance = (Service) param.thisObject;
                    ConnectionObserver observer = new ConnectionObserver(isConnected, write);
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
                    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                    try {
                        instance.registerReceiver(observer, filter, null, null, 2); // RECEIVER_EXPORTED
                        observers.put(instance, observer);
                        observer.refresh(instance);
                    } catch (RuntimeException error) {
                        Log.w(TAG, "Could not register the pen connection listener", error);
                    }
                }
            });
            XposedBridge.hookAllMethods(service, "onDestroy", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    Service instance = (Service) param.thisObject;
                    ConnectionObserver observer = observers.remove(instance);
                    if (observer != null) instance.unregisterReceiver(observer);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            Log.w(TAG, "Unsupported Intelligent Peripherals connection service", error);
        }
    }

    private static final class ConnectionObserver extends BroadcastReceiver {
        private final Method isConnected;
        private final Method write;

        ConnectionObserver(Method isConnected, Method write) {
            this.isConnected = isConnected;
            this.write = write;
        }

        @Override public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())
                    || (device != null && DeviceSpacePenHook.PEN_NAME.equals(device.getName()))) {
                refresh(context);
            }
        }

        void refresh(Context context) {
            try {
                boolean connected = isLenovoPenConnected();
                SharedPreferences prefs = context.getSharedPreferences(
                        "y700_pen_connection_bridge", Context.MODE_PRIVATE);
                boolean owned = prefs.getBoolean(OWNED, false);
                int state = Settings.Global.getInt(context.getContentResolver(), STATE, 0);
                if (connected && state != 2) {
                    write.invoke(null, 2, STATE);
                    if (Settings.Global.getInt(context.getContentResolver(), STATE, 0) == 2) {
                        prefs.edit().putBoolean(OWNED, true).apply();
                        Log.i(TAG, "Marked the connected Lenovo pen for ColorOS");
                    } else {
                        Log.w(TAG, "ColorOS did not save the pen connection state");
                    }
                } else if (!connected && owned) {
                    if (state == 2) write.invoke(null, 0, STATE);
                    if (Settings.Global.getInt(context.getContentResolver(), STATE, 0) != 2) {
                        prefs.edit().remove(OWNED).apply();
                        Log.i(TAG, "Cleared the Lenovo pen connection state");
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException error) {
                Log.w(TAG, "Could not synchronize the pen connection state", error);
            }
        }

        private boolean isLenovoPenConnected() throws ReflectiveOperationException {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) return false;
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                if (!DeviceSpacePenHook.PEN_NAME.equals(device.getName())) continue;
                if (Boolean.TRUE.equals(isConnected.invoke(null, device))) return true;
            }
            return false;
        }
    }
}
