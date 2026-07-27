import 'package:flutter_test/flutter_test.dart';

import 'package:asterion/main.dart';

void main() {
  testWidgets('AsterionApp builds without throwing', (WidgetTester tester) async {
    await tester.pumpWidget(const AsterionApp());
    await tester.pump();
  });
}
