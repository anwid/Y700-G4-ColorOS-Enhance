#!/system/bin/sh

ui_print "- Y700 Gen 4 ColorOS Stock Audio v1.0"
ui_print "- Restoring Lenovo wired-headset music attenuation curve"
ui_print "- Enabling stock headset dependent gain calibration"
ui_print "- Preventing the broken OPlus AudioX service from starting"
ui_print "- Bluetooth, USB, speaker, HAL and DSP are unchanged"

set_perm "$MODPATH/system/odm/etc/audio/audio_policy_volumes.xml" 0 0 0644
set_perm "$MODPATH/system/odm/etc/audio/default_volume_tables.xml" 0 0 0644
set_perm "$MODPATH/system.prop" 0 0 0644

ui_print "- Installed. Changes apply only after a manual reboot."
