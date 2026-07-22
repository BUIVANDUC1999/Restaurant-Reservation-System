# Restaurant Reservation Web

Repository độc lập cho hệ thống web đặt bàn và vận hành nhà hàng Khám Phá Việt.

## Thành phần

- `backend/`: Java 21, Spring Boot, PostgreSQL, Flyway, JWT/RBAC và REST API.
- `frontend/`: React 19, TypeScript và Vite.
- `docs/`: kiến trúc, kế hoạch kiểm thử, triển khai và ví dụ API.
- `docker-compose.yml`: PostgreSQL, backend và frontend.

Ứng dụng Flutter được duy trì trong repository riêng `restaurant-reservation-mobile` và sử dụng REST API của repository này.

## Chạy nhanh bằng Docker

```bash
docker compose up --build
```

- Web: http://localhost:5173
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html

## Chạy development không cần PostgreSQL

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo

cd frontend
npm ci
npm run dev
```

Tài khoản Admin demo: `admin@khamphaviet.vn` / `Admin@123`.

## Đặt cọc

- Có món đặt trước: cọc 10% tổng tiền món.
- Không đặt món trước: cọc 200.000 ₫ cho mỗi khách.
- PayPal sử dụng thông tin Sandbox trong `.env`.
- QR sử dụng `QR_BANK_ID`, `QR_ACCOUNT_NO`, `QR_ACCOUNT_NAME`. Chức năng xác nhận QR hiện là luồng demo; production cần webhook ngân hàng hoặc cổng thanh toán để tự động đối soát.

## Kiểm tra

```bash
cd backend && mvn test
cd frontend && npm ci && npm run lint && npm run build
```

Xem thêm [kiến trúc](docs/ARCHITECTURE.md), [kế hoạch kiểm thử](docs/TEST_PLAN.md) và [hướng dẫn triển khai](docs/DEPLOYMENT.md).
