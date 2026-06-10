import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/statistics_provider.dart';

class StatisticsScreen extends StatelessWidget {
  const StatisticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => StatisticsProvider(),
      child: const _StatisticsView(),
    );
  }
}

class _StatisticsView extends StatelessWidget {
  const _StatisticsView();

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<StatisticsProvider>();

    if (provider.isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    // Default to 0 if key not found
    final triggered = provider.stats['triggered'] ?? 0;
    final dismissed = provider.stats['dismissed'] ?? 0;
    final snoozed = provider.stats['snoozed'] ?? 0;
    final missed = provider.stats['missed'] ?? 0;
    final scheduled = provider.stats['scheduled'] ?? 0;
    final total = triggered + dismissed + snoozed + missed + scheduled;

    return Scaffold(
      appBar: AppBar(title: const Text('Alarm Statistics')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildStatCard(
            context: context,
            title: 'Total Instances',
            value: total.toString(),
            icon: Icons.analytics,
            color: Colors.blue,
          ),
          const SizedBox(height: 16),
          _buildStatCard(
            context: context,
            title: 'Dismissed',
            value: dismissed.toString(),
            icon: Icons.check_circle,
            color: Colors.green,
          ),
          const SizedBox(height: 16),
          _buildStatCard(
            context: context,
            title: 'Snoozed',
            value: snoozed.toString(),
            icon: Icons.snooze,
            color: Colors.orange,
          ),
          const SizedBox(height: 16),
          _buildStatCard(
            context: context,
            title: 'Missed',
            value: missed.toString(),
            icon: Icons.cancel,
            color: Colors.red,
          ),
          const SizedBox(height: 16),
          _buildStatCard(
            context: context,
            title: 'Currently Scheduled',
            value: scheduled.toString(),
            icon: Icons.schedule,
            color: Colors.purple,
          ),
          const SizedBox(height: 16),
          _buildStatCard(
            context: context,
            title: 'Triggered (Unresolved)',
            value: triggered.toString(),
            icon: Icons.notifications_active,
            color: Colors.amber,
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard({
    required BuildContext context,
    required String title,
    required String value,
    required IconData icon,
    required Color color,
  }) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: color.withValues(alpha: 0.2),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: color, size: 28),
            ),
            const SizedBox(width: 20),
            Expanded(
              child: Text(title, style: Theme.of(context).textTheme.titleLarge),
            ),
            Text(
              value,
              style: Theme.of(context).textTheme.displaySmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
