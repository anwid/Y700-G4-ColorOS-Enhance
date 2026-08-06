# Y700 Gen4 ColorOS Pen Compatibility v1.6.3

对应发布包：`6-Y700-Gen4-ColorOS-Pen-Compatibility-v1.6.3-KSU.zip`

发布包 SHA-256：`D864E8304AF504B88CB02B500E10EA34DD1E580EA59C919C1CAC470C75EE822D`

## 目录

- `ksu-module/`：KernelSU 模块源码和打包根目录。
- `control-app/`：控制应用及 Xposed Hook 的 Java 源码、资源和编译桩。
- `ksu-module/Y700PenControl.apk`：发布包内实际使用的预编译控制应用，便于核对和直接重新打包。

历史构建目录、旧 APK 备份、JADX 验证输出、ROM 提取物、签名密钥和 Android SDK 工具未收入本目录。

## 功能边界

- 使用内核已有的 `/proc/support_pen`，不包含 system/vendor 覆盖、触控 HAL 或 `.so` 注入。
- 接入 ColorOS 手写笔设置、随处写、悬停指针、按钮路由和输入法自动回退。
- 监听 ColorOS 游戏模式状态；游戏模式内关闭笔，退出后恢复笔。
- 不支持在 ColorOS 游戏模式内强行保留联想笔。控制应用中已删除对应实验性开关。
- 模块启动时会把旧测试版遗留的 `y700_pen_in_game` 强制清零。

## 构建控制应用

需要 JDK、Android SDK Platform 29、Android Build Tools，以及 Xposed API。`control-app/stubs/` 是本项目使用的最小编译桩，只能参与编译，不能打入最终 APK。

基本流程：

1. 以 `control-app/src/`、`control-app/res/`、`control-app/assets/` 和 `AndroidManifest.xml` 构建 APK。
2. 编译应用类时把 Android 29 `android.jar` 和单独编译的 Xposed stubs jar 放入 classpath。
3. 使用 D8，最低 API 设为 28；最终 APK 的 target SDK 为 29。
4. 不要把 `de.robv.android.xposed.*` 编译桩打入 APK。
5. 使用自己的密钥签名，并用 `apksigner verify --verbose` 验证。
6. 将生成的 APK 替换为 `ksu-module/Y700PenControl.apk` 后再打包 KSU 模块。

本地发布版使用 Android APK Signature Scheme v3。重新签名后 SHA-256 必然变化，升级安装还要求证书与已安装版本一致。

## 打包 KernelSU 模块

ZIP 根目录必须直接包含 `module.prop`、`customize.sh`、`service.sh`、其余脚本和 `Y700PenControl.apk`，不要额外套一层目录。不要把 `.bak`、build 目录或调试日志放入 ZIP。

安装脚本会配置控制应用的 Vector 作用域为：

- `system/0`
- `com.oplus.ipemanager/0`

LSPosed 用户需要手动选择“系统框架”和 `com.oplus.ipemanager`，不要全选应用。

## 安全说明

不要重新加入已废弃的 vendor touchscreen HAL 覆盖，也不要把 `/proc/support_pen` chmod 为只读。前者曾导致第一屏 Logo 循环重启，后者会阻止系统在模式切换后恢复笔。
