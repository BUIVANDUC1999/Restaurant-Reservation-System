class MenuItem {
  final int id;
  final String name;
  final String category;
  final double price;
  final String description;
  final String imageUrl;
  final bool featured;

  const MenuItem({
    required this.id,
    required this.name,
    required this.category,
    required this.price,
    required this.description,
    required this.imageUrl,
    required this.featured,
  });

  factory MenuItem.fromJson(Map<String, dynamic> json) => MenuItem(
        id: json['id'] as int,
        name: json['name'] as String,
        category: json['category'] as String,
        price: (json['price'] as num).toDouble(),
        description: json['description'] as String? ?? '',
        imageUrl: json['imageUrl'] as String? ?? '',
        featured: json['featured'] as bool? ?? false,
      );
}

class Reservation {
  final String code;
  final String customerName;
  final String phone;
  final String reservationDate;
  final String timeSlot;
  final String reservationTime;
  final int durationMinutes;
  final List<String> tableCodes;
  final int partySize;
  final String status;
  final double depositAmount;
  final String depositStatus;
  final String? depositMethod;

  const Reservation({
    required this.code,
    required this.customerName,
    required this.phone,
    required this.reservationDate,
    required this.timeSlot,
    required this.reservationTime,
    required this.durationMinutes,
    required this.tableCodes,
    required this.partySize,
    required this.status,
    required this.depositAmount,
    required this.depositStatus,
    this.depositMethod,
  });

  factory Reservation.fromJson(Map<String, dynamic> json) => Reservation(
        code: json['code'] as String,
        customerName: json['customerName'] as String,
        phone: json['phone'] as String,
        reservationDate: json['reservationDate'] as String,
        timeSlot: json['timeSlot'] as String,
        reservationTime: json['reservationTime'] as String? ??
            (json['timeSlot'] == 'LUNCH' ? '11:00' : '17:30'),
        durationMinutes: json['durationMinutes'] as int? ?? 120,
        tableCodes: ((json['assignedTables'] as List?) ?? const [])
            .map((e) => (e as Map<String, dynamic>)['code'] as String)
            .toList(),
        partySize: json['partySize'] as int,
        status: json['status'] as String,
        depositAmount: (json['depositAmount'] as num).toDouble(),
        depositStatus: json['depositStatus'] as String,
        depositMethod: json['depositMethod'] as String?,
      );
}

class AvailableTable {
  final int id, seats, layoutX, layoutY;
  final String code, name, area, shape;
  const AvailableTable(
      {required this.id,
      required this.seats,
      required this.layoutX,
      required this.layoutY,
      required this.code,
      required this.name,
      required this.area,
      required this.shape});
  factory AvailableTable.fromJson(Map<String, dynamic> json) => AvailableTable(
      id: json['id'] as int,
      seats: json['seats'] as int,
      layoutX: json['layoutX'] as int,
      layoutY: json['layoutY'] as int,
      code: json['code'] as String,
      name: json['name'] as String,
      area: json['area'] as String,
      shape: json['shape'] as String);
}

class DepositQr {
  final bool enabled;
  final String imageUrl, bankId, accountNo, transferContent;
  final double amount;
  const DepositQr(
      {required this.enabled,
      required this.imageUrl,
      required this.bankId,
      required this.accountNo,
      required this.transferContent,
      required this.amount});
  factory DepositQr.fromJson(Map<String, dynamic> json) => DepositQr(
      enabled: json['enabled'] as bool,
      imageUrl: json['imageUrl'] as String,
      bankId: json['bankId'] as String,
      accountNo: json['accountNo'] as String,
      transferContent: json['transferContent'] as String,
      amount: (json['amount'] as num).toDouble());
}

class PayPalOrder {
  final String orderId, approvalUrl;
  const PayPalOrder(this.orderId, this.approvalUrl);
  factory PayPalOrder.fromJson(Map<String, dynamic> json) => PayPalOrder(
      json['orderId'] as String, json['approvalUrl'] as String? ?? '');
}
