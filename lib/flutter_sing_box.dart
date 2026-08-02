export '../src/data/models/index.dart';
export '../src/constants/index.dart';
export '../src/utils/index.dart';
export '../src/core/index.dart';
export '../src/storage/index.dart';

// 导出方法通道实现，供 Flutter 自动生成的 dart_plugin_registrant.dart
// 通过主 library 访问并调用其 registerWith()（pubspec 中已声明 dartPluginClass）。
// 本分支 Android-only，不导出其他平台实现。
export 'flutter_sing_box_method_channel.dart';

import 'flutter_sing_box.dart';
import 'flutter_sing_box_platform_interface.dart';

/// The main class for interacting with the flutter_sing_box plugin.
class FlutterSingBox {
  /// Initializes the plugin.
  Future<void> init() async {
    return await FlutterSingBoxPlatform.instance.init();
  }

  /// Starts the VPN service.
  Future<void> startVpn() async {
    return await FlutterSingBoxPlatform.instance.startVpn();
  }

  /// Stops the VPN service.
  Future<void> stopVpn() async {
    return await FlutterSingBoxPlatform.instance.stopVpn();
  }

  /// Reloads the service configuration.
  Future<void> serviceReload() async {
    return await FlutterSingBoxPlatform.instance.serviceReload();
  }

  /// Sets the Clash mode.
  ///
  /// [clashMode] The new Clash mode to set (e.g., 'Global', 'Rule', 'Direct').
  Future<void> setClashMode(String clashMode) async {
    return await FlutterSingBoxPlatform.instance.setClashMode(clashMode);
  }

  /// Selects an outbound for a specific group.
  ///
  /// [groupTag] The tag of the group.
  /// [outboundTag] The tag of the outbound to select.
  Future<void> selectOutbound({
    required String groupTag,
    required String outboundTag,
  }) async {
    return await FlutterSingBoxPlatform.instance.selectOutbound(
      groupTag: groupTag,
      outboundTag: outboundTag,
    );
  }

  /// Sets the expansion state of a group in the UI.
  ///
  /// [groupTag] The tag of the group.
  /// [isExpand] Whether the group should be expanded or collapsed.
  Future<void> setGroupExpand({
    required String groupTag,
    required bool isExpand,
  }) async {
    return await FlutterSingBoxPlatform.instance.setGroupExpand(
      groupTag: groupTag,
      isExpand: isExpand,
    );
  }

  /// Performs a URL test for a specific group.
  ///
  /// [groupTag] The tag of the group to test.
  Future<void> urlTest({required String groupTag}) async {
    return await FlutterSingBoxPlatform.instance.urlTest(groupTag: groupTag);
  }

  /// Tests one outbound through the active sing-box core.
  ///
  /// The active configuration must expose an authenticated loopback Clash API.
  /// Unlike [urlTest], this method waits for the selected outbound's result and
  /// returns its delay in milliseconds.
  Future<int> urlTestOutbound({
    required String outboundTag,
    required String url,
    Duration timeout = const Duration(seconds: 10),
  }) async {
    return await FlutterSingBoxPlatform.instance.urlTestOutbound(
      outboundTag: outboundTag,
      url: url,
      timeout: timeout,
    );
  }

  /// Gets the version of the underlying sing-box core.
  Future<String> getSingBoxVersion() async {
    return await FlutterSingBoxPlatform.instance.getSingBoxVersion();
  }

  /// 查询 Windows 端 `clash_sing_service` 的安装/运行状态。
  ///
  /// 本分支 Android-only，恒返回 [WindowsServiceStatus.unsupported]；
  /// 接口仅为与上游门面形状保持一致。
  Future<WindowsServiceStatus> queryServiceStatus() {
    return FlutterSingBoxPlatform.instance.queryServiceStatus();
  }

  Future<bool> installService({
    required String serviceName,
    required String displayName,
    required String description,
  }) {
    return FlutterSingBoxPlatform.instance.installService(
      serviceName: serviceName,
      displayName: displayName,
      description: description,
    );
  }

  /// 卸载 Windows 系统服务。
  Future<bool> uninstallService() {
    return FlutterSingBoxPlatform.instance.uninstallService();
  }

  /// 启动已安装的 Windows 系统服务。
  Future<bool> startService() {
    return FlutterSingBoxPlatform.instance.startService();
  }

  /// 停止 Windows 系统服务。
  Future<bool> stopService() {
    return FlutterSingBoxPlatform.instance.stopService();
  }

  /// Validates a sing-box configuration with the bundled core.
  Future<void> checkConfig(String configuration) async {
    return await FlutterSingBoxPlatform.instance.checkConfig(configuration);
  }

  /// A stream of client connection status updates.
  Stream<ClientStatus> get connectedStatusStream =>
      FlutterSingBoxPlatform.instance.connectedStatusStream;

  /// A stream of client group updates.
  Stream<List<ClientGroup>> get groupStream =>
      FlutterSingBoxPlatform.instance.groupStream;

  /// A stream of client Clash mode updates.
  Stream<ClientClashMode> get clashModeStream =>
      FlutterSingBoxPlatform.instance.clashModeStream;

  /// A stream of log messages from the sing-box core.
  Stream<List<ClientLog>> get logStream =>
      FlutterSingBoxPlatform.instance.logStream;

  /// A stream of proxy state updates.
  Stream<ProxyState> get proxyStateStream =>
      FlutterSingBoxPlatform.instance.proxyStateStream;
}
