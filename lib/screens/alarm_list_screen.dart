import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:permission_handler/permission_handler.dart';
import '../models/alarm_model.dart';
import '../providers/alarm_provider.dart';
import '../services/alarm_service.dart';
import 'alarm_editor_screen.dart';
import 'settings_screen.dart';
import 'statistics_screen.dart';

class AlarmListScreen extends StatefulWidget {
  const AlarmListScreen({super.key});

  @override
  State<AlarmListScreen> createState() => _AlarmListScreenState();
}

class _AlarmListScreenState extends State<AlarmListScreen> {
  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  Future<void> _checkPermissions() async {
    // 1. Notification Permission (Required for Android 13+)
    var notificationStatus = await Permission.notification.status;
    if (!notificationStatus.isGranted) {
      await Permission.notification.request();
    }

    // 1.5. Display Over Other Apps (Required to reliably wake screen from sleep)
    var systemAlertStatus = await Permission.systemAlertWindow.status;
    if (!systemAlertStatus.isGranted) {
      if (mounted) {
        await showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Display Over Other Apps'),
            content: const Text(
              'To ensure the alarm screen can forcefully appear when your phone is asleep, please grant "Display over other apps".',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Skip'),
              ),
              FilledButton(
                onPressed: () async {
                  Navigator.pop(context);
                  await Permission.systemAlertWindow.request();
                },
                child: const Text('Grant'),
              ),
            ],
          ),
        );
      }
    }

    // 2. Exact Alarm Permission (Required for Android 12+)
    final hasExactPermission = await AlarmService.checkExactAlarmPermission();
    if (!hasExactPermission) {
      if (mounted) {
        await showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Exact Alarm Permission'),
            content: const Text(
              'For alarms to ring at the exact time in the background, you must grant exact alarms access.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Skip'),
              ),
              FilledButton(
                onPressed: () {
                  Navigator.pop(context);
                  AlarmService.requestExactAlarmPermission();
                },
                child: const Text('Grant'),
              ),
            ],
          ),
        );
      }
    }

    // 3. Battery Optimization
    final isBatteryOptimizedIgnored =
        await AlarmService.checkBatteryOptimization();
    if (!isBatteryOptimizedIgnored) {
      if (mounted) {
        await showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Battery Optimization'),
            content: const Text(
              'To prevent the system from killing the background alarm service, please disable battery optimization for this app.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Skip'),
              ),
              FilledButton(
                onPressed: () {
                  Navigator.pop(context);
                  AlarmService.requestBatteryOptimization();
                },
                child: const Text('Disable'),
              ),
            ],
          ),
        );
      }
    }

    // 4. Auto Start (OEM specific)
    final autoStartAvailable = await AlarmService.isAutoStartAvailable();
    if (autoStartAvailable) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text(
              'Consider checking Auto-Start in App Settings to ensure reliability.',
            ),
            action: SnackBarAction(
              label: 'Fix',
              onPressed: () => AlarmService.requestAutoStart(),
            ),
            duration: const Duration(seconds: 4),
          ),
        );
      }
    }
  }

  String _formatTime(int hour, int minute) {
    final period = hour >= 12 ? 'PM' : 'AM';
    var h = hour % 12;
    if (h == 0) h = 12;
    final m = minute.toString().padLeft(2, '0');
    return '$h:$m $period';
  }

  String _formatRepeatDays(int mask) {
    if (mask == 0) return 'Once';
    if (mask == 127) return 'Everyday';
    if (mask == 31) return 'Weekdays';
    if (mask == 96) return 'Weekends';

    final days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    final active = <String>[];
    for (var i = 0; i < 7; i++) {
      if ((mask & (1 << i)) != 0) active.add(days[i]);
    }
    return active.join(', ');
  }

  Future<void> _toggleAlarm(Alarm alarm, bool enabled) async {
    await context.read<AlarmProvider>().toggleAlarm(alarm, enabled);
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<AlarmProvider>();
    final isLoading = provider.isLoading;
    final alarms = provider.alarms;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Alarms'),
        actions: [
          IconButton(
            icon: const Icon(Icons.analytics),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const StatisticsScreen(),
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const SettingsScreen()),
              ).then((_) => _checkPermissions());
            },
          ),
        ],
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : alarms.isEmpty
          ? _buildEmptyState()
          : _buildAlarmList(alarms),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => const AlarmEditorScreen()),
          );
        },
        child: const Icon(Icons.add, size: 32),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.alarm_off,
            size: 80,
            color: Theme.of(context).colorScheme.surface,
          ),
          const SizedBox(height: 24),
          Text(
            'No alarms yet',
            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
              color: Theme.of(context).colorScheme.surface,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Tap the + button to create one.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }

  Widget _buildAlarmList(List<Alarm> alarms) {
    return ListView.builder(
      padding: const EdgeInsets.only(bottom: 100, top: 16),
      itemCount: alarms.length,
      itemBuilder: (context, index) {
        final alarm = alarms[index];
        return _buildAlarmCard(alarm);
      },
    );
  }

  Widget _buildAlarmCard(Alarm alarm) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Dismissible(
        key: Key('alarm_${alarm.id}'),
        direction: DismissDirection.endToStart,
        background: Container(
          alignment: Alignment.centerRight,
          padding: const EdgeInsets.only(right: 24),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.error,
            borderRadius: BorderRadius.circular(24),
          ),
          child: const Icon(Icons.delete, color: Colors.white, size: 32),
        ),
        onDismissed: (direction) {
          context.read<AlarmProvider>().deleteAlarm(alarm.id);
        },
        child: InkWell(
          onTap: () async {
            await Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => AlarmEditorScreen(alarm: alarm),
              ),
            );
          },
          borderRadius: BorderRadius.circular(24),
          child: Card(
            margin: EdgeInsets.zero,
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _formatTime(alarm.hour, alarm.minute),
                          style: Theme.of(context).textTheme.displayLarge
                              ?.copyWith(
                                color: alarm.enabled
                                    ? Theme.of(
                                        context,
                                      ).textTheme.displayLarge?.color
                                    : Theme.of(context).disabledColor,
                                fontWeight: FontWeight.w400,
                              ),
                        ),
                        if (alarm.label.isNotEmpty) ...[
                          const SizedBox(height: 4),
                          Text(
                            alarm.label,
                            style: Theme.of(context).textTheme.titleLarge
                                ?.copyWith(
                                  color: alarm.enabled
                                      ? Theme.of(
                                          context,
                                        ).textTheme.titleLarge?.color
                                      : Theme.of(context).disabledColor,
                                ),
                          ),
                        ],
                        const SizedBox(height: 8),
                        Text(
                          _formatRepeatDays(alarm.repeatDays),
                          style: Theme.of(context).textTheme.bodyMedium
                              ?.copyWith(
                                color: alarm.enabled
                                    ? Theme.of(context).colorScheme.primary
                                    : Theme.of(context).disabledColor,
                                fontWeight: FontWeight.w500,
                              ),
                        ),
                      ],
                    ),
                  ),
                  Switch(
                    value: alarm.enabled,
                    onChanged: (val) => _toggleAlarm(alarm, val),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
