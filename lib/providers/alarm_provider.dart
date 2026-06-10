import 'dart:developer' as developer;
import 'package:flutter/foundation.dart';
import '../models/alarm_model.dart';
import '../services/alarm_service.dart';

class AlarmProvider extends ChangeNotifier {
  List<Alarm> _alarms = [];
  bool _isLoading = true;

  List<Alarm> get alarms => _alarms;
  bool get isLoading => _isLoading;

  Future<void> loadAlarms() async {
    _isLoading = true;
    notifyListeners();

    try {
      developer.log(
        'Loading all alarms from native service',
        name: 'AlarmProvider',
      );
      _alarms = await AlarmService.getAllAlarms();
      developer.log(
        'Successfully loaded ${_alarms.length} alarms',
        name: 'AlarmProvider',
      );
    } catch (e) {
      developer.log(
        'Error loading alarms: $e',
        name: 'AlarmProvider',
        error: e,
      );
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> addAlarm(Alarm alarm) async {
    try {
      developer.log(
        'Creating new alarm: ${alarm.label.isNotEmpty ? alarm.label : "unnamed"}, ${alarm.hour}:${alarm.minute}',
        name: 'AlarmProvider',
      );
      final int? id = await AlarmService.createAlarm(alarm);
      if (id != null) {
        // Add to local state immediately and notify UI
        _alarms.add(alarm.copyWith(id: id));
        developer.log(
          'Successfully created alarm with id $id',
          name: 'AlarmProvider',
        );
        notifyListeners();
      }
    } catch (e) {
      developer.log(
        'Error creating alarm: $e',
        name: 'AlarmProvider',
        error: e,
      );
      // Reload on failure to sync state
      await loadAlarms();
    }
  }

  Future<void> updateAlarm(Alarm alarm) async {
    try {
      developer.log(
        'Updating alarm with id ${alarm.id}',
        name: 'AlarmProvider',
      );

      // Optimistic update
      final index = _alarms.indexWhere((a) => a.id == alarm.id);
      if (index != -1) {
        _alarms[index] = alarm;
        notifyListeners();
      }

      final success = await AlarmService.updateAlarm(alarm);
      if (success) {
        developer.log(
          'Successfully updated alarm ${alarm.id}',
          name: 'AlarmProvider',
        );
      } else {
        developer.log(
          'Failed to update alarm ${alarm.id}',
          name: 'AlarmProvider',
        );
        await loadAlarms(); // Revert on failure
      }
    } catch (e) {
      developer.log(
        'Error updating alarm: $e',
        name: 'AlarmProvider',
        error: e,
      );
      await loadAlarms();
    }
  }

  Future<void> toggleAlarm(Alarm alarm, bool enabled) async {
    try {
      developer.log(
        'Toggling alarm ${alarm.id} to ${enabled ? "enabled" : "disabled"}',
        name: 'AlarmProvider',
      );

      // Optimistic update
      final index = _alarms.indexWhere((a) => a.id == alarm.id);
      if (index != -1) {
        _alarms[index] = alarm.copyWith(enabled: enabled);
        notifyListeners();
      }

      final success = await AlarmService.toggleAlarm(alarm.id, enabled);
      if (success) {
        developer.log(
          'Successfully toggled alarm ${alarm.id}',
          name: 'AlarmProvider',
        );
      } else {
        developer.log(
          'Failed to toggle alarm ${alarm.id}',
          name: 'AlarmProvider',
        );
        await loadAlarms(); // Revert on failure
      }
    } catch (e) {
      developer.log(
        'Error toggling alarm: $e',
        name: 'AlarmProvider',
        error: e,
      );
      await loadAlarms();
    }
  }

  Future<void> deleteAlarm(int id) async {
    try {
      developer.log('Deleting alarm with id $id', name: 'AlarmProvider');

      // Optimistic update
      _alarms.removeWhere((a) => a.id == id);
      notifyListeners();

      final success = await AlarmService.deleteAlarm(id);
      if (success) {
        developer.log('Successfully deleted alarm $id', name: 'AlarmProvider');
      } else {
        developer.log('Failed to delete alarm $id', name: 'AlarmProvider');
        await loadAlarms(); // Revert on failure
      }
    } catch (e) {
      developer.log(
        'Error deleting alarm: $e',
        name: 'AlarmProvider',
        error: e,
      );
      await loadAlarms();
    }
  }
}
