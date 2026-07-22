class AppConfig {
  AppConfig._();

  // Android emulator uses 10.0.2.2 to reach the host machine.
  // Override for a physical device with:
  // flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080/api/v1
  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api/v1',
  );
}
