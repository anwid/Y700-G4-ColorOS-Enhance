#!/system/bin/sh

MODDIR=${0%/*}
RUNTIME="$MODDIR/.pen_button_reconnect"
PIDFILE="$RUNTIME/supervisor.pid"
TAG=Y700PenReconnect

find_pen_node() {
    for event in /sys/class/input/event*; do
        [ "$(cat "$event/device/name" 2>/dev/null)" = "Lenovo Tab Pen Plus Consumer Control" ] || continue
        node="/dev/input/${event##*/}"
        [ -c "$node" ] || continue
        printf '%s\n' "$node"
        return 0
    done
    return 1
}

rom_has_node() {
    for pid in $(pidof gugu_platform_hal 2>/dev/null); do
        for fd in /proc/"$pid"/fd/*; do
            if [ "$(readlink "$fd" 2>/dev/null)" = "$1" ]; then
                return 0
            fi
        done
    done
    return 1
}

supervisor_pid() {
    pid=$(cat "$PIDFILE" 2>/dev/null)
    case "$pid" in ''|*[!0-9]*) return 1 ;; esac
    case "$(tr '\000' ' ' < "/proc/$pid/cmdline" 2>/dev/null)" in
        *"$MODDIR/pen_button_reconnect.sh"*) printf '%s\n' "$pid" ;;
        *) return 1 ;;
    esac
}

if [ "$1" = "--stop" ]; then
    pid=$(supervisor_pid) && kill "$pid"
    exit 0
fi

supervisor_pid >/dev/null && exit 0
umask 077
mkdir -p "$RUNTIME" || exit 1
printf '%s\n' "$$" > "$PIDFILE" || exit 1
FIFO="$RUNTIME/hotplug.$$"
WATCHER=
LAST_NODE=
cleanup() {
    [ -z "$WATCHER" ] || kill "$WATCHER" 2>/dev/null
    rm -f "$FIFO" "$PIDFILE"
}
trap cleanup EXIT
trap 'exit 0' HUP INT TERM
mkfifo "$FIFO" || exit 1
inotifyd - /dev/input:nmdyo > "$FIFO" 2>/dev/null &
WATCHER=$!
exec 3< "$FIFO"

refresh() {
    if ! node=$(find_pen_node); then
        LAST_NODE=
        return
    fi
    identity=$(stat -c '%i' "$node" 2>/dev/null) || return
    current="$node:$identity"
    [ "$current" != "$LAST_NODE" ] || return

    if ! rom_has_node "$node"; then
        # Allow the ROM's own hotplug handler to open the new node first.
        sleep 1
        [ "$(find_pen_node)" = "$node" ] || return
        [ "$(stat -c '%i' "$node" 2>/dev/null)" = "$identity" ] || return
    fi

    LAST_NODE="$current"
    if rom_has_node "$node"; then
        log -t "$TAG" "ROM already listening on $node"
    elif [ "$(getprop init.svc.gugu_platform_hal)" = running ]; then
        if setprop ctl.restart gugu_platform_hal; then
            log -t "$TAG" "Requested ROM pen bridge restart for $node"
        else
            log -t "$TAG" "Could not restart ROM pen bridge for $node"
        fi
    else
        log -t "$TAG" "ROM pen bridge is not running for $node"
    fi
}

log -t "$TAG" "Watching Lenovo pen input hotplug"
refresh
while read -r events directory entry <&3; do
    case "$entry" in
        event*) refresh ;;
        '') case "$events" in *o*) refresh ;; esac ;;
    esac
done
