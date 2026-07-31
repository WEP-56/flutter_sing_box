import 'package:flutter_sing_box/flutter_sing_box.dart';

/// Builds the default User-Agent string for subscription requests.
class SubscribeUserAgent {
  static String? _cachedVersion;

  /// Returns the default User-Agent string for Android subscription requests.
  static Future<String> getDefaultUserAgent() async {
    _cachedVersion ??= await FlutterSingBox().getSingBoxVersion();
    const String clashVerge = 'clash-verge/2.4.7';
    return 'SFA/$_cachedVersion sing-box/$_cachedVersion ClashMetaForAndroid/2.11.27 $clashVerge';
  }
}
