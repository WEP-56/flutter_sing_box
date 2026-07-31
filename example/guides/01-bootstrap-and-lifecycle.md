# 初始化与生命周期

## 1. 环境要求

- Flutter `>=3.41.0`
- Dart `^3.11.0`
- Android `minSdk 26`
- Java 17 Android 构建链

插件只注册 Android。宿主若包含其他平台，必须在调用前自行做平台判断。

## 2. 初始化存储和插件

`ProfileStorage` 与 `CsSettingsStorage` 使用 MMKV。宿主在读写这些服务前先初始化 MMKV，并为
活动配置指定目录。

```dart
import 'package:flutter/widgets.dart';
import 'package:flutter_sing_box/flutter_sing_box.dart';
import 'package:mmkv/mmkv.dart';
import 'package:path_provider/path_provider.dart';

Future<void> bootstrapSingBox() async {
  WidgetsFlutterBinding.ensureInitialized();
  await MMKV.initialize();

  final documents = await getApplicationDocumentsDirectory();
  ProfileStorage().setUsingConfig(documents.path);

  await FlutterSingBox().init();
}
```

`ProfileStorage.getUsingConfig()` 最终返回 `<usingConfigDirectory>/using_config.json`。启动服务前该
文件必须存在且是有效配置。

## 3. 监听状态后再启动

```dart
final singBox = FlutterSingBox();

final stateSubscription = singBox.proxyStateStream.listen(
  (state) {
    switch (state) {
      case ProxyState.started:
        // 更新宿主连接状态。
      case ProxyState.stopped:
        // 清理运行中 UI 或进入重试策略。
      default:
        break;
    }
  },
  onError: (Object error, StackTrace stackTrace) {
    // 记录状态通道错误。
  },
);

await singBox.startVpn();
```

`startVpn()` 返回不等于 VPN 已经可用。Android 授权、服务启动和内核加载是分阶段完成的，宿主应以
`proxyStateStream` 的 `started` 作为连接完成信号。

## 4. 停止和重载

```dart
await singBox.serviceReload();
await singBox.stopVpn();
```

`serviceReload()` 和 `stopVpn()` 当前只确认命令已提交。宿主需要继续观察状态流，并设置自己的
超时和错误提示。

## 5. 生命周期清理

页面或状态容器销毁时取消所有流订阅。不要为每次重建 Widget 重复调用 `init()` 或重复创建长期
EventChannel 监听。

```dart
await stateSubscription.cancel();
```

## 6. 宿主名称

插件从 Android `applicationInfo` 读取宿主标签：

- FLsing 接入时，系统 VPN 面板显示 `FLsing`。
- 其他应用接入时，显示各自的 `android:label`。
- 策略/订阅名称仍可作为前台通知的内容标题，不等同于 VPN 会话名称。
