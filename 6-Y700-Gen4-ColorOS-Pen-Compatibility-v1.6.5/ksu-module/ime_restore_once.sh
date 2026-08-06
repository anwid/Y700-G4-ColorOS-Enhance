#!/system/bin/sh

MODDIR=${0%/*}
EXPECTED_GENERATION="$1"
sleep 0.8

[ -n "$EXPECTED_GENERATION" ] || exit 0
[ "$(cat "$MODDIR/pen_far_generation" 2>/dev/null)" = "$EXPECTED_GENERATION" ] || exit 0
[ "$(settings get global y700_pen_near 2>/dev/null)" = "0" ] || exit 0

NORMAL=$(settings get global y700_pen_normal_ime 2>/dev/null)
HANDWRITING=$(settings get global y700_pen_handwriting_ime 2>/dev/null)
[ -n "$NORMAL" ] && [ "$NORMAL" != "null" ] || NORMAL='com.baidu.input/.ImeService'
[ -n "$HANDWRITING" ] && [ "$HANDWRITING" != "null" ] || HANDWRITING='com.sohu.inputmethod.sogouoem/.SogouIME'
CURRENT=$(settings get secure default_input_method 2>/dev/null)

# Preserve any IME selected manually during the debounce window.  Only unwind
# the handwriting fallback that this compatibility stack selected.
if [ "$CURRENT" = "$HANDWRITING" ]; then
    settings put secure default_input_method "$NORMAL" 2>/dev/null
    log -t Y700PenProximity "token-guarded far restore: $NORMAL"
fi
