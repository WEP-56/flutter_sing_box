# 服务状态与事件流

## 1. 状态流

```dart
final client = FlutterSingBox();

final proxyStates = client.proxyStateStream.listen((state) {
  // stopped / starting / started / stopping / unknown
});

final status = client.connectedStatusStream.listen((value) {
  final uploadPerSecond = value.uplink;
  final downloadPerSecond = value.downlink;
});
```

`connectedStatusStream` 是运行指标，不应替代 `proxyStateStream` 的服务生命周期判断。

## 2. 策略组流

```dart
final groups = client.groupStream.listen((items) {
  for (final group in items) {
    final selected = group.selected;
    for (final item in group.items ?? const <ClientGroupItem>[]) {
      final delay = item.urlTestDelay > 0 ? item.urlTestDelay : null;
      final testedAt = item.urlTestTime;
    }
  }
});
```

`urlTestTime` 来自内核历史，可能使用秒或纳秒量级时间戳，宿主在比较请求时间时应兼容当前内核
输出，不要直接当作 Dart 毫秒时间戳。

## 3. 模式和日志

```dart
final modes = client.clashModeStream.listen((value) {
  final current = value.currentMode;
});

final logs = client.logStream.listen((batch) {
  // 宿主自行限制保留条数并脱敏。
});
```

日志可能包含服务器地址、订阅 URL 或错误上下文。导出和分享前必须脱敏。

## 4. 命令完成语义

下列方法的 Future 主要表示命令已提交：

- `startVpn`
- `stopVpn`
- `serviceReload`
- `setClashMode`
- `selectOutbound`
- `setGroupExpand`
- `urlTest`

下列方法返回时已经有明确结果：

- `getSingBoxVersion`
- `checkConfig`
- `urlTestOutbound`

宿主不要在命令提交后用固定延迟假设成功。优先等待状态流、目标事件或有返回值的 API。

## 5. 错误处理

```dart
try {
  await client.serviceReload();
} on PlatformException catch (error) {
  final code = error.code;
  final message = error.message ?? code;
  // 显示 message，诊断日志保留 code。
}
```

当前 `onServiceAlert` 还没有作为结构化 Dart 流公开。服务意外停止时，宿主只能结合
`proxyStateStream`、原生日志和自己的重试状态判断原因。

## 6. 清理

```dart
await Future.wait([
  proxyStates.cancel(),
  status.cancel(),
  groups.cancel(),
  modes.cancel(),
  logs.cancel(),
]);
```
