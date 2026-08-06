#!/system/bin/sh

MODDIR=${0%/*}
PIDFILE="$MODDIR/pen_game_event_proxy.pid"
TAG=Y700PenGame

if [ -r "$PIDFILE" ]; then
    OLD_PID=$(cat "$PIDFILE" 2>/dev/null)
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        OLD_CMD=$(tr '\000' ' ' < "/proc/$OLD_PID/cmdline" 2>/dev/null)
        case "$OLD_CMD" in
            *pen_game_event_proxy.sh*) exit 0 ;;
        esac
    fi
fi
echo $$ > "$PIDFILE"

while [ "$(getprop sys.boot_completed)" != '1' ]; do sleep 1; done

apply_state() {
    STATE="$1"
    REASON="$2"
    if printf '%s\n' "$STATE" > /proc/support_pen 2>/dev/null; then
        settings put global y700_pen_daemon_applied "$STATE" 2>/dev/null
        settings put global y700_pen_daemon_status "ok:$STATE:$REASON" 2>/dev/null
        log -t "$TAG" "applied support_pen=$STATE reason=$REASON"
    else
        settings put global y700_pen_daemon_status "write-failed:$STATE:$REASON" 2>/dev/null
        log -t "$TAG" "failed support_pen=$STATE reason=$REASON"
    fi
}

initial_game=$(settings get global debug_gamemode_value 2>/dev/null)
initial_allow=$(settings get global y700_pen_in_game 2>/dev/null)
[ "$initial_game" = '1' ] && [ "$initial_allow" != '1' ] && initial_state=0 || initial_state=1
apply_state "$initial_state" initial

while true; do
    # android.util.Log from system_server is routed to the main buffer on this
    # ColorOS build. Listening to system silently misses every transition.
    logcat -b main -T 1 -v raw -s Y700PenGame:I 2>/dev/null | while IFS= read -r LINE; do
        case "$LINE" in
            *desired=1*) apply_state 1 event ;;
            *desired=0*) apply_state 0 event ;;
        esac
    done
    sleep 1
done
