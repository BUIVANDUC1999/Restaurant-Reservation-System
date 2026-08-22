# Use case và luồng nghiệp vụ hệ thống

Tài liệu này mô tả các tác nhân, quyền sử dụng và luồng chính của hệ thống quản lý nhà hàng. Sơ đồ cơ sở dữ liệu tương ứng nằm tại [`DATABASE_DIAGRAM.md`](DATABASE_DIAGRAM.md).

## 1. Tác nhân và phạm vi quyền

| Tác nhân | Trách nhiệm chính |
|---|---|
| Khách hàng | Xem thực đơn, đặt bàn online, chọn bàn, đặt trước món, thanh toán cọc PayPal, tra cứu đơn và quét QR tại bàn |
| Nhân viên phục vụ | Xác nhận khách, tiếp nhận khách tại quán, nhận bàn, tạo đơn món, xử lý yêu cầu QR, phục vụ món và thanh toán cuối bữa |
| Nhân viên bếp | Xem hàng đợi món, nhận chế biến, cập nhật thời gian dự kiến, báo món chậm và báo món hoàn thành |
| Quản trị viên | Xem toàn bộ dashboard; quản lý tài khoản, ca làm, thực đơn, bàn, đặt bàn, vận hành, thanh toán và báo cáo |
| PayPal Sandbox | Xử lý giao dịch cọc và hóa đơn thử nghiệm |
| Gmail/SMTP | Gửi xác nhận đặt bàn và các thông báo cho khách |
| Bộ lập lịch hệ thống | Kiểm tra SLA, sinh cảnh báo món chậm, khách sắp đến, khách trễ và yêu cầu phục vụ quá hạn |

## 2. Sơ đồ use case tổng thể

```mermaid
flowchart LR
    KH["KHÁCH HÀNG"]
    NV["NHÂN VIÊN PHỤC VỤ"]
    BEP["NHÂN VIÊN BẾP"]
    AD["QUẢN TRỊ VIÊN"]
    PP["PAYPAL SANDBOX"]
    MAIL["GMAIL / SMTP"]
    TIMER["BỘ LẬP LỊCH"]

    subgraph HT["HỆ THỐNG QUẢN LÝ NHÀ HÀNG"]
        direction TB

        subgraph KHUC["Nhóm chức năng khách hàng"]
            C1(["Xem thực đơn"])
            C2(["Đăng ký / đăng nhập"])
            C3(["Tìm bàn theo ngày, giờ, số khách"])
            C4(["Chọn bàn và món đặt trước"])
            C5(["Tạo yêu cầu đặt bàn"])
            C6(["Thanh toán tiền cọc"])
            C7(["Tra cứu trạng thái đặt bàn"])
            C8(["Quét QR và gọi hỗ trợ"])
        end

        subgraph NVUC["Nhóm chức năng phục vụ"]
            S1(["Xác nhận / từ chối đặt bàn"])
            S2(["Tiếp nhận khách tại quán"])
            S3(["Check-in và mở phiên phục vụ"])
            S4(["Nhận bàn / phân công bàn"])
            S5(["Tạo đơn món cho bàn"])
            S6(["Xử lý yêu cầu QR"])
            S7(["Xác nhận đã mang món"])
            S8(["Thu ngân và kết thúc phiên"])
        end

        subgraph BUC["Nhóm chức năng bếp"]
            K1(["Xem hàng đợi theo ưu tiên"])
            K2(["Nhận món và bắt đầu chế biến"])
            K3(["Cập nhật ETA / báo món chậm"])
            K4(["Báo món đã hoàn thành"])
        end

        subgraph AUC["Nhóm chức năng quản trị"]
            A1(["Dashboard và chi tiết chỉ số"])
            A2(["Quản lý tài khoản và quyền"])
            A3(["Quản lý ca làm"])
            A4(["Quản lý thực đơn"])
            A5(["Quản lý sơ đồ bàn và phân công"])
            A6(["Theo dõi doanh thu, SLA và lịch sử"])
            A7(["Tạo tình huống demo có kiểm soát"])
        end

        subgraph AUTO["Chức năng tự động"]
            X1(["Kiểm tra bàn trống và xung đột"])
            X2(["Tính cọc và giữ bàn tạm thời"])
            X3(["Gửi email và thông báo realtime"])
            X4(["Phát hiện quá hạn / món chậm"])
            X5(["Lưu lịch sử thay đổi"])
        end
    end

    KH --> C1
    KH --> C2
    KH --> C3
    KH --> C4
    KH --> C5
    KH --> C6
    KH --> C7
    KH --> C8

    NV --> S1
    NV --> S2
    NV --> S3
    NV --> S4
    NV --> S5
    NV --> S6
    NV --> S7
    NV --> S8

    BEP --> K1
    BEP --> K2
    BEP --> K3
    BEP --> K4

    AD --> A1
    AD --> A2
    AD --> A3
    AD --> A4
    AD --> A5
    AD --> A6
    AD --> A7
    AD -. "có toàn quyền" .-> S1
    AD -. "có toàn quyền" .-> K1

    C5 -. "include" .-> X1
    C5 -. "include" .-> X2
    C6 --> PP
    S8 --> PP
    X3 --> MAIL
    TIMER --> X4
    X4 -. "cảnh báo" .-> X3
    S1 -. "gửi kết quả" .-> X3
    S3 -. "include" .-> X5
    K2 -. "include" .-> X5
    K3 -. "include" .-> X5
    K4 -. "include" .-> X5
```

