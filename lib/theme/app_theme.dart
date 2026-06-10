import 'package:flutter/material.dart';

class AppTheme {
  // Vibrant accents
  static const Color accentGradientStart = Color(0xFF60A5FA); // Light Blue
  static const Color accentGradientEnd = Color(0xFF3B82F6); // Deep Blue
  static const Color accentColor = Color(0xFF3B82F6);

  // Background and surfaces
  static const Color background = Color(0xFF0F172A); // Very dark slate
  static const Color surface = Color(0xFF1E293B); // Slightly lighter slate
  static const Color surfaceLight = Color(0xFF334155);

  // Text colors
  static const Color textPrimary = Color(0xFFF8FAFC);
  static const Color textSecondary = Color(0xFF94A3B8);

  // Functional colors
  static const Color error = Color(0xFFEF4444);
  static const Color success = Color(0xFF10B981);
  static const Color warning = Color(0xFFF59E0B);

  static final ThemeData darkTheme = ThemeData(
    brightness: Brightness.dark,
    scaffoldBackgroundColor: background,
    primaryColor: accentColor,
    colorScheme: const ColorScheme.dark(
      primary: accentColor,
      secondary: accentGradientStart,
      surface: surface,
      error: error,
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onSurface: textPrimary,
      onError: Colors.white,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(
        color: textPrimary,
        fontSize: 28,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.5,
      ),
      iconTheme: IconThemeData(color: textPrimary),
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: accentColor,
      foregroundColor: Colors.white,
      elevation: 4,
    ),
    switchTheme: SwitchThemeData(
      thumbColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) return Colors.white;
        return textSecondary;
      }),
      trackColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) return accentColor;
        return surfaceLight;
      }),
      trackOutlineColor: WidgetStateProperty.all(Colors.transparent),
    ),
    textTheme:
        const TextTheme(
          displayLarge: TextStyle(
            color: textPrimary,
            fontSize: 56,
            fontWeight: FontWeight.w300,
          ),
          displayMedium: TextStyle(
            color: textPrimary,
            fontSize: 48,
            fontWeight: FontWeight.w300,
          ),
          headlineLarge: TextStyle(
            color: textPrimary,
            fontSize: 32,
            fontWeight: FontWeight.w600,
          ),
          headlineMedium: TextStyle(
            color: textPrimary,
            fontSize: 24,
            fontWeight: FontWeight.w600,
          ),
          titleLarge: TextStyle(
            color: textPrimary,
            fontSize: 20,
            fontWeight: FontWeight.w600,
          ),
          bodyLarge: TextStyle(
            color: textPrimary,
            fontSize: 16,
            fontWeight: FontWeight.w400,
          ),
          bodyMedium: TextStyle(
            color: textSecondary,
            fontSize: 14,
            fontWeight: FontWeight.w400,
          ),
          labelLarge: TextStyle(
            color: textPrimary,
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ).apply(
          fontFamily:
              'Roboto', // Defaulting to Roboto; you can replace with Inter/Outfit if added later
        ),
  );
}
