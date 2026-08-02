> 本分支为 Android-only fork，版本号跟随上游 clash-sing/flutter_sing_box 的 release（2026-08-02 起，替代此前「冻结 1.1.4」策略）。每个版本条目分「与上游同步」与「本分支自研」两节；同步流程见 [docs/Upstream-Sync.md](docs/Upstream-Sync.md)。

## 1.1.5

### 与上游同步（clash-sing v1.1.5）
* 升级 Android 端 `libbox`（sing-box 内核）：`1.13.14` → `1.13.15`。
* 移除 Android 端未使用的位置权限（`ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` / `ACCESS_BACKGROUND_LOCATION`）。
* 存储层增强：`KeyValueStorage` 各 setter 接受 null（等价删除键）、新增 `getDouble`/`setDouble`；
  `CsSettingsStorage` 新增 `clash_api_port`、`test_url` 配置项（Android 侧暂无读方，Clash API
  端口的事实源仍在宿主应用的 using_config 补丁链路）。
* 服务管理接口形状对齐上游：门面/平台接口/方法通道新增 `queryServiceStatus` / `installService` /
  `uninstallService` / `startService` / `stopService`（Android 走默认实现，返回 `unsupported` /
  `true` / `false`），并吸收 `WindowsServiceStatus` 枚举与 `HelperConfig` 模型。
* Dart 插件自动注册：`MethodChannelFlutterSingBox.registerWith()` + pubspec `dartPluginClass`
  （Android 行为等价，主 library 相应导出方法通道实现）。
* 新增 `InboundType` 常量导出、`FlutterSingBoxConstants.assetBasePath` 与 `defaultClashApiPort`、
  Clash API 数据模型（`ClashApiProxy`、`ClashConfigs`）。
* 升级 `dio`：`^5.9.0` → `^5.10.0`。
* **未吸收**（本分支 Android-only）：Windows 平台全部实现（`FlutterSingBoxWindows`、`HelperCli`、
  原生插件与二进制资产、`asset_util` 及其 `crypto` 依赖、pubspec windows 声明）；上游 release
  所列「Clash API 端口动态适配」与「代理状态流控制器优化」的实现均位于 Windows 实现文件，一并跳过。

### 本分支自研

#### Features
* 新增 `checkConfig(String configuration)` API，在 Android 后台线程调用 libbox 校验配置，并提供稳定错误码。
* 新增 `urlTestOutbound(...)`，通过认证的 loopback Clash API 完成单 outbound 内核测速并返回延迟。
* VPN 会话名称和插件通知默认标题改为继承宿主应用标签，不再硬编码 `Clash Sing`。

#### Fixes
* `urlTestOutbound` 的 loopback controller 请求强制绕过系统代理（`Proxy.NO_PROXY`）。此前 TUN 的
  `platform.http_proxy` 开启时，Android 会把 controller 请求送进代理入站并按路由发往远端节点，
  表现为稳定的 `Clash API returned HTTP 502`（空响应体）。
* `setClashMode` 改为大小写不敏感匹配内核模式列表，并把列表中的精确值转发给 libbox（libbox 对
  未知模式静默忽略，插件必须先行校验）。避免配置里 `default_mode` / `clash_mode` 规则大小写
  不一致时误报 `INVALID_CLASH_MODE`。
* 模板配置：新增 `ip_is_private → direct` 路由规则（局域网/回环目标不再落入 `final` 代理出站）；
  修正 `clash_mode: direct` 规则此前与 `ip_is_private`、`domain_suffix` 组成 AND 条件导致
  直连模式下绝大多数流量仍走代理的问题。

#### Breaking Changes
* 插件调整为 Android-only，移除 iOS 注册、原生实现和示例工程。

#### Documentation
* 新增 Android 插件结构、运行链路、维护边界和包体优化说明。
* 删除旧 Flutter 示例工程，改为能力矩阵和分主题 Markdown 使用范例。
* 新增上游同步流程文档 [docs/Upstream-Sync.md](docs/Upstream-Sync.md)。

同步基线：clash-sing/flutter_sing_box v1.1.5（216830e）


## 👆👆👆👆 WEP-56 fork change log 👆👆👆👆（From 2026/7/31）

## 1.1.4
### Dependencies
* 升级 Android 端 `libbox`（sing-box 内核）：`1.13.12` → `1.13.14`


## 1.1.3
### Features
* 新增 naive 协议出站类型支持
* 新增独立的 TLS 配置模型（`tls.dart`）
* 为 Outbound 模型新增 `domainResolver` 字段
* DNS Server 模型新增 `domain_resolver` 与 `tls` 字段

### Fixes
* 修正 DNS Server 模型 `path` 字段类型：`List<String>` → `String`

### Improvements
* 移除 sing-box 模板配置中的 `sniff_override_destination` 选项


## 1.1.2
### Documentation
* 全面补全公开 API 的英文 dartdoc 注释，覆盖 constants、providers、services、network、storage 等模块，提升 pub.dev 文档覆盖率评分

### Dependencies
* 升级 `device_info_plus`：`^12.2.0` → `^13.1.0`
* 升级 `package_info_plus`：`^9.0.0` → `^10.1.0`


