#!/system/bin/sh

MODDIR=${0%/*}
[ ! -x "$MODDIR/pen_button_reconnect.sh" ] || "$MODDIR/pen_button_reconnect.sh" --stop

settings delete global y700_pen_near >/dev/null 2>&1
