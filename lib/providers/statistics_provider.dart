import 'package:flutter/material.dart';
import '../services/alarm_service.dart';

class StatisticsProvider extends ChangeNotifier {
  Map<String, int> stats = {};
  bool isLoading = true;

  StatisticsProvider() {
    loadStats();
  }

  Future<void> loadStats() async {
    isLoading = true;
    notifyListeners();

    stats = await AlarmService.getStatistics();
    isLoading = false;
    notifyListeners();
  }
}
