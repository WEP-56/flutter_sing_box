import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_sing_box/flutter_sing_box.dart';
import 'package:flutter_sing_box/flutter_sing_box_platform_interface.dart';
import 'package:flutter_sing_box/flutter_sing_box_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterSingBoxPlatform
    with MockPlatformInterfaceMixin
    implements FlutterSingBoxPlatform {
  String? checkedConfiguration;

  @override
  Future<void> checkConfig(String configuration) async {
    checkedConfiguration = configuration;
  }

  @override
  Future<void> init() {
    throw UnimplementedError();
  }

  @override
  Future<void> startVpn() {
    throw UnimplementedError();
  }

  @override
  Future<void> stopVpn() {
    throw UnimplementedError();
  }

  @override
  Future<void> serviceReload() {
    throw UnimplementedError();
  }

  @override
  Future<void> setClashMode(String mode) {
    throw UnimplementedError();
  }

  @override
  Future<void> selectOutbound({
    required String groupTag,
    required String outboundTag,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<void> urlTest({required String groupTag}) {
    throw UnimplementedError();
  }

  @override
  Future<void> setGroupExpand({
    required String groupTag,
    required bool isExpand,
  }) {
    throw UnimplementedError();
  }

  @override
  Stream<ClientClashMode> get clashModeStream => throw UnimplementedError();

  @override
  Stream<ClientStatus> get connectedStatusStream => throw UnimplementedError();

  @override
  Stream<List<ClientGroup>> get groupStream => throw UnimplementedError();

  @override
  Stream<List<ClientLog>> get logStream => throw UnimplementedError();

  @override
  Stream<ProxyState> get proxyStateStream => throw UnimplementedError();

  @override
  Future<String> getSingBoxVersion() {
    throw UnimplementedError();
  }
}

void main() {
  final FlutterSingBoxPlatform initialPlatform =
      FlutterSingBoxPlatform.instance;

  test('$MethodChannelFlutterSingBox is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterSingBox>());
  });

  test('checkConfig delegates to the platform implementation', () async {
    final fakePlatform = MockFlutterSingBoxPlatform();
    FlutterSingBoxPlatform.instance = fakePlatform;
    addTearDown(() => FlutterSingBoxPlatform.instance = initialPlatform);

    await FlutterSingBox().checkConfig('{"outbounds":[]}');

    expect(fakePlatform.checkedConfiguration, '{"outbounds":[]}');
  });
}
