# Y700 四代 ColorOS 触感控制 v2.11

适用于联想拯救者 Y700 四代刷入 OPPO Pad Mini / ColorOS 移植 ROM 的场景。

## 安装

1. 需要 KernelSU，以及可工作的 Vector Framework 或 LSPosed。
2. 在 KernelSU 中刷入 ZIP，然后重启一次。
3. Vector 会自动启用模块并设置 `system/0` 作用域。
4. LSPosed 用户需手动启用模块，作用域只勾选“系统框架”，随后重启。
5. 可从 KernelSU 的模块“操作”按钮或桌面图标打开控制界面。

## 功能

- 按返回手势、桌面/多任务、锁屏密码、输入法、普通触摸、硬件按键及通知等 14 类用途分别设置。
- “自定义多段序列”位于处理模式列表首位，可视化串联标准效果、合成原语、纯停顿与可调 1–300 ms 的限制时长脉冲。
- 每段可独立设置强度、前置延迟；支持直接测试以及 JSON 配置导入/导出。
- 包内默认值来自实机当前调好的完整配置；主页/多任务默认为 QUICK_RISE 加 20 ms、60% 脉冲，前置间隔 7 ms。升级不会覆盖已有用户配置。
- 保留 Android 标准效果、ZUXOS 原厂行为及针对左右双 AW86937 的 IVibratorDual 预设。
- 不替换 `/vendor/etc/Hapticsconfig.xml`，不修改内核、驱动电压、boost 电压或原厂增益上限。

## 卸载

在 KernelSU 中卸载模块并重启。伴随 APK 会被移除，但保留 `y700_haptic_*` 配置，便于重新安装后恢复。
