# 全局圆角关闭 v1.5 源码说明

## 目录

- `overlay-android/`：把 Android 框架圆角半径与修正值覆盖为 0。
- `overlay-systemui/`：覆盖 ColorOS/SystemUI 圆角 Drawable ID 数组。
- `xposed/`：SystemUI Hook 的 Apktool/smali 工程，禁止 RoundedCorner Decor Provider 创建遮罩。
- `ksu-module/`：KernelSU 模块模板及当前签名预构建件。

## 构建要求

需要 JDK 17、Apktool 3.x、Android SDK Build Tools，以及自己的 APK 签名密钥。

## 构建流程

1. 分别用 Apktool 构建 `overlay-android` 与 `overlay-systemui`，对齐、签名后替换 `ksu-module/system/product/overlay/` 中两个 APK。
2. 用 `apktool b xposed` 构建 Hook APK。
3. Apktool 默认不会保留现代 Xposed 元数据；签名前必须把 `xposed/META-INF/xposed/scope.list` 加回 APK。最终 APK 应同时包含：

   - `assets/xposed_init`
   - `assets/xposed_scope`
   - `META-INF/xposed/scope.list`

4. 对齐并签名后替换 `ksu-module/Y700RoundCornersOffXposed.apk`。
5. 在 `ksu-module` 目录封装：

   `tar -a -cf 7-Y700-Gen4-ColorOS-Global-Round-Corners-Off-v1.5-KSU.zip system customize.sh module.prop README.txt service.sh system.prop uninstall.sh Y700RoundCornersOffXposed.apk`

模块同时依赖 meta-overlayfs 和 Vector/LSPosed。Vector 作用域为 `com.android.systemui/0`；LSPosed 只选择“系统界面”（`com.android.systemui`）。
