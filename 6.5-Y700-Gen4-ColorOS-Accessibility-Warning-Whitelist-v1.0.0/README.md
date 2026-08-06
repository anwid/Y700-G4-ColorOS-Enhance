# ColorOS Accessibility Warning Whitelist v1.0.0

对应发布 APK：`6.5-Y700-Gen4-ColorOS-Accessibility-Warning-Whitelist-v1.0.0-LSPosed.apk`

发布 APK SHA-256：`9F879638B06390B95A3B86F5B2E9A23728199689AADA27BFF655306BEC18111D`

## 用途

此模块只在 ColorOS 即将显示“应用修改无障碍权限”警告时检查目标包名。目标包命中白名单才跳过该警告；其他应用、安装检查、支付保护和普通安全弹窗不受影响。

默认已允许手写笔控制应用：

`dev.lorana.y700.colorospenui.xposed`

桌面界面允许每行添加一个额外包名。不要把它改成无条件跳过，也不要 Hook 整个反诈或安全服务的总开关。

## 目录

- `src/`：界面和 Xposed Hook 的 Java 源码。
- `res/`、`assets/`：图标和 Xposed 入口。
- `stubs/`：最小 Xposed 编译桩，仅用于编译。
- `AndroidManifest.xml`、`apktool.yml`：包信息和资源构建元数据。

构建产物、JADX 验证目录、签名密钥和 Android SDK 工具未收入本目录。

## 构建

需要 JDK、Android SDK 和 Xposed API。先单独编译 `stubs/` 作为 compile-only classpath，再编译 `src/`；不要把 `de.robv.android.xposed.*` 桩类打进 APK。使用 D8 生成 dex，完成资源打包、zipalign 和签名后，运行：

`apksigner verify --verbose <apk>`

重新签名后 SHA-256 会变化；覆盖安装还要求签名证书与已安装版本一致。

## 作用域

只选择：

- System Framework / 系统框架（Vector：`system/0`）

不要选择 Android 系统、系统界面、设置、输入法或所有应用。首次安装或修改作用域后需要完整重启一次。
