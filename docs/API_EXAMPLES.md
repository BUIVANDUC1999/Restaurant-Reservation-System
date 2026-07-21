# API examples

## Đăng ký tài khoản khách hàng

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "fullName": "Nguyễn Văn An",
  "phone": "0984353577",
  "email": "an@example.com",
  "password": "Customer@123"
}
```

Tài khoản mới luôn được cấp vai trò `CUSTOMER`. Phản hồi chứa access token để giao diện tự đăng nhập.

## Tạo đặt bàn

```http
POST /api/v1/reservations
Content-Type: application/json

{
  "customerName": "Nguyễn Văn An",
  "phone": "0984353577",
  "email": "an@example.com",
  "reservationDate": "2026-08-01",
  "timeSlot": "DINNER",
  "partySize": 6,
  "preferredFloor": "FLOOR_1",
  "note": "Bàn sinh nhật, có trẻ em",
  "preOrderItems": [
    {"menuItemId": 1, "quantity": 2},
    {"menuItemId": 2, "quantity": 1}
  ]
}
```

## Tra cứu

```http
GET /api/v1/reservations/lookup?code=KV-ABC123&phone=0984353577
```

## Cập nhật trạng thái

```http
PATCH /api/v1/staff/reservations/1/status
Content-Type: application/json

{"status":"CONFIRMED"}
```

## Nhân viên xác nhận món khách chọn trước

```http
POST /api/v1/staff/reservations/1/preorder/confirm
Authorization: Bearer <access-token>
```

Món mới chọn có trạng thái `REQUESTED`; sau khi nhân viên trao đổi với khách và gọi API trên, trạng thái chuyển thành `CONFIRMED`.
Khi khách check-in, hệ thống tự tạo phiếu bếp nguồn `PREORDER` từ các món đã xác nhận. Nếu còn món ở trạng thái `REQUESTED`, thao tác check-in sẽ bị từ chối.

## Xếp bàn, check-in và hoàn tất phục vụ

```http
PUT /api/v1/staff/reservations/1/tables
Authorization: Bearer <access-token>
Content-Type: application/json

{"tableIds":[1,2]}
```

```http
POST /api/v1/staff/reservations/1/check-in
Authorization: Bearer <access-token>
```

```http
POST /api/v1/staff/reservations/1/complete
Authorization: Bearer <access-token>
```

Check-in tạo một phiên phục vụ và chuyển bàn sang `OCCUPIED`. Khi hoàn tất, phiên đóng và bàn chuyển sang `NEEDS_CLEANING`.

## Gọi món và điều phối bếp

Sau khi khách check-in và có `serviceSessionId`, nhân viên tạo phiếu gọi món:

```http
POST /api/v1/staff/service-sessions/1/orders
Authorization: Bearer <staff-token>
Content-Type: application/json

{
  "items": [
    { "menuItemId": 1, "quantity": 2 },
    { "menuItemId": 5, "quantity": 1 }
  ],
  "note": "Ít cay, một phần không hành"
}
```

Bếp lấy danh sách phiếu đang mở và cập nhật tuần tự:

```http
GET /api/v1/kitchen/orders
PATCH /api/v1/kitchen/orders/1/status

{ "status": "PREPARING" }
```

Sau `PREPARING`, bếp chuyển sang `READY`; nhân viên phục vụ xác nhận đã mang món:

```http
PATCH /api/v1/staff/orders/1/served
```

Trường `openOrderCount` trong dữ liệu đặt bàn cho biết số phiếu còn ở trạng thái `SUBMITTED`, `PREPARING` hoặc `READY`.

## Lập hóa đơn và thanh toán

Nhân viên lấy các phiên phục vụ đang hoạt động:

```http
GET /api/v1/staff/checkouts
Authorization: Bearer <staff-token>
```

Thanh toán hóa đơn theo phiên phục vụ:

```http
POST /api/v1/staff/checkouts/1/pay
Authorization: Bearer <staff-token>
Content-Type: application/json

{
  "method": "QR",
  "discountAmount": 50000
}
```

`method` hỗ trợ `CASH`, `BANK_TRANSFER`, `QR` và `CARD`. Hệ thống từ chối thanh toán khi còn phiếu món chưa phục vụ. Sau thanh toán, nhân viên mới có thể hoàn tất lượt khách và chuyển bàn sang trạng thái cần dọn.

### PayPal Sandbox

PayPal sử dụng luồng riêng để không thể đánh dấu đã thanh toán nếu chưa capture thành công. Trước tiên frontend lấy cấu hình công khai (không chứa Client Secret), sau đó yêu cầu backend tạo đơn:

```http
GET /api/v1/staff/checkouts/paypal/config

POST /api/v1/staff/checkouts/1/paypal/orders
Content-Type: application/json

{ "discountAmount": 0 }
```

Sau khi người mua phê duyệt trong cửa sổ PayPal Sandbox, frontend gửi mã đơn về backend:

```http
POST /api/v1/staff/checkouts/1/paypal/orders/5O190127TN364715T/capture
Content-Type: application/json

{ "discountAmount": 0 }
```

Backend kiểm tra trạng thái `COMPLETED`, phiên phục vụ, loại tiền và số tiền capture trước khi lưu hóa đơn. Giá VND được quy đổi sang USD theo biến `PAYPAL_VND_PER_USD`.

## Tra cứu trạng thái phục vụ theo bàn

```http
GET /api/v1/staff/tables/overview
Authorization: Bearer <staff-token>
```

Mỗi bàn có `serviceState`: `EMPTY`, `RESERVED`, `DINING`, `WAITING_KITCHEN`, `NEEDS_SERVING`, `NEEDS_CLEANING` hoặc `INACTIVE`. Khi có khách, phản hồi kèm thông tin đặt bàn, phiên phục vụ, `openOrderCount` và `readyOrderCount`.
