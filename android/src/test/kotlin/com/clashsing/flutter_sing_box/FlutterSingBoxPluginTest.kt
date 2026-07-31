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
}
