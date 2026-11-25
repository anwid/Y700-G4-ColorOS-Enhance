# Y700 Gen4 ColorOS Pen Compatibility v1.6.5

对应发布包：`6-Y700-Gen4-ColorOS-Pen-Compatibility-v1.6.5-KSU.zip`

发布包 SHA-256：`80FBF81E582868BD3AECFC19AB530E163AE17371127A11596E6EE0E8C1111025`

## 目录

- `ksu-module/`：KernelSU 模块源码和打包根目录。
- `control-app/`：控制应用及 Xposed Hook 的 Java 源码、资源和最小编译桩。
- `ksu-module/Y700PenControl.apk`：发布包内实际使用的预编译控制应用，便于核对和重新打包。

历史构建目录、旧 APK 备份、JADX 输出、ROM 提取物、签名密钥和 Android SDK 工具均未收录。

## v1.6.5 修复

- 修复按钮桥对 `getevent` 标签大小写过于敏感、导致双击扫描码 `0601` 偶发未转发的问题。
- 单击 `0600` 与双击 `0601` 继续使用独立路径，并保留最后一次按钮事件标记方便诊断。

## v1.6.4 修复

- 修复应用被结束或 ColorOS 重启后，无障碍服务不会自动恢复的问题。
- 看门狗不再读取受 SELinux 限制的 Vector 私有日志目录。
- 仅在独立的无障碍警告白名单模块已启用并作用于 `system/0` 后，才以 root 恢复本模块的无障碍服务。
- 保留 v1.6.3 的安全边界：不注入 vendor HAL，不在游戏模式内强行保留手写笔。

## 功能边界

- 使用内核已有的 `/proc/support_pen`，不包含 system/vendor 覆盖、触控 HAL 或 `.so` 注入。
- 接入 ColorOS 手写笔设置、随处写、悬停指针、按钮路由和输入法自动回退。
- 游戏模式内关闭笔，退出后恢复；控制应用不提供“游戏模式使用笔”实验开关。
- 启动时清除旧测试版遗留的 `y700_pen_in_game` 值。

## 构建控制应用

需要 JDK、Android SDK Platform 29、Android Build Tools 和 Xposed API。`control-app/stubs/` 仅用于编译，不得打入最终 APK。

1. 使用 `control-app/src/`、`control-app/res/`、`control-app/assets/` 和 `AndroidManifest.xml` 构建 APK。
2. 编译时将 Android 29 `android.jar` 与单独编译的 Xposed stubs jar 放入 classpath。
3. 使用 D8，最低 API 设为 28；最终 APK 的 target SDK 为 29。
4. 不要把 `de.robv.android.xposed.*` 编译桩打入 APK。
5. 使用自己的密钥签名，并用 `apksigner verify --verbose` 验证。
6. 将生成的 APK 替换为 `ksu-module/Y700PenControl.apk`，再打包 KSU 模块。

本地发布版使用 Android APK Signature Scheme v3。重新签名后 SHA-256 必然变化，升级安装还要求证书与已安装版本一致。

## 打包 KernelSU 模块

ZIP 根目录必须直接包含 `module.prop`、`customize.sh`、`service.sh`、其余脚本和 `Y700PenControl.apk`，不得额外套目录，也不要包含 `.bak`、build 目录或调试日志。

安装脚本会把控制应用的 Vector 作用域配置为：

- `system/0`
- `com.oplus.ipemanager/0`
- `com.oplus.healthservice/0`

### 手写输入法恢复延时

恢复滑块同时控制无障碍服务与 KernelSU 兜底。笔离开悬停感应范围后开始计时，期间重新靠近会取消本次恢复。升级后首次打开控制应用会迁移旧滑块设置。启动时会迁移旧常用输入法配置，并清除 ROM 用于固定 800 毫秒恢复的旧键。

### 轮盘例外应用与享做直发

在「Y700 手写笔控制 → ColorOS 系统笔功能 → 不弹轮盘的应用」支持应用名、包名、拼音模糊搜索及用户／系统应用筛选。勾选后保存。
默认勾选享做笔记（`com.newskyer.draw`）。取消全部勾选后，所有应用恢复 ColorOS 原有轮盘行为。
勾选包位于顶层任务时，Hook 跳过单击轮盘动作（`action=4`）和轮盘接收端的双击处理。
享做写字页若在例外列表内，单击由系统 Hook 直接发送享做已支持的双击事件。
其他已勾选应用只抑制轮盘。按键由 ROM 上报，模块不再启动 Shell 按钮桥接。
设置保存后立即用于下一次按键。可使用 `adb logcat -s Y700PenUI` 查看路由。

独立白名单模块 6.5 的作用域必须包含 `system/0`。如果使用 LSPosed 而非 Vector，需要手动选择对应作用域。

## 安全说明

不要重新加入已废弃的 vendor touchscreen HAL 覆盖，也不要把 `/proc/support_pen` 改为只读。前者曾导致第一屏 Logo 循环重启，后者会阻止系统在模式切换后恢复笔。
