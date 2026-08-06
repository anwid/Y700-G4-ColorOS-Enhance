#!/system/bin/sh
PKG=dev.lorana.y700.haptics
CLI=/data/adb/lspd/cli
[ -x "$CLI" ] && "$CLI" modules disable "$PKG" >/dev/null 2>&1
pm uninstall "$PKG" >/dev/null 2>&1
# Intentionally keep y700_haptic_* settings so reinstalling restores the user's profile.
