# Kế hoạch kiểm thử

## Kiểm thử tự động hiện có

| Nhóm | Trường hợp | Kỳ vọng |
|---|---|---|
| Public API | Lấy thực đơn | HTTP 200 và có món |
| Validation | Đặt ngày trong quá khứ | HTTP 400, có lỗi trường ngày |
| Capacity | Đặt đủ 300 chỗ rồi thêm 1 khách | Yêu cầu thứ hai bị từ chối |
| Authentication | Gọi API nhân viên khi chưa đăng nhập | Bị từ chối |
| RBAC | Nhân viên đăng nhập và xem danh sách đặt bàn | HTTP 200 |
| RBAC | Khách hàng gọi API Admin | HTTP 403 |
| Startup | Spring context với H2 | Khởi động thành công |

Chạy bằng `cd backend && mvn test`.

## Checklist kiểm thử thủ công trước demo

1. Khách xem menu, tìm kiếm và lọc danh mục.
2. Khách đặt bàn có/không có món chọn trước rồi tra cứu bằng mã và số điện thoại.
3. Nhân viên xác nhận đơn, xác nhận món, phân bàn và check-in.
4. Bếp nhận phiếu, chuyển chế biến và sẵn sàng.
5. Nhân viên đánh dấu đã phục vụ, lập hóa đơn và hoàn tất lượt khách.
6. Admin xem thống kê tài khoản và doanh thu.
7. Thử tài khoản sai quyền trên từng dashboard.
8. Thử hai trình duyệt cùng phân một bàn để xác nhận nhận HTTP 409.

## Quality gate

- Backend: `mvn verify`.
- Frontend: `npm run lint` và `npm run build`.
- Mobile: `flutter analyze` và `flutter test`.
- GitHub Actions thực hiện backend test và frontend lint/build trên mỗi pull request.
