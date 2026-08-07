# Y700 四代 ColorOS 原厂触感兼容与控制 v3.0

适用于联想拯救者 Y700 四代刷入 OPPO Pad Mini / ColorOS 移植 ROM 的场景。

## 安装

1. 需要 KernelSU、meta-overlayfs，以及可工作的 Vector Framework 或 LSPosed。
2. 在 KernelSU 中刷入 ZIP，然后重启一次。
3. Vector 会自动启用模块并设置 `system/0` 作用域。
4. LSPosed 用户需手动启用模块，作用域只勾选“系统框架”，随后重启。
5. 可从 KernelSU 的模块“操作”按钮或桌面图标打开控制界面。

## 功能

- 内置并覆盖为 ZUXOS 1.5.10.259 原厂 `Hapticsconfig.xml`，SHA-256 为 `0A81641AA9AE68A90739D815DA381D3A8E11695AD38560C9F14A94199899C5F2`。
- 在 `system_server` 统一接管 Android 振动服务。普通应用、第三方输入法、SystemUI 和 Launcher 均无需 Root；只要通过标准 `Vibrator`、`VibratorManager` 或 View 触感 API 请求，就会使用相同的原厂标定与翻译规则。
- 自动把 ColorOS/COUI 私有的 300–308 号触感翻译为最接近的 ZUXOS 原厂 HAL 预制效果，避免 `ignored_unsupported`、无振动和异常长振动，并保留 Android 的队列、取消、优先级与调用归属。
- 默认关闭所有“用户自定义”，即默认使用 ZUXOS 原厂兼容路径，而不是放行 ColorOS 的坏掉振动。每个分类的开关只控制是否用用户选择的效果覆盖原厂映射。
- 按返回手势、桌面/多任务、锁屏密码、输入法、普通触摸、硬件按键及通知等 14 类用途分别设置。
- “自定义多段序列”位于处理模式列表首位，可视化串联标准效果、合成原语、纯停顿与可调 1–300 ms 的限制时长脉冲。
- 每段可独立设置强度、前置延迟；支持直接测试以及 JSON 配置导入/导出。
- 原厂模式置于模式列表前部，包括 CLICK、DOUBLE_CLICK、TICK、THUD、POP、HEAVY_CLICK，以及从 ZUI 行为核对得到的解锁、开关、滑条刻度、密码键和列表刻度双马达场景。
- 仍保留 Android 标准效果、合成原语、可视化多段序列和针对左右双 AW86937 的实验预设。v3 首次安装或从旧版升级时会把全部自定义开关关闭，但保留已保存的参数与编排；之后用户主动开启的项目不会在重启时被重置。“恢复 ZUXOS 原厂默认”可再次一次关闭全部自定义。
- 不修改内核、驱动电压、boost 电压或联想原厂增益上限。

## 覆盖范围与限制

- Hook 位于系统振动服务端，因此调用应用是否 Root、是否为系统应用均不影响覆盖。
- 通知、来电、闹钟等本来就有效的长波形在未启用自定义时保持应用原始节奏，仅由 ZUXOS 原厂驱动标定播放。
- 标准 Android 触感常量直接走 ZUXOS 原厂 provider/HAL；ColorOS 私有 300–308 会在 `performHapticFeedbackInternal` 入口转换为系统支持的语义。SystemUI/Launcher 绕开 View API、直接提交的匿名 65 ms 触摸波形也会按返回、桌面/多任务或锁屏场景转换为 CLICK、DOUBLE_CLICK 或 TICK，避免关闭自定义后仍保留 ColorOS 的长而松散振动。
- vendor 覆盖文件显式使用 `u:object_r:vendor_configs_file:s0`。错误标成普通 `system_file` 会让当前 ColorOS 音频 HAL 无法读取并卡在开机动画；安装器会同时验证 XML SHA-256 与 SELinux 标签，任一不符即中止安装。
- 极少数拥有系统权限、绕开 Android 振动服务直接访问厂商设备节点的程序不在 Hook 范围内；普通第三方应用没有这种权限。

## 卸载

在 KernelSU 中卸载模块并重启。ZUXOS 配置覆盖和兼容 Hook 会同时撤销，系统回到移植 ROM 自身的行为。伴随 APK 会被移除，但保留 `y700_haptic_*` 配置，便于重新安装后恢复。
