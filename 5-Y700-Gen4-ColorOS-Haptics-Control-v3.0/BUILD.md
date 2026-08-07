# 振动原厂兼容与控制 v3.0 源码说明

## 目录

- `app/src/`：设置界面、配置模型和运行在 `system_server` 的 Xposed Hook。
- `app/res/`、`app/assets/`、`app/AndroidManifest.xml`：应用资源、入口及中文显示名。
- `app/stubs/`：编译时使用的最小 Xposed/隐藏 API 桩，不得打入最终 APK。
- `ksu-module/`：KernelSU 安装、开机服务、卸载和操作脚本。
- `ksu-module/system/vendor/etc/Hapticsconfig.xml`：ZUXOS 1.5.10.259 原厂标定，SHA-256 为 `0A81641AA9AE68A90739D815DA381D3A8E11695AD38560C9F14A94199899C5F2`。

## 构建要求

- JDK 17 或更新版本；
- Android SDK Platform/Build Tools（`android.jar`、`aapt2`、`d8`、`zipalign`、`apksigner`）；
- Xposed API 编译桩；
- 与既有发布 APK 相同的签名密钥，保证可覆盖升级。

## 构建流程

1. 单独编译 `app/stubs`，制成只供 `javac` 使用的桩 JAR。
2. 以 Java 8 字节码目标编译 `app/src`，classpath 包含 `android.jar` 与桩 JAR。
3. 只把应用源码生成的 class 制成 JAR，再用 D8（min API 28）生成 `classes.dex`；不要把桩类打入 DEX。
4. 用 AAPT2 编译资源并链接 Manifest，加入 `classes.dex` 与 `app/assets`。
5. `zipalign` 后使用发布密钥签名，并以 `apksigner verify --verbose --print-certs` 验证。
6. 将签名 APK 放入 `ksu-module/Y700HapticsControl.apk`。
7. 从 `ksu-module` 根目录封装 `5-Y700-Gen4-ColorOS-Haptics-Control-v3.0-KSU.zip`，保留 `system/vendor/etc/Hapticsconfig.xml`。

## 运行机制

- Vector Framework：安装脚本自动启用并设置 `system/0`。
- LSPosed：只勾选“系统框架”（`android`）。
- Hook 位于系统振动服务端，普通应用无需 Root。它在 `performHapticFeedbackInternal` 将 COUI 私有反馈常量 300–308 转换为当前 provider 支持的 ZUXOS/AOSP 语义；provider 级直接预制效果 Hook 作为兼容兜底。
- 用户没有为分类启用自定义时，标准 Android 请求保持原样并由 ZUXOS 原厂配置播放；通知、来电、闹钟等长波形不会被强制改成点击。SystemUI/Launcher 直接提交且没有 reason 的 ColorOS 65 ms 触摸波形会按场景转换成 ZUXOS CLICK、DOUBLE_CLICK 或 TICK。
- 新装默认所有自定义开关为关闭。schema 5 升级只会一次性关闭旧版启用位，保留每个分类的模式、强度、时长和多段编排。
- `Hapticsconfig.xml` 必须安装为 `u:object_r:vendor_configs_file:s0`；使用 `system_file` 会导致当前 ColorOS 音频 HAL 读取失败。安装脚本对原厂哈希与标签执行硬校验，失败即中止。
- 模块不修改内核、驱动电压、boost 或联想原厂增益上限。
