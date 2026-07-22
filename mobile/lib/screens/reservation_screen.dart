import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../api_service.dart';
import '../models.dart';

class ReservationScreen extends StatefulWidget {
  final ApiService api;
  const ReservationScreen({super.key, required this.api});
  @override
  State<ReservationScreen> createState() => _ReservationScreenState();
}

class _ReservationScreenState extends State<ReservationScreen> {
  final formKey = GlobalKey<FormState>();
  final name = TextEditingController(),
      phone = TextEditingController(),
      email = TextEditingController();
  DateTime date = DateTime.now().add(const Duration(days: 1));
  String slot = 'DINNER';
  int guests = 2;
  bool loading = false;
  Reservation? booking;
  DepositQr? qr;
  String? error, paypalOrder;

  Future<void> submit() async {
    if (!formKey.currentState!.validate()) return;
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final value = await widget.api.createReservation(
          customerName: name.text.trim(),
          phone: phone.text.trim(),
          email: email.text.trim(),
          date: date,
          timeSlot: slot,
          partySize: guests);
      final qrValue = await widget.api.depositQr(value.code, value.phone);
      setState(() {
        booking = value;
        qr = qrValue;
      });
    } catch (e) {
      setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Reservation paid(String method) => Reservation(
      code: booking!.code,
      customerName: booking!.customerName,
      phone: booking!.phone,
      reservationDate: booking!.reservationDate,
      timeSlot: booking!.timeSlot,
      partySize: booking!.partySize,
      status: booking!.status,
      depositAmount: booking!.depositAmount,
      depositStatus: 'PAID',
      depositMethod: method);
  Future<void> confirmQr() async {
    try {
      await widget.api.confirmDepositQr(booking!.code, booking!.phone);
      setState(() => booking = paid('QR'));
    } catch (e) {
      setState(() => error = e.toString());
    }
  }

  Future<void> startPayPal() async {
    try {
      final order =
          await widget.api.createDepositPayPal(booking!.code, booking!.phone);
      setState(() => paypalOrder = order.orderId);
      if (order.approvalUrl.isNotEmpty) {
        await launchUrl(Uri.parse(order.approvalUrl),
            mode: LaunchMode.externalApplication);
      }
    } catch (e) {
      setState(() => error = e.toString());
    }
  }

  Future<void> finishPayPal() async {
    try {
      await widget.api
          .captureDepositPayPal(booking!.code, booking!.phone, paypalOrder!);
      setState(() => booking = paid('PAYPAL'));
    } catch (e) {
      setState(() => error = e.toString());
    }
  }

  Widget payment() {
    if (booking!.depositStatus == 'PAID') {
      return const Card(
          color: Color(0xffe2f2e9),
          child: Padding(
              padding: EdgeInsets.all(18),
              child: Text('✓ Đã thanh toán tiền cọc')));
    }
    return Column(children: [
      if (qr?.enabled == true)
        Card(
            child: Padding(
                padding: const EdgeInsets.all(15),
                child: Column(children: [
                  Image.network(qr!.imageUrl),
                  Text('${qr!.bankId} • ${qr!.accountNo}'),
                  Text(qr!.transferContent),
                  FilledButton(
                      onPressed: confirmQr,
                      child: const Text('Tôi đã chuyển khoản QR'))
                ]))),
      if (qr?.enabled != true)
        const Text('QR chưa được cấu hình trên backend.',
            style: TextStyle(color: Colors.orange)),
      const SizedBox(height: 12),
      FilledButton(
          onPressed: startPayPal, child: const Text('Thanh toán bằng PayPal')),
      if (paypalOrder != null)
        OutlinedButton(
            onPressed: finishPayPal,
            child: const Text('Tôi đã duyệt PayPal – Hoàn tất')),
    ]);
  }

  @override
  Widget build(BuildContext context) {
    if (booking != null) {
      return ListView(padding: const EdgeInsets.all(20), children: [
        const Icon(Icons.check_circle, color: Colors.green, size: 58),
        const Center(child: Text('Đặt bàn thành công')),
        Center(
            child: Text(booking!.code,
                style: const TextStyle(
                    fontSize: 28, fontWeight: FontWeight.bold))),
        const SizedBox(height: 18),
        Card(
            color: const Color(0xff173f35),
            child: Padding(
                padding: const EdgeInsets.all(18),
                child: Column(children: [
                  const Text('TIỀN ĐẶT CỌC',
                      style: TextStyle(color: Colors.white)),
                  Text('${booking!.depositAmount.toStringAsFixed(0)} ₫',
                      style: const TextStyle(
                          color: Color(0xffefc778),
                          fontSize: 27,
                          fontWeight: FontWeight.bold)),
                  Text('200.000 ₫ × ${booking!.partySize} khách',
                      style: const TextStyle(color: Colors.white70))
                ]))),
        payment(),
        if (error != null)
          Text(error!, style: const TextStyle(color: Colors.red)),
      ]);
    }
    return Form(
        key: formKey,
        child: ListView(padding: const EdgeInsets.all(20), children: [
          Text('Đặt bàn',
              style: Theme.of(context)
                  .textTheme
                  .headlineMedium
                  ?.copyWith(fontWeight: FontWeight.bold)),
          const Text('Không chọn món trước: cọc 200.000 ₫ cho mỗi khách.'),
          const SizedBox(height: 18),
          TextFormField(
              controller: name,
              decoration: const InputDecoration(labelText: 'Họ và tên'),
              validator: required),
          const SizedBox(height: 12),
          TextFormField(
              controller: phone,
              decoration: const InputDecoration(labelText: 'Số điện thoại'),
              validator: (v) => RegExp(r'^[0-9+ ]{9,15}$').hasMatch(v ?? '')
                  ? null
                  : 'Số điện thoại chưa hợp lệ'),
          const SizedBox(height: 12),
          TextFormField(
              controller: email,
              decoration: const InputDecoration(labelText: 'Email')),
          const SizedBox(height: 12),
          OutlinedButton.icon(
              onPressed: () async {
                final value = await showDatePicker(
                    context: context,
                    initialDate: date,
                    firstDate: DateTime.now(),
                    lastDate: DateTime.now().add(const Duration(days: 365)));
                if (value != null) setState(() => date = value);
              },
              icon: const Icon(Icons.calendar_month),
              label: Text('${date.day}/${date.month}/${date.year}')),
          const SizedBox(height: 12),
          DropdownButtonFormField(
              initialValue: slot,
              decoration: const InputDecoration(labelText: 'Ca phục vụ'),
              items: const [
                DropdownMenuItem(value: 'LUNCH', child: Text('Bữa trưa')),
                DropdownMenuItem(value: 'DINNER', child: Text('Bữa tối'))
              ],
              onChanged: (v) => setState(() => slot = v!)),
          const SizedBox(height: 12),
          DropdownButtonFormField(
              initialValue: guests,
              decoration: const InputDecoration(labelText: 'Số khách'),
              items: List.generate(
                  20,
                  (i) => DropdownMenuItem(
                      value: i + 1, child: Text('${i + 1} khách'))),
              onChanged: (v) => setState(() => guests = v!)),
          Card(
              margin: const EdgeInsets.only(top: 16),
              child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text('Tiền cọc dự kiến: ${200000 * guests} ₫'))),
          if (error != null)
            Text(error!, style: const TextStyle(color: Colors.red)),
          const SizedBox(height: 16),
          FilledButton(
              onPressed: loading ? null : submit,
              child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Text(
                      loading ? 'Đang gửi...' : 'Đặt bàn và thanh toán cọc'))),
        ]));
  }

  String? required(String? value) =>
      value == null || value.trim().isEmpty ? 'Vui lòng nhập thông tin' : null;
  @override
  void dispose() {
    name.dispose();
    phone.dispose();
    email.dispose();
    super.dispose();
  }
}
