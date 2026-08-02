# 策略组与延迟测试

插件提供两种不同语义的测速能力。

## 1. 整组测速

```dart
await FlutterSingBox().urlTest(groupTag: 'proxy');
```

libbox 1.13.14 的 command API 只接受 group tag。调用后：

1. 内核并发测试该组内的非 group outbound。
2. 方法在命令提交后返回，不等待网络请求完成。
3. 延迟历史异步写入 `groupStream`。
4. 没有请求 ID；宿主需要用 `urlTestTime`、超时和目标 group 自行关联。

不能把普通节点 tag 传给此方法，服务端会返回 `outbound is not a group`。

## 2. 单 outbound 内核测速

```dart
final delay = await FlutterSingBox().urlTestOutbound(
  outboundTag: 'node-a',
  url: 'https://www.gstatic.com/generate_204',
  timeout: const Duration(seconds: 10),
);
```

该方法调用活动 sing-box 实例的 Clash API：

```text
GET /proxies/{outboundTag}/delay?url={url}&timeout={milliseconds}
```

返回值是该 outbound 实际建立测试连接得到的毫秒延迟。它不是节点服务器物理地址的 TCP 握手
时间。

## 3. 活动配置前提

活动配置必须启用带随机密钥的 loopback controller：

```json
{
  "experimental": {
    "clash_api": {
      "external_controller": "127.0.0.1:19090",
      "secret": "generate-a-random-secret-per-install"
    }
  }
}
```

安全要求：

- 只绑定 `127.0.0.1`、`::1` 或本机任意地址；插件拒绝远程 controller。
- 每个安装生成独立 secret，不要写死在仓库、日志或设置备份中。
- controller 端口应由宿主分配并持久化，运行中不能随意改变。
- 修改 controller 后需要重载或重启 sing-box 服务。

插件从活动 `using_config.json` 读取 controller 和 secret，调用方不传递凭据。

controller 请求强制以 `Proxy.NO_PROXY` 直连发出。TUN 的 `platform.http_proxy` 开启时，
Android 会向所有应用下发系统 HTTP 代理且默认不排除 localhost；若不绕过，回环 controller
请求会被送进代理入站并按路由发往远端节点，表现为稳定的空响应体 `HTTP 502`。

## 4. 失败行为

| 场景 | 错误码 |
| --- | --- |
| tag、URL 或 timeout 无效 | `INVALID_ARGUMENTS` |
| 活动配置不存在或未启用 controller | `CLASH_API_UNAVAILABLE` |
| controller 不是本机地址 | `CLASH_API_UNAVAILABLE` |
| outbound 不存在 | `URL_TEST_FAILED` |
| 网络失败或超时 | `URL_TEST_FAILED` |
| 插件已从 engine 分离 | `PLUGIN_UNAVAILABLE` |

## 5. 宿主策略建议

- `direct`：对节点服务器地址做 TCP 握手，只反映物理可达性。
- `smart`：未连接时 TCP 直连；连接后使用 `urlTestOutbound`。
- `proxy`：必须先连接，再使用 `urlTestOutbound`。
- 全节点内核测速仍使用 `urlTest(groupTag)`，避免串行调用大量单节点请求。

单节点请求返回后可立即更新目标节点；`groupStream` 后续仍可能推送同一条新历史，宿主应允许
幂等覆盖。
