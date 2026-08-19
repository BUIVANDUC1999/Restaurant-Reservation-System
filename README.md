# Restaurant Reservation Web

Repository độc lập cho hệ thống web đặt bàn và vận hành nhà hàng Khám Phá Việt.

Repository này **chỉ chứa Web React và Backend Spring Boot**. Mã Flutter nằm hoàn toàn trong repository riêng
[`Restaurant-Reservation-Mobile`](https://github.com/BUIVANDUC1999/Restaurant-Reservation-Mobile); không đặt thư mục `mobile/` bên trong repository này.

## Thành phần

- `backend/`: Java 21, Spring Boot, PostgreSQL, Flyway, JWT/RBAC và REST API.
- `frontend/`: React 19, TypeScript và Vite.
- `docs/`: kiến trúc, kế hoạch kiểm thử, triển khai và ví dụ API.
- `docker-compose.yml`: PostgreSQL, backend và frontend.

Ứng dụng Flutter được duy trì trong repository riêng `restaurant-reservation-mobile` và sử dụng REST API của repository này.

Xem [hướng dẫn chạy hai dự án độc lập](docs/RUN_SEPARATE_PROJECTS.md) để tránh mở nhầm thư mục hoặc chạy nhầm cổng.

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
- Đặt cọc và thanh toán hóa đơn chỉ sử dụng PayPal Sandbox trong môi trường đồ án; không phát sinh tiền thật.

## Kiểm tra

```bash
cd backend && mvn test
cd frontend && npm ci && npm run lint && npm run build
```

## Quản lý thời gian và vận hành tại bàn

- Khách chọn giờ đến chính xác và thời lượng dùng bàn (mặc định 120 phút).
- Bàn được giữ 10 phút khi chờ đặt cọc; mỗi lượt cộng 15 phút dọn bàn để chống trùng lịch.
- Sơ đồ một tầng có tổng 8 bàn: 2 bàn trung tâm và 6 bàn bao quanh, hiển thị màu theo trạng thái vận hành.
- Mỗi bàn có QR riêng. Trước check-in khách có thể gọi nhân viên hỗ trợ nhận bàn; sau check-in có thêm yêu cầu nước, dụng cụ và thanh toán. Tất cả yêu cầu đều có giới hạn chống spam.
- Khi chạy nội bộ, cấu hình `VITE_GUEST_BASE_URL` trong `frontend/.env.local` bằng IPv4 của máy; có thể sửa trực tiếp địa chỉ này trong cửa sổ QR. Không dùng `localhost` trong QR quét bằng điện thoại.
- Bếp cập nhật SLA từng món: mới nhận, đang nấu, chậm, sẵn sàng và đã mang ra. Hệ thống tự cảnh báo trước ETA 3 phút, chuyển nhân viên bàn sau khi chậm 5 phút và chuyển mức nghiêm trọng sau 10 phút.
- Khi bếp bấm **Báo món xong**, hệ thống gửi thông báo kèm tên món và bàn; trang phục vụ hiển thị danh sách từng món chờ mang lên để nhân viên xác nhận riêng.
- Bộ lập lịch kiểm tra mỗi phút để cảnh báo lịch mới, khách sắp đến 30 phút và khách trễ 15/20 phút.
- Đơn đã cọc phải được nhân viên xác nhận trong 5 phút; quá thời gian sẽ tạo cảnh báo và không tự hủy đơn.
- Xác nhận đơn, check-in, hoàn tất, hủy, từ chối và no-show đều có bước xác nhận trên giao diện; thao tác kết thúc đơn bắt buộc lưu lý do.
- Khách theo dõi timeline từ lúc gửi yêu cầu, thanh toán cọc, nhà hàng xác nhận, check-in đến hoàn tất.
- Trung tâm timeout lưu lịch sử đang mở/đã xử lý cho giữ bàn chưa cọc, khách trễ, món quá SLA, QR chưa được nhận và bàn dọn quá lâu. Chỉ lượt giữ chưa cọc tự hết hạn; khách đã xác nhận không bị tự động hủy.
- Mỗi timeout có người phụ trách, thao tác nhận việc/chuyển việc/xác nhận/hoàn tất và nhật ký truy vết người thực hiện.
- Các màn hình bàn, phục vụ, bếp và walk-in nhận sự kiện realtime qua SSE; polling vẫn chạy dự phòng khi kết nối bị gián đoạn.
- Đặt bàn và xếp khách khóa bàn ở tầng cơ sở dữ liệu; thanh toán thường/PayPal có idempotency để tránh hai nhân viên thao tác trùng.
- Toàn bộ ngưỡng thời gian cấu hình tập trung bằng các biến `*_MINUTES` trong `.env.example`.

## Điều phối khách tại quán (Walk-in)

- Module `/staff/walk-in` tách riêng khách đến trực tiếp khỏi danh sách đặt bàn online.
- Mỗi lượt có mã `WI-*`, ETA, mức ưu tiên có lý do, cảnh báo SLA và timeline lưu người thao tác.
- Hệ thống đề xuất bàn theo sức chứa/khu vực/thời điểm sẵn sàng và từ chối bàn có lịch online sắp tới.
- Luồng chuẩn: chờ bàn → mời khách → vào bàn → dùng bữa → thanh toán → dọn bàn → hoàn thành.
- Khi khách vào bàn, module dùng chung phiên gọi món, bếp, QR tại bàn và thanh toán của hệ thống.
- Trong profile `demo`, nút **Tạo tình huống demo** mở xưởng gồm 23 tình huống thuộc 5 nhóm: khách tại quán, đặt bàn online, bếp/món ăn, QR gọi phục vụ và dọn bàn. Người trình bày tự chọn bàn, món, thời gian, lý do rồi xác nhận; dữ liệu không được tạo ngay khi chọn. Production mặc định khóa công cụ này.

### Gmail và SMS Sandbox

Gmail mặc định tắt. Bật xác minh 2 bước cho tài khoản gửi, tạo Gmail App Password 16 ký tự, rồi cấu hình `EMAIL_NOTIFICATIONS_ENABLED=true`, `MAIL_USERNAME` và `MAIL_PASSWORD` trong `.env`. Khi dán App Password hãy bỏ dấu cách và không commit mật khẩu này lên Git. Khởi động lại backend sau khi thay đổi; chỉ các email mới được tạo sau đó mới được gửi thật.

`SMS_SANDBOX=true` là mặc định cho đồ án: SMS được lưu trạng thái `DEMO` và ghi vào log, không phát sinh phí.

Xem thêm [luồng nghiệp vụ đồ án](docs/LUONG_NGHIEP_VU_DO_AN.md), [kiến trúc](docs/ARCHITECTURE.md),
[sơ đồ cơ sở dữ liệu](docs/DATABASE_DIAGRAM.md), [kế hoạch kiểm thử](docs/TEST_PLAN.md) và
[hướng dẫn triển khai](docs/DEPLOYMENT.md).
