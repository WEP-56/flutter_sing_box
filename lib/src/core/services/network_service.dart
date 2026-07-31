import 'dart:io' as io;

import 'package:dio/dio.dart';
import 'package:flutter_sing_box/src/core/services/subscribe_user_agent.dart';

import '../../data/network/api_result.dart';
import '../../data/network/dio_client.dart';

/// Fetches subscription content over the network.
class NetworkService {
  NetworkService._internal();

  /// The singleton instance of [NetworkService].
  static final NetworkService instance = NetworkService._internal();

  /// Creates the singleton [NetworkService] instance.
  factory NetworkService() => instance;

  /// Downloads the subscription at [uri] and returns it as an [ApiResult].
  ///
  /// Uses [userAgent] when provided, otherwise the default subscribe user agent.
  Future<ApiResult<dynamic>> fetchSubscription(
    Uri uri, {
    String? userAgent,
  }) async {
    try {
      final response = await DioClient().dio.getUri(
        uri,
        options: Options(
          headers: {
            ...DioClient().dio.options.headers,
            'User-Agent':
                userAgent ?? await SubscribeUserAgent.getDefaultUserAgent(),
          },
        ),
      );
      if (response.statusCode == io.HttpStatus.ok) {
        final apiResult = ApiResult(response.data, response.headers.map);
        return apiResult;
      } else {
        throw Exception(
          'Failed to load, statusCode: ${response.statusCode}, message: ${response.statusMessage}, for $uri',
        );
      }
    } catch (e) {
      rethrow;
    }
  }
}
