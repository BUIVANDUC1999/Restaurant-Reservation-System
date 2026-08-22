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

### Chạy bằng Docker Compose

1. Sao chép `.env.example` thành `.env`.
2. Điền tối thiểu `DB_PASSWORD`, `JWT_SECRET` (chuỗi ngẫu nhiên tối thiểu 32 byte) và `ALLOWED_ORIGINS` (domain HTTPS của web).
3. Điền PayPal Sandbox và Gmail App Password nếu dùng các chức năng tương ứng.
4. Chạy:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

File production tự bật profile `prod`, tắt công cụ tạo tình huống demo, không công khai cổng PostgreSQL và giữ dữ liệu trong volume `postgres_prod_data`. Container frontend chuyển tiếp `/api/` sang backend và đã tắt buffering cho SSE realtime.

Docker Compose chỉ mở HTTP ở `WEB_PORT` (mặc định `80`). Khi đưa lên Internet phải đặt phía sau HTTPS của Render/Railway hoặc reverse proxy như Caddy/Nginx có chứng chỉ TLS. Không dùng URL HTTP cho PayPal hoặc bản Android release.

## Mobile (repository riêng)

Android emulator:

```bash
cd restaurant-reservation-mobile
flutter run
```

Thiết bị thật cần địa chỉ IP LAN của máy chạy backend:

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080/api/v1
```

Build Flutter Web để deploy tĩnh phải truyền API HTTPS thật:

```bash
flutter build web --release --dart-define=API_BASE_URL=https://api.your-domain.example/api/v1
```

Thư mục triển khai là `build/web`. Bản Android đưa lên Play Store cần keystore release riêng trong `android/key.properties`; không commit keystore hoặc mật khẩu ký ứng dụng.
