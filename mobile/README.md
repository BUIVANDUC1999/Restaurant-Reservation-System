# Restaurant Reservation Mobile

Repository Flutter độc lập cho ứng dụng khách hàng Khám Phá Việt.

## Chức năng

- Xem và làm mới thực đơn từ REST API.
- Tạo đơn đặt bàn và nhận mã đơn.
- Tra cứu đơn bằng mã đặt bàn và số điện thoại.
- Thanh toán tiền đặt cọc bằng QR hoặc PayPal Sandbox.
- Chạy trên Android và Web.

Backend/API nằm trong repository riêng `restaurant-reservation-web`.

## Chạy trên Android emulator

Khởi động backend ở cổng 8080, sau đó:

```bash
flutter pub get
flutter run
```

Ứng dụng mặc định gọi `http://10.0.2.2:8080/api/v1`, là địa chỉ host machine từ Android emulator.

## Chạy trên thiết bị thật

Thay IP dưới đây bằng IP LAN của máy chạy backend:

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080/api/v1
```

## Chạy Flutter Web

```bash
flutter run -d web-server --web-port 5174 --dart-define-from-file=config/dev.json
```

Sau khi khởi động, mobile chạy độc lập tại `http://localhost:5174`. Web React nằm ở repository khác và chạy tại `http://localhost:5173`.

## Kiểm tra

```bash
flutter analyze
flutter test
flutter build web --release --dart-define=API_BASE_URL=https://api.example.com/api/v1
```
