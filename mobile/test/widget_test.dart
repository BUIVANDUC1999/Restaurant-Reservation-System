import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kham_pha_viet_mobile/screens/home_screen.dart';

void main() {
  testWidgets('home screen exposes primary customer actions', (tester) async {
    var selectedPage = -1;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: WelcomeScreen(onNavigate: (value) => selectedPage = value),
      ),
    ));

    expect(find.text('Đặt bàn ngay'), findsOneWidget);
    expect(find.text('Xem thực đơn'), findsOneWidget);
    expect(find.text('Tra cứu đơn'), findsOneWidget);

    await tester.tap(find.text('Đặt bàn ngay'));
    expect(selectedPage, 2);
  });
}
