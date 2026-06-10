import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/alarm_model.dart';
import '../providers/alarm_provider.dart';
import '../providers/alarm_editor_provider.dart';
import '../services/alarm_service.dart';

class AlarmEditorScreen extends StatelessWidget {
  final Alarm? alarm; // If null, creating new. If provided, editing existing.

  const AlarmEditorScreen({super.key, this.alarm});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AlarmEditorProvider(alarm: alarm),
      child: _AlarmEditorView(alarm: alarm),
    );
  }
}

class _AlarmEditorView extends StatelessWidget {
  final Alarm? alarm;

  const _AlarmEditorView({this.alarm});

  final List<String> _daysOfWeek = const ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

  Future<void> _selectTime(BuildContext context) async {
    final editorProvider = context.read<AlarmEditorProvider>();
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay(
        hour: editorProvider.selectedHour,
        minute: editorProvider.selectedMinute,
      ),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            timePickerTheme: TimePickerThemeData(
              backgroundColor: Theme.of(context).colorScheme.surface,
              dialBackgroundColor: Theme.of(context).scaffoldBackgroundColor,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      if (context.mounted) {
        context.read<AlarmEditorProvider>().updateTime(
          picked.hour,
          picked.minute,
        );
      }
    }
  }

  Future<void> _saveAlarm(BuildContext context) async {
    final editorProvider = context.read<AlarmEditorProvider>();
    final newAlarm = Alarm(
      id: alarm?.id ?? 0,
      label: editorProvider.labelController.text.trim(),
      hour: editorProvider.selectedHour,
      minute: editorProvider.selectedMinute,
      enabled: true,
      repeatDays: editorProvider.repeatDays,
      soundUri: editorProvider.soundUri,
      vibrationEnabled: editorProvider.vibrationEnabled,
      snoozeMinutes: editorProvider.snoozeMinutes,
    );

    if (alarm != null) {
      await context.read<AlarmProvider>().updateAlarm(newAlarm);
    } else {
      await context.read<AlarmProvider>().addAlarm(newAlarm);
    }

    if (!context.mounted) return;
    Navigator.pop(context, true);
  }

  String _formatTime(int hour, int minute) {
    final period = hour >= 12 ? 'PM' : 'AM';
    var h = hour % 12;
    if (h == 0) h = 12;
    final m = minute.toString().padLeft(2, '0');
    return '$h:$m $period';
  }

  @override
  Widget build(BuildContext context) {
    final editorProvider = context.watch<AlarmEditorProvider>();

    return Scaffold(
      appBar: AppBar(
        title: Text(alarm != null ? 'Edit Alarm' : 'New Alarm'),
        actions: [
          IconButton(
            icon: const Icon(Icons.check, color: Colors.white, size: 28),
            onPressed: () => _saveAlarm(context),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          // Time Selector
          Center(
            child: GestureDetector(
              onTap: () => _selectTime(context),
              child: Text(
                _formatTime(
                  editorProvider.selectedHour,
                  editorProvider.selectedMinute,
                ),
                style: Theme.of(context).textTheme.displayLarge?.copyWith(
                  color: Theme.of(context).colorScheme.primary,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ),
          const SizedBox(height: 48),

          // Label Input
          TextField(
            controller: editorProvider.labelController,
            decoration: InputDecoration(
              labelText: 'Label',
              labelStyle: TextStyle(
                color: Theme.of(context).colorScheme.primary,
              ),
              filled: true,
              fillColor: Theme.of(context).colorScheme.surface,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: BorderSide.none,
              ),
              prefixIcon: Icon(
                Icons.label,
                color: Theme.of(context).colorScheme.primary,
              ),
            ),
          ),
          const SizedBox(height: 32),

          // Repeat Days
          Text('Repeat', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: List.generate(7, (index) {
              final isSelected =
                  (editorProvider.repeatDays & (1 << index)) != 0;
              return GestureDetector(
                onTap: () =>
                    context.read<AlarmEditorProvider>().toggleDay(index),
                child: Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: isSelected
                        ? Theme.of(context).colorScheme.primary
                        : Theme.of(context).colorScheme.surface,
                  ),
                  child: Center(
                    child: Text(
                      _daysOfWeek[index],
                      style: TextStyle(
                        color: isSelected
                            ? Colors.white
                            : Theme.of(context).textTheme.bodyMedium?.color,
                        fontWeight: isSelected
                            ? FontWeight.bold
                            : FontWeight.normal,
                      ),
                    ),
                  ),
                ),
              );
            }),
          ),
          const SizedBox(height: 32),

          // Sound Picker
          Container(
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              borderRadius: BorderRadius.circular(16),
            ),
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () async {
                final uri = await AlarmService.pickAlarmSound(
                  editorProvider.soundUri,
                );
                if (uri != null && context.mounted) {
                  context.read<AlarmEditorProvider>().setSoundUri(uri);
                }
              },
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 16,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        Icon(
                          Icons.music_note,
                          color: Theme.of(context).colorScheme.primary,
                        ),
                        const SizedBox(width: 16),
                        Text(
                          'Sound',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                      ],
                    ),
                    const Icon(Icons.chevron_right),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Vibration Toggle
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(
                      Icons.vibration,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(width: 16),
                    Text(
                      'Vibration',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ],
                ),
                Switch(
                  value: editorProvider.vibrationEnabled,
                  onChanged: (val) =>
                      context.read<AlarmEditorProvider>().setVibration(val),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Snooze Duration
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(
                      Icons.snooze,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(width: 16),
                    Text(
                      'Snooze',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ],
                ),
                DropdownButtonHideUnderline(
                  child: DropdownButton<int>(
                    value: editorProvider.snoozeMinutes,
                    dropdownColor: Theme.of(context).colorScheme.surface,
                    items: [5, 10, 15, 20, 30].map((int value) {
                      return DropdownMenuItem<int>(
                        value: value,
                        child: Text(
                          '$value min',
                          style: TextStyle(
                            color: Theme.of(context).textTheme.bodyLarge?.color,
                          ),
                        ),
                      );
                    }).toList(),
                    onChanged: (int? newValue) {
                      if (newValue != null) {
                        context.read<AlarmEditorProvider>().setSnooze(newValue);
                      }
                    },
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 48),

          // Delete Button (only if editing)
          if (alarm != null)
            ElevatedButton(
              onPressed: () async {
                await context.read<AlarmProvider>().deleteAlarm(alarm!.id);
                if (!context.mounted) return;
                Navigator.pop(context, true);
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: const Text(
                'DELETE ALARM',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
        ],
      ),
    );
  }
}
