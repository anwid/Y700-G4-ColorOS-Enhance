# Y700 ColorOS 全局圆角关闭 v1.5

## 需求

- KernelSU
- meta-overlayfs
- Vector Framework 或兼容 LSPosed API 的 Xposed 框架

## Vector Framework

模块内的启动脚本会在重启后安装伴随 Hook APK、自动启用模块，并把作用域限制为 `com.android.systemui/0`。

## LSPosed

重启后打开 LSPosed 管理器，启用“Y700 全局圆角关闭”，作用域只选择“系统界面”（`com.android.systemui`），然后再完整重启。伴随 APK 已内置推荐作用域声明，兼容的管理器可以自动预选。

## 卸载

从 KernelSU 卸载本模块时，伴随 Hook APK 也会自动删除。完成后请重启设备。
