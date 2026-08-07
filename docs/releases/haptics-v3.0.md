# Y700 四代 ColorOS 原厂触感兼容与控制 v3.0

适用于联想拯救者 Y700 四代的 OPPO Pad Mini / ColorOS 移植 ROM。

## 依赖与作用域

- 需要 KernelSU 与 meta-overlayfs。
- 需要 Vector Framework 或 LSPosed。
- Vector 安装时自动启用并设置 `system/0`。
- LSPosed 只勾选“系统框架”，不要勾选 SystemUI、设置、桌面或所有应用。

## v3.0 变化

- 内置 ZUXOS 1.5.10.259 原厂 `Hapticsconfig.xml`。
- 在 system_server 将 ColorOS/COUI 私有触感常量 300–308 转换为 ZUXOS/AOSP 支持的系统语义。
- 将 SystemUI/Launcher 直接提交的匿名 65 ms 触摸波形按场景转换为 CLICK、DOUBLE_CLICK 或 TICK，修复关闭自定义后仍然长而松散的返回、桌面/多任务与锁屏振动。
- 默认关闭全部 14 个用户自定义分类；schema 5 升级仅关闭旧启用位，保留模式、强度、时长与多段编排。
- 保留标准效果、合成原语、可视化多段序列、JSON 导入导出、ZUXOS HAL 预制效果和 Y700 双马达预设。
- 普通第三方应用无需 Root；只要调用 Android 振动服务，就由 system_server 的同一兼容路径处理。

## 启动安全

原厂 XML 的 SHA-256 必须为 `0A81641AA9AE68A90739D815DA381D3A8E11695AD38560C9F14A94199899C5F2`，SELinux 标签必须为 `u:object_r:vendor_configs_file:s0`。安装器会同时硬校验两项，任一不符即中止，避免 ColorOS 音频 HAL 解析失败并卡在开机动画。

## 发布资产

- `5-Y700-Gen4-ColorOS-Haptics-Control-v3.0-KSU.zip`

## SHA-256

| 文件 | SHA-256 |
| --- | --- |
| `5-Y700-Gen4-ColorOS-Haptics-Control-v3.0-KSU.zip` | `79E3DD7EE7D299226D286CC791E062B21653E408EECF224C09C74138E5EA68D5` |

## 验证记录

- APK 签名校验通过，中文应用名为“Y700 触感控制”。
- 静态映射测试通过：37 个模式、14 个分类，默认启用位全部为 0。
- ColorOS 私有常量 300–308 的九项运行时请求均不再出现 `ignored_unsupported`。
- 实机从 KSU 包安装并重启成功；system_server、SystemUI、Launcher 与 audio HAL 正常，Vector Hook 已加载。
- 当前、待更新与 meta-overlayfs 镜像中的原厂 XML 哈希和 SELinux 标签均核验通过。