## 3. Danh mục use case chi tiết

### 3.1. Khách hàng

| Mã | Use case | Dữ liệu vào | Xử lý chính | Kết quả |
|---|---|---|---|---|
| UC-C01 | Xem thực đơn | Danh mục hoặc từ khóa | Lọc các món đang bán | Danh sách món, giá và thời gian dự kiến |
| UC-C02 | Tìm và chọn bàn | Ngày, giờ, số khách | Loại bàn xung đột, kiểm tra sức chứa | Danh sách bàn phù hợp trên sơ đồ màu |
| UC-C03 | Đặt bàn online | Thông tin khách, thời gian, bàn, món | Tạo mã đặt bàn, giữ bàn, tính tiền cọc | Đơn ở trạng thái `PENDING` chờ cọc/xác nhận |
| UC-C04 | Thanh toán cọc | Mã đặt bàn, PayPal order | Backend xác thực đúng đơn, đúng số tiền và capture | Cọc `PAID`, đặt bàn tiếp tục chờ xác nhận |
| UC-C05 | Tra cứu đặt bàn | Mã đặt bàn hoặc tài khoản | Kiểm tra quyền và tải lịch sử trạng thái | Chi tiết bàn, món, cọc và trạng thái |
| UC-C06 | Gọi hỗ trợ bằng QR | Token QR, loại yêu cầu | Kiểm tra QR, chống gửi liên tục, tạo yêu cầu | Nhân viên nhận cảnh báo realtime |

### 3.2. Nhân viên phục vụ

