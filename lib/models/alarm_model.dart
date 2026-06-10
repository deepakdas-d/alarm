class Alarm {
  final int id;
  final String label;
  final int hour;
  final int minute;
  final bool enabled;
  final int
  repeatDays; // Bitmask: 1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun
  final String soundUri;
  final bool vibrationEnabled;
  final int snoozeMinutes;
  final int createdAt;
  final int updatedAt;

  Alarm({
    this.id = 0,
    this.label = '',
    required this.hour,
    required this.minute,
    this.enabled = true,
    this.repeatDays = 0,
    this.soundUri = '',
    this.vibrationEnabled = true,
    this.snoozeMinutes = 10,
    this.createdAt = 0,
    this.updatedAt = 0,
  });

  factory Alarm.fromMap(Map<Object?, Object?> map) {
    return Alarm(
      id: map['id'] as int? ?? 0,
      label: map['label'] as String? ?? '',
      hour: map['hour'] as int? ?? 0,
      minute: map['minute'] as int? ?? 0,
      enabled: map['enabled'] == null
          ? true
          : (map['enabled'] == true || map['enabled'] == 1),
      repeatDays: map['repeatDays'] as int? ?? 0,
      soundUri: map['soundUri'] as String? ?? '',
      vibrationEnabled: map['vibrationEnabled'] == null
          ? true
          : (map['vibrationEnabled'] == true || map['vibrationEnabled'] == 1),
      snoozeMinutes: map['snoozeMinutes'] as int? ?? 10,
      createdAt: map['createdAt'] as int? ?? 0,
      updatedAt: map['updatedAt'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'label': label,
      'hour': hour,
      'minute': minute,
      'enabled': enabled,
      'repeatDays': repeatDays,
      'soundUri': soundUri,
      'vibrationEnabled': vibrationEnabled,
      'snoozeMinutes': snoozeMinutes,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
    };
  }

  Alarm copyWith({
    int? id,
    String? label,
    int? hour,
    int? minute,
    bool? enabled,
    int? repeatDays,
    String? soundUri,
    bool? vibrationEnabled,
    int? snoozeMinutes,
    int? createdAt,
    int? updatedAt,
  }) {
    return Alarm(
      id: id ?? this.id,
      label: label ?? this.label,
      hour: hour ?? this.hour,
      minute: minute ?? this.minute,
      enabled: enabled ?? this.enabled,
      repeatDays: repeatDays ?? this.repeatDays,
      soundUri: soundUri ?? this.soundUri,
      vibrationEnabled: vibrationEnabled ?? this.vibrationEnabled,
      snoozeMinutes: snoozeMinutes ?? this.snoozeMinutes,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  bool get isRepeating => repeatDays != 0;

  bool repeatsOnDay(int dayBit) => (repeatDays & dayBit) != 0;
}

class AlarmInstance {
  final int instanceId;
  final int alarmId;
  final int triggerTime;
  final String status;

  AlarmInstance({
    required this.instanceId,
    required this.alarmId,
    required this.triggerTime,
    required this.status,
  });

  factory AlarmInstance.fromMap(Map<Object?, Object?> map) {
    return AlarmInstance(
      instanceId: map['instanceId'] as int? ?? 0,
      alarmId: map['alarmId'] as int? ?? 0,
      triggerTime: map['triggerTime'] as int? ?? 0,
      status: map['status'] as String? ?? 'scheduled',
    );
  }
}
