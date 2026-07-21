import 'package:flutter/material.dart';

void main() => runApp(const RestaurantApp());

class RestaurantApp extends StatelessWidget {
  const RestaurantApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'Khám Phá Việt',
    theme: ThemeData(colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff173f35)), useMaterial3: true),
    home: const HomeScreen(),
  );
}

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('KHÁM PHÁ VIỆT'), actions: [IconButton(onPressed: () {}, icon: const Icon(Icons.notifications_none))]),
    body: ListView(padding: const EdgeInsets.all(20), children: [
      ClipRRect(borderRadius: BorderRadius.circular(24), child: Container(
        height: 260, padding: const EdgeInsets.all(24), alignment: Alignment.bottomLeft,
        decoration: const BoxDecoration(gradient: LinearGradient(colors: [Color(0xff173f35), Color(0xff3a765f)])),
        child: const Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('ẨM THỰC GIỮA MÂY NGÀN', style: TextStyle(color: Color(0xffefc778), letterSpacing: 1.5)),
          SizedBox(height: 10), Text('Hương vị Sa Pa\ntrong từng cuộc sum vầy', style: TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold)),
        ]),
      )),
      const SizedBox(height: 24),
      FilledButton.icon(onPressed: () {}, icon: const Icon(Icons.calendar_month), label: const Padding(padding: EdgeInsets.all(14), child: Text('Đặt bàn ngay'))),
      const SizedBox(height: 28), const Text('Khám phá', style: TextStyle(fontSize: 23, fontWeight: FontWeight.bold)),
      const SizedBox(height: 12),
      const Row(children: [Expanded(child: _Action(icon: Icons.restaurant_menu, label: 'Thực đơn')), SizedBox(width: 12), Expanded(child: _Action(icon: Icons.search, label: 'Tra cứu đơn'))]),
    ]),
    bottomNavigationBar: NavigationBar(destinations: const [NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Trang chủ'), NavigationDestination(icon: Icon(Icons.calendar_month_outlined), label: 'Đặt bàn'), NavigationDestination(icon: Icon(Icons.person_outline), label: 'Tài khoản')]),
  );
}

class _Action extends StatelessWidget {
  final IconData icon; final String label;
  const _Action({required this.icon, required this.label});
  @override Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.symmetric(vertical: 24), child: Column(children: [Icon(icon, color: const Color(0xffa6742c)), const SizedBox(height: 9), Text(label)])));
}

