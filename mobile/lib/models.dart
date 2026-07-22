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
  final int partySize;
  final String status;

  const Reservation({
    required this.code,
    required this.customerName,
    required this.phone,
    required this.reservationDate,
    required this.timeSlot,
    required this.partySize,
    required this.status,
  });

  factory Reservation.fromJson(Map<String, dynamic> json) => Reservation(
    code: json['code'] as String,
    customerName: json['customerName'] as String,
    phone: json['phone'] as String,
    reservationDate: json['reservationDate'] as String,
    timeSlot: json['timeSlot'] as String,
    partySize: json['partySize'] as int,
    status: json['status'] as String,
  );
}
