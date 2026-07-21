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
