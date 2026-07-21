# API examples

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

Trường `openOrderCount` trong dữ liệu đặt bàn cho biết số phiếu còn ở trạng thái `SUBMITTED`, `PREPARING` hoặc `READY`. Chỉ khi giá trị này bằng `0`, nhân viên mới có thể hoàn tất lượt khách.