| Mã | Use case | Điều kiện | Xử lý chính | Kết quả |
|---|---|---|---|---|
| UC-S01 | Duyệt đặt bàn | Đơn hợp lệ | Xác nhận, từ chối hoặc hủy kèm lý do | Cập nhật trạng thái và gửi email khách |
| UC-S02 | Tiếp nhận khách tại quán | Có bàn phù hợp hoặc danh sách chờ | Ghi nhận khách, tạo đề xuất/giữ chỗ | Khách được xếp bàn hoặc chờ bàn |
| UC-S03 | Check-in | Đơn đã xác nhận hoặc walk-in được xếp bàn | Gán bàn, nhân viên, số khách; mở phiên | `service_sessions` ở trạng thái `OPEN` |
| UC-S04 | Nhận bàn | Nhân viên đang trong ca | Claim bàn hoặc admin phân công | Bàn hiển thị đúng người phụ trách |
| UC-S05 | Tạo đơn món | Phiên phục vụ đang mở | Chọn món, số lượng, ghi chú | Đơn chuyển vào hàng đợi bếp |
| UC-S06 | Xử lý yêu cầu QR | Yêu cầu ở trạng thái `NEW` | Nhận xử lý rồi hoàn tất | `NEW → ACKNOWLEDGED → DONE` |
| UC-S07 | Phục vụ món | Bếp đã báo `READY` | Xác nhận món đã mang lên | Món chuyển sang `SERVED` |
| UC-S08 | Thanh toán cuối bữa | Mọi món đã `SERVED`/`CANCELLED` | Tính tiền, PayPal capture, đóng phiên | Hóa đơn `PAID`, bàn chờ dọn/trống |

### 3.3. Nhân viên bếp

| Mã | Use case | Xử lý chính | Kết quả |
|---|---|---|---|
| UC-K01 | Xem hàng đợi | Ưu tiên món đặt trước, món gần/quá hạn và thời điểm tạo | Danh sách cần làm trước hiển thị rõ màu |
| UC-K02 | Nhận chế biến | Nhận món `NEW` và bắt đầu làm | `NEW → ACCEPTED → PREPARING` |
| UC-K03 | Điều chỉnh ETA | Nhập thời gian dự kiến mới và lý do chậm | Nhân viên phục vụ nhận cảnh báo cập nhật |
| UC-K04 | Hoàn thành món | Bếp xác nhận chế biến xong | `PREPARING → READY`, nhân viên được báo mang món |

### 3.4. Quản trị viên

Quản trị viên được dùng toàn bộ chức năng của nhân viên phục vụ và nhân viên bếp, đồng thời có các use case riêng:

- Xem từng thẻ chỉ số dashboard và danh sách bản ghi tạo nên chỉ số đó.
- Quản lý tài khoản, vai trò, trạng thái hoạt động và ca làm.
- Quản lý thực đơn, giá, trạng thái bán và thời gian chế biến chuẩn.
- Quản lý tám bàn, sơ đồ màu, QR, lịch giữ bàn và nhân viên phụ trách.
- Theo dõi đặt bàn, khách tại quán, doanh thu, thanh toán, món chậm và SLA.
- Tạo tình huống demo bằng biểu mẫu có dữ liệu, trạng thái và thời gian do người dùng chọn.

## 4. Luồng nghiệp vụ chính

### 4.1. Đặt bàn online và thanh toán cọc

```mermaid
flowchart TD
    A([Khách chọn ngày, giờ, số người]) --> B[Hệ thống tìm bàn còn trống]
    B --> C{Có bàn phù hợp?}
    C -- Không --> C1[Đề nghị đổi giờ hoặc số người]
    C -- Có --> D[Khách chọn bàn và món đặt trước]
    D --> E[Kiểm tra lại xung đột và sức chứa]
    E --> F[Tính cọc: 10% món đặt trước]
    F --> G{Có đặt trước món?}
    G -- Không --> H[Tính cọc 200.000đ x số người]
    G -- Có --> I[Giữ mức cọc 10% tổng tiền món]
    H --> J[Tạo reservation PENDING và giữ bàn]
    I --> J
    J --> K[Khách thanh toán PayPal Sandbox]
    K --> L{Capture thành công?}
    L -- Không / hết hạn --> M[Cho thanh toán lại hoặc giải phóng giữ bàn]
    L -- Có --> N[Ghi deposit PAID]
    N --> O[Nhân viên xác nhận đặt bàn]
    O --> P[Reservation CONFIRMED]
    P --> Q[Gửi email xác nhận và nhắc lịch]
```

Ngoại lệ quan trọng:

- Bàn vừa bị người khác giữ: yêu cầu khách chọn lại bàn.
- PayPal order không thuộc đơn hoặc sai số tiền: từ chối capture.
- Hết thời gian giữ bàn mà chưa thanh toán: đơn chuyển `EXPIRED` và bàn được giải phóng.

