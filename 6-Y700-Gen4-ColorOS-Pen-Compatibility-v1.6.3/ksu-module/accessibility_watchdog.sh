#!/system/bin/sh

MODDIR=${0%/*}
PIDFILE="$MODDIR/accessibility_watchdog.pid"
COMPONENT='dev.lorana.y700.colorospenui.xposed/dev.lorana.y700.colorospenui.xposed.PenAccessibilityService'

if [ -r "$PIDFILE" ]; then
    OLD_PID=$(cat "$PIDFILE" 2>/dev/null)
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        OLD_CMD=$(tr '\000' ' ' < "/proc/$OLD_PID/cmdline" 2>/dev/null)
        case "$OLD_CMD" in
            *accessibility_watchdog.sh*) exit 0 ;;
        esac
    fi
fi
echo $$ > "$PIDFILE"

while [ "$(getprop sys.boot_completed)" != '1' ]; do sleep 2; done

# Never touch the audited accessibility setting unless the system_server hook
# that filters only this module's warning has positively armed this boot.
READY=0
CHECK=0
while [ "$CHECK" -lt 15 ]; do
    if grep -q 'Y700PenUI: scoped ColorOS accessibility-warning filter armed' \
        /data/adb/lspd/log/modules_*.log 2>/dev/null; then
        READY=1
        break
    fi
    sleep 2
    CHECK=$((CHECK + 1))
done
if [ "$READY" != '1' ]; then
    log -t Y700PenCompat "accessibility watchdog stopped: warning filter not armed"
    exit 0
fi

pm grant dev.lorana.y700.colorospenui.xposed android.permission.WRITE_SECURE_SETTINGS 2>/dev/null

force_rebind() {
    CURRENT=$(settings get secure enabled_accessibility_services 2>/dev/null)
    CLEAN=''
    OLD_IFS="$IFS"
    IFS=':'
    for ITEM in $CURRENT; do
        [ "$ITEM" = "$COMPONENT" ] && continue
        if [ -z "$CLEAN" ]; then CLEAN="$ITEM"; else CLEAN="$CLEAN:$ITEM"; fi
    done
    IFS="$OLD_IFS"

    if [ -z "$CLEAN" ] || [ "$CLEAN" = 'null' ]; then
        settings delete secure enabled_accessibility_services >/dev/null 2>&1
    else
        settings put secure enabled_accessibility_services "$CLEAN" 2>/dev/null
    fi
    sleep 1
    if [ -z "$CLEAN" ] || [ "$CLEAN" = 'null' ]; then
        settings put secure enabled_accessibility_services "$COMPONENT" 2>/dev/null
    else
        settings put secure enabled_accessibility_services "$CLEAN:$COMPONENT" 2>/dev/null
    fi
    settings put secure accessibility_enabled 1 2>/dev/null
    log -t Y700PenCompat "root-rebound pen accessibility service after user unlock/app stop"
}

while true; do
    CURRENT=$(settings get secure enabled_accessibility_services 2>/dev/null)
    case "$CURRENT" in
        ''|'null') UPDATED="$COMPONENT" ;;
        *"$COMPONENT"*) UPDATED="$CURRENT" ;;
        *) UPDATED="$CURRENT:$COMPONENT" ;;
    esac
    if [ "$UPDATED" != "$CURRENT" ]; then
        settings put secure enabled_accessibility_services "$UPDATED" 2>/dev/null
        log -t Y700PenCompat "root-restored pen accessibility service"
    fi
    if [ "$(settings get secure accessibility_enabled 2>/dev/null)" != '1' ]; then
        settings put secure accessibility_enabled 1 2>/dev/null
    fi

    # ColorOS reads the setting before credential-encrypted app data is ready,
    # logs "Ignoring non-encryption-aware service", and never retries.  A
    # remove/add of only our component after CE unlock makes AMS bind it.  The
    # same check also recovers it after the app/service is force-stopped.
    if [ "$(getprop sys.user.0.ce_available)" = 'true' ]; then
        if ! dumpsys activity services "$COMPONENT" 2>/dev/null | grep -q 'ServiceRecord'; then
            force_rebind
        fi
    fi
    sleep 5
done
