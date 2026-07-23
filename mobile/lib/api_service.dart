import 'dart:convert';

import 'package:http/http.dart' as http;

import 'config.dart';
import 'models.dart';

class ApiException implements Exception {
  final String message;
  const ApiException(this.message);
  @override
  String toString() => message;
}

class ApiService {
  const ApiService();

  Future<List<MenuItem>> menu() async {
    final response = await http
        .get(Uri.parse('${AppConfig.apiBaseUrl}/menu/items'))
        .timeout(const Duration(seconds: 15));
    final data = _decode(response);
    return (data as List)
        .map((item) => MenuItem.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<Reservation> createReservation({
    required String customerName,
    required String phone,
    required String email,
    required DateTime date,
    required String timeSlot,
    required String reservationTime,
    required int durationMinutes,
    required int partySize,
    required List<int> selectedTableIds,
    required bool notifyEmail,
    required bool notifySms,
  }) async {
    final response = await http
        .post(
          Uri.parse('${AppConfig.apiBaseUrl}/reservations'),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({
            'customerName': customerName,
            'phone': phone,
            'email': email.isEmpty ? null : email,
            'reservationDate': _date(date),
            'timeSlot': timeSlot,
            'reservationTime': reservationTime,
            'durationMinutes': durationMinutes,
            'partySize': partySize,
            'selectedTableIds': selectedTableIds,
            'notifyEmail': notifyEmail,
            'notifySms': notifySms,
            'preOrderItems': <Object>[],
          }),
        )
        .timeout(const Duration(seconds: 15));
    return Reservation.fromJson(_decode(response) as Map<String, dynamic>);
  }

  Future<List<AvailableTable>> availableTables(
      DateTime date, String time, int duration, int guests) async {
    final uri =
        Uri.parse('${AppConfig.apiBaseUrl}/reservations/available-tables')
            .replace(queryParameters: {
      'date': _date(date),
      'time': time,
      'durationMinutes': '$duration',
      'partySize': '$guests'
    });
    final data =
        _decode(await http.get(uri).timeout(const Duration(seconds: 15)))
            as List;
    return data
        .map((e) => AvailableTable.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<Reservation> lookup(String code, String phone) async {
    final uri = Uri.parse(
      '${AppConfig.apiBaseUrl}/reservations/lookup',
    ).replace(queryParameters: {'code': code, 'phone': phone});
    final response = await http.get(uri).timeout(const Duration(seconds: 15));
    return Reservation.fromJson(_decode(response) as Map<String, dynamic>);
  }

  Future<DepositQr> depositQr(String code, String phone) async =>
      DepositQr.fromJson(_decode(await http
          .post(
              Uri.parse(
                  '${AppConfig.apiBaseUrl}/reservations/$code/deposit/qr'),
              headers: {'Content-Type': 'application/json'},
              body: jsonEncode({'phone': phone}))
          .timeout(const Duration(seconds: 15))) as Map<String, dynamic>);

  Future<void> confirmDepositQr(String code, String phone) async {
    _decode(await http
        .post(
            Uri.parse(
                '${AppConfig.apiBaseUrl}/reservations/$code/deposit/qr/confirm'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'phone': phone}))
        .timeout(const Duration(seconds: 15)));
  }

  Future<PayPalOrder> createDepositPayPal(String code, String phone) async =>
      PayPalOrder.fromJson(_decode(await http
          .post(
              Uri.parse(
                  '${AppConfig.apiBaseUrl}/reservations/$code/deposit/paypal/orders'),
              headers: {'Content-Type': 'application/json'},
              body: jsonEncode({'phone': phone}))
          .timeout(const Duration(seconds: 30))) as Map<String, dynamic>);

  Future<void> captureDepositPayPal(
      String code, String phone, String orderId) async {
    _decode(await http
        .post(
            Uri.parse(
                '${AppConfig.apiBaseUrl}/reservations/$code/deposit/paypal/orders/capture'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'phone': phone, 'orderId': orderId}))
        .timeout(const Duration(seconds: 30)));
  }

  dynamic _decode(http.Response response) {
    dynamic data;
    try {
      data = jsonDecode(utf8.decode(response.bodyBytes));
    } catch (_) {
      data = null;
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message =
          data is Map<String, dynamic> ? data['message'] as String? : null;
      throw ApiException(
        message ?? 'Không thể kết nối máy chủ (${response.statusCode})',
      );
    }
    return data;
  }

  String _date(DateTime value) => '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';
}
