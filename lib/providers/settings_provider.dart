import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import '../services/alarm_service.dart';

class SettingsProvider extends ChangeNotifier {
  bool exactAlarmPermission = true;
  bool batteryOptimized = true;
  bool autoStartAvailable = false;
  bool notificationPermission = true;
  bool systemAlertWindow = true;

  SettingsProvider() {
    loadStatus();
  }

  Future<void> loadStatus() async {
    exactAlarmPermission = await AlarmService.checkExactAlarmPermission();
    batteryOptimized = await AlarmService.checkBatteryOptimization();
    autoStartAvailable = await AlarmService.isAutoStartAvailable();
    notificationPermission = await Permission.notification.isGranted;
    systemAlertWindow = await Permission.systemAlertWindow.isGranted;
    notifyListeners();
  }

  Future<void> requestExactAlarmPermission() async {
    await AlarmService.requestExactAlarmPermission();
    await loadStatus();
  }

  Future<void> requestBatteryOptimization() async {
    await AlarmService.requestBatteryOptimization();
    await loadStatus();
  }

  Future<void> requestNotificationPermission() async {
    await Permission.notification.request();
    await loadStatus();
  }

  Future<void> requestSystemAlertWindow() async {
    await Permission.systemAlertWindow.request();
    await loadStatus();
  }
}
