# Y700 Gen 4 ColorOS APK Parse Fix v1.3

这是发布包 `4-Y700-Gen4-ColorOS-APK-Parse-Fix-v1.3-LSPosed.apk` 对应的干净源码快照。
目录中只保留模块源码、Android 资源、Xposed 入口、作用域声明和一个验证器；不包含
反编译缓存、设备数据库、日志、签名密钥、已编译 DEX 或 APK。

## 解决的问题

部分 Y700 四代 ColorOS 移植 ROM 会在安装原本有效的 APK 时错误返回：

```text
Failure [-124: Failed parse during installPackageLI: Targeting R+ (version 30 and
above) requires the resources.arsc of installed APKs to be stored uncompressed and
aligned on a 4-byte boundary]
```

Hook 只在 `system_server` 的 `ParsingPackageUtils.parseBaseApk` 安装解析路径中工作。
系统原方法报告资源表已分配后，模块还会确认目标是 `/data/app/vmdl.../base.apk`，
并自行验证 `resources.arsc` 确实未压缩且满足 4 字节对齐。只有全部条件成立时才把
这一次误判改为 `false`；其他调用、无效 ZIP、真正压缩或错位的资源表均保留系统结果。

## 目录

- `src/`：Java Hook 源码。
- `res/`：模块图标。
- `assets/xposed_init`：legacy Xposed 入口。
- `META-INF/xposed/scope.list`：现代推荐作用域，内容为 `system`。
- `AndroidManifest.xml`：版本和 legacy Xposed 元数据。
- `test/TestValidator.java`：通过反射调用 ZIP/ARSC 验证器的简单命令行测试。

## 作用域兼容说明

目标始终是 **System Framework/系统框架**。Vector Framework 对 legacy
`xposedscope` 有兼容转换，因此 Manifest 中写 `android`，Vector 会把它映射成真正的
`system/0`；现代 `scope.list` 直接写 `system`。不要把 Vector 界面中的“Android 系统”
当成 System Framework。

## 构建概要

已验证的发布构建使用 JDK、Android SDK `android.jar`、Xposed API、D8、Apktool 3.0.3、
zipalign 33.0.2 和 APK 签名工具。大致流程如下：

1. 使用 `android.jar` 与 Xposed API 编译 `src/` 中的 Java，并用 D8 生成 `classes.dex`。
2. 在本目录执行 `apktool b` 构建资源 APK。
3. 把 `classes.dex` 和 `META-INF/xposed/scope.list` 加入 APK。
4. 执行 `zipalign -f -p 4`。
5. 使用自己的密钥签名；不要发布未签名 APK，也不要提交私钥或 keystore。
6. 用 `zipalign -c -v 4` 和签名验证工具检查成品，再在测试设备安装验证。

重新签名后的 APK 哈希必然与发布包不同；升级安装还要求签名证书与之前安装的版本一致。

## 发布包校验

```text
4-Y700-Gen4-ColorOS-APK-Parse-Fix-v1.3-LSPosed.apk
SHA-256: 37C30C4631C7C6386F8AF388E40B710F4079054B30D03E0C6C7B84BAF274E4BA
```

该模块不修复签名剥离、证书损坏、ABI 不兼容、版本降级、包冲突或真正不符合 APK
资源对齐要求的文件。