### 4.2. Khách đến quán và mở phiên phục vụ

```mermaid
flowchart TD
    A([Khách đến quán]) --> B{Đã đặt trước?}
    B -- Có --> C[Nhân viên tra mã đặt bàn]
    C --> D{Đúng giờ và đơn hợp lệ?}
    D -- Có --> E[Check-in và xác nhận số khách thực tế]
    D -- Trễ --> F[Ghi nhận ARRIVED_LATE và xử lý theo chính sách]
    F --> E
    B -- Không --> G[Tạo lượt khách tại quán]
    G --> H{Có bàn phù hợp?}
    H -- Không --> I[WAITING và tạo đề nghị bàn khi có chỗ]
    I --> H
    H -- Có --> J[Gán bàn cho khách]
    J --> E
    E --> K[Gán hoặc nhận nhân viên phụ trách]
    K --> L[Mở service session]
    L --> M[Bàn chuyển sang đang phục vụ]
```

### 4.3. Gọi món, bếp chế biến và phục vụ

```mermaid
flowchart LR
    A[Nhân viên tạo đơn món] --> B[NEW]
    B --> C[Bếp ACCEPTED]
    C --> D[PREPARING]
    D --> E{Vượt ETA?}
    E -- Có --> F[Tự báo món chậm]
    F --> G[Bếp cập nhật ETA và lý do]
    G --> D
    E -- Không --> H[Bếp báo READY]
    D --> H
    H --> I[Thông báo realtime cho phục vụ]
    I --> J[Nhân viên mang món]
    J --> K[SERVED]
```

Thứ tự hàng đợi ưu tiên: món đã chậm → món gần đến hạn → món đặt trước theo giờ khách đến → món tạo trước.

### 4.4. Khách quét QR tại bàn

```mermaid
flowchart TD
    A([Khách quét QR]) --> B[Đọc public token của bàn]
    B --> C{Token hợp lệ và bàn hoạt động?}
    C -- Không --> D[Thông báo QR không hợp lệ]
    C -- Có --> E{Đã có phiên phục vụ?}
    E -- Chưa --> F[Chỉ cho phép Gọi nhân viên để hỗ trợ nhận bàn]
    E -- Có --> G[Cho phép gọi nhân viên / nước / dụng cụ / thanh toán]
    F --> H[Kiểm tra giới hạn chống spam]
    G --> H
    H --> I[Tạo table_service_request NEW]
    I --> J[Gửi TABLE_CALL realtime cho nhân viên]
    J --> K[Nhân viên bấm Nhận xử lý]
    K --> L[ACKNOWLEDGED]
    L --> M[Nhân viên hoàn tất yêu cầu]
    M --> N[DONE]
```

Khách quét QR không phải đăng nhập. QR xác định bàn bằng token công khai; phiên đăng nhập chỉ bắt buộc ở màn hình nghiệp vụ của nhân viên.

### 4.5. Thanh toán và kết thúc bàn

```mermaid
flowchart TD
    A[Khách yêu cầu thanh toán] --> B{Còn món chưa phục vụ?}
    B -- Có --> C[Không cho đóng hóa đơn và hiển thị món còn thiếu]
    B -- Không --> D[Tính tạm tính và giảm giá]
    D --> E[Tạo PayPal order]
    E --> F[Khách thanh toán Sandbox]
    F --> G{Capture thành công?}
    G -- Không --> H[Giữ phiên mở để thử lại]
    G -- Có --> I[Ghi payment PAID]
    I --> J[Đóng service session]
    J --> K[Hoàn tất reservation hoặc walk-in]
    K --> L[Bàn chuyển chờ dọn]
    L --> M[Nhân viên xác nhận đã dọn]
    M --> N[Bàn AVAILABLE]
```

### 4.6. Cảnh báo timeout và realtime

