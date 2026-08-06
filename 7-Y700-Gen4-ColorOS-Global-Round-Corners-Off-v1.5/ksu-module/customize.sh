ui_print "- Installing Y700 ColorOS Global Round Corners Off v1.5"
ui_print "- Vector will be configured automatically after reboot"
ui_print "- LSPosed recommended scope: System UI (com.android.systemui) only"

PKG=dev.lorana.y700.roundcornersoff.xposed
APK="$MODPATH/Y700RoundCornersOffXposed.apk"
TMP_APK=/data/local/tmp/y700-round-corners-off.apk

# Install while KernelSU Manager is already running in a fully booted system.
# The boot service remains a fallback for restored or partially removed APKs.
cp -f "$APK" "$TMP_APK"
chmod 0644 "$TMP_APK"
pm install -r "$TMP_APK" >/dev/null 2>&1 || abort "! Failed to install round-corner hook APK"
rm -f "$TMP_APK"
ui_print "- Companion hook APK installed"

set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/Y700RoundCornersOffXposed.apk" 0 0 0644
