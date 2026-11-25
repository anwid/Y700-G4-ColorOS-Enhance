#!/system/bin/sh

PEN_PKG='dev.lorana.y700.colorospenui.xposed'
PEN_APK="$MODPATH/Y700PenControl.apk"
TMP_APK='/data/local/tmp/y700-pen-control.apk'

ui_print "- Installing Y700 ColorOS Pen Control v0.5.1"
if ! pm install -r "$PEN_APK" >/dev/null 2>&1; then
    cp -f "$PEN_APK" "$TMP_APK"
    chmod 0644 "$TMP_APK"
    pm install -r "$TMP_APK" >/dev/null 2>&1 || abort "! Failed to install pen controller APK"
    rm -f "$TMP_APK"
fi
pm grant "$PEN_PKG" android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 || \
    ui_print "! Permission will be retried automatically after reboot"

CLI=''
for CANDIDATE in /data/adb/lspd/cli /data/adb/modules/zygisk_vector/cli; do
    if [ -x "$CANDIDATE" ]; then CLI="$CANDIDATE"; break; fi
done
if [ -n "$CLI" ]; then
    "$CLI" modules enable "$PEN_PKG" >/dev/null 2>&1
    "$CLI" scope set "$PEN_PKG" system/0 com.oplus.ipemanager/0 com.oplus.healthservice/0 >/dev/null 2>&1
    ui_print "- Vector scope: system/0 + com.oplus.ipemanager/0 + com.oplus.healthservice/0"
else
    ui_print "- LSPosed: manually enable System Framework, com.oplus.ipemanager and com.oplus.healthservice"
fi

for SCRIPT in customize.sh service.sh post-fs-data.sh action.sh uninstall.sh \
    accessibility_watchdog.sh pen_game_event_proxy.sh pen_button_bridge.sh pen_proximity_bridge.sh ime_restore_once.sh; do
    set_perm "$MODPATH/$SCRIPT" 0 0 0755
done
set_perm "$PEN_APK" 0 0 0644
ui_print "- No system/vendor/HAL overlay is included"
ui_print "- Game mode disables pen and restores it on exit; no proc polling"
ui_print "- Install module 6.5 before reboot if ColorOS shows accessibility warnings"
