#!/system/bin/sh

MODDIR=${0%/*}
PKG=dev.lorana.y700.haptics
APK="$MODDIR/Y700HapticsControl.apk"
LOG="$MODDIR/service.log"
TMP_APK=/data/local/tmp/y700-haptics-control.apk

exec >>"$LOG" 2>&1
echo "[$(date)] service start"

i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ "$i" -lt 90 ]; do
    sleep 1
    i=$((i + 1))
done

# Some ColorOS ports publish sys.boot_completed before PackageManager accepts
# binder transactions. Wait for the package service instead of racing it.
i=0
while ! pm path android >/dev/null 2>&1 && [ "$i" -lt 60 ]; do
    sleep 1
    i=$((i + 1))
done

if ! pm path "$PKG" >/dev/null 2>&1; then
    echo "APK missing; reinstalling from the KSU module"
    if ! pm install -r "$APK"; then
        cp -f "$APK" "$TMP_APK"
        chmod 0644 "$TMP_APK"
        pm install -r "$TMP_APK"
        rm -f "$TMP_APK"
    fi
    pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1
fi

migrate_category() {
    source=$1
    target=$2
    for item in enabled:1 duration:10 amplitude:160 mode:0 primitive_ids:1,7 primitive_scales:1.0,0.6 primitive_delays:0,40 segment_durations:25,25; do
        suffix=${item%%:*}
        fallback=${item#*:}
        value=$(settings get global "y700_haptic_${source}_$suffix")
        [ -z "$value" ] || [ "$value" = "null" ] && value=$fallback
        settings put global "y700_haptic_${target}_$suffix" "$value"
    done
}

schema=$(settings get global y700_haptic_schema)
if [ -z "$schema" ] || [ "$schema" = "null" ]; then
    # Leave a genuinely fresh install at schema 0. The companion app will
    # atomically write the complete per-category defaults on its first launch.
    echo "Fresh configuration detected; app will initialize bundled defaults"
elif [ "$schema" -lt 2 ] 2>/dev/null; then
    migrate_category touch gesture
    migrate_category touch lockscreen
    settings put global y700_haptic_schema 2
    schema=2
    echo "Migrated legacy TOUCH settings into gesture and lockscreen categories"
fi
if [ "$schema" -lt 3 ] 2>/dev/null; then
    migrate_category gesture back
    migrate_category gesture multitask
    settings put global y700_haptic_schema 3
    echo "Split legacy gesture settings into back and launcher/multitask categories"
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
        "$CLI" scope set "$PKG" system/0
        echo "Vector module enabled with system/0 scope"
    fi
fi
