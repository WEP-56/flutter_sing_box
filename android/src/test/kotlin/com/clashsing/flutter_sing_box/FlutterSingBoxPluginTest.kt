package com.clashsing.flutter_sing_box

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.mockito.Mockito
import kotlin.test.Test

internal class FlutterSingBoxPluginTest {
    @Test
    fun checkConfigRejectsNonStringArguments() {
        val plugin = FlutterSingBoxPlugin()
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)

        plugin.onMethodCall(MethodCall("checkConfig", null), mockResult)

        Mockito.verify(mockResult).error(
            "INVALID_ARGUMENTS",
            "Configuration must be a string",
            null,
        )
    }

    @Test
    fun checkConfigRejectsBlankConfigurations() {
        val plugin = FlutterSingBoxPlugin()
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)

        plugin.onMethodCall(MethodCall("checkConfig", "  "), mockResult)

        Mockito.verify(mockResult).error(
            "CONFIG_EMPTY",
            "Configuration must not be empty",
            null,
        )
    }

    @Test
    fun checkConfigRejectsCallsWhenPluginIsDetached() {
        val plugin = FlutterSingBoxPlugin()
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)

        plugin.onMethodCall(MethodCall("checkConfig", "{}"), mockResult)

        Mockito.verify(mockResult).error(
            "PLUGIN_UNAVAILABLE",
            "Plugin is not attached to an engine",
            null,
        )
    }

    @Test
    fun urlTestOutboundRejectsInvalidArguments() {
        val plugin = FlutterSingBoxPlugin()
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)

        plugin.onMethodCall(
            MethodCall(
                "urlTestOutbound",
                mapOf(
                    "outboundTag" to "",
                    "url" to "not-a-url",
                    "timeoutMs" to 0,
                ),
            ),
            mockResult,
        )

        Mockito.verify(mockResult).error(
            "INVALID_ARGUMENTS",
            "Invalid outbound URL test arguments",
            null,
        )
    }

    @Test
    fun urlTestOutboundRejectsCallsWhenPluginIsDetached() {
        val plugin = FlutterSingBoxPlugin()
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)

        plugin.onMethodCall(
            MethodCall(
                "urlTestOutbound",
                mapOf(
                    "outboundTag" to "node-a",
                    "url" to "https://www.gstatic.com/generate_204",
                    "timeoutMs" to 8_000,
                ),
            ),
            mockResult,
        )

        Mockito.verify(mockResult).error(
            "PLUGIN_UNAVAILABLE",
            "Plugin is not attached to an engine",
            null,
        )
    }

    @Test
    fun setClashModeRejectsBlankArguments() {
        val plugin = FlutterSingBoxPlugin()
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)

        plugin.onMethodCall(MethodCall("setClashMode", "  "), mockResult)

        Mockito.verify(mockResult).error(
            "INVALID_ARGUMENTS",
            "无效的参数",
            null,
        )
    }

    @Test
    fun setClashModeRejectsModesOutsideTheKnownList() {
        val plugin = FlutterSingBoxPlugin()
        val mockResult: MethodChannel.Result = Mockito.mock(MethodChannel.Result::class.java)

        // 未附加 engine 时没有内核模式列表，任何模式都应被稳定拒绝而不是崩溃。
        plugin.onMethodCall(MethodCall("setClashMode", "Rule"), mockResult)

        Mockito.verify(mockResult).error(
            "INVALID_CLASH_MODE",
            "无效的Clash模式",
            null,
        )
    }
}
