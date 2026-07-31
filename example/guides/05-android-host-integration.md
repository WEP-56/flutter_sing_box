# Android 宿主集成

## 1. Manifest 合并

插件声明 VPN 服务、前台服务、网络、通知和部分设备权限。宿主最终 APK 的权限由 Manifest merge
决定，发布前必须检查合并结果，不要只查看插件文件。

宿主可以覆盖前台服务类型等属性：

```xml
<service
    android:name="io.nekohasekai.sfa.bg.VPNService"
    android:foregroundServiceType="specialUse"
    android:permission="android.permission.BIND_VPN_SERVICE"
    tools:replace="android:foregroundServiceType">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="vpn" />
</service>
```

具体要求取决于 target SDK 和应用商店政策。

## 2. 应用标签和 VPN 会话名

插件不应向系统 UI 暴露 `Clash Sing` 等固定品牌。当前实现从宿主 `applicationInfo` 读取标签：

```xml
<application android:label="My VPN Client" />
```

该标签用于 Android 系统 VPN 面板中的会话名称和插件通知默认标题。活动 profile 名称仍可作为
通知内容标题。

## 3. VPN 权限

`startVpn()` 内部调用 `VpnService.prepare`。用户拒绝时返回 `VPN_PERMISSION_DENIED`。宿主应提供
明确反馈，不要自动循环弹出系统授权页。

## 4. Android 13+ 通知权限

前台服务必须存在，即使通知展示权限被拒绝。宿主负责请求 `POST_NOTIFICATIONS` 并解释实时速率
通知的用途。

## 5. 多进程存储

VPN 服务运行在 `:remote` 进程。插件使用 MMKV multi-process mode 共享：

- 选中的 profile
- 活动配置目录
- 服务模式
- 分应用 include/exclude 设置
- 通知和系统代理设置

宿主不要直接用普通 `SharedPreferences` 替代这些键，否则主进程写入后远程服务可能看不到。

## 6. 分应用代理

```dart
final settings = CsSettingsStorage();
settings.setAppList(
  ['com.example.browser'],
  2,
);
```

当前数字模式与原生约定一致：

- `0`：关闭
- `1`：仅代理列表应用
- `2`：排除列表应用

宿主应封装自己的 enum，避免 UI 直接散布数字。恢复或修改该能力后必须真机验证 Android
`VpnService.Builder` 实际应用了包名。

## 7. 包体

Android 包体主要来自 `libbox.aar` 的多 ABI `libbox.so`。优先使用 ABI split 或 AAB；删除文档、
示例和其他平台源码不会直接缩小最终 Android 原生库。