## 1.1.0
### ⚠️ 重要变更（Breaking Changes）
* 最低环境要求提升：Dart SDK `^3.9.0` → `^3.11.0`，Flutter `>=3.3.0` → `>=3.41.0`
* 修正 Android 插件包名拼写：`com.clashsiing.flutter_sing_box` → `com.clashsing.flutter_sing_box`（如有原生层引用或 consumer-rules 配置，请同步更新）
* 引入 freezed 代码生成框架，新增 `freezed` 与 `freezed_annotation` 依赖

### Features
* 新增 Rule Set（规则集）配置支持，并优化 sing-box 配置模板结构
* 支持自定义订阅请求的 User-Agent，并内置默认 User-Agent 生成工具
* 大幅扩展 sing-box 配置模型字段：Outbound 新增 `username`、`quic` 等出站配置；DNS / Route 模型补充多项字段
* 新增独立 storage 存储模块，引入 `KeyValueStorage` 抽象接口（支持 MMKV / Memory 等实现）
* 新增 `CsSettingsStorage`，用于管理按应用代理设置
* 新增 `ClientLog` 数据模型
* Profile 新增 `outboundsCount` 出站数量统计字段，并支持获取所有 Profile ID
* 新增 `defaultTestUrl` 常量

### Improvements
* 缓存 sing-box 版本号，避免重复获取（性能优化）
* 为模型添加 `explicitToJson` 配置，修复嵌套模型的 JSON 序列化问题
* 重构 VPN 服务启动逻辑，缓存事件流状态
* 升级 JSON 序列化依赖：`json_annotation` → 4.12.0、`json_serializable` → 6.14.0
* 移除 VpnService 的 `android:process=":remote"` 属性

### Fixes
* 修复 VPN 服务重启功能
* 修复 Clash 模式设置与连接管理问题
* 修复 Android 端关闭服务的逻辑
* `appendLogs` 方法增加 `logSink` 空值保护，避免空指针异常

### Refactoring
* 重构存储层：`ProfileManager` 重命名为 `ProfileStorage`，以 `KeyValueStorage` 抽象接口替代 MMKV 直接调用
* `ClientGroupItem` / `ClientGroup` 迁移至 freezed 数据类
* 将 Route 与 RuleSet 部分必填字段改为可选，提升配置灵活性
* 重构 Android 服务实现并更新依赖
* 清理废弃的 custom 模块及调试代码


## 1.0.12
### Features
* 新增应用级代理模式功能（禁用/排除/包含三种模式）
* 支持按应用列表配置代理规则

### Improvements
* 更新 MMKV 依赖至 2.4.0
* 优化应用列表存储方式（使用 JSON 数组格式）
* 更新 Android 构建环境（Kotlin 2.3.20, Gradle 8.14.4）

### Refactoring
* 移除 Inbound 模型中的 sniff 字段
* 重构应用列表数据结构（List 与 Set 转换优化）


## 1.0.11

* Update `sing-box` dependency to `1.12.25` for Android.

## 1.0.10

- **Route Rule**: Added IP CIDR and port filtering support
    - New fields: `ip_cidr`, `source_ip_cidr`, `port`, `port_range`, `source_port`, `source_port_range`
    - Enhanced traffic matching with IP and port-based routing
    - Full JSON serialization support with backward compatibility

## 1.0.9

### Features
* Add DNS rule action configuration and new domain matching fields
* Add `RuleAction` constants for routing rules

### Improvements
* Optimize proxy configuration template and simplify DNS/route rules
* Adjust default log level from `trace` to `info`
* Update User-Agent string for better compatibility

### Refactoring
* Remove remote rule set configuration (geoip-cn, geosite-cn)
* Simplify configuration structure and improve performance
* Make Route fields optional for flexibility

## 1.0.8

* Update `sing-box` dependency to `1.12.24` for Android.

## 1.0.7

* Update `sing-box` dependency to `1.12.23` for Android.
 
## 1.0.6

* Update `sing-box` dependency to `1.12.22` for Android.

## 1.0.5

* Update `sing-box` dependency to `1.12.20` for Android.

## 1.0.4

* Update `libbox` dependency to `1.12.19` for Android.
* Update documentation and project links in README.
* Bump version to 1.0.4.

## 1.0.3

* Add `getSingBoxVersion()` API to retrieve the underlying sing-box core version.
* Optimize memory usage and stability for long-running VPN services.
* Improve error handling during remote profile synchronization.
* Update dependencies to latest versions (dio, mmkv, package_info_plus, etc.).
* Minor bug fixes and performance improvements.

## 1.0.2

* Improve package description and API documentation coverage to increase pub score.
* Add comprehensive README with features, platform support, and basic usage.
* Add Chinese documentation (`README_CN.md`).
* Showcase projects using this plugin (clash_sing_app).

## 1.0.0

* Initial release of the `flutter_sing_box` plugin.
* Support for sing-box as a VPN service.
* Ability to import remote profiles.
* Clash API support for managing proxies and groups.
* UI for managing profiles and viewing connection status.
* Support for various protocols like Hysteria, TUIC, etc.
* Core functionalities like network service, profile management, and custom logging.
* Many bug fixes and performance improvements.
