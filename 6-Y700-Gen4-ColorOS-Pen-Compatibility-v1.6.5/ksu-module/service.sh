#!/system/bin/sh

MODDIR=${0%/*}
TAG=Y700PenCompat

log -t "$TAG" "integrated service start"

if [ -x "$MODDIR/accessibility_watchdog.sh" ]; then
    nohup "$MODDIR/accessibility_watchdog.sh" >/dev/null 2>&1 &
fi

# Establish the non-game default once.  Subsequent game enter/exit transitions
# are handled event-by-event by the system_server hook, never by polling.
if [ -e /proc/support_pen ]; then
    printf '1\n' > /proc/support_pen 2>/dev/null
fi

if [ -x "$MODDIR/pen_game_event_proxy.sh" ]; then
    nohup "$MODDIR/pen_game_event_proxy.sh" >/dev/null 2>&1 &
fi

# Start input bridges immediately.  Each bridge validates both its saved PID
# and process command line, so unrelated PID reuse cannot suppress startup.
if [ -x "$MODDIR/pen_proximity_bridge.sh" ]; then
    nohup "$MODDIR/pen_proximity_bridge.sh" >/dev/null 2>&1 &
fi

# Finish package permissions and Vector scope setup after PackageManager is
# ready.  This is deliberately backgrounded so pen input is never held up by
# framework startup or by a temporarily unavailable Xposed manager.
(
    while [ "$(getprop sys.boot_completed)" != '1' ]; do sleep 2; done

    PEN_PKG='dev.lorana.y700.colorospenui.xposed'
    FRAUD_PKG='dev.lorana.coloros.fraudwhitelist'
    pm grant "$PEN_PKG" android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1
    if pm path "$FRAUD_PKG" >/dev/null 2>&1; then
        pm grant "$FRAUD_PKG" android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1
    fi

    # The ROM HAL uses the legacy key for its own fixed-delay IME restore.
    # Migrate the selected IME before clearing that key so only this module restores it.
    NORMAL=$(settings get global y700_pen_module_normal_ime 2>/dev/null)
    case "$NORMAL" in
        ''|null)
            NORMAL=$(settings get global y700_pen_normal_ime 2>/dev/null)
            case "$NORMAL" in
                ''|null) NORMAL='com.baidu.input/.ImeService' ;;
            esac
            settings put global y700_pen_module_normal_ime "$NORMAL"
            ;;
    esac
    if [ "$(settings get global y700_pen_module_normal_ime 2>/dev/null)" = "$NORMAL" ]; then
        settings delete global y700_pen_normal_ime
    fi
    [ "$(settings get global y700_pen_handwriting_ime 2>/dev/null)" != 'null' ] || \
        settings put global y700_pen_handwriting_ime 'com.sohu.inputmethod.sogouoem/.SogouIME'
    # Public builds do not expose the unreliable "pen inside ColorOS game
    # mode" experiment.  Clear stale values left by earlier test builds.
    settings put global y700_pen_in_game 0

    CLI=''
    for CANDIDATE in /data/adb/lspd/cli /data/adb/modules/zygisk_vector/cli; do
        if [ -x "$CANDIDATE" ]; then CLI="$CANDIDATE"; break; fi
    done
    if [ -n "$CLI" ]; then
        "$CLI" modules enable "$PEN_PKG" >/dev/null 2>&1
        "$CLI" scope set "$PEN_PKG" system/0 com.oplus.ipemanager/0 com.oplus.healthservice/0 com.google.android.inputmethod.latin/0 >/dev/null 2>&1
        if pm path "$FRAUD_PKG" >/dev/null 2>&1; then
            "$CLI" modules enable "$FRAUD_PKG" >/dev/null 2>&1
            "$CLI" scope set "$FRAUD_PKG" system/0 >/dev/null 2>&1
        fi
    fi
) >/dev/null 2>&1 &

log -t "$TAG" "integrated service complete"
