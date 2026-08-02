# 能力矩阵

本表描述 WEP-56 `master` 当前可依赖的行为。`Future` 的完成含义必须单独确认，不能统一理解为
“内核操作已经完成”。

## 核心通信

| 能力 | 公开入口 | 完成语义 | 前提与限制 |
| --- | --- | --- | --- |
| 初始化 | `FlutterSingBox.init()` | MethodChannel 初始化和服务连接命令已提交 | 需要可用的 Android Activity |
| 启动 VPN | `startVpn()` | VPN 授权完成并提交服务启动 | 最终状态以 `proxyStateStream` 为准 |
| 停止 VPN | `stopVpn()` | 停止命令已提交 | 最终状态以 `proxyStateStream` 为准 |
| 重载服务 | `serviceReload()` | 重载命令已提交 | 不保证新配置已成功启动 |
| 配置校验 | `checkConfig(configuration)` | libbox 校验完成 | 在插件后台线程运行，不替换文件 |
| 内核版本 | `getSingBoxVersion()` | 直接返回版本字符串 | 当前 AAR 为 1.13.15 |

## 策略组与测速

| 能力 | 公开入口 | 完成语义 | 前提与限制 |
| --- | --- | --- | --- |
| 切换模式 | `setClashMode(mode)` | command client 已接受命令 | mode 必须存在于内核模式列表 |
| 选择 outbound | `selectOutbound(...)` | 命令已提交 | 目标必须属于 selector group |
| 展开策略组 | `setGroupExpand(...)` | 命令已提交 | 主要用于组状态展示 |
| 整组测速 | `urlTest(groupTag)` | 仅表示命令提交成功 | 结果异步进入 `groupStream`，无请求 ID |
| 单 outbound 测速 | `urlTestOutbound(...)` | 返回时已经得到该 outbound 延迟 | 需要认证的 loopback Clash API；返回毫秒 |

libbox 1.13.15 的 command 绑定没有单 outbound URL test 方法。插件的单 outbound API 调用同一
sing-box 实例的 Clash API `/proxies/{tag}/delay`，实际连接仍由指定 outbound 建立。

## 事件流

| 流 | 数据 | 注意事项 |
| --- | --- | --- |
| `proxyStateStream` | `ProxyState` | 当前仍是服务状态收敛的主要依据 |
| `connectedStatusStream` | `ClientStatus` | 流量、连接数、内存和 goroutine 状态 |
| `groupStream` | `List<ClientGroup>` | 包含选中项及每个 item 的 URL test 历史 |
| `clashModeStream` | `ClientClashMode` | 模式列表和当前模式 |
| `logStream` | `List<ClientLog>` | 原生日志批次；宿主应限制内存保留量 |

EventChannel 可能重复推送、分批推送或在服务停止时中断。宿主必须管理订阅生命周期并处理
`onError`。

## 上游接口形状兼容（Android 上为空操作）

以下 API 为与上游 clash-sing 门面形状保持一致而存在（v1.1.5 起），服务概念仅存在于
Windows 桌面端；本分支 Android-only，不含任何 Windows 实现，宿主不应调用它们承载业务：

| 公开入口 | Android 返回值 |
| --- | --- |
| `queryServiceStatus()` | 恒 `WindowsServiceStatus.unsupported` |
| `installService(...)` | 恒 `true`（无需安装，视为就绪） |
| `uninstallService()` / `startService()` / `stopService()` | 恒 `false` |

## 配置与订阅

| 能力 | 类型/服务 | 边界 |
| --- | --- | --- |
| 订阅下载 | `NetworkService` | HTTP 下载和默认 Android User-Agent |
| 订阅导入 | `ProfileService` | 支持本地文件和远程地址 |
| sing-box JSON | `SingBoxConfigProvider` | 解析并补齐默认模板 |
| Clash YAML | `ClashProvider` | 只转换插件明确支持的代理类型 |
| Base64 分享链接 | `Base64Provider` | 当前协议覆盖有限，不是通用 URI 解析器 |
| 配置模型 | `SingBox`、`Dns`、`Route`、`Inbound`、`Outbound` 等 | 升级内核前需检查字段弃用 |
| Profile 存储 | `ProfileStorage` | 使用 `cs_profile` MMKV 和应用文档目录 |
| 插件设置 | `CsSettingsStorage` | 使用 `cs_settings` MMKV，包含分应用代理设置 |

## Android 原生能力

- Android `VpnService` 与 TUN 建立。
- libbox 平台网络、DNS、接口变化和应用信息桥接。
- Android 前台服务和通知。
- 主进程与 `:remote` VPN 进程之间的 AIDL 通信。
- 分应用 include/exclude 设置读取。
- 系统 HTTP 代理参数应用。

VPN 系统面板中的会话名称和插件通知默认标题继承宿主应用标签，不再硬编码插件品牌。

## 稳定错误码

| 错误码 | 含义 |
| --- | --- |
| `INVALID_ARGUMENTS` | 方法参数类型、空值或范围错误 |
| `CONFIG_EMPTY` | 配置字符串为空 |
| `CONFIG_INVALID` | libbox 拒绝配置 |
| `CLASH_API_UNAVAILABLE` | 活动配置未启用可用的 loopback Clash API |
| `URL_TEST_FAILED` | outbound 不存在、连接失败、超时或无延迟结果 |
| `PLUGIN_UNAVAILABLE` | Flutter engine 已分离或后台执行器不可用 |
| `VPN_PERMISSION_DENIED` | 用户拒绝 Android VPN 权限 |

其他历史方法仍可能返回各自的原生错误码。宿主应优先展示 `PlatformException.message`，日志中同时
保留 `code`，但不要记录订阅密钥或 Clash API secret。
