# Triển khai

## Development/demo

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo

cd frontend
npm ci
npm run dev
```

## Production

Không dùng tài khoản demo hoặc secret mặc định. Khởi động backend với profile `prod` và truyền:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET` ngẫu nhiên, tối thiểu 32 byte
- `ALLOWED_ORIGINS` là domain frontend thực tế
- `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET` nếu dùng PayPal

Swagger bị tắt trong profile production. Nên đặt ứng dụng sau HTTPS reverse proxy, giới hạn quyền database, sao lưu PostgreSQL hằng ngày và kiểm tra phục hồi định kỳ.

## Mobile

Android emulator:

```bash
cd mobile
flutter run
```

Thiết bị thật cần địa chỉ IP LAN của máy chạy backend:

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080/api/v1
```