```mermaid
flowchart TD
    A[Bộ lập lịch kiểm tra định kỳ] --> B{Phát hiện vượt SLA?}
    B -- Không --> A
    B -- Có --> C[Kiểm tra đã có timeout đang mở chưa]
    C --> D{Đã tồn tại?}
    D -- Có --> E[Cập nhật mức độ / thời gian nếu cần]
    D -- Không --> F[Tạo operational_timeout]
    E --> G[Tạo notification ngắn gọn]
    F --> G
    G --> H[Đẩy SSE đến đúng vai trò]
    H --> I[Nhân viên mở chi tiết đối tượng]
    I --> J[Nhận xử lý]
    J --> K[Khắc phục nguyên nhân]
    K --> L[RESOLVED và lưu sự kiện]
```

Các nhóm timeout chính: đặt bàn mới chưa xác nhận, khách sắp đến, khách trễ 15–20 phút, món vượt ETA, món `READY` chưa mang, yêu cầu QR chưa nhận và bàn chưa dọn.

## 5. Vòng đời trạng thái quan trọng

### Đặt bàn

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> CONFIRMED: nhân viên xác nhận
    PENDING --> REJECTED: từ chối
    PENDING --> EXPIRED: hết hạn giữ/cọc
    CONFIRMED --> CHECKED_IN: khách đến
    CONFIRMED --> NO_SHOW: quá thời gian chờ
    CONFIRMED --> CANCELLED: hủy đặt bàn
    CHECKED_IN --> COMPLETED: kết thúc phục vụ
    CHECKED_IN --> CANCELLED: sự cố đặc biệt
```

### Món trong đơn phục vụ

```mermaid
stateDiagram-v2
    [*] --> NEW
    NEW --> ACCEPTED
    ACCEPTED --> PREPARING
    PREPARING --> READY
    READY --> SERVED
    NEW --> CANCELLED
    ACCEPTED --> CANCELLED
```

### Yêu cầu QR

```mermaid
stateDiagram-v2
    [*] --> NEW
    NEW --> ACKNOWLEDGED: nhân viên nhận
    ACKNOWLEDGED --> DONE: hoàn tất
    NEW --> CANCELLED: hủy yêu cầu
```

## 6. Liên kết use case với cơ sở dữ liệu

| Nhóm nghiệp vụ | Bảng chính |
|---|---|
| Tài khoản và phân quyền | `app_users`, `staff_shifts` |
| Đặt bàn online | `reservations`, `reservation_items`, `reservation_deposits`, `reservation_table_assignments`, `reservation_status_events` |
| Sơ đồ bàn và phiên phục vụ | `restaurant_tables`, `service_sessions`, `waiter_assignment_events` |
| Gọi món và bếp | `menu_items`, `dining_orders`, `dining_order_items` |
| QR gọi nhân viên | `table_service_requests` |
| Khách tại quán | `walk_in_visits`, `walk_in_events` |
| Thanh toán cuối bữa | `payments` |
| Cảnh báo, SLA và realtime | `operational_notifications`, `operational_timeouts`, `operational_timeout_events` |

## 7. Cách trình bày ngắn khi bảo vệ

> Hệ thống quản lý trọn vòng đời một lượt ăn: khách tìm và đặt bàn, thanh toán cọc, nhà hàng xác nhận và check-in, nhân viên nhận bàn, bếp xử lý món theo ETA, phục vụ mang món, khách yêu cầu hỗ trợ qua QR và thanh toán cuối bữa. Mọi bước quan trọng đều có trạng thái, lịch sử và cảnh báo quá hạn. Admin có quyền giám sát toàn bộ, còn nhân viên phục vụ và bếp chỉ thấy đúng dashboard nghiệp vụ của mình.

ERD đầy đủ: [`DATABASE_DIAGRAM.md`](DATABASE_DIAGRAM.md). Bản DBML để nhập vào dbdiagram.io: [`database.dbml`](database.dbml).
