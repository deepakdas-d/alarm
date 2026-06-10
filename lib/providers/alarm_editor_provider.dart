import 'package:flutter/material.dart';
import '../models/alarm_model.dart';

class AlarmEditorProvider extends ChangeNotifier {
  int selectedHour;
  int selectedMinute;
  int repeatDays;
  String label;
  bool vibrationEnabled;
  int snoozeMinutes;
  String soundUri;

  final TextEditingController labelController = TextEditingController();

  AlarmEditorProvider({Alarm? alarm})
    : selectedHour = alarm?.hour ?? DateTime.now().hour,
      selectedMinute = alarm?.minute ?? DateTime.now().minute,
      repeatDays = alarm?.repeatDays ?? 0,
      label = alarm?.label ?? "",
      vibrationEnabled = alarm?.vibrationEnabled ?? true,
      snoozeMinutes = alarm?.snoozeMinutes ?? 10,
      soundUri = alarm?.soundUri ?? "" {
    labelController.text = label;
  }

  void updateTime(int hour, int minute) {
    selectedHour = hour;
    selectedMinute = minute;
    notifyListeners();
  }

  void toggleDay(int index) {
    final bit = 1 << index;
    if ((repeatDays & bit) != 0) {
      repeatDays &= ~bit; // Turn off
    } else {
      repeatDays |= bit; // Turn on
    }
    notifyListeners();
  }

  void setSoundUri(String uri) {
    soundUri = uri;
    notifyListeners();
  }

  void setVibration(bool enabled) {
    vibrationEnabled = enabled;
    notifyListeners();
  }

  void setSnooze(int minutes) {
    snoozeMinutes = minutes;
    notifyListeners();
  }

  @override
  void dispose() {
    labelController.dispose();
    super.dispose();
  }
}
