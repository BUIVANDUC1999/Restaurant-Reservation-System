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
  TimeOfDay time = const TimeOfDay(hour: 18, minute: 0);
  int guests = 2, duration = 120;
  bool loading = false, notifyEmail = true, notifySms = true;
  String? error, paypalOrder;
  Reservation? booking;
  DepositQr? qr;
  List<AvailableTable> tables = [];
  Set<int> selected = {};
  String get timeText =>
      '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
  int get capacity => tables
      .where((t) => selected.contains(t.id))
      .fold(0, (sum, t) => sum + t.seats);
  @override
  void initState() {
    super.initState();
    loadTables();
  }

  Future<void> loadTables() async {
    try {
      final value =
          await widget.api.availableTables(date, timeText, duration, guests);
      if (mounted) {
        setState(() {
          tables = value;
          selected.removeWhere((id) => !value.any((t) => t.id == id));
        });
      }
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    }
  }

  Future<void> submit() async {
    if (!formKey.currentState!.validate()) return;
    if (capacity < guests) {
      setState(() => error = 'Hãy chọn bàn có đủ ghế');
      return;
    }
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
          timeSlot: time.hour < 15 ? 'LUNCH' : 'DINNER',
          reservationTime: timeText,
          durationMinutes: duration,
          partySize: guests,
          selectedTableIds: selected.toList(),
          notifyEmail: notifyEmail,
          notifySms: notifySms);
      final q = await widget.api.depositQr(value.code, value.phone);
      setState(() {
        booking = value;
        qr = q;
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
      reservationTime: booking!.reservationTime,
      durationMinutes: booking!.durationMinutes,
      tableCodes: booking!.tableCodes,
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
        Card(
            child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(children: [
                  Text(
                      '${booking!.reservationDate} lúc ${booking!.reservationTime}'),
                  Text(
                      '${booking!.durationMinutes} phút · ${booking!.tableCodes.join(', ')}'),
                  Text(
                      'Tiền cọc ${booking!.depositAmount.toStringAsFixed(0)} ₫')
                ]))),
        if (booking!.depositStatus == 'PAID')
          const Card(
              color: Color(0xffe2f2e9),
              child: Padding(
                  padding: EdgeInsets.all(18),
                  child: Text('✓ Đã thanh toán tiền cọc')))
        else ...[
          if (qr?.enabled == true)
            Card(
                child: Column(children: [
              Image.network(qr!.imageUrl),
              Text(qr!.transferContent),
              FilledButton(
                  onPressed: confirmQr,
                  child: const Text('Tôi đã chuyển khoản QR'))
            ])),
          FilledButton(
              onPressed: startPayPal,
              child: const Text('Thanh toán bằng PayPal Sandbox')),
          if (paypalOrder != null)
            OutlinedButton(
                onPressed: finishPayPal, child: const Text('Hoàn tất PayPal'))
        ],
        if (error != null)
          Text(error!, style: const TextStyle(color: Colors.red))
      ]);
    }
    return Form(
        key: formKey,
        child: ListView(padding: const EdgeInsets.all(20), children: [
          Text('Đặt bàn theo giờ',
              style: Theme.of(context)
                  .textTheme
                  .headlineMedium
                  ?.copyWith(fontWeight: FontWeight.bold)),
          const Text(
              'Giữ bàn 10 phút · cộng 15 phút dọn bàn để tránh trùng lịch'),
          const SizedBox(height: 16),
          TextFormField(
              controller: name,
              decoration: const InputDecoration(labelText: 'Họ và tên'),
              validator: required),
          const SizedBox(height: 10),
          TextFormField(
              controller: phone,
              decoration: const InputDecoration(labelText: 'Số điện thoại'),
              validator: (v) => RegExp(r'^[0-9+ ]{9,15}$').hasMatch(v ?? '')
                  ? null
                  : 'Số điện thoại chưa hợp lệ'),
          const SizedBox(height: 10),
          TextFormField(
              controller: email,
              decoration: const InputDecoration(labelText: 'Email')),
          const SizedBox(height: 10),
          Wrap(spacing: 10, children: [
            OutlinedButton.icon(
                onPressed: () async {
                  final v = await showDatePicker(
                      context: context,
                      initialDate: date,
                      firstDate: DateTime.now(),
                      lastDate: DateTime.now().add(const Duration(days: 365)));
                  if (v != null) {
                    setState(() => date = v);
                    await loadTables();
                  }
                },
                icon: const Icon(Icons.calendar_month),
                label: Text('${date.day}/${date.month}/${date.year}')),
            OutlinedButton.icon(
                onPressed: () async {
                  final v =
                      await showTimePicker(context: context, initialTime: time);
                  if (v != null) {
                    setState(() => time = v);
                    await loadTables();
                  }
                },
                icon: const Icon(Icons.schedule),
                label: Text(timeText))
          ]),
          const SizedBox(height: 10),
          DropdownButtonFormField(
              initialValue: duration,
              decoration:
                  const InputDecoration(labelText: 'Thời lượng dùng bàn'),
              items: [90, 120, 150, 180]
                  .map(
                      (v) => DropdownMenuItem(value: v, child: Text('$v phút')))
                  .toList(),
              onChanged: (v) async {
                setState(() => duration = v!);
                await loadTables();
              }),
          const SizedBox(height: 10),
          DropdownButtonFormField(
              initialValue: guests,
              decoration: const InputDecoration(labelText: 'Số khách'),
              items: List.generate(
                  20,
                  (i) => DropdownMenuItem(
                      value: i + 1, child: Text('${i + 1} khách'))),
              onChanged: (v) async {
                setState(() => guests = v!);
                await loadTables();
              }),
          const SizedBox(height: 16),
          Text('Chọn bàn · $capacity/$guests ghế',
              style: const TextStyle(fontWeight: FontWeight.bold)),
          Wrap(
              spacing: 8,
              runSpacing: 8,
              children: tables
                  .map((t) => FilterChip(
                      selected: selected.contains(t.id),
                      label: Text('${t.code} · ${t.seats} ghế'),
                      onSelected: (_) => setState(() => selected.contains(t.id)
                          ? selected.remove(t.id)
                          : selected.add(t.id))))
                  .toList()),
          SwitchListTile(
              value: notifySms,
              onChanged: (v) => setState(() => notifySms = v),
              title: const Text('Nhắc lịch qua SMS Sandbox')),
          SwitchListTile(
              value: notifyEmail && email.text.isNotEmpty,
              onChanged: email.text.isEmpty
                  ? null
                  : (v) => setState(() => notifyEmail = v),
              title: const Text('Nhắc lịch qua Gmail/email')),
          Card(
              child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text('Tiền cọc dự kiến: ${200000 * guests} ₫'))),
          if (error != null)
            Text(error!, style: const TextStyle(color: Colors.red)),
          FilledButton(
              onPressed: loading ? null : submit,
              child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Text(loading
                      ? 'Đang giữ bàn...'
                      : 'Giữ bàn và thanh toán cọc')))
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
