import 'package:flutter/services.dart';
import '../models/alarm_model.dart';

class AlarmService {
  static const MethodChannel _channel = MethodChannel(
    'com.deepak.alarm/alarms',
  );

  /// Get all alarms
  static Future<List<Alarm>> getAllAlarms() async {
    final List<dynamic>? result = await _channel.invokeMethod('getAllAlarms');
    if (result == null) return [];
    return result
        .map((e) => Alarm.fromMap(e as Map<Object?, Object?>))
        .toList();
  }

  /// Get instances for a specific alarm
  static Future<List<AlarmInstance>> getInstances(int alarmId) async {
    final List<dynamic>? result = await _channel.invokeMethod('getInstances', {
      'alarmId': alarmId,
    });
    if (result == null) return [];
    return result
        .map((e) => AlarmInstance.fromMap(e as Map<Object?, Object?>))
        .toList();
  }

  /// Create a new alarm and schedule it
  static Future<int?> createAlarm(Alarm alarm) async {
    final int? id = await _channel.invokeMethod('createAlarm', alarm.toMap());
    return id;
  }

  /// Update an existing alarm and reschedule if needed
  static Future<bool> updateAlarm(Alarm alarm) async {
    final bool? success = await _channel.invokeMethod(
      'updateAlarm',
      alarm.toMap(),
    );
    return success ?? false;
  }

  /// Delete an alarm and cancel all its scheduled instances
  static Future<bool> deleteAlarm(int id) async {
    final bool? success = await _channel.invokeMethod('deleteAlarm', {
      'id': id,
    });
    return success ?? false;
  }

  /// Toggle an alarm on or off
  static Future<bool> toggleAlarm(int id, bool enabled) async {
    final bool? success = await _channel.invokeMethod('toggleAlarm', {
      'id': id,
      'enabled': enabled,
    });
    return success ?? false;
  }

  /// Check if we have exact alarm permission (Android 12+)
  static Future<bool> checkExactAlarmPermission() async {
    final bool? hasPermission = await _channel.invokeMethod(
      'checkExactAlarmPermission',
    );
    return hasPermission ?? true;
  }

  /// Request exact alarm permission (Android 12+)
  static Future<bool> requestExactAlarmPermission() async {
    final bool? success = await _channel.invokeMethod(
      'requestExactAlarmPermission',
    );
    return success ?? false;
  }

  /// Check if battery optimizations are ignored
  static Future<bool> checkBatteryOptimization() async {
    final bool? ignored = await _channel.invokeMethod(
      'checkBatteryOptimization',
    );
    return ignored ?? true;
  }

  /// Request ignore battery optimizations
  static Future<bool> requestBatteryOptimization() async {
    final bool? success = await _channel.invokeMethod(
      'requestBatteryOptimization',
    );
    return success ?? false;
  }

  /// Check if OEM auto-start logic is applicable and available
  static Future<bool> isAutoStartAvailable() async {
    final bool? available = await _channel.invokeMethod('isAutoStartAvailable');
    return available ?? false;
  }

  /// Get manufacturer-specific auto-start instructions
  static Future<Map<String, dynamic>?> getAutoStartInfo() async {
    final dynamic info = await _channel.invokeMethod('getAutoStartInfo');
    if (info == null) return null;
    return Map<String, dynamic>.from(info as Map);
  }

  /// Launch OEM auto-start settings page
  static Future<bool> requestAutoStart() async {
    final bool? success = await _channel.invokeMethod('requestAutoStart');
    return success ?? false;
  }

  /// Launch Android RingtonePicker to select an alarm sound
  static Future<String?> pickAlarmSound(String? currentUri) async {
    final String? uri = await _channel.invokeMethod('pickAlarmSound', {
      'currentUri': currentUri,
    });
    return uri; // empty string means silent, null means cancelled
  }

  /// Get alarm statistics mapped by status (e.g. scheduled, triggered, snoozed, dismissed, missed)
  static Future<Map<String, int>> getStatistics() async {
    final Map<dynamic, dynamic>? result = await _channel.invokeMethod(
      'getStatistics',
    );
    if (result == null) return {};
    return result.map((key, value) => MapEntry(key.toString(), value as int));
  }
}
