import 'package:flutter_sing_box/flutter_sing_box_method_channel.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late MethodChannelFlutterSingBox platform;
  late MethodCall receivedCall;

  setUp(() {
    platform = MethodChannelFlutterSingBox();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(platform.methodChannel, (
          MethodCall methodCall,
        ) async {
          receivedCall = methodCall;
          return null;
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(platform.methodChannel, null);
  });

  test('checkConfig sends the configuration and completes', () async {
    const configuration = '{"outbounds":[]}';

    await expectLater(platform.checkConfig(configuration), completes);

    expect(receivedCall.method, 'checkConfig');
    expect(receivedCall.arguments, configuration);
  });

  test('checkConfig propagates PlatformException', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(platform.methodChannel, (methodCall) async {
          throw PlatformException(
            code: 'CONFIG_INVALID',
            message: 'invalid configuration',
          );
        });

    await expectLater(
      platform.checkConfig('{}'),
      throwsA(
        isA<PlatformException>()
            .having((error) => error.code, 'code', 'CONFIG_INVALID')
            .having(
              (error) => error.message,
              'message',
              'invalid configuration',
            ),
      ),
    );
  });

  test('urlTestOutbound sends arguments and returns the delay', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(platform.methodChannel, (methodCall) async {
          receivedCall = methodCall;
          return 86;
        });

    final delay = await platform.urlTestOutbound(
      outboundTag: 'node-a',
      url: 'https://www.gstatic.com/generate_204',
      timeout: const Duration(seconds: 8),
    );

    expect(delay, 86);
    expect(receivedCall.method, 'urlTestOutbound');
    expect(receivedCall.arguments, {
      'outboundTag': 'node-a',
      'url': 'https://www.gstatic.com/generate_204',
      'timeoutMs': 8000,
    });
  });
}
