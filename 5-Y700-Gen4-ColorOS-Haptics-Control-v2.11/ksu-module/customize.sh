#!/system/bin/sh

PKG=dev.lorana.y700.haptics
APK="$MODPATH/Y700HapticsControl.apk"
TMP_APK=/data/local/tmp/y700-haptics-control.apk

ui_print "- Installing Y700 ColorOS Haptics Control v2.11"
ui_print "- Installing companion APK without incremental storage"

if ! pm install -r "$APK" >/dev/null 2>&1; then
    cp -f "$APK" "$TMP_APK"
    chmod 0644 "$TMP_APK"
    pm install -r "$TMP_APK" >/dev/null 2>&1 || abort "! Failed to install haptics controller APK"
    rm -f "$TMP_APK"
fi

pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 || abort "! Failed to grant WRITE_SECURE_SETTINGS"

CLI=/data/adb/lspd/cli
if [ -x "$CLI" ]; then
    "$CLI" modules enable "$PKG" >/dev/null 2>&1
    "$CLI" scope set "$PKG" system/0 >/dev/null 2>&1
    ui_print "- Vector enabled; scope set to system/0"
else
    ui_print "- LSPosed users: enable the module for System Framework manually"
fi

set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$APK" 0 0 0644
rm -f "$MODPATH/system/vendor/etc/Hapticsconfig.xml"
ui_print "- Using the ROM's stock Hapticsconfig.xml; no vendor gain or voltage override"
ui_print "- Reboot once, then open Y700 触感控制"
