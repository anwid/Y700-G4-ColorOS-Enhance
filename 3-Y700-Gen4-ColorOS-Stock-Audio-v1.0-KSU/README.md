# Y700 Gen 4 ColorOS Stock Audio v1.0

This KernelSU module makes three narrowly scoped changes:

1. Routes only `AUDIO_STREAM_MUSIC` on `DEVICE_CATEGORY_HEADSET` to the stock Lenovo TB322FC/ZUXOS volume curve. The maximum attenuation changes from the ColorOS value of about -1.5 dB to the stock value of -8.5 dB. Bluetooth, USB and speaker curves remain unchanged.
2. Sets `vendor.audio.volume.headset.gain.depcal=true`, matching the stock vendor configuration.
3. Sets `ro.oplus.audio.effect.type=none`, preventing `AudioxService` from repeatedly trying to create the unavailable AudioX effect UUID.

The module does not flash or write the physical ODM, vendor, Bluetooth or DSP partitions. KernelSU overlays two XML files at boot and overrides two properties.

## Recovery

- Normal recovery: disable or uninstall `Y700 Gen 4 ColorOS Stock Audio` in KernelSU, then reboot.
- If Android cannot finish booting, disable the module from KernelSU safe mode/recovery or create `/data/adb/modules/y700_coloros_stock_audio/disable`, then reboot.

Installing the ZIP alone does not activate the changes. They take effect only after a reboot initiated by the user.
