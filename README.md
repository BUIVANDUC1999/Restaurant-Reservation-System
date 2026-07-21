# Restaurant Reservation System

Bộ khung đồ án đặt bàn và vận hành nhà hàng, lấy cảm hứng từ không gian và thực đơn đặc sản Sa Pa của Nhà hàng Khám Phá Việt.

## Công nghệ

- Backend: Java 21, Spring Boot 3, Spring Data JPA, Flyway, PostgreSQL
- Frontend: React 19, TypeScript, Vite
- Mobile: Flutter (khung ứng dụng khách hàng)
- Hạ tầng: Docker Compose

## Chức năng đã có trong starter

- Trang chủ responsive mang phong cách ẩm thực Tây Bắc.
- Danh sách món nổi bật lấy từ API.
- Form đặt bàn với kiểm tra ngày, ca ăn và số lượng khách.
- Sinh mã đặt bàn và tra cứu đơn.
- Dashboard nhân viên xem, lọc và cập nhật trạng thái đặt bàn.
- API quản lý menu, kiểm tra sức chứa và đặt bàn.
- PostgreSQL migration và dữ liệu mẫu cho 2 tầng (120/180 ghế).
- Swagger UI, health check và xử lý lỗi thống nhất.

## Chạy nhanh bằng Docker

```bash
docker compose up --build
```

- Web: http://localhost:5173
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html

## Chạy khi phát triển

```bash
cd backend
mvn spring-boot:run

cd frontend
npm install
npm run dev
```

PostgreSQL mặc định: `localhost:5432/restaurant`, tài khoản `restaurant`, mật khẩu `restaurant`.

### Chạy demo không cần PostgreSQL

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Chế độ `demo` sử dụng H2 trong bộ nhớ và tự nạp dữ liệu món mẫu.

## Cấu trúc

```text
backend/   Spring Boot REST API
frontend/  React customer web + staff dashboard
mobile/    Flutter customer starter
docs/      kế hoạch và API mẫu
```

## API chính

- `GET /api/v1/menu/items`
- `GET /api/v1/reservations/availability`
- `POST /api/v1/reservations`
- `GET /api/v1/reservations/lookup?code=...&phone=...`
- `GET /api/v1/staff/reservations`
- `PATCH /api/v1/staff/reservations/{id}/status`

> Đây là bộ khung học tập. Trước khi dùng thực tế cần bổ sung xác thực JWT/RBAC, thanh toán, notification, audit log và kiểm thử tải.
