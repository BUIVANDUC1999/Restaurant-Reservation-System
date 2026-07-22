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
    required int partySize,
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
            'partySize': partySize,
            'preOrderItems': <Object>[],
          }),
        )
        .timeout(const Duration(seconds: 15));
    return Reservation.fromJson(_decode(response) as Map<String, dynamic>);
  }

  Future<Reservation> lookup(String code, String phone) async {
    final uri = Uri.parse(
      '${AppConfig.apiBaseUrl}/reservations/lookup',
    ).replace(queryParameters: {'code': code, 'phone': phone});
    final response = await http.get(uri).timeout(const Duration(seconds: 15));
    return Reservation.fromJson(_decode(response) as Map<String, dynamic>);
  }

  dynamic _decode(http.Response response) {
    dynamic data;
    try {
      data = jsonDecode(utf8.decode(response.bodyBytes));
    } catch (_) {
      data = null;
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = data is Map<String, dynamic>
          ? data['message'] as String?
          : null;
      throw ApiException(
        message ?? 'Không thể kết nối máy chủ (${response.statusCode})',
      );
    }
    return data;
  }

  String _date(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';
}
