package com.deepak.alarm.bridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.deepak.alarm.data.AlarmEntity
import com.deepak.alarm.data.AlarmRepository
import com.deepak.alarm.engine.AlarmScheduler
import com.deepak.alarm.engine.AutoStartHelper
import com.deepak.alarm.engine.BatteryOptimizationHelper
import com.deepak.alarm.engine.ExactAlarmPermissionManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

/**
 * Bridges Flutter dart calls to native Kotlin alarm implementation.
 */
class AlarmMethodChannel : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var repository: AlarmRepository
    private var activity: Activity? = null
    private var pendingResult: MethodChannel.Result? = null

    companion object {
        private const val REQUEST_CODE_RINGTONE = 999
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        repository = AlarmRepository(context)
        channel = MethodChannel(binding.binaryMessenger, "com.deepak.alarm/alarms")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getAllAlarms" -> {
                val alarms = repository.getAllAlarms().map { it.toMap() }
                result.success(alarms)
            }
            "getInstances" -> {
                val alarmId = call.argument<Number>("alarmId")?.toLong() ?: -1L
                val instances = repository.getInstancesForAlarm(alarmId).map { it.toMap() }
                result.success(instances)
            }
            "createAlarm" -> {
                Log.d("AlarmApp", "createAlarm called from Flutter")
                try {
                    val map = call.arguments as Map<String, Any?>
                    val entity = AlarmEntity.fromMap(map)
                    val id = repository.insertAlarm(entity)
                    val newAlarm = repository.getAlarm(id)
                    if (newAlarm != null && newAlarm.enabled) {
                        AlarmScheduler.scheduleNextAlarm(context, newAlarm)
                    }
                    Log.d("AlarmApp", "Successfully created and scheduled alarm id: $id")
                    result.success(id)
                } catch (e: Exception) {
                    Log.e("AlarmApp", "Failed to create alarm", e)
                    result.error("CREATE_ERROR", e.message, null)
                }
            }
            "updateAlarm" -> {
                Log.d("AlarmApp", "updateAlarm called from Flutter")
                try {
                    val map = call.arguments as Map<String, Any?>
                    val entity = AlarmEntity.fromMap(map)
                    repository.updateAlarm(entity)
                    
                    AlarmScheduler.cancelAlarm(context, entity.id)
                    if (entity.enabled) {
                        AlarmScheduler.scheduleNextAlarm(context, entity)
                    }
                    Log.d("AlarmApp", "Successfully updated alarm id: ${entity.id}")
                    result.success(true)
                } catch (e: Exception) {
                    Log.e("AlarmApp", "Failed to update alarm", e)
                    result.error("UPDATE_ERROR", e.message, null)
                }
            }
            "deleteAlarm" -> {
                val id = call.argument<Number>("id")?.toLong() ?: -1L
                Log.d("AlarmApp", "deleteAlarm called for id: $id")
                AlarmScheduler.cancelAlarm(context, id)
                repository.deleteAlarm(id)
                result.success(true)
            }
            "toggleAlarm" -> {
                val id = call.argument<Number>("id")?.toLong() ?: -1L
                val enabled = call.argument<Boolean>("enabled") ?: false
                Log.d("AlarmApp", "toggleAlarm called for id: $id enabled: $enabled")
                repository.setAlarmEnabled(id, enabled)
                
                AlarmScheduler.cancelAlarm(context, id)
                if (enabled) {
                    val alarm = repository.getAlarm(id)
                    if (alarm != null) {
                        AlarmScheduler.scheduleNextAlarm(context, alarm)
                    }
                }
                result.success(true)
            }
            "checkExactAlarmPermission" -> {
                result.success(ExactAlarmPermissionManager.canScheduleExactAlarms(context))
            }
            "requestExactAlarmPermission" -> {
                val intent = ExactAlarmPermissionManager.createRequestIntent()
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                result.success(true)
            }
            "checkBatteryOptimization" -> {
                result.success(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
            }
            "requestBatteryOptimization" -> {
                val intent = BatteryOptimizationHelper.createRequestIntent(context)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                result.success(true)
            }
            "isAutoStartAvailable" -> {
                result.success(AutoStartHelper.isAutoStartAvailable(context))
            }
            "getAutoStartInfo" -> {
                val info = AutoStartHelper.getAutoStartInfo(context)
                if (info != null) {
                    result.success(mapOf(
                        "manufacturer" to info.manufacturer,
                        "instructions" to info.instructions,
                        "available" to (info.intent != null)
                    ))
                } else {
                    result.success(null)
                }
            }
            "requestAutoStart" -> {
                val info = AutoStartHelper.getAutoStartInfo(context)
                if (info?.intent != null) {
                    try {
                        info.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(info.intent)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("AUTO_START_ERROR", "Failed to launch intent", null)
                    }
                } else {
                    result.success(false)
                }
            }
            "getStatistics" -> {
                val stats = repository.getStatistics()
                result.success(stats)
            }
            "pickAlarmSound" -> {
                if (activity == null) {
                    result.error("NO_ACTIVITY", "Activity is not attached", null)
                    return
                }
                val currentUriString = call.argument<String>("currentUri") ?: ""
                val currentUri = if (currentUriString.isNotEmpty()) Uri.parse(currentUriString) else null
                
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    if (currentUri != null) {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                    }
                }
                pendingResult = result
                activity?.startActivityForResult(intent, REQUEST_CODE_RINGTONE)
            }
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_RINGTONE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                pendingResult?.success(uri?.toString() ?: "")
            } else {
                pendingResult?.success(null) // Cancelled
            }
            pendingResult = null
            return true
        }
        return false
    }
}
