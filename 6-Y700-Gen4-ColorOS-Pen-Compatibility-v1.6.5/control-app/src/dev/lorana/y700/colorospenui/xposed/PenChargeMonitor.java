package dev.lorana.y700.colorospenui.xposed;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import java.util.UUID;

final class PenChargeMonitor extends BluetoothGattCallback {
    private static final UUID BATTERY_SERVICE = uuid("180f");
    private static final UUID POWER_STATE = uuid("2a1a");
    private static final UUID CLIENT_CONFIG = uuid("2902");
    private static final String TAG = "Y700DeviceSpace";

    private final Context context;
    private final Handler handler;
    private final Runnable onChanged;
    private BluetoothGatt gatt;
    private boolean charging;
    private boolean subscribed;

    PenChargeMonitor(Context context, Handler handler, Runnable onChanged) {
        this.context = context;
        this.handler = handler;
        this.onChanged = onChanged;
    }

    boolean isCharging() {
        return charging;
    }

    void watch(BluetoothDevice pen) {
        if (gatt != null && gatt.getDevice().equals(pen)) return;
        close();
        if (pen != null) {
            gatt = pen.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_1M_MASK, handler);
        }
    }

    private void close() {
        BluetoothGatt old = gatt;
        gatt = null;
        charging = false;
        subscribed = false;
        if (old != null) old.close();
    }

    @Override public void onConnectionStateChange(BluetoothGatt client, int status, int state) {
        if (client != gatt) return;
        if (status == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED
                && client.discoverServices()) return;
        boolean wasCharging = charging;
        close();
        if (wasCharging) onChanged.run();
    }

    @Override public void onServicesDiscovered(BluetoothGatt client, int status) {
        if (client != gatt) return;
        BluetoothGattService service = client.getService(BATTERY_SERVICE);
        BluetoothGattCharacteristic power = service == null ? null : service.getCharacteristic(POWER_STATE);
        if (status != BluetoothGatt.GATT_SUCCESS || power == null) {
            close();
            return;
        }
        if (!client.readCharacteristic(power)) {
            subscribed = true;
            subscribe(client, power);
        }
    }

    private void subscribe(BluetoothGatt client, BluetoothGattCharacteristic power) {
        BluetoothGattDescriptor config = power.getDescriptor(CLIENT_CONFIG);
        if (config != null && client.setCharacteristicNotification(power, true)) {
            config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            client.writeDescriptor(config);
        }
    }

    @Override public void onCharacteristicRead(BluetoothGatt client,
            BluetoothGattCharacteristic characteristic, int status) {
        onCharacteristicRead(client, characteristic, characteristic.getValue(), status);
    }

    // Android 13+ can deliver the value through the immutable callback overload.
    public void onCharacteristicRead(BluetoothGatt client,
            BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        if (client != gatt || !POWER_STATE.equals(characteristic.getUuid())) return;
        if (status == BluetoothGatt.GATT_SUCCESS) accept(value);
        else Log.w(TAG, "Could not read pen power state (GATT status " + status + ")");
        if (!subscribed) {
            subscribed = true;
            subscribe(client, characteristic);
        }
    }

    @Override public void onCharacteristicChanged(BluetoothGatt client, BluetoothGattCharacteristic characteristic) {
        onCharacteristicChanged(client, characteristic, characteristic.getValue());
    }

    public void onCharacteristicChanged(BluetoothGatt client, BluetoothGattCharacteristic characteristic,
            byte[] value) {
        if (client == gatt && POWER_STATE.equals(characteristic.getUuid())) accept(value);
    }

    private void accept(byte[] value) {
        // The Lenovo pen pads the one-byte power state with two zero bytes.
        if (value == null || (value.length != 1
                && (value.length != 3 || value[1] != 0 || value[2] != 0))) return;
        // Battery Power State bits 5–4 encode whether the pen is charging.
        boolean updated = (value[0] & 0x30) == 0x30;
        Log.i(TAG, "Pen power state 0x" + Integer.toHexString(value[0] & 0xff) + ", charging=" + updated);
        if (updated == charging) return;
        charging = updated;
        onChanged.run();
    }

    private static UUID uuid(String shortId) {
        return UUID.fromString("0000" + shortId + "-0000-1000-8000-00805f9b34fb");
    }
}
