# WEP-56 flutter_sing_box 使用范例

`example/` 不再维护独立 Flutter 演示应用。本目录是 WEP-56 Android-only
分支的文档入口，用于说明当前真实能力、接入契约、代码范例和已知限制。

这样处理有三个目的：

1. 文档与插件公开 API 同步，不让过期演示界面掩盖底层行为。
2. 不在插件仓库复制一套宿主应用架构、状态管理和 Android 构建资产。
3. 明确区分“插件已经完成的能力”和“宿主应用仍需负责的工作”。

## 分支定位

- 仓库：`https://github.com/WEP-56/flutter_sing_box.git`
- 分支：`master`
- 平台：Android only
- 当前包版本：`1.1.4`，WEP-56 分支通过 Git commit 持续迭代
- libbox：`1.13.14`

建议生产应用固定到已验证 commit；开发阶段可跟随 `master`，但必须提交更新后的
`pubspec.lock`。

## 阅读顺序

1. [能力矩阵](capabilities.md)：先确认方法是否存在、何时完成、需要哪些前提。
2. [初始化与生命周期](guides/01-bootstrap-and-lifecycle.md)：建立最小接入骨架。
3. [配置与订阅](guides/02-profiles-and-configuration.md)：理解源配置和活动配置的所有权。
4. [服务状态与事件流](guides/03-service-and-streams.md)：正确消费异步状态。
5. [策略组与延迟测试](guides/04-groups-and-latency.md)：区分整组测速和单 outbound 测速。
6. [Android 宿主集成](guides/05-android-host-integration.md)：处理权限、服务和应用标签。
7. [发布前验证](guides/06-production-checklist.md)：完成代码和真机验收。

## 最小依赖

```yaml
dependencies:
  flutter_sing_box:
    git:
      url: https://github.com/WEP-56/flutter_sing_box.git
      ref: master
```

更新插件后执行：

```bash
flutter pub get
flutter analyze
flutter test
```

## 重要边界

- 插件负责 Android VPN 服务、libbox 通信、事件流、配置模型和基础存储。
- 宿主负责活动配置的生成、校验后替换、备份回滚、权限说明和用户反馈。
- `urlTest(groupTag)` 只提交整组测速命令，不代表结果已经返回。
- `urlTestOutbound(...)` 会等待单个 outbound 的内核结果，但活动配置必须启用带密钥的
  loopback Clash API。
- APK 构建成功不等于 VPN 行为正确；至少需要真机覆盖连接、网络访问、重载和错误配置。
- 插件仍有待完善能力，尤其是结构化服务告警、明确的启停完成反馈和更完整的原生测试。

插件内部结构和维护边界见 [Android 结构说明](../docs/Android-Architecture.md)。
