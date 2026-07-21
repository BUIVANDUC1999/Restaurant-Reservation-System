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
  "note": "Bàn sinh nhật, có trẻ em"
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

