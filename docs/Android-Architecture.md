# flutter_sing_box Android 结构说明

## 1. 定位与边界

此分支由 `WEP-56` 维护，服务于 FLsing 等 Android 客户端。插件只注册 Android 平台，不保留 iOS、macOS、Windows、Linux 或 Web 的原生实现和示例。

Android-only 的含义是减少无效平台声明、仓库噪音和错误维护预期。Flutter 构建本来就不会把 `ios/` 目录打入 Android APK，因此删除非 Android 文件不会直接降低 APK 体积。

## 2. 目录结构

```text
flutter_sing_box/
|- lib/
|  |- flutter_sing_box.dart                    # 对应用公开的入口
|  |- flutter_sing_box_platform_interface.dart # 平台接口
|  |- flutter_sing_box_method_channel.dart     # Method/EventChannel 实现
|  `- src/
|     |- constants/                            # 协议、状态和动作常量
|     |- core/provider/                        # JSON/YAML/Base64 配置转换
|     |- core/services/                        # 配置与网络服务
|     |- data/models/                          # sing-box、Clash、客户端和存储模型
|     |- data/network/                         # HTTP 访问封装
|     |- storage/                              # MMKV 与内存存储
|     `- utils/                                # 配置合并和格式辅助
|- android/
|  |- build.gradle                             # Android 插件和 libbox 依赖
|  `- src/main/
|     |- AndroidManifest.xml                   # VPN 服务与权限合并入口
|     |- aidl/                                 # 主进程与服务进程接口
|     |- kotlin/com/clashsing/flutter_sing_box/
|     |  |- FlutterSingBoxPlugin.kt            # Flutter 插件入口
|     |  |- cs/                                # 连接器、事件流与 libbox 初始化
|     |  `- utils/                             # 原生配置与设置存储
|     `- kotlin/io/nekohasekai/sfa/            # VPN/代理服务及平台适配层
|- assets/configs/                             # 默认 sing-box 配置模板
|- example/                                    # 文档驱动的能力与使用范例
`- test/                                       # Dart 单元测试
```

## 3. 运行链路

### 初始化

1. 应用调用 `FlutterSingBox().init()`。
2. Dart MethodChannel 调用 Android `FlutterSingBoxPlugin`。
3. `PluginManager` 初始化 MMKV、工作目录和 `Libbox.setup`。
4. `SingBoxConnector` 绑定 Android 服务，并建立状态、策略组、日志和代理状态事件流。

### 启停 VPN

1. `startVpn()` 通过 `VpnService.prepare` 请求系统 VPN 授权。
2. 授权通过后由 `BoxService.start()` 启动插件内的 VPN 服务。
3. `VPNService` 创建 TUN，`PlatformInterfaceWrapper` 向 libbox 提供 Android 网络、DNS、接口和应用信息。
4. libbox 负责解析配置、建立出站和处理流量。
5. `stopVpn()`、`serviceReload()` 和策略组选择通过原生服务或 libbox command client 执行。

### 数据回传

Android 服务通过 AIDL 向插件进程回传状态，`SingBoxConnector` 再写入 Flutter EventChannel。Dart 侧公开以下流：

- `connectedStatusStream`
- `groupStream`
- `clashModeStream`
- `logStream`
- `proxyStateStream`

插件还通过 MethodChannel 提供配置校验和两类测速命令：

- `checkConfig` 在后台线程完成 libbox 配置校验。
- `urlTest` 提交整组 command 测速，结果异步进入 `groupStream`。
- `urlTestOutbound` 通过活动实例的认证 loopback Clash API 返回单 outbound 延迟。

## 4. 原生依赖与包体

插件当前通过 Maven 引入：

```gradle
implementation("com.github.singbox-android:libbox:1.13.15")
```

`libbox.aar` 是 Android 原生产物，内部包含多个 ABI 的 `libbox.so`。包体问题需要区分两个层次：

1. 通用 APK 同时携带 `arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64` 时，体积会明显增大。
2. 即使只保留一个 ABI，单个 `libbox.so` 仍包含 sing-box 已编译启用的协议和功能。

因此，删除 iOS 文件只清理仓库结构，不会缩小 Android APK。

`pubspec.lock` 仍可能列出 `mmkv_ios`、`path_provider_windows` 等联合插件的平台包。这是 Dart/Flutter 的依赖解析结果，不表示这些平台的原生实现会注册或打入 Android APK。

包体优化应按以下顺序推进：

1. 发布 APK 时按 ABI 拆分，日常真机优先验证 `arm64-v8a`。
2. 上架应用商店时使用 AAB，由商店按设备 ABI 分发。
3. 用 APK Analyzer 记录各 ABI 的 `libbox.so`、`libflutter.so` 和 Dart AOT 体积，避免凭总包大小判断。
4. 只有在协议兼容范围明确后，才维护自定义 libbox 构建标签和独立 Maven 产物。裁剪协议会直接影响订阅兼容性，不能默认执行。

## 5. FLsing 集成方式

FLsing 通过 Git 分支引用插件，不引用本地路径：

```yaml
flutter_sing_box:
  git:
    url: https://github.com/WEP-56/flutter_sing_box.git
    ref: master
```

FLsing 主目录中的 `flutter_sing_box/` 是独立 Git 工作区，仅用于本地维护，已由 FLsing 的 `.gitignore` 排除。插件修改需要在插件仓库独立提交并推送；FLsing 再执行 `flutter pub get` 更新 lockfile 中的提交引用。

## 6. 当前维护风险

- Android 原生层包含从 sing-box-for-android 派生的 `io.nekohasekai.sfa` 服务代码，升级 libbox 时需要同步检查接口变化。
- 插件清单声明的权限较多，权限最小化应逐项结合实际调用核对，不能直接批量删除。
- VPN 服务当前使用独立 `:remote` 进程，MMKV、AIDL、事件流和重载行为都依赖多进程语义。
- `BoxService` 中将包名应用到 `VpnService.Builder` 的 `includePackage/excludePackage` 调用当前被注释，分应用代理恢复后必须做真机闭环验证。
- `onServiceAlert` 当前只在原生层记录错误并发送 stopped 状态，Dart 无法取得告警类型和消息。
- 插件已提供 `checkConfig` 配置校验 API，FLsing 不再直接编译依赖 libbox。
- 单 outbound 测速依赖活动配置中的认证 loopback Clash API；controller 缺失时返回稳定错误，不回退为物理地址 TCP 测速。
- `Dns`、`Route`、`Inbound` 等模型使用 `json_serializable`。修改带注解模型后必须重新运行代码生成，不能手改 `*.g.dart`。

## 7. 建议维护顺序

1. 保持 Android-only 注册和文档范例，阻止 Flutter 工具重新生成其他平台目录和演示应用。
2. 为服务状态、错误告警和命令完成反馈补充结构化协议。
3. 审计 AndroidManifest 权限、服务导出状态和前台服务类型。
4. 清点未被 FLsing 使用的 Dart API 与原生服务，确认无外部使用者后再删除。
5. 建立自定义 libbox 构建与发布流程，再评估协议级裁剪。
6. 每次升级 libbox 时验证启动、停止、重载、网络切换、分应用代理、日志流和配置校验。

## 8. 基础验证

插件仓库的最低代码验证为：

```bash
flutter analyze --fatal-infos
flutter test
```

FLsing 侧在插件提交推送并更新依赖后执行：

```bash
flutter pub get
flutter analyze
flutter test
```

APK 构建、安装、VPN、订阅兼容和真实网络测试由客户端项目完成。
