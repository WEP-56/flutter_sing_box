# 配置与订阅

## 1. 两类配置文件

生产宿主应区分：

1. **源配置**：订阅下载或本地导入的原始内容，只在订阅更新时替换。
2. **活动配置**：从源配置复制并应用本地覆写后生成的 `using_config.json`。

DNS、TUN、路由、测试 URL、Clash API 和默认模式等本地设置应写入活动配置，不能污染源订阅。

## 2. 导入订阅

```dart
final profile = await ProfileService().importProfile(
  subscribeLink: Uri.parse('https://example.com/subscription'),
  name: 'Primary',
  userAgent: 'MyClient/1.0',
);

ProfileStorage().setSelectedProfile(profile.id);
```

`ProfileService` 可处理本地文件和远程地址，并尝试识别 sing-box JSON、Clash YAML 或有限的
Base64 分享链接。无法识别的协议必须由宿主补充解析器或明确报错。

## 3. 校验候选配置

```dart
import 'dart:convert';
import 'dart:io';

Future<void> replaceUsingConfigSafely(
  File usingConfig,
  Map<String, dynamic> candidate,
) async {
  final encoded = jsonEncode(candidate);
  await FlutterSingBox().checkConfig(encoded);

  final temporary = File('${usingConfig.path}.tmp');
  final backup = File('${usingConfig.path}.bak');
  await temporary.writeAsString(encoded, flush: true);
  if (await backup.exists()) await backup.delete();
  if (await usingConfig.exists()) await usingConfig.rename(backup.path);
  try {
    await temporary.rename(usingConfig.path);
    if (await backup.exists()) await backup.delete();
  } catch (_) {
    if (await usingConfig.exists()) await usingConfig.delete();
    if (await backup.exists()) await backup.rename(usingConfig.path);
    rethrow;
  }
}
```

`checkConfig` 只校验字符串，不写文件。原子替换和崩溃恢复属于宿主职责。

## 4. 配置模型

插件导出以下主要模型：

- `SingBox`
- `Dns`、`Server`、`DnsRule`
- `Route`、`RouteRule`、`RuleSet`
- `Inbound`、`Platform`、`HttpProxy`
- `Outbound`、`Transport`、`Multiplex`
- `Tls`、`Utls`
- `Experimental`、`CacheFile`、`ClashApi`

模型主要用于结构化读取和生成。sing-box 字段会随版本弃用或变更，升级 libbox 时必须把生成配置
重新交给 `checkConfig`，不能只依赖 Dart JSON 序列化成功。

## 5. 活动配置的最小契约

启动前至少确保：

- 存在有效 inbound 和 outbound。
- route final 和规则引用的 outbound tag 存在。
- `ProfileStorage` 已选中 profile。
- `ProfileStorage.setUsingConfig(...)` 指向活动配置目录。
- `using_config.json` 已通过当前 libbox 版本校验。

如果需要单 outbound 内核测速，还要满足
[策略组与延迟测试](04-groups-and-latency.md) 中的 Clash API 契约。
