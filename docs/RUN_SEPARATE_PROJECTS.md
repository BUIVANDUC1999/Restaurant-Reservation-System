# Chạy Web và Mobile độc lập

Hai ứng dụng là hai Git repository và hai thư mục ngang hàng. Không sao chép Flutter vào repository web và không đặt backend/frontend vào repository mobile.

```text
buivanduc1999-restaurant-reservation-system-https-github/
├── Restaurant-Reservation-System/     # Repository Web + Backend
│   ├── backend/                        # Spring Boot, cổng 8080
│   ├── frontend/                       # React/Vite, cổng 5173
│   └── docs/
└── restaurant-reservation-mobile/     # Repository Flutter độc lập
    ├── android/
    ├── lib/
    ├── test/
    └── web/                            # Flutter Web, cổng 5174
```

## 1. Chạy backend

Mở terminal thứ nhất tại repository web:

```powershell
cd Restaurant-Reservation-System\backend
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Backend chạy tại `http://localhost:8080`.

## 2. Chạy web React

Mở terminal thứ hai:

```powershell
cd Restaurant-Reservation-System\frontend
npm install
npm run dev
```

Web React chạy tại `http://localhost:5173`.

## 3. Chạy Flutter Web

Mở terminal thứ ba tại repository mobile riêng:

```powershell
cd restaurant-reservation-mobile
flutter pub get
flutter run -d web-server --web-port 5174 --dart-define-from-file=config/dev.json
```

Flutter Web chạy độc lập tại `http://localhost:5174` và gọi backend cổng `8080`.

## 4. Chạy Flutter trên điện thoại thật

Điện thoại và máy tính phải dùng cùng mạng. Thay `192.168.1.10` bằng IPv4 của máy chạy backend:

```powershell
cd restaurant-reservation-mobile
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080/api/v1
```

Không dùng `localhost` làm API URL trên điện thoại vì khi đó `localhost` là chính điện thoại, không phải máy tính chạy backend.

## Cổng chuẩn

| Thành phần | Thư mục | Cổng |
|---|---|---:|
| Backend Spring Boot | `Restaurant-Reservation-System/backend` | 8080 |
| Web React | `Restaurant-Reservation-System/frontend` | 5173 |
| Flutter Web | `restaurant-reservation-mobile` | 5174 |

Backend là phần dùng chung duy nhất qua REST API và cơ sở dữ liệu; mã nguồn, dependency và lệnh chạy của Web/Flutter không phụ thuộc trực tiếp vào nhau.
