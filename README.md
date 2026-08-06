# 联想拯救者 Y700 四代 ColorOS 移植版修复与增强

适用于联想拯救者 Y700 四代刷入 ColorOS 移植 ROM 后的兼容性修复。已测试 ROM 为酷安 [@微微薇的 OPPO Pad Mini 版 ColorOS ROM](https://www.coolapk.com/feed/73057139?s=NDljYzk2ZDAyOTliZDM4ZzZhNzQxOTBkega1650)。

`@GUF296` 的一加金标 ROM 理论上可使用部分通用模块，但未经本项目实机验证。显示修复和圆角关闭明确不要安装到该 ROM。

# ⚠️ meta-overlayfs 不在本仓库发布，请去原项目下载

## 本仓库不会上传、镜像或重新分发 `1-meta-overlayfs-v1.3.1-KSU.zip`。

## 请从 [KernelSU-Modules-Repo/meta-overlayfs Releases](https://github.com/KernelSU-Modules-Repo/meta-overlayfs/releases) 下载与自己 KernelSU 环境匹配的版本。

meta-overlayfs 是独立的第三方元模块，版权、更新和支持均归原项目。不要从本项目 Issue 索要该 ZIP。

## 发布包

所有当前可安装包均在 [Releases](https://github.com/LoranaAurelia/Y700-G4-ColorOS-Enhance/releases/latest)。仓库根目录中的编号目录是对应源码和模块模板，不是直接刷入包。

| 编号 | 发布包 | 用途 | 依赖/作用域 |
| --- | --- | --- | --- |
| 2 | `2-Y700-Gen4-ColorOS-Display-Fix-v1.3-KSU.zip` | 移除错误虚拟刘海，修复竖屏时间遮挡 | 依赖 meta-overlayfs；仅 @微微薇 ROM |
| 3 | `3-Y700-Gen4-ColorOS-Stock-Audio-v1.0-KSU.zip` | 恢复原厂有线耳机音量曲线和增益标定，关闭异常 AudioX | 依赖 meta-overlayfs |
| 4 | `4-Y700-Gen4-ColorOS-APK-Parse-Fix-v1.3-LSPosed.apk` | 修复部分合法 APK 被 ColorOS 误报 `-124` | Vector/LSPosed；只选系统框架 |
| 5 | `5-Y700-Gen4-ColorOS-Haptics-Control-v2.11-KSU.zip` | 按场景调节振动，可视化多段触感编排 | Vector/LSPosed；只选系统框架 |
| 6 | `6-Y700-Gen4-ColorOS-Pen-Compatibility-v1.6.3-KSU.zip` | 联想 Tab Pen Plus 接入 ColorOS 原生手写笔功能 | Vector/LSPosed；系统框架 + `com.oplus.ipemanager` |
| 6.5 | `6.5-Y700-Gen4-ColorOS-Accessibility-Warning-Whitelist-v1.0.0-LSPosed.apk` | 仅对白名单应用跳过 ColorOS 无障碍十秒警告 | Vector/LSPosed；只选系统框架 |
| 7 | `7-Y700-Gen4-ColorOS-Global-Round-Corners-Off-v1.5-KSU.zip` | 关闭 @微微薇 ROM 的全局软件圆角 | meta-overlayfs + Vector/LSPosed；只选 SystemUI |
| 8 | `8-Y700-Gen4-ColorOS-Stock-Thermal-v1.1-KSU.zip` | 恢复 TB322 ZUXOS 的 CPU、GPU、电池和 common 温控配置 | 依赖 meta-overlayfs |

## 建议安装顺序

1. 如需 2、3、7、8，先从原项目安装 meta-overlayfs，然后完整重启。
2. 安装所需的 KernelSU ZIP；每次安装完成后按模块说明重启。
3. APK 类 Xposed 模块当普通应用安装，在 Vector/LSPosed 中启用并严格按上表选择作用域。
4. 手写笔用户可先在 KernelSU 安装 6 但不重启，接着安装 6.5 APK、设好作用域，再统一完整重启一次。

## 重要限制

- 2 和 7 是针对 @微微薇 OPPO Pad Mini 移植版的定向修复，不要装到 `@GUF296` ROM。
- 3 只修复有线耳机曲线/增益并停止无法初始化的 OPlus AudioX；不改蓝牙、USB、Audio HAL、DSP 或物理分区。
- 4 只针对已确认 `resources.arsc` 未压缩且 4 字节对齐却仍被误判的 `-124` 问题；不会绕过签名、ABI、降级或包冲突检查。
- 5 不替换 ROM 触感配置，不改内核、马达电压或厂商增益上限。只装伴随 APK 不会加载 `system_server` Hook。
- 6 不注入 vendor HAL/内核，不伪造压感；ColorOS 游戏模式会让联想触控固件停止笔流，公开版不强行绕过该限制。
- 6.5 不会全局关闭反诈或安全检查，仅对明确白名单中的无障碍服务跳过指定警告。
- 8 不改刷新率、应用帧率白名单、触控采样率、HAL、内核或 DTBO；恢复原厂保护后高温时更早降频属于预期行为。

## 卸载

- KernelSU 模块：在 KernelSU 中停用或卸载对应模块，然后完整重启。
- Xposed APK：先在 Vector/LSPosed 中禁用，再卸载 APK 并重启。
- 7 和 5 的 KernelSU 卸载脚本会删除各自的伴随 APK；5 默认保留用户触感配置，便于重装恢复。

## 问题反馈

请使用 [GitHub Issues](https://github.com/LoranaAurelia/Y700-G4-ColorOS-Enhance/issues)，并附上：

- 设备与 ROM 完整版本；
- KernelSU、meta-overlayfs、Vector/LSPosed 版本；
- 安装的完整文件名与 SHA-256；
- Xposed 作用域截图；
- 可重现步骤和相关 logcat。

只反馈“没效”、“装不上”、“笔没了”无法用于定位问题。

## 源码与构建

每个编号目录都包含可编辑源码、模块模板与 README/BUILD 说明。部分目录保留当前签名的预构建 APK/RRO，用于校验发布包和重新封装；签名私钥不随仓库分发。自行重建时请使用自己的密钥。

## License

本项目自制代码按 [MIT License](LICENSE) 发布。从 Android、ColorOS、ZUXOS 或其他项目提取的配置、接口声明和预构建件仍归各自权利人所有。
