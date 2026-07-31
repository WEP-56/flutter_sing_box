package com.clashsing.flutter_sing_box

import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.util.Log
import com.clashsing.flutter_sing_box.cs.PluginManager
import com.clashsing.flutter_sing_box.cs.SingBoxConnector
import com.clashsing.flutter_sing_box.utils.ProfileManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.bg.BoxService
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class FlutterSingBoxPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    companion object {
        private const val TAG = "FlutterSingBoxPlugin"
        private const val METHOD_CHANNEL_NAME = "flutter_sing_box_method"
        private const val VPN_REQUEST_CODE = 1001
    }

    private lateinit var channel: MethodChannel
    private var singBoxConnector: SingBoxConnector? = null
    private var activityBinding: ActivityPluginBinding? = null
    private val pendingStartVpnResult = AtomicReference<Result?>(null)
    private val pendingBackgroundResults = ConcurrentHashMap.newKeySet<Result>()
    private var backgroundExecutor: ExecutorService? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(TAG, "onAttachedToEngine ---------------------------------")
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, METHOD_CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        PluginManager.init(flutterPluginBinding.applicationContext)
        singBoxConnector = SingBoxConnector(flutterPluginBinding.binaryMessenger)
        backgroundExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(TAG, "onDetachedFromEngine ---------------------------------")
        channel.setMethodCallHandler(null)
        singBoxConnector = null
        val executor = backgroundExecutor
        backgroundExecutor = null
        executor?.shutdownNow()
        pendingBackgroundResults.toList().forEach { result ->
            if (pendingBackgroundResults.remove(result)) {
                result.error("PLUGIN_UNAVAILABLE", "Plugin is not attached to an engine", null)
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "init" -> {
                Log.d(TAG, "onMethodCall init ---------------------------------")
                val activity = activityBinding?.activity
                if (activity != null) {
                    singBoxConnector?.connect(activity)
                    result.success(null)
                    return
                } else {
                    result.error("NO_ACTIVITY", "无法获取Activity实例", null)
                    return
                }
            }
            "startVpn" -> {
                val activity = activityBinding?.activity
                if (activity == null) {
                    result.error("NO_ACTIVITY", "无法获取Activity实例", null)
                    return
                }
                pendingStartVpnResult.set(result)
                val intent = VpnService.prepare(activity)
                if (intent != null) {
                    activity.startActivityForResult(intent, VPN_REQUEST_CODE)
                } else {
                    startVpnService(result)
                }
            }
            "stopVpn" -> {
                BoxService.stop()
                result.success(null)
            }
            "serviceReload" -> {
                try {
                    Libbox.newStandaloneCommandClient().serviceReload()
                    result.success(null)
                } catch (e: Exception) {
                    result.error("SERVICE_RELOAD_FAILED", "服务重载失败", e)
                }
            }
            "setClashMode" -> {
                val clashMode = call.arguments as String
                if (singBoxConnector?.clientClashMode?.modes?.contains(clashMode) == true) {
                    Libbox.newStandaloneCommandClient().setClashMode(clashMode)
                    singBoxConnector?.clashModeClient?.connect()
                    result.success(null)
                } else {
                    result.error("INVALID_CLASH_MODE", "无效的Clash模式", null)
                }
            }
            "selectOutbound" -> {
                val groupArgName = "groupTag"
                val outboundArgName = "outboundTag"
                if (call.arguments !is Map<*, *>) {
                    result.error("INVALID_ARGUMENTS", "无效的参数", null)
                    return
                }
                val argsMap = call.arguments as Map<*, *>
                val groupTag = argsMap[groupArgName] as String?
                val outboundTag = argsMap[outboundArgName] as String?
                if (groupTag.isNullOrBlank() || outboundTag.isNullOrBlank()) {
                    result.error("INVALID_ARGUMENTS", "无效的参数", null)
                    return
                }
                try {
                    Libbox.newStandaloneCommandClient().selectOutbound(groupTag, outboundTag)
                    singBoxConnector?.groupClient?.connect()
                    result.success(null)
                } catch (e: Exception) {
                    result.error("INVALID_ARGUMENTS", "无效的参数", e)
                    return
                }
            }
            "setGroupExpand" -> {
                val groupArgName = "groupTag"
                val expandArgName = "isExpand"
                if (call.arguments !is Map<*, *>) {
                    result.error("INVALID_ARGUMENTS", "无效的参数", null)
                    return
                }
                val argsMap = call.arguments as Map<*, *>
                val groupTag = argsMap[groupArgName] as String?
                val isExpand = argsMap[expandArgName] as Boolean?
                if (groupTag.isNullOrBlank() || isExpand == null) {
                    result.error("INVALID_ARGUMENTS", "无效的参数", null)
                    return
                }
                try {
                    Libbox.newStandaloneCommandClient().setGroupExpand(groupTag, isExpand)
                    singBoxConnector?.groupClient?.connect()
                    result.success(null)
                } catch (e: Exception) {
                    result.error("INVALID_ARGUMENTS", "无效的参数", e)
                    return
                }
            }
            "urlTest" -> {
                val groupTag = call.arguments as String?
                if (groupTag.isNullOrBlank()) {
                    result.error("INVALID_ARGUMENTS", "无效的参数", null)
                    return
                }
                try {
                    Libbox.newStandaloneCommandClient().urlTest(groupTag)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("INVALID_ARGUMENTS", "无效的参数", e)
                    return
                }
            }
            "urlTestOutbound" -> {
                val arguments = call.arguments as? Map<*, *>
                val outboundTag = arguments?.get("outboundTag") as? String
                val url = arguments?.get("url") as? String
                val timeoutMs = (arguments?.get("timeoutMs") as? Number)?.toInt()
                val testUri = url?.let(Uri::parse)
                if (
                    outboundTag.isNullOrBlank() ||
                    testUri?.host.isNullOrBlank() ||
                    testUri?.scheme?.lowercase() !in setOf("http", "https") ||
                    timeoutMs == null ||
                    timeoutMs !in 1..60_000
                ) {
                    result.error("INVALID_ARGUMENTS", "Invalid outbound URL test arguments", null)
                    return
                }
                submitBackground(
                    result = result,
                    errorCode = "URL_TEST_FAILED",
                    errorMessage = "Outbound URL test failed",
                ) {
                    testOutbound(outboundTag, url, timeoutMs)
                }
            }
            "getSingBoxVersion" -> {
                val version = Libbox.version()
                result.success(version)
            }
            "checkConfig" -> {
                if (call.arguments !is String) {
                    result.error("INVALID_ARGUMENTS", "Configuration must be a string", null)
                    return
                }
                val configuration = call.arguments as String
                if (configuration.isBlank()) {
                    result.error("CONFIG_EMPTY", "Configuration must not be empty", null)
                    return
                }
                submitBackground(
                    result = result,
                    errorCode = "CONFIG_INVALID",
                    errorMessage = "Configuration is invalid",
                ) {
                    Libbox.checkConfig(configuration)
                    null
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun submitBackground(
        result: Result,
        errorCode: String,
        errorMessage: String,
        block: () -> Any?,
    ) {
        val executor = backgroundExecutor
        if (executor == null || executor.isShutdown) {
            result.error("PLUGIN_UNAVAILABLE", "Plugin is not attached to an engine", null)
            return
        }
        pendingBackgroundResults.add(result)
        runCatching {
            executor.execute {
                runCatching(block)
                    .onSuccess { value ->
                        if (pendingBackgroundResults.remove(result)) {
                            result.success(value)
                        }
                    }
                    .onFailure { error ->
                        if (pendingBackgroundResults.remove(result)) {
                            val failure = error as? PluginFailure
                            result.error(
                                failure?.code ?: errorCode,
                                error.message ?: errorMessage,
                                null,
                            )
                        }
                    }
            }
        }.onFailure {
            if (pendingBackgroundResults.remove(result)) {
                result.error("PLUGIN_UNAVAILABLE", "Plugin is not attached to an engine", null)
            }
        }
    }

    private fun testOutbound(outboundTag: String, testUrl: String, timeoutMs: Int): Int {
        val access = readClashApiAccess()
        val requestUri = Uri.Builder()
            .scheme("http")
            .encodedAuthority(access.authority)
            .appendPath("proxies")
            .appendPath(outboundTag)
            .appendPath("delay")
            .appendQueryParameter("url", testUrl)
            .appendQueryParameter("timeout", timeoutMs.toString())
            .build()
        val connection = URL(requestUri.toString()).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs + 1_000
            connection.setRequestProperty("Accept", "application/json")
            if (access.secret.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer ${access.secret}")
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw PluginFailure(
                    "URL_TEST_FAILED",
                    "Clash API returned HTTP $status${body.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}",
                )
            }
            JSONObject(body).optInt("delay").takeIf { it > 0 }
                ?: throw PluginFailure("URL_TEST_FAILED", "Clash API returned no delay result")
        } finally {
            connection.disconnect()
        }
    }

    private fun readClashApiAccess(): ClashApiAccess {
        val configFile = ProfileManager.getUsingConfig()
        if (!configFile.isFile) {
            throw PluginFailure("CLASH_API_UNAVAILABLE", "Active configuration was not found")
        }
        val clashApi = runCatching {
            JSONObject(configFile.readText())
                .optJSONObject("experimental")
                ?.optJSONObject("clash_api")
        }.getOrNull()
            ?: throw PluginFailure("CLASH_API_UNAVAILABLE", "Clash API is not configured")
        val controller = clashApi.optString("external_controller").trim()
        if (controller.isEmpty()) {
            throw PluginFailure("CLASH_API_UNAVAILABLE", "Clash API controller is not configured")
        }
        val controllerUri = Uri.parse(if (controller.contains("://")) controller else "http://$controller")
        val host = controllerUri.host
            ?: throw PluginFailure("CLASH_API_UNAVAILABLE", "Clash API controller is invalid")
        val port = controllerUri.port
        if (port !in 1..65_535) {
            throw PluginFailure("CLASH_API_UNAVAILABLE", "Clash API controller port is invalid")
        }
        val address = runCatching { InetAddress.getByName(host) }.getOrNull()
            ?: throw PluginFailure("CLASH_API_UNAVAILABLE", "Clash API controller host is invalid")
        if (!address.isLoopbackAddress && !address.isAnyLocalAddress) {
            throw PluginFailure("CLASH_API_UNAVAILABLE", "Clash API controller must be loopback-only")
        }
        val authority = if (address.isAnyLocalAddress) {
            "127.0.0.1:$port"
        } else {
            controllerUri.encodedAuthority
                ?: throw PluginFailure("CLASH_API_UNAVAILABLE", "Clash API controller is invalid")
        }
        val secret = clashApi.optString("secret").trim()
        if (secret.isEmpty()) {
            throw PluginFailure("CLASH_API_UNAVAILABLE", "Clash API controller must be authenticated")
        }
        return ClashApiAccess(authority, secret)
    }

    private data class ClashApiAccess(val authority: String, val secret: String)

    private class PluginFailure(val code: String, message: String) : Exception(message)

    private fun startVpnService(result: Result) {
        try {
            BoxService.start()
            result.success(null)
/*
            val context = activityBinding?.activity?.applicationContext
            if (context != null) {
                val intent = Intent(context, ClashSingVpnService::class.java)
                context.startForegroundService(intent)
                result.success(null)
                return
            } else {
                result.error("NO_CONTEXT", "无法获取Context实例", null)
                return
            }
*/
        } catch (e: Exception) {
            result.error("VPN_ERROR", "启动VPN服务失败:\n${e.message}", null)
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d(TAG, "onAttachedToActivity --------------------")
        activityBinding = binding
        binding.addActivityResultListener(this)
//        singBoxConnector?.connect()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.d(TAG, "onDetachedFromActivityForConfigChanges --------------------")
        activityBinding?.removeActivityResultListener(this)
        activityBinding = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d(TAG, "onReattachedToActivityForConfigChanges --------------------")
        activityBinding = binding
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        Log.d(TAG, "onDetachedFromActivity --------------------")
        singBoxConnector?.disconnect()
        activityBinding?.removeActivityResultListener(this)
        activityBinding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == VPN_REQUEST_CODE) {
            val result = pendingStartVpnResult.getAndSet(null)
            if (result != null) {
                if (resultCode == android.app.Activity.RESULT_OK) {
                    startVpnService(result)
                } else {
                    result.error("VPN_PERMISSION_DENIED", "用户拒绝了VPN权限", null)
                }
            }
            return true
        }
        return false
    }
}
