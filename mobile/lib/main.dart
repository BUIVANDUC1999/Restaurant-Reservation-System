import 'package:flutter/material.dart';

import 'screens/home_screen.dart';

void main() => runApp(const RestaurantApp());

class RestaurantApp extends StatelessWidget {
  const RestaurantApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'Khám Phá Việt',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xff173f35),
            primary: const Color(0xff173f35),
            secondary: const Color(0xffdcae5b),
          ),
          scaffoldBackgroundColor: const Color(0xfffbfaf6),
          useMaterial3: true,
          inputDecorationTheme: const InputDecorationTheme(
            border: OutlineInputBorder(),
            filled: true,
            fillColor: Colors.white,
          ),
        ),
        home: const HomeScreen(),
      );
}
