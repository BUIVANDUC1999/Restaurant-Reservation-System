import 'package:flutter/material.dart';
import '../api_service.dart';

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
  String? result, error;

  Future<void> submit() async {
    if (!formKey.currentState!.validate()) return;
    setState(() {
      loading = true;
      error = null;
      result = null;
    });
    try {
      final booking = await widget.api.createReservation(
        customerName: name.text.trim(),
        phone: phone.text.trim(),
        email: email.text.trim(),
        date: date,
        timeSlot: slot,
        partySize: guests,
      );
      setState(() => result = booking.code);
    } catch (e) {
      setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Form(
    key: formKey,
    child: ListView(
      padding: const EdgeInsets.all(20),
      children: [
        Text(
          'Đặt bàn',
          style: Theme.of(
            context,
          ).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        const Text('Chọn ngày, ca phục vụ và số khách.'),
        const SizedBox(height: 22),
        TextFormField(
          controller: name,
          decoration: const InputDecoration(labelText: 'Họ và tên'),
          validator: required,
        ),
        const SizedBox(height: 14),
        TextFormField(
          controller: phone,
          keyboardType: TextInputType.phone,
          decoration: const InputDecoration(labelText: 'Số điện thoại'),
          validator: (v) => RegExp(r'^[0-9+ ]{9,15}$').hasMatch(v ?? '')
              ? null
              : 'Số điện thoại chưa hợp lệ',
        ),
        const SizedBox(height: 14),
        TextFormField(
          controller: email,
          keyboardType: TextInputType.emailAddress,
          decoration: const InputDecoration(
            labelText: 'Email (không bắt buộc)',
          ),
        ),
        const SizedBox(height: 14),
        OutlinedButton.icon(
          icon: const Icon(Icons.calendar_month),
          label: Text('${date.day}/${date.month}/${date.year}'),
          onPressed: () async {
            final picked = await showDatePicker(
              context: context,
              initialDate: date,
              firstDate: DateTime.now(),
              lastDate: DateTime.now().add(const Duration(days: 365)),
            );
            if (picked != null) setState(() => date = picked);
          },
        ),
        const SizedBox(height: 14),
        DropdownButtonFormField(
          initialValue: slot,
          decoration: const InputDecoration(labelText: 'Ca phục vụ'),
          items: const [
            DropdownMenuItem(value: 'LUNCH', child: Text('Bữa trưa')),
            DropdownMenuItem(value: 'DINNER', child: Text('Bữa tối')),
          ],
          onChanged: (value) => setState(() => slot = value!),
        ),
        const SizedBox(height: 14),
        DropdownButtonFormField(
          initialValue: guests,
          decoration: const InputDecoration(labelText: 'Số khách'),
          items: List.generate(
            20,
            (i) =>
                DropdownMenuItem(value: i + 1, child: Text('${i + 1} khách')),
          ),
          onChanged: (value) => setState(() => guests = value!),
        ),
        if (error != null)
          Padding(
            padding: const EdgeInsets.only(top: 14),
            child: Text(error!, style: const TextStyle(color: Colors.red)),
          ),
        if (result != null)
          Card(
            color: const Color(0xffe2f2e9),
            margin: const EdgeInsets.only(top: 14),
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                children: [
                  const Text('Đặt bàn thành công'),
                  Text(
                    result!,
                    style: const TextStyle(
                      fontSize: 25,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
          ),
        const SizedBox(height: 20),
        FilledButton(
          onPressed: loading ? null : submit,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: loading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Xác nhận đặt bàn'),
          ),
        ),
      ],
    ),
  );

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
