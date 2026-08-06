#!/system/bin/sh

ui_print "- Y700 Gen 4 ColorOS Stock Thermal v1.1"
ui_print "- Target: SM8750P / OP6548L1"
ui_print "- Restoring 14 stock CPU, GPU and battery thermal profiles"
ui_print "- Refresh rate, touch sampling and thermal FPS mapping stay unchanged"

SOC="$(getprop ro.soc.model)"
DEVICE="$(getprop ro.product.vendor.device)"
[ -n "$DEVICE" ] || DEVICE="$(getprop ro.product.device)"

[ "$SOC" = "SM8750P" ] || abort "! Unsupported SoC: $SOC"
[ "$DEVICE" = "OP6548L1" ] || abort "! Unsupported device: $DEVICE"

BASELINE_CONF="c32b39af7a19759a7db7f8b2d92d069a0534848aa7a195ca2cec24aa6c317cb3"
BASELINE_RC="de3e4dcef678f7fdf448b827b643a28eb2d6b164131ec3d218a67d9ebff6dc8e"

LIVE_CONF="$(sha256sum /vendor/etc/thermal-engine.conf 2>/dev/null | cut -d ' ' -f 1)"
LIVE_RC="$(sha256sum /vendor/etc/init/init_thermal-engine-v2.rc 2>/dev/null | cut -d ' ' -f 1)"
[ "$LIVE_CONF" = "$BASELINE_CONF" ] || abort "! thermal-engine.conf does not match the supported ROM base"
[ "$LIVE_RC" = "$BASELINE_RC" ] || abort "! thermal-engine init RC does not match the supported ROM base"

FILES="
thermal-engine_battery_0.conf
thermal-engine_battery_1.conf
thermal-engine_battery_2.conf
thermal-engine_battery_3.conf
thermal-engine_common_0.conf
thermal-engine_common_1.conf
thermal-engine_cpu_0.conf
thermal-engine_cpu_1.conf
thermal-engine_cpu_2.conf
thermal-engine_cpu_3.conf
thermal-engine_cpu_4.conf
thermal-engine_gpu_0.conf
thermal-engine_gpu_1.conf
thermal-engine_gpu_2.conf
"

for FILE in $FILES; do
  LIVE="/vendor/etc/$FILE"
  PAYLOAD="$MODPATH/system/vendor/etc/$FILE"
  [ -f "$LIVE" ] || abort "! Missing target: $LIVE"
  [ -s "$PAYLOAD" ] || abort "! Missing payload: $FILE"

  LIVE_SIZE="$(wc -c < "$LIVE" 2>/dev/null)"
  if [ "$LIVE_SIZE" != "0" ]; then
    LIVE_HASH="$(sha256sum "$LIVE" | cut -d ' ' -f 1)"
    PAYLOAD_HASH="$(sha256sum "$PAYLOAD" | cut -d ' ' -f 1)"
    [ "$LIVE_HASH" = "$PAYLOAD_HASH" ] || abort "! $FILE is neither the known empty ROM file nor the stock payload"
  fi

  # meta-overlayfs copies the payload context into its ext4 staging image.
  # thermal-engine runs in a vendor domain and cannot read system_file here.
  set_perm "$PAYLOAD" 0 0 0644 u:object_r:vendor_configs_file:s0
done

set_perm "$MODPATH/module.prop" 0 0 0644
set_perm "$MODPATH/README.md" 0 0 0644

ui_print "- thermallevel_to_fps.xml is intentionally excluded"
ui_print "- Installed. Changes apply after a manual reboot."
