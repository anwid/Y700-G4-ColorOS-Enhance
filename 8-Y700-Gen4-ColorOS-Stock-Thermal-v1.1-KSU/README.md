# Y700 Gen 4 ColorOS Stock Thermal v1.1

适用于本项目的 SM8750P / OP6548L1 ColorOS ROM。

本模块只恢复 TB322 ZUXOS 1.5.10.259 原厂镜像中的 14 份非空 thermal-engine 配置：4 份电池配置、2 份 common 配置、5 份 CPU 配置和 3 份 GPU 配置。

明确不会修改：

- `/vendor/etc/display/thermallevel_to_fps.xml`
- 屏幕刷新率和应用刷新率白名单
- `/proc/HighReportRate`、`/proc/game_mode` 或其他触控节点
- thermal/display/touch HAL
- boot、DTBO、vendor_boot 或内核模块

预期效果是在保留 ColorOS 当前 120Hz + 游戏高触控采样行为的同时，恢复联想原厂 CPU、GPU 和电池温控曲线。高温下可能更早出现 CPU/GPU 降频，这是预期行为。

## 回退

在 KernelSU 中停用或卸载 `Y700 Gen 4 ColorOS Stock Thermal` 后重启。若系统无法正常完成启动，可在安全模式/Recovery 中创建 `/data/adb/modules/y700_coloros_stock_thermal/disable` 后重启。

安装 ZIP 不会立即修改运行状态；由用户手动重启后生效。

v1.1 为恢复的配置显式设置 `u:object_r:vendor_configs_file:s0`。缺少该标签时，文件虽然对 root 可见，`thermal-engine-v2` 仍会因 SELinux 域限制而无法打开。
