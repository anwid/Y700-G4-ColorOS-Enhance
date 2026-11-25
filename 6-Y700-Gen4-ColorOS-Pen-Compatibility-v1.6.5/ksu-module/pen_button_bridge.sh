#!/system/bin/sh

MODDIR=${0%/*}
TAG=Y700PenBridge
PIDFILE="$MODDIR/pen_button_bridge.pid"

if [ -r "$PIDFILE" ]; then
    OLD_PID=$(cat "$PIDFILE" 2>/dev/null)
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        OLD_CMD=$(tr '\000' ' ' < "/proc/$OLD_PID/cmdline" 2>/dev/null)
        case "$OLD_CMD" in
            *pen_button_bridge.sh*) exit 0 ;;
        esac
    fi
fi
echo $$ > "$PIDFILE"

touchnotes_has_focus() {
    FOCUS=$(dumpsys window displays 2>/dev/null | grep 'mCurrentFocus=')
    case "$FOCUS" in
        *" com.newskyer.draw/com.newskyer.draw.PadActivity}"*|*" com.newskyer.draw/.PadActivity}"*) return 0 ;;
    esac
    return 1
}

send_single_click() {
    ROUTE=$(settings get global y700_pen_single_button_route 2>/dev/null)
    [ "$ROUTE" = "null" ] || [ -z "$ROUTE" ] && ROUTE=1
    if [ "$ROUTE" = "0" ]; then
        log -t "$TAG" "ignored Lenovo single click; route=off"
        return
    fi
    if [ "$ROUTE" = "1" ]; then
        if touchnotes_has_focus && cmd activity broadcast --user current \
            -a com.oplus.ipemanager.action.PENCIL_DOUBLE_CLICK \
            -p com.newskyer.draw >/dev/null 2>&1; then
            log -t "$TAG" "forwarded Lenovo single click as TouchNotes pen double click"
            return
        fi
        input stylus keyevent 308 >/dev/null 2>&1
        log -t "$TAG" "forwarded Lenovo single click as Android stylus primary key"
        return
    fi

    MODE=$(settings get global ipe_pencil_single_click 2>/dev/null)
    [ "$MODE" = "null" ] || [ -z "$MODE" ] && MODE=4
    if [ "$MODE" = "4" ]; then
        cmd activity broadcast --user current \
            -a com.oplus.ipemanager.action.PENCIL_SINGLE_CLICK \
            -p com.oplus.healthservice \
            -f 0x11000000 --ei action "$MODE" >/dev/null 2>&1
    else
        cmd activity broadcast --user current \
            -a com.oplus.ipemanager.action.PENCIL_SINGLE_CLICK \
            -f 0x11000000 --ei action "$MODE" >/dev/null 2>&1
    fi
    log -t "$TAG" "forwarded Lenovo single click; ColorOS mode=$MODE"
}

send_double_click() {
    settings put global y700_pen_last_button_event "double:$(date +%s)" 2>/dev/null
    if touchnotes_has_focus; then
        # The ROM already sends the physical double click to the note receiver.
        log -t "$TAG" "skipped duplicate Lenovo double click in TouchNotes"
        return
    fi
    MODE=$(settings get global ipe_pencil_double_click 2>/dev/null)
    [ "$MODE" = "null" ] || [ -z "$MODE" ] && MODE=1
    if [ "$MODE" = "4" ]; then
        cmd activity broadcast --user current \
            -a com.oplus.ipemanager.action.PENCIL_DOUBLE_CLICK \
            -p com.oplus.healthservice \
            -f 0x11000000 \
            --receiver-permission com.oplus.ipemanager.permission.receiver.DOUBLE_CLICK >/dev/null 2>&1
    else
        cmd activity broadcast --user current \
            -a com.oplus.ipemanager.action.PENCIL_DOUBLE_CLICK \
            -f 0x11000000 \
            --receiver-permission com.oplus.ipemanager.permission.receiver.DOUBLE_CLICK >/dev/null 2>&1
    fi
    log -t "$TAG" "forwarded Lenovo double click; ColorOS mode=$MODE"
}

find_button_device() {
    for SYS_EVENT in /sys/class/input/event*; do
        [ -r "$SYS_EVENT/device/name" ] || continue
        NAME=$(cat "$SYS_EVENT/device/name" 2>/dev/null)
        if [ "$NAME" = "Lenovo Tab Pen Plus Consumer Control" ]; then
            printf '/dev/input/%s\n' "${SYS_EVENT##*/}"
            return 0
        fi
    done
    return 1
}

while true; do
    DEVICE=$(find_button_device)
    if [ -z "$DEVICE" ] || [ ! -e "$DEVICE" ]; then
        sleep 2
        continue
    fi

    log -t "$TAG" "listening on $DEVICE"
    getevent -l "$DEVICE" 2>/dev/null | while IFS= read -r LINE; do
        # ColorOS toybox output is not stable across boots: the event label can
        # change case and the scan code can lose leading zeroes.  Normalize the
        # line and match the HID usage suffix instead of one exact rendering.
        NORMALIZED=$(printf '%s\n' "$LINE" | tr '[:upper:]' '[:lower:]')
        case "$NORMALIZED" in
            *msc_scan*c0600*)
                send_single_click
                settings put global y700_pen_last_button_event "single:$(date +%s)" 2>/dev/null
                ;;
            *msc_scan*c0601*) send_double_click ;;
        esac
    done
    sleep 1
done
