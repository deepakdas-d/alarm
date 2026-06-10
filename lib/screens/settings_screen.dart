import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/settings_provider.dart';
import '../services/alarm_service.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => SettingsProvider(),
      child: const _SettingsView(),
    );
  }
}

class _SettingsView extends StatelessWidget {
  const _SettingsView();

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<SettingsProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('Settings & Permissions')),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
        children: [
          _buildSectionHeader(context, 'Reliability'),
          _buildInfoText(
            context,
            'For alarms to ring exactly on time when the app is closed, please grant the following permissions.',
          ),
          const SizedBox(height: 16),

          _buildPermissionCard(
            context: context,
            title: 'Exact Alarms',
            description:
                'Required to fire alarms at the precise time on Android 12+.',
            isGranted: provider.exactAlarmPermission,
            icon: Icons.access_alarms,
            onTap: () async {
              await context
                  .read<SettingsProvider>()
                  .requestExactAlarmPermission();
            },
          ),
          const SizedBox(height: 12),

          _buildPermissionCard(
            context: context,
            title: 'Notifications',
            description:
                'Required to display the alarm ringing screen on Android 13+.',
            isGranted: provider.notificationPermission,
            icon: Icons.notifications_active,
            onTap: () async {
              await context
                  .read<SettingsProvider>()
                  .requestNotificationPermission();
            },
          ),
          const SizedBox(height: 12),

          _buildPermissionCard(
            context: context,
            title: 'Display Over Other Apps',
            description:
                'CRITICAL: Forces the alarm screen to wake up the phone when the screen is off (System Alert Window).',
            isGranted: provider.systemAlertWindow,
            icon: Icons.layers,
            onTap: () async {
              await context.read<SettingsProvider>().requestSystemAlertWindow();
            },
          ),
          const SizedBox(height: 12),

          _buildPermissionCard(
            context: context,
            title: 'Ignore Battery Optimizations',
            description:
                'Prevents Android from killing the alarm engine to save power (Doze mode pass).',
            isGranted: provider.batteryOptimized,
            icon: Icons.battery_alert,
            onTap: () async {
              await context
                  .read<SettingsProvider>()
                  .requestBatteryOptimization();
            },
          ),

          if (provider.autoStartAvailable) ...[
            const SizedBox(height: 12),
            _buildPermissionCard(
              context: context,
              title: 'Auto-Start (OEM specific)',
              description:
                  'Allows alarms to schedule themselves after a device reboot.',
              isGranted:
                  false, // We can't automatically know if it's granted, default to false.
              icon: Icons.restart_alt,
              isFixableLabel: 'FIX',
              onTap: () async {
                final info = await AlarmService.getAutoStartInfo();
                if (!context.mounted) return;
                if (info != null) {
                  showDialog(
                    context: context,
                    builder: (context) => AlertDialog(
                      backgroundColor: Theme.of(context).colorScheme.surface,
                      title: Text('${info['manufacturer']} Auto-Start'),
                      content: Text(
                        info['instructions'] ??
                            'Please enable auto-start in settings.',
                      ),
                      actions: [
                        TextButton(
                          onPressed: () => Navigator.pop(context),
                          child: const Text('CANCEL'),
                        ),
                        ElevatedButton(
                          onPressed: () {
                            Navigator.pop(context);
                            AlarmService.requestAutoStart();
                          },
                          child: const Text('OPEN SETTINGS'),
                        ),
                      ],
                    ),
                  );
                } else {
                  AlarmService.requestAutoStart();
                }
              },
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildSectionHeader(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 8, bottom: 8),
      child: Text(
        title,
        style: Theme.of(context).textTheme.titleLarge?.copyWith(
          color: Theme.of(context).colorScheme.primary,
        ),
      ),
    );
  }

  Widget _buildInfoText(BuildContext context, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Text(text, style: Theme.of(context).textTheme.bodyMedium),
    );
  }

  Widget _buildPermissionCard({
    required BuildContext context,
    required String title,
    required String description,
    required bool isGranted,
    required IconData icon,
    required VoidCallback onTap,
    String isFixableLabel = 'GRANT',
  }) {
    return Card(
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        onTap: isGranted ? null : onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: isGranted
                      ? const Color(0xFF10B981).withValues(alpha: 0.2)
                      : Theme.of(
                          context,
                        ).colorScheme.error.withValues(alpha: 0.2),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  icon,
                  color: isGranted
                      ? const Color(0xFF10B981)
                      : Theme.of(context).colorScheme.error,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: Theme.of(
                        context,
                      ).textTheme.titleLarge?.copyWith(fontSize: 18),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      description,
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              if (isGranted)
                const Icon(Icons.check_circle, color: Colors.green, size: 32)
              else
                ElevatedButton(
                  onPressed: onTap,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.error,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: Text(isFixableLabel),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
