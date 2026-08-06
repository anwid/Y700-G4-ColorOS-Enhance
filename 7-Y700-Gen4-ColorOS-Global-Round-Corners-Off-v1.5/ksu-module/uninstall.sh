#!/system/bin/sh

PKG=dev.lorana.y700.roundcornersoff.xposed
CLI=/data/adb/lspd/cli

if [ -x "$CLI" ]; then
    "$CLI" modules disable "$PKG" >/dev/null 2>&1
fi
pm uninstall "$PKG" >/dev/null 2>&1
