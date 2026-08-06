# 振动控制 v2.11 源码说明

## 目录

- `app/src/`：应用界面、配置模型与 system_server Xposed Hook 的 Java 源码。
- `app/res/`、`app/assets/`、`app/AndroidManifest.xml`：Android 资源、入口和中文应用名。
- `app/stubs/`：编译时使用的最小 Xposed/隐藏 API 桩源码，不应打入最终 APK。
- `ksu-module/`：KernelSU 安装、开机服务、卸载与操作脚本；其中 APK 是当前 v2.11 预构建件。

## 构建要求

- JDK 17；
- Android SDK Platform/Build Tools 34（`android.jar`、`aapt2`、`d8`、`zipalign`、`apksigner`）；
- Xposed API 编译桩；
- 自己的 APK 签名密钥。

## 构建流程

1. 单独编译 `app/stubs` 并制成仅用于 `javac` classpath 的桩 JAR。
2. 以 Java 8 字节码目标编译 `app/src`；classpath 包含 `android.jar` 与桩 JAR。
3. 只把应用源码生成的 class 制成 JAR，再用 D8（min API 28）生成 `classes.dex`；不要把桩类打入 DEX。
4. 用 AAPT2 编译 `app/res`，链接 `app/AndroidManifest.xml`，加入 `classes.dex` 和 `app/assets`。
5. 用 `zipalign` 对齐并使用自己的密钥通过 `apksigner` 签名。
6. 将签名 APK 放为 `ksu-module/Y700HapticsControl.apk`。
7. 在 `ksu-module` 目录封装：

   `tar -a -cf 5-Y700-Gen4-ColorOS-Haptics-Control-v2.11-KSU.zip module.prop customize.sh service.sh action.sh uninstall.sh README.md Y700HapticsControl.apk`

## 运行范围

- Vector Framework：安装脚本自动启用并设置 `system/0`。
- LSPosed：只勾选“系统框架”（`android`）。
- 不替换 `/vendor/etc/Hapticsconfig.xml`，不修改内核、马达电压、boost 或厂商增益上限。
