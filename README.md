# Restaurant Reservation System

Bộ khung đồ án đặt bàn và vận hành nhà hàng, lấy cảm hứng từ không gian và thực đơn đặc sản Sa Pa của Nhà hàng Khám Phá Việt.

## Công nghệ

- Backend: Java 21, Spring Boot 3, Spring Data JPA, Flyway, PostgreSQL
- Frontend: React 19, TypeScript, Vite
- Mobile: Flutter (khung ứng dụng khách hàng)
- Hạ tầng: Docker Compose

## Chức năng đã có trong starter

- Trang chủ responsive mang phong cách ẩm thực Tây Bắc.
- Banner thương hiệu “Hương vị quê hương”.
- Danh sách món nổi bật lấy từ API.
- Thực đơn 30 món Sa Pa/Tây Bắc với tìm kiếm và lọc danh mục; đã thay nhóm thực đơn đoàn bằng các món gọi lẻ.
- Admin và nhân viên quản lý thực đơn: thêm món, sửa tên/danh mục/giá/mô tả/ảnh, đánh dấu nổi bật và bật–tắt phục vụ.
- Form đặt bàn với kiểm tra ngày, ca ăn và số lượng khách.
- Khách chọn số lượng món trước; nhân viên xem và xác nhận lại trên dashboard.
- Khi check-in, các món chọn trước đã xác nhận tự động trở thành phiếu bếp có nhãn “Món đặt trước”.
- Hệ thống chặn check-in nếu yêu cầu món chọn trước vẫn chưa được nhân viên xác nhận.
- Sinh mã đặt bàn và tra cứu đơn.
- Dashboard nhân viên xem, lọc và cập nhật trạng thái đặt bàn.
- Dashboard Admin riêng, thống kê tổng tài khoản, nhân viên, khách hàng và trạng thái hoạt động.
- Danh sách tài khoản có tìm kiếm, lọc vai trò và không làm lộ mật khẩu.
- Đăng nhập JWT/RBAC và bảo vệ API nhân viên.
- Khách hàng tự đăng ký tài khoản bằng họ tên, số điện thoại, email và mật khẩu.
- Sơ đồ 22 bàn mẫu theo hai tầng, cập nhật trạng thái trực tiếp.
- Tra cứu bàn theo mã, tên hoặc khu vực; hiển thị khách đang ngồi, số khách và phiếu món cần phục vụ.
- Phân biệt bàn đang dùng bữa, chờ bếp, cần mang món, cần dọn và bàn trống; tự động làm mới mỗi 10 giây.
- Xếp một hoặc nhiều bàn theo sức chứa, check-in và mở phiên phục vụ.
- Hoàn tất lượt khách và chuyển bàn sang trạng thái cần dọn.
- Nhân viên tạo nhiều phiếu gọi món cho bàn đang phục vụ, tìm kiếm trong 30 món và gửi ghi chú cho bếp.
- Màn hình bếp riêng cập nhật phiếu theo luồng mới gửi → đang chế biến → sẵn sàng.
- Nhân viên xác nhận đã mang món; hệ thống chặn kết thúc lượt khách khi còn phiếu món đang mở.
- Nhân viên lập hóa đơn theo món đã phục vụ, áp dụng giảm giá và thanh toán bằng tiền mặt, chuyển khoản, QR, thẻ hoặc PayPal Sandbox.
- Hệ thống chỉ cho hoàn tất lượt khách sau khi mọi phiếu món đã phục vụ hoặc hủy và hóa đơn đã thanh toán.
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

### Cấu hình PayPal Sandbox

Tạo ứng dụng Sandbox trong PayPal Developer Dashboard, sau đó đặt biến môi trường trước khi chạy backend:

```bash
PAYPAL_CLIENT_ID=your_sandbox_client_id
PAYPAL_CLIENT_SECRET=your_sandbox_client_secret
PAYPAL_VND_PER_USD=25000
```

Với Docker, sao chép `.env.example` thành `.env`, điền hai khóa Sandbox rồi chạy `docker compose up --build`. `Client Secret` chỉ được dùng ở backend và không được commit lên GitHub. Khi chưa cấu hình khóa, các phương thức thanh toán demo khác vẫn hoạt động và giao diện sẽ hiển thị hướng dẫn thay vì nút PayPal.

Tài khoản demo:

- Admin: `admin@khamphaviet.vn` / `Admin@123`
- Nhân viên: `staff@khamphaviet.vn` / `Staff@123`
- Nhân viên 2: `staff2@khamphaviet.vn` / `Staff2@123`
- Nhân viên 3: `staff3@khamphaviet.vn` / `Staff3@123`
- Nhân viên 4: `staff4@khamphaviet.vn` / `Staff4@123`
- Khách hàng: `customer@khamphaviet.vn` / `Customer@123`
- Nhân viên bếp: `kitchen@khamphaviet.vn` / `Kitchen@123`

## Cấu trúc

```text
backend/   Spring Boot REST API
frontend/  React customer web + staff dashboard
mobile/    Flutter customer starter
docs/      kế hoạch và API mẫu
```

## API chính

- `GET /api/v1/menu/items`
- `GET /api/v1/staff/menu/items` (Admin/Nhân viên, gồm cả món đang ẩn)
- `POST /api/v1/staff/menu/items` (thêm món)
- `PUT /api/v1/staff/menu/items/{id}` (sửa món)
- `PATCH /api/v1/staff/menu/items/{id}/availability` (bật/tắt món)
- `POST /api/v1/auth/register`
- `GET /api/v1/reservations/availability`
- `POST /api/v1/reservations`
- `GET /api/v1/reservations/lookup?code=...&phone=...`
- `GET /api/v1/staff/reservations`
- `PATCH /api/v1/staff/reservations/{id}/status`
- `GET /api/v1/admin/users/stats` (chỉ Admin)
- `GET /api/v1/admin/users` (chỉ Admin)
- `POST /api/v1/staff/service-sessions/{id}/orders`
- `PATCH /api/v1/staff/orders/{id}/served`
- `GET /api/v1/kitchen/orders` (Admin/Bếp)
- `PATCH /api/v1/kitchen/orders/{id}/status` (Admin/Bếp)
- `GET /api/v1/staff/tables/overview`
- `GET /api/v1/staff/checkouts` (Admin/Nhân viên)
- `POST /api/v1/staff/checkouts/{serviceSessionId}/pay` (Admin/Nhân viên)
- `GET /api/v1/staff/checkouts/paypal/config` (Admin/Nhân viên)
- `POST /api/v1/staff/checkouts/{serviceSessionId}/paypal/orders` (tạo đơn Sandbox)
- `POST /api/v1/staff/checkouts/{serviceSessionId}/paypal/orders/{orderId}/capture` (capture Sandbox)

> Đây là bộ khung học tập. Trước khi dùng thực tế cần tích hợp cổng thanh toán thật, notification, audit log và kiểm thử tải.
