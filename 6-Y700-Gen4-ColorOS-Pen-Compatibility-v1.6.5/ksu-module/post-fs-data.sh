#!/system/bin/sh

MODDIR=${0%/*}
TAG=Y700PenCompat

# Preserve only the original NEC/Y700 activation behavior.  Do not mount or
# replace vendor binaries here: doing that through meta-overlayfs broke the
# linker namespace on this ColorOS port and caused bpfloader reboot loops.
if [ -e /proc/support_pen ]; then
    printf '1\n' > /proc/support_pen 2>/dev/null
fi
