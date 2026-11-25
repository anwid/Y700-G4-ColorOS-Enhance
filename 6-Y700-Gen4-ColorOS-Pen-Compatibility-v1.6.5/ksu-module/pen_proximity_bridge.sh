#!/system/bin/sh

MODDIR=${0%/*}
TAG=Y700PenProximity
PIDFILE="$MODDIR/pen_proximity_bridge.pid"

if [ -r "$PIDFILE" ]; then
    OLD_PID=$(cat "$PIDFILE" 2>/dev/null)
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        OLD_CMD=$(tr '\000' ' ' < "/proc/$OLD_PID/cmdline" 2>/dev/null)
        case "$OLD_CMD" in
            *pen_proximity_bridge.sh*) exit 0 ;;
        esac
    fi
fi
echo $$ > "$PIDFILE"

find_pen_device() {
    for SYS_EVENT in /sys/class/input/event*; do
        [ -r "$SYS_EVENT/device/name" ] || continue
        NAME=$(cat "$SYS_EVENT/device/name" 2>/dev/null)
        if [ "$NAME" = "NVTCapacitivePen" ]; then
            printf '/dev/input/%s\n' "${SYS_EVENT##*/}"
            return 0
        fi
    done
    return 1
}

settings put global y700_pen_near 0
while true; do
    DEVICE=$(find_pen_device)
    if [ -z "$DEVICE" ] || [ ! -e "$DEVICE" ]; then
        settings put global y700_pen_near 0
        sleep 2
        continue
    fi
    log -t "$TAG" "listening on $DEVICE"
    getevent -l "$DEVICE" 2>/dev/null | while IFS= read -r LINE; do
        case "$LINE" in
            *BTN_DIGI*DOWN*)
                settings put global y700_pen_near 1
                ;;
            *BTN_DIGI*UP*)
                settings put global y700_pen_near 0
                ;;
        esac
    done
    settings put global y700_pen_near 0
    sleep 1
done
