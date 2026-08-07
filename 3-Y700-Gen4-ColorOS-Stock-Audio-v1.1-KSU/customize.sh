#!/system/bin/sh

ui_print "- Y700 Gen 4 ColorOS Stock Audio v1.1"
ui_print "- Restoring Lenovo wired-headset music attenuation curve"
ui_print "- Enabling stock headset dependent gain calibration"
ui_print "- Preventing the broken OPlus AudioX service from starting"
ui_print "- Fixing the Audio Policy 0xFF volume-curve category mismatch"
ui_print "- Bluetooth, USB, Audio HAL and DSP are unchanged"

POLICY_LIB="/system/lib64/libaudiopolicyenginedefault.so"
POLICY_SHA="$(sha256sum "$POLICY_LIB" 2>/dev/null | awk '{print $1}')"
case "$POLICY_SHA" in
  84abd687407f0eb2941105b97e5e3f129f764d0b316e852e5b295493493c261d)
    ui_print "- Verified compatible ColorOS 401 Audio Policy library"
    ;;
  84a6982cb242eb561543f6f2a43d5ccddff42c24eea446ab9d18c85717213d2e)
    ui_print "- Audio Policy library is already patched"
    ;;
  *)
    abort "! Unsupported Audio Policy library SHA-256: ${POLICY_SHA:-unreadable}"
    ;;
esac

set_perm "$MODPATH/system/odm/etc/audio/audio_policy_volumes.xml" 0 0 0644
set_perm "$MODPATH/system/odm/etc/audio/default_volume_tables.xml" 0 0 0644
set_perm "$MODPATH/system/lib64/libaudiopolicyenginedefault.so" 0 0 0644
set_perm "$MODPATH/system.prop" 0 0 0644

ui_print "- Installed. Changes apply only after a manual reboot."
