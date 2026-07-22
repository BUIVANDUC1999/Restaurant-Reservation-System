# Khám Phá Việt Mobile

MVP Flutter dùng chung API với website, gồm:

- Xem và làm mới thực đơn.
- Tạo đơn đặt bàn.
- Hiển thị mã đặt bàn sau khi tạo.
- Tra cứu đơn bằng mã và số điện thoại.

Repository ban đầu chưa có platform scaffold. Nếu thư mục `android/` hoặc `web/` chưa tồn tại, chạy một lần:

```bash
flutter create --platforms=android,web --project-name kham_pha_viet_mobile .
flutter pub get
flutter analyze
flutter run
```

API mặc định là `http://10.0.2.2:8080/api/v1` dành cho Android emulator. Dùng `--dart-define=API_BASE_URL=...` để đổi địa chỉ.
