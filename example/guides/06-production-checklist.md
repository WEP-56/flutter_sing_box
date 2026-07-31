# 发布前验证

## 1. 插件代码验证

```bash
flutter analyze --fatal-infos
flutter test
```

若修改带注解模型，再执行项目既有代码生成流程，并确认生成文件只包含预期差异。

## 2. 宿主代码验证

```bash
flutter pub get
flutter analyze
flutter test
```

Git 依赖更新后检查 `pubspec.lock` 的 `resolved-ref`，确认它指向已推送且完成验证的插件 commit。

## 3. APK/AAB

- GitHub Actions 或本地 release APK 构建通过。
- 检查 merged Manifest、minSdk、targetSdk 和前台服务类型。
- 用 APK Analyzer 记录各 ABI 的 `libbox.so` 体积。
- 发布 APK 时按 ABI 拆分，商店发布优先 AAB。

## 4. 真机基础验收

- 首次 VPN 授权接受和拒绝路径。
- 启动、停止、再次启动。
- 基础网页和应用网络访问。
- Wi-Fi 与蜂窝网络切换。
- 后台、息屏和重新打开应用。
- 系统 VPN 面板显示宿主应用名。
- 前台通知停止按钮和实时速率。

## 5. 配置安全

- 空配置和错误配置不会覆盖当前可用活动配置。
- 写入中断后可以从备份恢复。
- Clash API 只监听 loopback，并使用每安装随机 secret。
- 设置备份不包含订阅链接、节点凭据或 Clash API secret。

## 6. 测速

- 整组 `urlTest` 能通过 `groupStream` 收敛。
- 单 outbound `urlTestOutbound` 返回目标节点延迟。
- 不存在的 tag 返回 `URL_TEST_FAILED`。
- controller 缺失返回 `CLASH_API_UNAVAILABLE`。
- 超时后 UI 正确结束 loading，不覆盖其他节点延迟。

## 7. 深度网络场景

- 手动 DoH/DoT。
- system、gVisor、mixed TUN 栈。
- IPv4 only、IPv6 only 和双栈。
- route exclusion、严格路由和自定义规则顺序。
- 分应用仅代理和排除模式。
- 连接中配置重载、失败回滚和订阅切换。

代码验证不能替代这些真机测试。每次升级 libbox 或 target SDK 后重新执行完整清单。
