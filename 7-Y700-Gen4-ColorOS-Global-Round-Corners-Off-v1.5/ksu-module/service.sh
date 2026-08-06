#!/system/bin/sh

MODDIR=${0%/*}
PKG=dev.lorana.y700.roundcornersoff.xposed
APK="$MODDIR/Y700RoundCornersOffXposed.apk"
LOG="$MODDIR/service.log"
TMP_APK=/data/local/tmp/y700-round-corners-off.apk

exec >>"$LOG" 2>&1
echo "[$(date)] service start"

i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ "$i" -lt 90 ]; do
    sleep 1
    i=$((i + 1))
done

# ColorOS may publish sys.boot_completed before PackageManager accepts APK
# install transactions. Wait for its binder service before first installation.
i=0
while ! pm path android >/dev/null 2>&1 && [ "$i" -lt 60 ]; do
    sleep 1
    i=$((i + 1))
done

current_version=$(dumpsys package "$PKG" 2>/dev/null | sed -n 's/.*versionCode=\([0-9][0-9]*\).*/\1/p' | head -n 1)
if [ "$current_version" != "4" ]; then
    echo "Installing hook APK (installed version: ${current_version:-none})"
    cp -f "$APK" "$TMP_APK"
    chmod 0644 "$TMP_APK"
    i=0
    while ! pm install -r "$TMP_APK" && [ "$i" -lt 30 ]; do
        i=$((i + 1))
        echo "PackageManager install not ready; retry $i/30"
        sleep 2
    done
    rm -f "$TMP_APK"
fi

if ! pm path "$PKG" >/dev/null 2>&1; then
    echo "Hook APK is still unavailable; skipping Vector configuration"
    exit 1
fi

CLI=/data/adb/lspd/cli
if [ -x "$CLI" ]; then
    i=0
    while ! "$CLI" status >/dev/null 2>&1 && [ "$i" -lt 30 ]; do
        sleep 1
        i=$((i + 1))
    done
    if "$CLI" status >/dev/null 2>&1; then
        "$CLI" modules enable "$PKG"
        "$CLI" scope set "$PKG" com.android.systemui/0
        echo "Vector module enabled with SystemUI-only scope"
        killall com.android.systemui 2>/dev/null
    else
        echo "Vector CLI found but daemon was unavailable"
    fi
else
    echo "No Vector CLI found; enable and scope the module to SystemUI in LSPosed manually"
fi
