#!/system/bin/sh

PEN_PKG='dev.lorana.y700.colorospenui.xposed'
PEN_APK="$MODPATH/system/app/$PEN_PKG.apk"
[ -s "$PEN_APK" ] || abort "! Missing system app APK"
ui_print "- Mounting Y700 ColorOS Pen Control at /system/app/$PEN_PKG.apk after reboot"

# Keep a same-signed data app update so the control app survives module removal.
if ! pm install -r "$PEN_APK" >/dev/null 2>&1; then
    TMP_APK=$(mktemp /data/local/tmp/y700-pen-control.XXXXXX.apk) || abort "! Could not stage pen controller APK"
    cp -f "$PEN_APK" "$TMP_APK" && chmod 0644 "$TMP_APK" && \
        pm install -r "$TMP_APK" >/dev/null 2>&1
    INSTALL_STATUS=$?
    rm -f "$TMP_APK"
    [ "$INSTALL_STATUS" -eq 0 ] || abort "! Failed to install the controller's data app copy"
fi
pm grant "$PEN_PKG" android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 || \
    ui_print "! Permission will be retried automatically after reboot"

CLI=''
for CANDIDATE in /data/adb/lspd/cli /data/adb/modules/zygisk_vector/cli; do
    if [ -x "$CANDIDATE" ]; then CLI="$CANDIDATE"; break; fi
done
if [ -n "$CLI" ]; then
    "$CLI" modules enable "$PEN_PKG" >/dev/null 2>&1
    "$CLI" scope set "$PEN_PKG" system/0 com.oplus.ipemanager/0 com.oplus.healthservice/0 com.google.android.inputmethod.latin/0 com.heytap.mydevices/0 >/dev/null 2>&1
    ui_print "- Vector scope: system/0 + com.oplus.ipemanager/0 + com.oplus.healthservice/0 + com.google.android.inputmethod.latin/0 + com.heytap.mydevices/0"
else
    ui_print "- LSPosed: manually enable System Framework, com.oplus.ipemanager, com.oplus.healthservice, Gboard and com.heytap.mydevices"
fi

for SCRIPT in customize.sh service.sh post-fs-data.sh action.sh uninstall.sh \
    accessibility_watchdog.sh pen_game_event_proxy.sh pen_proximity_bridge.sh; do
    set_perm "$MODPATH/$SCRIPT" 0 0 0755
done
set_perm_recursive "$MODPATH/system" 0 0 0755 0644
ui_print "- Game mode disables pen and restores it on exit; no proc polling"
ui_print "- Install module 6.5 before reboot if ColorOS shows accessibility warnings"
