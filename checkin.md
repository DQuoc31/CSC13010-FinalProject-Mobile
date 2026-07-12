# Đặc tả: Luồng soát vé Offline P2P (Mô hình Hub-Scanner)

## Mô tả
Do điều kiện tại sự kiện/sân vận động đông người thường xuyên nghẽn mạng 4G/Wifi, ứng dụng TicketBox Mobile được thiết kế theo mô hình **P2P Hub-Scanner** sử dụng công nghệ Google Nearby Connections. 

Mô hình đột phá này giúp quá trình soát vé diễn ra trơn tru, offline hoàn toàn tại cổng, đồng thời giải quyết triệt để bài toán "1 vé quét nhiều lần ở nhiều cổng khác nhau" mà không cần đến mạng Internet.

## Kiến trúc P2P Hub-Scanner
Thay vì mỗi máy quét tự lưu dữ liệu offline và đồng bộ riêng lẻ lên Server (gây rủi ro dữ liệu phân tán và trùng lặp), hệ thống sử dụng:
1. **Máy Trưởng (Local Hub):** 1 thiết bị đặt tại mỗi cụm cổng (gate), có nhiệm vụ tải toàn bộ dữ liệu vé về DB nội bộ trước khi sự kiện bắt đầu. Thiết bị này đóng vai trò như một Server cục bộ, phát sóng (Advertising) mạng nội bộ P2P.
2. **Máy Quét (Scanner):** Các thiết bị con do nhân viên cầm đi lại để quét mã QR. Các máy này kết nối trực tiếp với Máy Trưởng qua sóng P2P (Bluetooth/Wifi Direct) mà không cần phải có mạng Internet hay chung một mạng Wifi.

## Luồng hoạt động

### Giai đoạn 1: Chuẩn bị (Cần Internet)
1. Trước giờ soát vé, Tổ Trưởng mở App, chọn vai trò **Máy Trưởng**.
2. Chọn sự kiện và Hạng vé. Ở bước này, bên cạnh các hạng vé thông thường, App sẽ tải luôn cả các danh sách Khách Mời đặc biệt (VD: `Khách Mời VIP - Anh Trai Say Hi`).
3. App Máy Trưởng gọi API `/api/mobile/tickets/:eventId/:ticketTypeId` để tải toàn bộ vé tương ứng và lưu vào Room Database (SQLite).
4. Máy Trưởng tự động bật chế độ phát sóng (Advertising) liên tục.

### Giai đoạn 2: Quét vé (Hoàn toàn Offline)
1. Nhân viên bật App trên thiết bị khác, chọn vai trò **Máy Quét**.
2. Máy Quét dò tìm (Discovery) và tự động kết nối với Máy Trưởng gần nhất thông qua Nearby Connections.
3. Nhân viên quét mã QR E-Ticket của khán giả.
4. Máy Quét lập tức gửi chuỗi `qr_code_hash` sang Máy Trưởng qua sóng P2P.
5. Máy Trưởng tiếp nhận, kiểm tra trong Database cục bộ và khóa vé lại nếu hợp lệ. Quá trình kiểm tra:
   - TỒN TẠI & `is_checked_in = false` (Vé Khách mời): Máy Trưởng khóa vé, trả về kết quả `VIP_GUEST`. Máy quét hiện giao diện màu Vàng Đồng, Icon Ngôi Sao.
   - TỒN TẠI & `is_checked_in = false` (Vé thường): Máy Trưởng khóa vé, trả về kết quả `VALID`. Máy quét hiện màn hình Xanh lá.
   - TỒN TẠI & `is_checked_in = true`: Máy Trưởng trả về kết quả `USED`. Máy quét hiện cảnh báo màu Cam.
   - KHÔNG TỒN TẠI: Máy Trưởng trả về `INVALID`. Máy quét hiện cảnh báo màu Đỏ.
6. Toàn bộ thao tác truyền, xử lý và hiển thị UI diễn ra cực nhanh (< 0.5s) mà không tốn một byte băng thông Internet nào.

### Giai đoạn 3: Đồng bộ lên Server (Cần Internet)
1. Khi kết thúc sự kiện hoặc vào giờ nghỉ có mạng 4G/Wifi ổn định, Tổ Trưởng nhấn "Đồng Bộ Lên Server" trên Máy Trưởng.
2. Máy Trưởng gom toàn bộ danh sách các vé đã quét (cả vé bán và vé khách mời), gửi gói payload nén lên API `POST /api/mobile/sync`.
3. Backend tiếp nhận, đối chiếu và cập nhật trạng thái đồng loạt vào cơ sở dữ liệu gốc (Postgres) cho các bảng `Ticket` và `VipGuest`. Dữ liệu xuất hiện ngay lập tức trên màn hình Dashboard của Ban Tổ Chức.

## Xử lý Kịch bản Lỗi & Tính Ổn định
1. **Xung đột 1 vé quét tại 2 cổng cùng lúc:** 
   - Đã được **XỬ LÝ TRIỆT ĐỂ**. Khán giả A in vé làm 2 bản đưa cho 2 nhân viên ở 2 luồng xếp hàng khác nhau. 
   - Vì cả 2 Máy Quét đều đẩy dữ liệu về cùng một Máy Trưởng, Máy Trưởng đóng vai trò là "Single Source of Truth". Vé nào đến Máy Trưởng trước (dù chỉ trước 1 mili-giây) sẽ được ghi nhận hợp lệ, vé thứ hai sẽ lập tức bị chặn với lỗi "VÉ ĐÃ SỬ DỤNG". Rủi ro quét trùng đã bị triệt tiêu hoàn toàn ở chế độ Offline.
2. **Crash App hoặc sập nguồn:** 
   - Dữ liệu quét được lưu an toàn xuống đĩa cứng bằng Room DB trên Máy Trưởng. Máy Quét là một "Terminal mù" không lưu dữ liệu quan trọng nào. Nếu Máy Quét hỏng hoặc hết pin, chỉ cần lấy máy khác kết nối lại vào Máy Trưởng là chạy tiếp ngay. Nếu Máy Trưởng sập nguồn, khi khởi động lại dữ liệu vẫn nguyên vẹn.
3. **Vé mua giờ chót (Sau thời điểm tải offline):** 
   - Nếu khán giả mua vé lúc 19:00 mà Máy Trưởng đã chốt tải dữ liệu offline lúc 18:00, App sẽ báo "Vé không tồn tại". Xử lý: Tổ trưởng tạm bật 4G trên Máy Trưởng và bấm "Tải Dữ Liệu" lại để kéo bổ sung những vé mới nhất từ Server xuống.
