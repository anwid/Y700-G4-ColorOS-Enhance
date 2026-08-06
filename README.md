# 联想拯救者 Y700 四代 ColorOS 移植版修复与优化模块

适用于 **联想拯救者 Y700 四代** 的 ColorOS 移植 ROM。

当前测试环境为酷安 **@微微薇** 制作的 OPPO Pad Mini 版 ColorOS ROM：

[https://www.coolapk.com/feed/73057139?s=NDljYzk2ZDAyOTliZDM4ZzZhNzQxOTBkega1650](https://www.coolapk.com/feed/73057139?s=NDljYzk2ZDAyOTliZDM4ZzZhNzQxOTBkega1650)

部分模块理论上可用于 **@GUF296 的一加金标 ROM**，但尚未全部测试。实际兼容情况请通过 GitHub Issue 反馈。

> 问题只在 GitHub Issue 中处理。请不要在酷安留言，我不常看。
>
> 装模块需要智商健全的人来操作，如果你什么都不懂，请把这里的文本拿去问 Deepseek，让它指导你。别问二臂豆包！

---

## 使用前说明

- 仅适用于联想拯救者 Y700 四代和对应的 ColorOS 移植 ROM。
- 安装、更新、停用或卸载模块后，均需要完整重启。
- 安装前请核对模块文件名、版本和 SHA-256。
- 不需要某项功能时，不要预防性安装对应模块。
- 遇到异常时，可先在 KernelSU 中停用对应模块并重启回退。
- Vector Framework 与 LSPosed 的作用域名称可能略有区别，请以本文描述的系统组件为准。
- 不要给 Xposed 模块勾选所有应用。

---

## 模块列表

| 编号  | 模块                                                                        | 功能                                | 主要依赖                            |
| --- | ------------------------------------------------------------------------- | --------------------------------- | ------------------------------- |
| 1   | [meta-overlayfs](https://github.com/KernelSU-Modules-Repo/meta-overlayfs) | 为资源覆盖类模块提供支持                      | KernelSU                        |
| 2   | Display Fix                                                               | 修复竖屏状态栏时间被错误刘海区域遮挡                | meta-overlayfs                  |
| 3   | Stock Audio                                                               | 恢复原厂有线耳机音量曲线和增益校准                 | meta-overlayfs                  |
| 4   | APK Parse Fix                                                             | 修复部分有效 APK 被 ColorOS 错误拒绝安装的问题    | Vector 或 LSPosed                |
| 5   | Haptics Control                                                           | 分场景调整系统振动和双马达效果                   | Vector 或 LSPosed                |
| 6   | Pen Compatibility                                                         | 将联想 Tab Pen Plus 接入 ColorOS 手写笔功能 | Vector 或 LSPosed                |
| 6.5 | Accessibility Warning Whitelist                                           | 跳过白名单应用触发的无障碍修改警告                 | Vector 或 LSPosed                |
| 7   | Global Round Corners Off                                                  | 移除系统绘制的软件圆角遮罩                     | meta-overlayfs、Vector 或 LSPosed |
| 8   | Stock Thermal                                                             | 恢复联想原厂 CPU、GPU 和电池温控配置            | meta-overlayfs                  |

---

## ROM 兼容性

| 模块                              | @微微薇 OPPO Pad Mini ROM | @GUF296 一加金标 ROM |
| ------------------------------- | ---------------------: | ---------------: |
| Display Fix                     |                     支持 |         **不要安装** |
| Stock Audio                     |                     支持 |             理论可用 |
| APK Parse Fix                   |                     支持 |    未知，出现相同错误时再测试 |
| Haptics Control                 |                     支持 |             理论可用 |
| Pen Compatibility               |                     支持 |             理论可用 |
| Accessibility Warning Whitelist |                     支持 |             理论可用 |
| Global Round Corners Off        |                     支持 |         **不要安装** |
| Stock Thermal                   |                     支持 |         **不要安装** |

---

# 安装

## 推荐安装顺序

### 1. 安装 meta-overlayfs

在 KernelSU 中安装：

`1-meta-overlayfs-v1.3.1-KSU.zip`

安装完成后完整重启。

以下模块依赖 meta-overlayfs：

- Display Fix
- Stock Audio
- Global Round Corners Off
- Stock Thermal

---

### 2. 修复竖屏状态栏

在 KernelSU 中安装：

`2-Y700-Gen4-ColorOS-Display-Fix-v1.3-KSU.zip`

安装完成后完整重启。

该模块会关闭 ROM 按 OPPO Pad Mini 写入的错误屏幕刘海配置，修复以下问题：

- 竖屏状态栏时间只显示小时或部分字符
- 状态栏左侧预留不存在的遮挡区域
- 时间和通知图标排列异常

修复后，竖屏状态栏会从左侧依次显示完整时间和通知图标，横屏布局保持不变。

该模块：

- 只需要 KernelSU 和 meta-overlayfs
- 不需要 Vector 或 LSPosed
- 没有需要设置的作用域
- 不是通用状态栏美化模块

> 不要安装到 @GUF296 的 ROM。

---

### 3. 恢复原厂音频行为

在 KernelSU 中安装：

`3-Y700-Gen4-ColorOS-Stock-Audio-v1.0-KSU.zip`

安装完成后完整重启。

该模块会：

- 恢复联想原厂有线耳机音乐音量曲线
- 恢复有线耳机增益校准
- 关闭当前无法初始化并持续重试的 OPlus AudioX

该模块不会修改：

- 蓝牙音频
- USB 音频
- Audio HAL
- DSP
- 物理分区

理论上可用于 @GUF296 的 ROM。

---

### 4. 修复部分 APK 安装错误

将以下 APK 作为普通应用安装：

`4-Y700-Gen4-ColorOS-APK-Parse-Fix-v1.3-LSPosed.apk`

然后在 Vector Framework 或 LSPosed 中启用模块。

作用域只能选择：

- **System Framework**
- **系统框架**
- Vector 内部对应 `system/0`

不要选择：

- Android 系统
- `android/0`
- 系统界面
- 设置
- 所有应用

设置完成后完整重启。

该模块用于修复以下安装错误：

`Failure [-124: Failed parse during installPackageLI: Targeting R+ (version 30 and above) requires the resources.arsc of installed APKs to be stored uncompressed and aligned on a 4-byte boundary]`

它只会在系统对安装暂存 APK 作出错误判断，并且模块重新检查确认 `resources.arsc` 实际未压缩且满足四字节对齐时，绕过该错误结果。

该模块不会：

- 修改待安装 APK
- 修改 `resources.arsc`
- 修改 APK 签名
- 修改 PackageManager 数据库
- 修改系统分区
- 关闭其他 APK 安全检查
- 修复证书、签名、ABI、降级安装或包冲突问题

没有出现上述 `-124` 错误的 ROM 不需要安装。

---

### 5. 安装触感控制

在 KernelSU 中安装：

`5-Y700-Gen4-ColorOS-Haptics-Control-v2.11-KSU.zip`

安装完成后完整重启。

KSU 模块会自动安装桌面应用：

**Y700 触感控制**

不要单独提取并安装伴随 APK。只安装 APK 不会加载 system\_server Hook。

#### Vector Framework

安装脚本会自动：

- 启用伴随模块
- 设置 `system/0` 作用域

刷入后完整重启即可。

#### LSPosed

在 LSPosed 中启用 **Y700 触感控制**，作用域只选择：

- System Framework
- 系统框架

不要选择系统界面、设置、桌面或所有应用。

设置完成后再次完整重启。

#### 支持的振动分类

可以分别控制：

- 返回手势
- 主页和多任务
- 锁屏密码
- 当前输入法
- 普通应用触摸
- 硬件按键
- 通知
- 未分类振动

每个分类关闭时，会保留 ROM 原始行为；开启后才使用自定义设置。

#### 支持的振动模式

- 系统标准效果
- 合成原语
- 限制最长时间
- Y700 双马达预设
- 自定义多段序列

自定义多段序列可以混合：

- 标准效果
- CLICK、THUD、SPIN、RISE、FALL、TICK 等合成原语
- 纯停顿
- 1 至 300 ms 的可调脉冲
- 每段独立强度
- 每段前置延迟

设置会实时保存。Hook 正确加载后，修改振动参数通常不需要重启。

模块支持通过 Android 文件选择器导入和导出 JSON 配置。

该模块不会修改：

- `Hapticsconfig.xml`
- 内核
- 马达电压
- Boost 电压
- 厂商增益上限

最终振动强度仍受马达、固件和 ColorOS HAL 限制。

---

### 6. 安装手写笔兼容模块

在 KernelSU 中安装：

`6-Y700-Gen4-ColorOS-Pen-Compatibility-v1.6.3-KSU.zip`

建议安装后暂时不要重启，继续安装下面的 6.5 模块，最后只完整重启一次。

该模块会自动安装桌面应用：

**Y700 ColorOS 手写笔控制**

它会将联想 Tab Pen Plus 接入 ColorOS 的以下功能：

- ColorOS 官方手写笔设置
- 随处写
- 右上角下扫动作
- 双击动作
- 全局悬停小圆点
- 标准手写笔按钮事件
- 自动输入法切换和回退

该模块不要求固定 120 Hz，也不会修改：

- 刷新率
- HighReportRate
- 触控采样率
- `report_threshold`
- 游戏模式配置
- 触控 HAL
- Vendor `.so`
- 物理分区

> ColorOS 游戏模式下无法使用手写笔。需要在游戏中使用手写笔时，请将该游戏移出 ColorOS 游戏空间。

#### Vector Framework

安装脚本会自动配置两个作用域：

- `system/0`
- `com.oplus.ipemanager/0`

#### LSPosed

需要手动启用 **Y700 ColorOS 手写笔控制**，并选择两个作用域：

- System Framework / 系统框架
- `com.oplus.ipemanager`

不要勾选所有应用。

---

### 6.5 安装无障碍警告白名单

将以下 APK 作为普通应用安装：

`6.5-Y700-Gen4-ColorOS-Accessibility-Warning-Whitelist-v1.0.0-LSPosed.apk`

在 Vector Framework 或 LSPosed 中启用模块。

作用域只能选择：

- System Framework
- 系统框架
- Vector 内部对应 `system/0`

不要选择：

- Android 系统
- 系统界面
- 设置
- 输入法
- 所有应用

设置完成后完整重启。

该模块只会在 ColorOS 即将显示无障碍修改警告时读取目标包名，目标应用命中白名单后才跳过该警告。

它不会全局关闭：

- ColorOS 反诈功能
- 应用安装检查
- 支付保护
- 普通安全弹窗
- 其他应用的无障碍警告

手写笔控制应用已经内置白名单，不需要重复填写。

不使用自动恢复无障碍，或从未遇到十秒警告的用户，可以不安装 6.5。

---

### 7. 关闭系统软件圆角

在 KernelSU 中安装：

`7-Y700-Gen4-ColorOS-Global-Round-Corners-Off-v1.5-KSU.zip`

安装完成后完整重启。

该模块用于移除 @微微薇 ROM 中由系统绘制的黑色圆角遮罩。

它不会改变：

- 屏幕实体边框
- 保护膜边缘
- 保护壳形状
- 面板本身的物理圆角

该模块同时使用：

- Android 和 SystemUI 资源覆盖
- SystemUI Hook

因此必须同时具备：

- meta-overlayfs
- Vector Framework 或 LSPosed

#### Vector Framework

安装脚本会自动：

- 启用伴随 Hook
- 将作用域限制为 `com.android.systemui/0`

#### LSPosed

启用 **Y700 全局圆角关闭**，作用域只选择：

- 系统界面
- `com.android.systemui`

不要选择系统框架或所有应用。

设置完成后完整重启。

> 不要安装到 @GUF296 的 ROM。

---

### 8. 恢复联想原厂温控

在 KernelSU 中安装：

`8-Y700-Gen4-ColorOS-Stock-Thermal-v1.1-KSU.zip`

安装完成后完整重启。

该模块会恢复 TB322 ZUXOS 1.5.10.259 中的 14 份原厂配置，包括：

- CPU 温控
- GPU 温控
- 电池温控
- Common 温控

该模块不会包含或修改：

- `/vendor/etc/display/thermallevel_to_fps.xml`
- 屏幕刷新率
- 游戏高触控采样
- 手写笔节点
- Display 温控降帧配置

适合希望保留当前 120 Hz 和游戏高触控配置，同时恢复联想原厂温控保护的用户。

> 不要安装到 @GUF296 的 ROM。

---

# 手写笔控制说明

## 推荐安装方式

最省事的安装顺序：

1. 在 KernelSU 中安装手写笔兼容模块。
2. 暂时不要重启。
3. 安装无障碍警告白名单 APK。
4. 在 Vector 或 LSPosed 中启用两个模块。
5. 核对作用域。
6. 完整重启一次。

已经先安装手写笔模块并重启也没有关系，之后安装 6.5、设置作用域并再次重启即可。

---

## 无障碍授权状态

控制应用底部显示的是系统保存的无障碍组件授权状态。

只有显示 **已启用**，才表示授权项已经存在。

主模块会在以下情况自动恢复服务：

- 系统开机
- 用户解锁
- ColorOS 杀死服务后

该服务不会读取输入文字，只会：

- 判断当前焦点是否为可编辑区域
- 监听手写笔靠近和离开
- 监听手写笔按钮事件

---

## 自动输入法切换

默认常用输入法：

`com.baidu.input/.ImeService`

默认手写输入法：

`com.sohu.inputmethod.sogouoem/.SogouIME`

使用前需要先在系统中启用对应输入法，然后在 **Y700 ColorOS 手写笔控制** 中选择输入法，并点击 **保存输入法选择**。

保存操作会同时更新：

- 界面服务配置
- Root 回退脚本配置

不要只修改系统默认输入法。

工作逻辑：

- 手写笔靠近可编辑框时，切换到支持 Android Stylus Handwriting 的输入法
- 手写笔离开后约 800 ms，恢复常用输入法
- 如果用户在这 800 ms 内手动切换了其他键盘，模块不会覆盖新的选择

也可以通过 Root ADB 设置完整 IME ID：

`adb shell su -c 'settings put global y700_pen_normal_ime 包名/服务名'`

`adb shell su -c 'settings put global y700_pen_handwriting_ime 包名/服务名'`

只有输入法自身实现 Android Stylus Handwriting，才能支持随处写。模块无法为不支持该接口的输入法增加手写识别。

---

## 手写笔按钮

联想 Tab Pen Plus 只有一个实体按钮。

默认行为：

- 单击：转换为 Android 标准手写笔主按钮 `KEYCODE_STYLUS_BUTTON_PRIMARY`，键值 308
- 双击：交给 ColorOS 官方 `PENCIL_DOUBLE_CLICK` 动作

不要在同一个绘图应用中把单击和双击绑定为相同行为。

可以通过 Root ADB 修改单击路由：

关闭单击：

`adb shell su -c 'settings put global y700_pen_single_button_route 0'`

发送标准键值 308，默认值：

`adb shell su -c 'settings put global y700_pen_single_button_route 1'`

交给 ColorOS 单击动作：

`adb shell su -c 'settings put global y700_pen_single_button_route 2'`

修改后可重启，或重新触发按键配置。

---

## 悬停小圆点

悬停指针由两个开关共同控制：

1. ColorOS 原生的“允许应用显示笔悬停指针”
2. 控制应用中的“补全全局悬停小圆点”

第二项需要正确的 System Framework 作用域，并在完整重启后加载。

关闭“补全全局悬停小圆点”后，会保留 ROM 原始的悬停行为。

是否显示小圆点只代表指针渲染状态，不代表笔尖坐标或压感是否正常。

---

## 压感

压感来自 NVT 触控驱动，并由 Android 输入系统归一化。

本模块不会伪造或修改压力曲线。

最终是否有压感还取决于绘图应用是否读取 `MotionEvent pressure`。

请使用支持压感的第三方绘图软件测试。不要只使用本 ROM 的 ColorOS 笔记判断。

ColorOS 笔记点击画笔后返回或闪退，属于该应用自身的图形库问题，不代表手写笔输入链路失效。

---

## 游戏模式限制

不支持在 ColorOS 游戏模式中强行保留手写笔。

进入游戏模式后，ColorOS 会切换 HighReportRate 和 `game_mode`，联想触控固件可能停止手写笔数据流。

退出游戏模式后，模块会恢复 `support_pen`。

确实需要在游戏中使用手写笔时请将游戏移出 ColorOS 游戏空间

不要通过 ADB 强制写入触控节点。

---

# APK 解析修复说明

## 如何验证

重启后，使用原本会出现 `-124` 的 APK 测试：

`adb install --no-streaming "待测试.apk"`

显示 `Success` 表示安装解析已经通过。

检查 Hook 是否进入 system\_server：

Windows：

`adb logcat -d | findstr /i "ColorOSApkParseFix"`

Linux 或 macOS：

`adb logcat -d | grep -i "ColorOSApkParseFix"`

正常日志应包含：

`hook installed in system_server`

---

## 不属于修复范围的问题

以下问题不会由本模块修复：

- APK 中的 `resources.arsc` 确实被压缩
- APK 对齐确实错误
- APK 签名被移除
- 证书损坏
- ABI 不兼容
- 版本降级
- 已安装包签名冲突
- APK 文件损坏

例如以下错误不属于本模块处理范围：

- `INSTALL_PARSE_FAILED_NO_CERTIFICATES`
- `Signature stripped?`

不要为了 `-124` 错误随意解压和重新打包 APK。重新打包通常会破坏原始签名。

---

# 触感控制说明

## 分类开关

每个分类的开关关闭时，会保留 ROM 原始振动。

开启后，才会使用该分类中的自定义模式和参数。

部分第三方应用没有使用标准振动用途，可能被归入：

- 其他应用触摸
- 未分类振动

---

## 标准效果与合成原语

标准效果和系统自带效果的波形由 ColorOS HAL 决定。

强度可能只存在 LIGHT、MEDIUM、STRONG 等少量档位，因此拖动相邻强度值时，实际效果可能没有明显区别。

合成原语由系统和硬件实现。部分原语在 Y700 固件中的映射可能相近，因此不同名称不一定有明显差异。

---

## 限制最长时间

该模式会将过长反馈截取为指定时长的脉冲。

可调范围：

- 1 至 300 ms

通常 15 至 45 ms 仍能保留清晰的段落感。继续增加时长会逐渐变成长而偏软的振动。

标准效果和预制效果的时长由 HAL 固定。“最终目标时长”对这些模式可能没有效果。

需要精确控制时长时，请使用：

- 限制最长时间
- 自定义多段序列中的限时脉冲段

---

## 默认配置

主页和多任务的默认配置为：

- QUICK\_RISE
- 20 ms 脉冲
- 60% 强度
- 7 ms 前置延迟

默认值只会用于：

- 第一次安装
- 点击恢复默认设置

升级模块不会覆盖已有配置。

---

# 排错

## 通用排查顺序

出现“没有效果”时，请按以下顺序检查：

1. 确认模块文件名和版本正确。
2. 确认 KernelSU 中模块处于启用状态。
3. 确认 meta-overlayfs 等依赖已经安装并启用。
4. 确认 Vector 或 LSPosed 模块已经启用。
5. 确认作用域完全正确。
6. 确认修改作用域后做过完整重启。
7. 再收集日志并提交 GitHub Issue。

不要只重启 SystemUI、桌面或框架管理器。安装和作用域变更后需要完整重启系统。

---

## 显示修复无效

确认：

- 使用的是 @微微薇 OPPO Pad Mini 版 ROM
- meta-overlayfs 已启用
- Display Fix 已启用
- 安装 meta-overlayfs 后重启过一次
- 安装 Display Fix 后又完整重启过一次

该模块不适用于 @GUF296 ROM。

---

## APK 解析修复无效

确认：

- 安装的是 v1.3
- 模块已经启用
- 作用域只有 System Framework
- 没有选择 Android 系统
- 修改作用域后做过完整重启
- 日志中存在 `hook installed in system_server`

如果错误已经变成证书、签名、ABI 或包冲突错误，说明安装流程已经越过 `-124`，新的错误不属于本模块处理范围。

---

## 触感控制无效

确认：

- KernelSU 中 Haptics Control 已启用
- 桌面上存在 Y700 触感控制
- Vector 使用 `system/0`
- LSPosed 只选择 System Framework
- 修改作用域后完整重启
- 对应振动分类开关已经打开

应用显示“已启用”不等于 Hook 一定已经加载。作用域错误时，应用仍可能正常打开，但不会影响系统振动。

---

## 软件圆角仍然存在

确认：

- meta-overlayfs 已启用
- Global Round Corners Off 已启用
- Vector 或 LSPosed 中伴随 Hook 已启用
- 作用域只有 `com.android.systemui`
- 修改作用域后完整重启

建议使用另一台设备拍摄屏幕四角，并显示纯白或浅色全屏页面进行判断。

不要只使用系统截图。部分最终遮罩可能不会稳定出现在截图中。

---

## 手写笔异常

按以下顺序检查：

1. 确认手写笔已连接且电量正常。
2. 确认 KernelSU 中手写笔兼容模块已启用。
3. 确认两个 Xposed 作用域正确。
4. 确认无障碍状态显示为已启用。
5. 完整重启。
6. 使用支持压感的第三方绘图软件测试。
7. 最后收集日志。

手写笔模块需要以下作用域：

- System Framework
- `com.oplus.ipemanager`

警告白名单只需要：

- System Framework

不要通过是否显示悬停圆点单独判断笔输入是否正常。

---

## 手写笔日志

执行：

`adb shell su -c 'logcat -d | grep -E "Y700PenCompat|Y700PenBridge|Y700PenProximity|Y700PenUI|ColorOSFraudWhitelist"'`

同时收集：

`adb shell settings get secure enabled_accessibility_services`

`adb shell settings get secure default_input_method`

`adb shell settings get global y700_pen_near`

Windows 用户可以先执行完整 logcat，再使用 `findstr` 过滤相同关键词。

---

## 误装旧版手写笔测试包后卡在 Logo

如果曾安装包含旧版 HAL 覆盖的测试包，并在第一屏 Logo 循环：

1. 在开机 Logo 出现后，连续按住或多次按音量下。
2. 尝试触发 KernelSU 安全模式。
3. 进入系统后停用或删除旧版手写笔模块。
4. 完整重启。

v1.6.3 不包含：

- `system/`
- `vendor/`
- 触控 HAL 覆盖
- `.so` 注入
- 早期 Bind Mount

升级前应先删除或停用其他旧版手写笔修复模块，避免多个脚本同时读取笔设备或重复发送按钮事件。

不要为脚本模块问题随意刷写 Boot 或 Vendor 分区。

---

# 正常验证标准

## 显示修复

- 竖屏状态栏时间完整显示
- 时间位于左侧
- 通知图标排列在时间之后
- 横屏布局正常

## APK 解析修复

- 原本报 `-124` 的有效 APK 可以安装
- logcat 中存在 system\_server Hook 安装日志

## 触感控制

- 各分类开关能够独立生效
- 修改参数后振动效果实时变化
- JSON 配置可以正常导入和导出
- 未开启的分类保持 ROM 原始行为

## 手写笔

- ColorOS 手写笔设置可以打开
- 笔尖可以正常书写
- 支持压感的软件中存在压力变化
- 悬停时可以显示小圆点
- 单击在支持标准笔键的软件中触发一次
- 双击触发一次 ColorOS 动作
- 靠近可编辑框时切换手写输入法
- 离开约 800 ms 后恢复常用输入法
- 重启后无障碍状态显示已启用
- 安装 6.5 后不出现十秒警告

## 圆角关闭

- 系统绘制的黑色软件圆角遮罩消失
- 实机照片中屏幕四角不再被系统遮罩裁切

---

# 卸载

## KernelSU 模块

在 KernelSU 中卸载或停用对应模块，然后完整重启。

适用于：

- Display Fix
- Stock Audio
- Haptics Control
- Pen Compatibility
- Global Round Corners Off
- Stock Thermal

卸载 Haptics Control 时：

- 伴随 APK 会自动删除
- 已保存的触感配置会保留，方便重新安装后恢复

卸载 Pen Compatibility 时：

- Y700 ColorOS 手写笔控制应用会自动删除
- 已安装和启用的系统输入法不会被卸载

卸载 Global Round Corners Off 时，伴随 Xposed APK 会自动删除。

---

## APK 解析修复

1. 在 Vector 或 LSPosed 中禁用模块。
2. 卸载 APK。
3. 完整重启。

---

## 无障碍警告白名单

1. 在 Vector 或 LSPosed 中禁用模块。
2. 卸载 APK。
3. 完整重启。

只卸载 6.5 不会影响手写笔功能，但 ColorOS 后续自动恢复无障碍时，可能再次出现警告。

---

## 完整卸载手写笔相关模块

1. 在 Vector 或 LSPosed 中禁用 Y700 ColorOS 手写笔控制。
2. 禁用 ColorOS 无障碍警告白名单。
3. 在 KernelSU 中卸载 Pen Compatibility。
4. 卸载 6.5 APK。
5. 完整重启。

---

# GitHub Issue 反馈要求

问题请提交到 GitHub Issue。

请不要在酷安留言，我不常看，也不会在那里持续处理排错。

Issue 至少需要包含：

- 设备型号
- ROM 作者
- ROM 完整版本号
- KernelSU 版本
- Vector Framework 或 LSPosed 版本
- 模块完整文件名和版本
- KernelSU 模块启用状态
- Xposed 模块作用域截图
- 是否进行过完整重启
- 具体复现步骤
- 预期结果
- 实际结果

不同问题还需要附加：

| 问题类型     | 必须提供                                        |
| -------- | ------------------------------------------- |
| 状态栏问题    | 系统截图                                        |
| 圆角问题     | 另一台设备拍摄的实机照片                                |
| 振动问题     | 应用导出的 JSON 配置                               |
| APK 安装问题 | APK 的 SHA-256、完整 `adb install` 输出、相关 logcat |
| 手写笔问题    | 当前默认输入法、两个作用域截图、相关状态输出和日志                   |

请明确描述：

- 在哪个场景出现问题
- 执行了什么操作
- 预期发生什么
- 实际发生什么

不要只提交以下描述：

- 没用
- 不生效
- 还是有
- 笔没了
- 没有点
- 切换坏了
- 装不上

---

# SHA-256

## Display Fix

文件：

`2-Y700-Gen4-ColorOS-Display-Fix-v1.3-KSU.zip`

SHA-256：

`BA4CC636C3AE899B4324949AC93231F5262236E2B38E2CE33D3CF166349D3255`

---

## APK Parse Fix

文件：

`4-Y700-Gen4-ColorOS-APK-Parse-Fix-v1.3-LSPosed.apk`

SHA-256：

`37C30C4631C7C6386F8AF388E40B710F4079054B30D03E0C6C7B84BAF274E4BA`

---

## Haptics Control

文件：

`5-Y700-Gen4-ColorOS-Haptics-Control-v2.11-KSU.zip`

SHA-256：

`5C6606FD15484297F266B8A8D2C756178DF17DB03B1C8DC516D2A3981A88C189`

---

## Pen Compatibility

文件：

`6-Y700-Gen4-ColorOS-Pen-Compatibility-v1.6.3-KSU.zip`

SHA-256：

`D864E8304AF504B88CB02B500E10EA34DD1E580EA59C919C1CAC470C75EE822D`

---

## Accessibility Warning Whitelist

文件：

`6.5-Y700-Gen4-ColorOS-Accessibility-Warning-Whitelist-v1.0.0-LSPosed.apk`

SHA-256：

`9F879638B06390B95A3B86F5B2E9A23728199689AADA27BFF655306BEC18111D`

---

## Global Round Corners Off

文件：

`7-Y700-Gen4-ColorOS-Global-Round-Corners-Off-v1.5-KSU.zip`

SHA-256：

`F48FD831491310574169FB3E4757369D03657E5C1AFB2B28963FAB1D12FDDC6A`

---

## Stock Thermal

文件：

`8-Y700-Gen4-ColorOS-Stock-Thermal-v1.1-KSU.zip`

SHA-256：

`90EB7C97E1D8BFD845483BA7236E25809A689528DA5ACDB4EF497D7E59525F3B`

---

安装前请核对文件名、版本和 SHA-256。安装或卸载后均需完整重启。
