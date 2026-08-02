# 上游同步流程（Upstream Sync）

更新时间：2026-08-02

本文档定义 WEP-56 fork（本仓库）跟随上游 [clash-sing/flutter_sing_box](https://github.com/clash-sing/flutter_sing_box) release 更新的完整流程，写给后续 agent / 维护者按步骤直接执行。2026-08-02 的 v1.1.5 同步即按本流程完成，实录见附录 A，可作范例对照。

配套阅读：插件结构与维护边界见 [Android-Architecture.md](Android-Architecture.md)；FLsing 宿主侧的 git 依赖锁定铁律见 FLsing 仓库 `docs/Handoff.md` 第三节。

## 0. 本分支的两处有意分歧（永不回收上游对应改动）

1. **客户端支持简化**：Android-only。已删除 iOS/桌面/Web 的注册、原生实现与旧 example 应用；上游为这些平台新增的实现一律不吸收。
2. **内核通信能力强化**：本分支自研 API，上游没有。合并冲突区文件时必须逐项确认以下能力完好：
   - `checkConfig(String)`：Dart 三层（门面/平台接口/方法通道）+ Kotlin 后台线程 libbox 校验、稳定错误码；
   - `urlTestOutbound(...)`：Dart 三层 + Kotlin 经认证 loopback Clash API 单点测速，**`Proxy.NO_PROXY` 强制绕过系统代理**；
   - `setClashMode` 大小写不敏感校验并把列表精确值转发给 libbox；
   - 模板配置自研规则：`ip_is_private → direct` 独立规则、`clash_mode` 单字段规则（多字段是 AND，禁止合并）、`cache_file`、mixed 入站；
   - VPN 会话名称与通知默认标题继承宿主应用标签（不硬编码 `Clash Sing`）。

## 1. 前置检查

```powershell
Set-Location D:\FLsing\flutter_sing_box   # 所有 git/flutter 命令前显式定位，防工作目录漂移
git status                                # 工作区应干净（或只含本次同步的准备改动）
git log --oneline -3                      # 确认 master 与 origin/master 一致
```

FLsing 仓库（`D:\FLsing`）也应处于干净可验证状态——第 7 步要做跨仓预验证。

## 2. upstream remote 与 fetch

```powershell
git remote add upstream https://github.com/clash-sing/flutter_sing_box.git   # 仅首次
git fetch upstream --tags
git tag --list                            # 确认目标 tag 已到位
git log -1 --format='%h %s' vX.Y.Z        # 看 tag 实际指向
```

注意：**tag 不一定指向 `chore(release)` 提交本身**。v1.1.5 就指向 release 提交之后的 `216830e`（又追加了两个 chore）。同步范围一律以 tag 指向为准。

## 3. 确定同步范围

- **上次同步基线**：查本仓库 `CHANGELOG.md` 最新版本条目末尾的「同步基线」行（固定格式，见第 6 节）。首次同步时基线 = fork 分叉点，用 `git merge-base master <tag>` 求得（本 fork 为 `773c0e7`，即上游 1.1.4 发布准备提交）。
- 同步范围 = `<上次基线>..<新 tag>`：

```powershell
git log --oneline <上次基线>..vX.Y.Z              # commit 清单
git diff --stat <上次基线> vX.Y.Z --               # 文件级全貌（PowerShell 下长输出用 Select-Object 分段看）
```

## 4. 逐 commit triage（三分类）

对范围内每个 commit 执行 `git show --stat <sha>` 看触及文件，按下面清单归类。**不要凭 commit message 猜分类**——v1.1.5 的教训：message 写「Clash API 端口动态适配」「优化代理状态流」，看似通用改进，实际全部落在 Windows 实现文件里（见附录 A）。

### 4.1 分歧区文件清单（B 类：跳过/裁剪）

**已删除、不回收**（上游对这些路径的任何改动直接跳过）：

- `ios/`、`example/lib/`、`example/android/`、`example/ios/`、`example/windows/`、`example/pubspec.*`、`example/integration_test/`（旧 example 应用；本分支 example/ 只有 guides 与 capabilities.md）
- `.metadata`、`CLAUDE.md`、`GEMINI.md`、`.claude/skills/gitnexus/`（上游内部工具文件）

**Windows 实现区**（实现跳过，接口形状按 4.3 吸收）：

- `lib/flutter_sing_box_windows.dart`
- `lib/src/windows/`（clash_http_client / helper_cli / helper_http_client 等）
- `lib/src/constants/windows_constants.dart`（未被 index 导出、仅 Windows 文件直接引用时跳过）
- `lib/src/utils/asset_util.dart` 及其独有依赖（如 `crypto`）——上游为 Windows 运行时释放二进制引入
- `windows/`（原生插件骨架）、`assets/windows/`（二进制资产）
- `test/flutter_sing_box_windows_test.dart`、`test/helper_cli_test.dart`（依赖 Windows 实现的测试）

### 4.2 冲突区文件清单（C 类：人工逐块合并）

本分支强化过、双方都可能改动的文件：

- `lib/flutter_sing_box.dart`、`lib/flutter_sing_box_method_channel.dart`、`lib/flutter_sing_box_platform_interface.dart`
- `android/src/main/kotlin/**`（`FlutterSingBoxPlugin.kt`、`SingBoxConnector` 等）
- `assets/configs/singbox_config_template.json`
- `test/flutter_sing_box_test.dart`、`android/src/test/**`
- `pubspec.yaml`、`CHANGELOG.md`、`README.md` / `README_CN.md`

### 4.3 接口形状兼容规则（对 B 类的例外）

上游在**平台接口 / 方法通道 / 门面**新增的方法签名一律吸收，即使其唯一真实实现在被跳过的平台：

- 三层都加：平台接口默认 `UnimplementedError`，方法通道照抄上游的非目标平台默认返回（如 `queryServiceStatus` → `unsupported`、`uninstallService` → `false`），门面纯转发；
- 签名依赖的**纯 Dart** 模型/枚举（如 `HelperConfig`、`WindowsServiceStatus`）一并吸收，含对应 index 导出与纯 Dart 单测；
- 平台实现文件本身、原生代码、二进制、以及仅被实现使用的依赖不吸收。

目的：保持与上游接口形状一致，后续同步这三个文件的 diff 才小而干净。

### 4.4 其余共享文件（A 类：直接吸收）

不在上述两个清单里的都默认吸收：`android/build.gradle`、`android/src/main/AndroidManifest.xml`、`lib/src/constants/`、`lib/src/storage/`、`lib/src/data/models/`、`lib/src/core/`、`.gitignore` 等。

**吸收共享文件前必查 Android 行为等价性**：如 v1.1.5 的 `profile_storage.dart` 引入 `getStorageDirectory()` 平台分支，Android 分支仍返回 documents 目录，行为不变才可整文件吸收；若上游改变了 Android 行为，降级为 C 类逐块审。

### triage 记录

产出一张表（写进 CHANGELOG 或同步 commit message）：`commit | 分类 | 处理方式 | 备注`。

## 5. 应用改动

按优先级选择手段：

1. **整文件对齐**（A 类且本分支未改过）：先验证未分歧——`git diff <上次基线> master -- <path>` 输出为空——再 `git checkout vX.Y.Z -- <path>`。新文件同样用 checkout 引入。
2. **cherry-pick**（commit 只触及 A 类文件时）：`git cherry-pick <sha>`。commit 混有分歧区文件时**不要用**，会把 Windows/已删除文件带回来。
3. **手工合并**（C 类）：以 `git diff <上次基线> vX.Y.Z -- <file>` 为蓝本，把上游新增块插到本分支对应位置。约定：上游新增方法放在上游所在位置，本分支自研方法（`checkConfig`、`urlTestOutbound` 等）聚集紧随其后、流 getter 之前——固定聚集位能让后续同步的 diff 干净。格式风格保持本分支（80 列），不追上游的重排版噪音。

**禁止**：
- `git merge upstream/master` / `git merge vX.Y.Z`——本分支删除的 example、iOS 等路径会产生大量 delete/modify 冲突；
- 整目录 checkout（如 `git checkout vX.Y.Z -- lib/`）——会覆盖冲突区与分歧区。

## 6. 版本号与 CHANGELOG

- **版本号跟随上游 release**：`pubspec.yaml` 的 `version` 与上游一致（1.1.4 冻结策略已于 2026-08-02 作废）。
- CHANGELOG 顶部新增 `## X.Y.Z` 条目，分两节：
  - **与上游同步（clash-sing vX.Y.Z）**：列吸收项；被跳过的大项也要写明（如「Windows 平台实现仅吸收接口形状」），后续 agent 才知道哪些没进来；
  - **本分支自研**：fork 侧新增/修复（若把此前 `Unreleased` 的内容并入发布，移入此节）。
- 条目末行固定格式记录基线，供下次同步定位范围：

  ```
  同步基线：clash-sing/flutter_sing_box vX.Y.Z（<tag 指向的短 sha>）
  ```

- 保留 fork 分割线（`## 👆👆👆👆 WEP-56 fork change log 👆👆👆👆`），其下是上游原始历史，不改写。

## 7. 验证矩阵

### 7.1 插件本仓

```powershell
Set-Location D:\FLsing\flutter_sing_box
flutter analyze --fatal-infos      # 0 问题
flutter test                       # Dart 单测全过
```

Kotlin 单测需 gradle 宿主环境；本机跑不了时如实说明，交 CI / Android Studio。

### 7.2 跨仓编译面预验证（push 之前，在 FLsing）

插件接口变化可能波及 FLsing。**已知跨仓耦合点**（随发现追加）：

- `test/app_state_latency_test.dart` 的 `_MemoryStorage implements KeyValueStorage`——存储接口加方法/改签名会破坏它；
- 排查方法：在 FLsing 全仓搜索 `implements`/`extends` 插件类型（注意 FLsing 的 `.gitignore` 排除了 `flutter_sing_box/`，ripgrep 类工具在 FLsing 根目录搜不到插件内部，反之在插件目录搜不到 FLsing——两边各搜一次）。

用临时 path 覆写让 FLsing 在 push 前直接编译新插件代码：

```powershell
# 1) FLsing pubspec.yaml 临时追加（不提交）：
#    dependency_overrides:
#      flutter_sing_box:
#        path: flutter_sing_box
Set-Location D:\FLsing
flutter pub get
flutter analyze
flutter test
# 2) 验证完删除 dependency_overrides，恢复锁定：
flutter pub get
Select-String resolved-ref pubspec.lock    # 必须回到原锁定 commit，此步不可省
```

### 7.3 libbox 升级附加审查

- 读上游 sing-box 对应版本 release notes，关注破坏性变更；
- 审查废弃字段：`independent_cache`、入站 `sniff` 等（更高版本已废弃）；检查模板 `assets/configs/singbox_config_template.json` 与 FLsing `_patchUsingConfig` / 高级网络覆写是否用到受影响字段；
- 版本号引用同步更新：`docs/Android-Architecture.md`、`example/capabilities.md`、`example/README.md`、`example/guides/*` 中的 libbox 版本字样。

## 8. 提交与 FLsing lockfile 前移

提交与推送由用户执行（agent 不 commit/push）。顺序不可乱（FLsing `docs/Handoff.md` 第三节铁律）：

1. 用户 review 插件改动 → 在插件仓库 commit → `git push origin master`；
2. FLsing 前移锁定：

   ```powershell
   Set-Location D:\FLsing
   flutter pub upgrade flutter_sing_box
   Select-String resolved-ref pubspec.lock   # 与插件仓库 git log -1 的新 commit 比对
   ```

3. FLsing `flutter analyze` + `flutter test`（此时 FLsing 侧适配改动才会转绿）；
4. 用户把 **lockfile 与 FLsing 侧适配改动放同一提交** commit → push 触发 CI 构建 APK。

注意：跨仓适配（如 `_MemoryStorage`）在插件 push 前就已改好时，FLsing 工作区会短暂 analyze 不过——这是预期状态，lockfile 前移后即恢复；保证「每个 commit 绿」而不是「每时每刻工作区绿」。

## 9. 真机验收清单

CI 出包后由用户在真机过一遍（agent 提供清单，不代替执行）：

- 启动 / 停止 / 重载 VPN；WiFi↔蜂窝网络切换后连接恢复；
- 日志流正常滚动（诊断→日志）；
- 配置校验：导入坏配置应被 `checkConfig` 拒绝且不破坏当前 using_config；
- 测速三条：TCP 直连；内核整组（结果回流 groupStream）；内核单点（正常返回延迟，无 502）；
- 模式切换即时生效、重启应用后回到用户偏好（无旧模式跳回）；
- 订阅导入/刷新两条管线（分享链接、sing-box JSON / Clash YAML）+ 按订阅 UA；
- libbox 升级版本追加：长时间连接稳定性观察；
- 其他随该次同步引入的行为点（triage 表中标注）。

## 10. 收尾登记

- FLsing `docs/Handoff.md`：第一节 libbox/插件版本号、第六节状态表、相关工作项进度；
- 插件 API 有增删 → `example/guides/*` 与 `example/capabilities.md` 增补；
- 本文档若在同步中暴露流程缺陷，随手修订。

---

## 附录 A：v1.1.5 同步实录（2026-08-02）

- 范围：`773c0e7..v1.1.5`（tag 指向 `216830e`，release 提交为 `34d249f`，其后追加 2 个 chore），约 70 commits / 73 文件。上游 master 与 v1.1.5 同指。
- 上游 release 7 条主项的 triage 结果（与 FLsing Handoff 预判的差异用 ⚠ 标出）：

| 上游条目 | 关键 commit | 分类 | 处理 |
| --- | --- | --- | --- |
| libbox 1.13.14→1.13.15、移除位置权限 | `defb307` | A | checkout `android/build.gradle` + `AndroidManifest.xml`（`d2e745c` 为 Windows sing-box.exe，跳过） |
| Clash API 端口动态适配 | `75e1dec` | ⚠ B | 实现全部在 `lib/src/windows/clash_http_client.dart`，跳过；其存储层底座 `1a4eac6` 平台中立，吸收 |
| 代理状态流控制器优化 | `8457daa` `f170e30` 等 | ⚠ B | 全部在 `flutter_sing_box_windows.dart` / `helper_http_client.dart`，跳过 |
| installService 收敛 HelperConfig、路径常量化 | `5f1addb` `185b96c` 等 | B+形状 | 接口形状吸收：门面/接口/通道三层 5 方法 + `HelperConfig` 模型；`windows_constants.dart` 跳过 |
| 服务管理上升平台抽象与门面 | `cb86f75` `e3da3aa` | B+形状 | 同上：`queryServiceStatus`/`installService`/`uninstallService`/`startService`/`stopService`，通道层默认返回 `unsupported`/`true`/`false`；`WindowsServiceStatus` 枚举 + 纯 Dart 单测吸收 |
| FlutterSingBoxWindows 委托 HelperCli | `2091a72` 等 | B | 跳过 |
| （release 未单列）存储层增强 | `1a4eac6` | A | setter 可空=删除语义、新增 getDouble/setDouble、`clash_api_port`/`test_url` 键；**触发 FLsing `_MemoryStorage` 适配** |

- 其他吸收：`registerWith()` + pubspec `dartPluginClass`（自动注册机制，Android 侧行为等价）、`InboundType` 常量、`FlutterSingBoxConstants.assetBasePath`/`defaultClashApiPort`、Clash 数据模型两组（`clash_api_proxy`/`clash_configs`，惰性保形状）、`profile_storage` 平台分支（Android 行为不变）、dio `^5.10.0`、`.gitignore` 加 `/doc/`。
- 其他跳过：`crypto` 依赖（仅 `asset_util` 使用）、`utils/index.dart` 的 asset_util 导出、example 旧应用文件、`.metadata`、gitnexus 文档。
- **Clash API 端口对账结论**：上游动态适配只作用于 Windows Dart 客户端（每次请求前从存储重读端口）。Android 链路——FLsing `_ensureClashApiPort` 分配 → `_patchUsingConfig` 写入 using_config → 插件 Kotlin `readClashApiAccess` 读取——不受影响，**FLsing 仍是 Android 端口的唯一事实源**；插件新增的 `CsSettingsStorage.clashApiPort/testUrl` 在 Android 无读方，与 FLsing 在 `cs_settings` 的既有键（`system_proxy_enabled`、`dynamic_notification`）无冲突，不做收敛。
- 版本号：1.1.4 → 1.1.5（冻结策略作废后的首个跟随版本）。
