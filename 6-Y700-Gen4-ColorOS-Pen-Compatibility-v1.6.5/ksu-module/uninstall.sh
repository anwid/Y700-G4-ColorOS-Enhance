#!/system/bin/sh

pm uninstall dev.lorana.y700.colorospenui.xposed >/dev/null 2>&1
settings delete global y700_pen_near >/dev/null 2>&1
