# Y700 Gen4 ColorOS Pen Compatibility v1.6.5

对应发布包：`6-Y700-Gen4-ColorOS-Pen-Compatibility-v1.6.5-KSU.zip`

发布包 SHA-256：`80FBF81E582868BD3AECFC19AB530E163AE17371127A11596E6EE0E8C1111025`

## 目录

- `ksu-module/`：KernelSU 模块源码和打包根目录。
- `control-app/`：控制应用及 Xposed Hook 的 Java 源码、资源和最小编译桩。
- `ksu-module/system/app/dev.lorana.y700.colorospenui.xposed.apk`：发布包内实际使用的预编译控制应用，便于核对和重新打包。

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

- 使用内核已有的 `/proc/support_pen`，通过 `system/app` 挂载控制应用，不覆盖触控 HAL 或注入 `.so`。
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
6. 将生成的 APK 替换为 `ksu-module/system/app/dev.lorana.y700.colorospenui.xposed.apk`，再打包 KSU 模块。

本地发布版使用 Android APK Signature Scheme v3。重新签名后 SHA-256 必然变化，升级安装还要求证书与已安装版本一致。

## 打包 KernelSU 模块

ZIP 根目录包含模块脚本和 `system/app/dev.lorana.y700.colorospenui.xposed.apk`。KernelSU 需要启用支持系统挂载的元模块（如 Mountify），重启后由 Android 从 `/system/app` 扫描控制应用。安装时还会用同一 APK 安装普通应用副本，使应用在模块卸载后仍可保留。模块卸载脚本不卸载控制应用。

安装脚本会把控制应用的 Vector 作用域配置为：

- `system/0`
- `com.oplus.ipemanager/0`
- `com.oplus.healthservice/0`
- `com.google.android.inputmethod.latin/0`（Gboard 键盘交接）
- `com.heytap.mydevices/0`（设备空间）

### 设备空间手写笔卡片

设备空间 17.4.15（1704015）加载设备列表时，Hook 检查已连接的 Lenovo Tab Pen Plus，并使用设备空间自己的 `PENCIL` 卡片类型和单电池组件显示 OPPO Pencil。电量来自 ColorOS 蓝牙服务的缓存。设备空间页面收到该笔的电量变化或 ACL 连接状态广播时，复用原生卡片刷新入口；未上报时隐藏电量。若列表已有原生手写笔卡片，则保留原生列表。LSPosed 需勾选「设备空间」（`com.heytap.mydevices`），再重新打开其进程。

桌面小组件和设备空间共用设备库里的 `y700_lenovo_tab_pen_plus` 记录。模块通过设备空间原有的数据访问层写入笔的连接状态和单电池读数，并沿用其数据变化通知。充电标记来自笔通过 BLE GATT 电源状态特征（`0x2A1A`）上报的充电位。控制中心通过设备空间的原生设备更新回调接收新的电量和充电状态。小组件可在「选择设备」中选择 OPPO Pencil；从设备空间、控制中心或小组件点击笔卡片都会打开 ColorOS 原生 OPPO Pencil 详情，打开时读取连接、电量和充电状态，保持打开时随设备库更新。卡片底部的「手写笔设置」进入「Y700 手写笔控制」。原生详情需要启用「智能外设」（`com.oplus.ipemanager`）并将它加入 LSPosed 作用域。

### 手写与常用输入法

笔靠近时切到配置的手写输入法。Gboard 请求显示虚拟键盘时，改为切到已启用的常用输入法。笔触发的显示请求保留，以便启动随处写。
笔离开不切换输入法。启动时会迁移旧常用输入法配置，并清除 ROM 用于固定 800 毫秒恢复的旧键。
切换日志可使用 `adb logcat -s Y700Gboard Y700PenAccess` 查看。

### ColorOS 手写笔连接标记

智能外设 BLE 服务根据已配对的 Lenovo Tab Pen Plus 的实际连接维护 `ipe_pencil_connect_state`。ColorOS 便签可据此注册原生笔动作监听。模块只在需要时写入已连接状态，并在联想笔断开时清理由模块设置的状态。

### 实体按钮与原生双击动作

在系统广播投递前识别移植 ROM 的 `gugu_platform_hal` 所发的联想笔单击轮盘事件。ColorOS「轻点两下」设为工具或调色板时，原生双击事件只发给前台应用；设为轮盘时保留 ROM 的原始路由。设为直通的前台应用优先收到定向的 OPPO Pencil 双击事件，并由应用处理。

### 按键直通应用

在「Y700 手写笔控制 → ColorOS 系统笔功能 → 按键直通应用」可按应用名、包名或拼音搜索，并筛选用户应用或系统应用。保存后，按一次联想笔实体按钮，模块只向名单里的前台应用发送一次原生 OPPO Pencil 双击广播。按键动作由各应用的双击设置决定。享做笔记和 StarNote 使用相同规则。

默认勾选享做笔记（`com.newskyer.draw`）。取消全部勾选会关闭按键直通。保存的名单沿用原有配置，可用 `adb logcat -s Y700PenButton Y700PenUI` 查看路由。

独立白名单模块 6.5 的作用域必须包含 `system/0`。如果使用 LSPosed 而非 Vector，需要手动选择对应作用域。

## 安全说明

不要重新加入已废弃的 vendor touchscreen HAL 覆盖，也不要把 `/proc/support_pen` 改为只读。前者曾导致第一屏 Logo 循环重启，后者会阻止系统在模式切换后恢复笔。
