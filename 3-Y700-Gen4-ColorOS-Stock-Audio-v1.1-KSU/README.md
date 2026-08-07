# Y700 Gen 4 ColorOS Stock Audio v1.1

This KernelSU module makes four narrowly scoped changes:

1. Routes only `AUDIO_STREAM_MUSIC` on `DEVICE_CATEGORY_HEADSET` to the stock Lenovo TB322FC/ZUXOS volume curve. The maximum attenuation changes from the ColorOS value of about -1.5 dB to the stock value of -8.5 dB. Bluetooth, USB and speaker curves remain unchanged.
2. Sets `vendor.audio.volume.headset.gain.depcal=true`, matching the stock vendor configuration.
3. Sets `ro.oplus.audio.effect.type=none`, preventing `AudioxService` from repeatedly trying to create the unavailable AudioX effect UUID.
4. Overlays `/system/lib64/libaudiopolicyenginedefault.so` with the ColorOS 401 library patched at file offsets `0x484BD-0x484BE` (`19 00` to `FD 03`). This changes `cmp w8, #6` to `cmp w8, #0xff` inside `aidl2legacy_AudioHalVolumeCurve_VolumeCurve`, allowing the HAL's `0xFF` fallback device category to be converted instead of returning `-EINVAL` and delaying volume application.

The module does not flash or write the physical system, ODM, vendor, Bluetooth or DSP partitions. KernelSU overlays two XML files and one native library at boot, and overrides two properties.

The installer accepts only the verified ColorOS 401 original or already-patched Audio Policy library hashes. An unknown build aborts installation instead of overlaying an incompatible native library.

## Reproducing the native patch

`tools/patch_audio_policy.py` applies the two-byte patch to the unmodified ColorOS 401 mini library. It validates both input and output SHA-256 values and refuses any unknown input. The shipped patched library has SHA-256 `84A6982CB242EB561543F6F2A43D5CCDDFF42C24EEA446AB9D18C85717213D2E`.

## Recovery

- Normal recovery: disable or uninstall `Y700 Gen 4 ColorOS Stock Audio` in KernelSU, then reboot.
- If Android cannot finish booting, disable the module from KernelSU safe mode/recovery or create `/data/adb/modules/y700_coloros_stock_audio/disable`, then reboot.

Installing the ZIP alone does not activate the changes. They take effect only after a reboot initiated by the user.
