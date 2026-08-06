# 显示修复 v1.3 源码说明

## 目录

- `overlay/`：面向 `android` 的静态 RRO，覆盖 `config_mainBuiltInDisplayCutout`，移除错误的虚拟刘海。
- `ksu-module/`：可直接封装的 KernelSU 模块模板；`system.prop` 同时清空 ColorOS 的 screen-hole 坐标。

## 构建

需要 JDK 17、Apktool 3.x、Android SDK Build Tools，以及自己的 APK 签名密钥。

1. 使用 `apktool b overlay -o Y700DisplayFixOverlay-unsigned.apk` 构建 RRO。
2. 使用 `zipalign` 对齐并用 `apksigner` 签名。
3. 将结果替换为 `ksu-module/system/product/overlay/Y700DisplayFixOverlay.apk`。
4. 在 `ksu-module` 目录执行：

   `tar -a -cf 2-Y700-Gen4-ColorOS-Display-Fix-v1.3-KSU.zip module.prop system.prop system`

发布包依赖 meta-overlayfs。此模块不使用 Vector/LSPosed，也没有 Xposed 作用域。
