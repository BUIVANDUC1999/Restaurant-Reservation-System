import 'package:flutter/material.dart';
import '../api_service.dart';
import '../models.dart';

class LookupScreen extends StatefulWidget {
  final ApiService api;
  const LookupScreen({super.key, required this.api});
  @override
  State<LookupScreen> createState() => _LookupScreenState();
}

class _LookupScreenState extends State<LookupScreen> {
  final code = TextEditingController(), phone = TextEditingController();
  Reservation? result;
  String? error;
  bool loading = false;
  Future<void> lookup() async {
    if (code.text.trim().isEmpty || phone.text.trim().isEmpty) return;
    setState(() {
      loading = true;
      error = null;
      result = null;
    });
    try {
      final value = await widget.api.lookup(
        code.text.trim(),
        phone.text.trim(),
      );
      setState(() => result = value);
    } catch (e) {
      setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(
            'Tra cứu đơn',
            style: Theme.of(
              context,
            ).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 22),
          TextField(
            controller: code,
            textCapitalization: TextCapitalization.characters,
            decoration: const InputDecoration(labelText: 'Mã đặt bàn'),
          ),
          const SizedBox(height: 14),
          TextField(
            controller: phone,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(labelText: 'Số điện thoại'),
          ),
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: loading ? null : lookup,
            icon: const Icon(Icons.search),
            label: const Padding(
              padding: EdgeInsets.all(13),
              child: Text('Tra cứu'),
            ),
          ),
          if (error != null)
            Padding(
              padding: const EdgeInsets.only(top: 15),
              child: Text(error!, style: const TextStyle(color: Colors.red)),
            ),
          if (result != null)
            Card(
              margin: const EdgeInsets.only(top: 20),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      result!.code,
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const Divider(),
                    Text('Khách hàng: ${result!.customerName}'),
                    Text('Ngày: ${result!.reservationDate}'),
                    Text(
                      'Ca: ${result!.timeSlot == 'LUNCH' ? 'Bữa trưa' : 'Bữa tối'}',
                    ),
                    Text('Số khách: ${result!.partySize}'),
                    Text('Trạng thái: ${result!.status}'),
                    const Divider(),
                    Text(
                        'Tiền cọc: ${result!.depositAmount.toStringAsFixed(0)} ₫',
                        style: const TextStyle(fontWeight: FontWeight.bold)),
                    Text(result!.depositStatus == 'PAID'
                        ? 'Đã thanh toán bằng ${result!.depositMethod}'
                        : 'Chưa thanh toán tiền cọc'),
                  ],
                ),
              ),
            ),
        ],
      );
  @override
  void dispose() {
    code.dispose();
    phone.dispose();
    super.dispose();
  }
}
