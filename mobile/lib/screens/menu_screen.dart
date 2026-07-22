import 'package:flutter/material.dart';
import '../api_service.dart';
import '../models.dart';

class MenuScreen extends StatefulWidget {
  final ApiService api;
  const MenuScreen({super.key, required this.api});
  @override
  State<MenuScreen> createState() => _MenuScreenState();
}

class _MenuScreenState extends State<MenuScreen> {
  late Future<List<MenuItem>> items = widget.api.menu();
  @override
  Widget build(BuildContext context) => RefreshIndicator(
    onRefresh: () async {
      setState(() => items = widget.api.menu());
      await items;
    },
    child: FutureBuilder<List<MenuItem>>(
      future: items,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return ListView(
            children: [
              const SizedBox(height: 180),
              Center(child: Text('${snapshot.error}')),
            ],
          );
        }
        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: snapshot.data!.length,
          itemBuilder: (context, index) {
            final item = snapshot.data![index];
            return Card(
              margin: const EdgeInsets.only(bottom: 14),
              clipBehavior: Clip.antiAlias,
              child: Row(
                children: [
                  SizedBox(
                    width: 112,
                    height: 112,
                    child: item.imageUrl.isEmpty
                        ? const Icon(Icons.restaurant)
                        : Image.network(
                            item.imageUrl,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) =>
                                const Icon(Icons.restaurant),
                          ),
                  ),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.all(13),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.category.toUpperCase(),
                            style: const TextStyle(
                              fontSize: 10,
                              color: Color(0xffa6742c),
                            ),
                          ),
                          const SizedBox(height: 5),
                          Text(
                            item.name,
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            '${item.price.toStringAsFixed(0)} ₫',
                            style: const TextStyle(color: Color(0xff173f35)),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    ),
  );
}
