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
# Vector's private log directory is not readable from KernelSU's service
# domain on this ColorOS build.  Wait for the dedicated whitelist module to be
# enabled and scoped to system_server, then give Vector time to finish its
# boot-time injection.  Do not give up after a fixed window: a permanent exit
# here would leave the service disabled until the next reboot.
VECTOR_CLI=/data/adb/lspd/cli
while true; do
    if pm path dev.lorana.coloros.fraudwhitelist >/dev/null 2>&1 \
        && [ -x "$VECTOR_CLI" ] \
        && "$VECTOR_CLI" modules ls 2>/dev/null \
            | grep -q '^dev\.lorana\.coloros\.fraudwhitelist .*enabled' \
        && "$VECTOR_CLI" scope ls dev.lorana.coloros.fraudwhitelist 2>/dev/null \
            | grep -q '^system[[:space:]]'; then
        sleep 10
        break
    fi
    sleep 5
done
log -t Y700PenCompat "accessibility watchdog armed after whitelist scope confirmation"

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
