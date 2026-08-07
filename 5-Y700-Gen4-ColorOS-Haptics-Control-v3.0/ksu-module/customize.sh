#!/system/bin/sh

PKG=dev.lorana.y700.haptics
APK="$MODPATH/Y700HapticsControl.apk"
TMP_APK=/data/local/tmp/y700-haptics-control.apk

ui_print "- Installing Y700 ColorOS Haptics Control v3.0"
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
STOCK_CONFIG="$MODPATH/system/vendor/etc/Hapticsconfig.xml"
[ -f "$STOCK_CONFIG" ] || abort "! Missing bundled ZUXOS Hapticsconfig.xml"
EXPECTED_SHA=0a81641aa9ae68a90739d815da381d3a8e11695ad38560c9f14a94199899c5f2
ACTUAL_SHA=$(sha256sum "$STOCK_CONFIG" 2>/dev/null | awk '{print $1}')
[ "$ACTUAL_SHA" = "$EXPECTED_SHA" ] || abort "! Bundled Hapticsconfig.xml hash mismatch; refusing unsafe install"
set_perm "$STOCK_CONFIG" 0 0 0644 u:object_r:vendor_configs_file:s0
ls -Z "$STOCK_CONFIG" 2>/dev/null | grep -q 'u:object_r:vendor_configs_file:s0' \
    || abort "! Hapticsconfig.xml SELinux context is unsafe; refusing install"
ui_print "- Installing ZUXOS 1.5.10.259 stock Hapticsconfig.xml"
ui_print "- Expected SHA-256: 0A81641AA9AE68A90739D815DA381D3A8E11695AD38560C9F14A94199899C5F2"
ui_print "- ColorOS private feedback 300-308 will be translated in system_server"
ui_print "- Reboot once, then open Y700 触感控制"
