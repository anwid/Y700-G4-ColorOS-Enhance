#!/system/bin/sh

MODDIR=${0%/*}
EXPECTED_GENERATION="$1"
[ -n "$EXPECTED_GENERATION" ] || exit 0
DELAY_MS=$(settings get global y700_pen_ime_restore_ms 2>/dev/null)
case "$DELAY_MS" in
    ''|*[!0-9]*) DELAY_MS=800 ;;
esac
[ "$DELAY_MS" -ge 500 ] 2>/dev/null || DELAY_MS=500
[ "$DELAY_MS" -le 15000 ] 2>/dev/null || DELAY_MS=15000
sleep "$(printf '%d.%03d' "$((DELAY_MS / 1000))" "$((DELAY_MS % 1000))")"

pen_still_far() {
    [ "$(cat "$MODDIR/pen_far_generation" 2>/dev/null)" = "$EXPECTED_GENERATION" ] &&
        [ "$(settings get global y700_pen_near 2>/dev/null)" = "0" ]
}

pen_still_far || exit 0

NORMAL=$(settings get global y700_pen_module_normal_ime 2>/dev/null)
HANDWRITING=$(settings get global y700_pen_handwriting_ime 2>/dev/null)
[ -n "$NORMAL" ] && [ "$NORMAL" != "null" ] || exit 0
[ -n "$HANDWRITING" ] && [ "$HANDWRITING" != "null" ] || HANDWRITING='com.sohu.inputmethod.sogouoem/.SogouIME'
CURRENT=$(settings get secure default_input_method 2>/dev/null)

# Preserve any IME selected manually during the debounce window.  Only unwind
# the handwriting fallback that this compatibility stack selected.
if [ "$CURRENT" = "$HANDWRITING" ] && [ "$CURRENT" != "$NORMAL" ] && pen_still_far; then
    settings put secure default_input_method "$NORMAL" 2>/dev/null
    log -t Y700PenProximity "token-guarded far restore: $NORMAL"
fi
