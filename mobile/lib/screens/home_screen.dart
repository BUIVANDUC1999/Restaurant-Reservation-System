import 'package:flutter/material.dart';

import '../api_service.dart';
import 'lookup_screen.dart';
import 'menu_screen.dart';
import 'reservation_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});
  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int index = 0;
  final api = const ApiService();

  @override
  Widget build(BuildContext context) {
    final pages = [
      WelcomeScreen(onNavigate: (value) => setState(() => index = value)),
      MenuScreen(api: api),
      ReservationScreen(api: api),
      LookupScreen(api: api),
    ];
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'KHÁM PHÁ VIỆT',
          style: TextStyle(fontWeight: FontWeight.bold, letterSpacing: 1.2),
        ),
      ),
      body: IndexedStack(index: index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: (value) => setState(() => index = value),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Trang chủ',
          ),
          NavigationDestination(
            icon: Icon(Icons.restaurant_menu),
            label: 'Thực đơn',
          ),
          NavigationDestination(
            icon: Icon(Icons.calendar_month_outlined),
            label: 'Đặt bàn',
          ),
          NavigationDestination(icon: Icon(Icons.search), label: 'Tra cứu'),
        ],
      ),
    );
  }
}

class WelcomeScreen extends StatelessWidget {
  final ValueChanged<int> onNavigate;
  const WelcomeScreen({super.key, required this.onNavigate});
  @override
  Widget build(BuildContext context) => ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Container(
            height: 270,
            padding: const EdgeInsets.all(24),
            alignment: Alignment.bottomLeft,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              gradient: const LinearGradient(
                colors: [Color(0xff173f35), Color(0xff3a765f)],
              ),
            ),
            child: const Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'ẨM THỰC GIỮA MÂY NGÀN',
                  style:
                      TextStyle(color: Color(0xffefc778), letterSpacing: 1.5),
                ),
                SizedBox(height: 10),
                Text(
                  'Hương vị Sa Pa\ntrong từng cuộc sum vầy',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: () => onNavigate(2),
            icon: const Icon(Icons.calendar_month),
            label: const Padding(
              padding: EdgeInsets.all(15),
              child: Text('Đặt bàn ngay'),
            ),
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              Expanded(
                child: _Action(
                  icon: Icons.restaurant_menu,
                  label: 'Xem thực đơn',
                  onTap: () => onNavigate(1),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _Action(
                  icon: Icons.search,
                  label: 'Tra cứu đơn',
                  onTap: () => onNavigate(3),
                ),
              ),
            ],
          ),
        ],
      );
}

class _Action extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  const _Action({required this.icon, required this.label, required this.onTap});
  @override
  Widget build(BuildContext context) => Card(
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 24),
            child: Column(
              children: [
                Icon(icon, color: const Color(0xffa6742c)),
                const SizedBox(height: 9),
                Text(label),
              ],
            ),
          ),
        ),
      );
}
