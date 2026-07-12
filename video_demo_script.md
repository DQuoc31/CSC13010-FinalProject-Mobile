# Kịch Bản Quay Video Demo: Hệ Thống Soát Vé TicketBox Mobile

Video này kéo dài khoảng **3 - 4 phút**, nhằm phô diễn tối đa sức mạnh của **Kiến trúc P2P Hub-Scanner** (Soát vé Offline và Chống trùng vé theo thời gian thực) cùng với tính năng **Nhận diện Khách Mời VIP**.

---

## 🎬 Chuẩn Bị Góc Quay & Đạo Cụ
- **Góc quay:** Đặt máy quay từ trên xuống (Top-down) hoặc góc chéo nhìn rõ cả 2 màn hình điện thoại đặt cạnh nhau.
- **Đạo cụ:** 
  - **Điện thoại trái:** Đóng vai trò **Máy Trưởng (Hub)**.
  - **Điện thoại phải:** Đóng vai trò **Máy Quét (Scanner)**.
  - **Màn hình Laptop/iPad:** Đặt phía trên, dùng để mở sẵn 3 mã QR (QR vé thường, QR vé đã dùng/vé giả, QR Khách mời VIP).

---

## 📝 Phân Cảnh Chi Tiết (Scenes)

### Scene 1: Giới thiệu & Chuẩn bị tải dữ liệu (0:00 - 0:45)
- **Hình ảnh:** Quay zoom vào Điện thoại trái (Máy Trưởng).
- **Hành động:** 
  1. Mở App, đăng nhập tài khoản Staff.
  2. Chọn vai trò "Máy Trưởng". 
  3. Bấm nút "Tải Dữ Liệu". Chọn một sự kiện (VD: *Anh Trai Say Hi*) và một Hạng vé.
- **Thuyết minh/Phụ đề:** *"Bước đầu tiên, Tổ trưởng cổng sử dụng mạng Internet để tải danh sách mã vé hợp lệ từ Server về máy. Điện thoại này giờ đây trở thành một Máy Chủ Cục Bộ (Local Hub)."*

### Scene 2: Ngắt kết nối Internet - "Màn Ảo Thuật" (0:45 - 1:15)
- **Hình ảnh:** Quay toàn cảnh cả 2 điện thoại.
- **Hành động:**
  1. Vuốt thanh Control Center từ trên xuống ở **CẢ 2 ĐIỆN THOẠI**.
  2. **TẮT WIFI, TẮT 4G** (có thể Bật chế độ Máy bay để thuyết phục tuyệt đối).
  3. Trên Điện thoại phải, mở App, chọn vai trò "Máy Quét".
  4. Chờ 1-2 giây, màn hình Máy Quét tự động báo "Đã kết nối với Hub...".
- **Thuyết minh/Phụ đề:** *"Đây là sức mạnh cốt lõi của hệ thống: Chúng tôi ngắt hoàn toàn Internet. Máy Quét sẽ tự động dò tìm và kết nối với Máy Trưởng bằng sóng P2P nội bộ (Nearby Connections) mà không cần dây cáp hay Router Wifi."*

### Scene 3: Demo Tốc độ & Chống Trùng Vé Offline (1:15 - 2:00)
- **Hình ảnh:** Máy Quét (bên phải) đưa lên soi mã QR trên Laptop. Máy Trưởng (bên trái) để màn hình sáng hiện dòng Log nhảy liên tục.
- **Hành động:**
  1. Soi mã **QR Hợp lệ thứ nhất**. Máy Quét lập tức nháy **XANH LÁ** báo "VÉ HỢP LỆ". Tiếng Bíp vang lên.
  2. Rút máy ra, rồi **LẬP TỨC soi lại mã đó một lần nữa**. Máy Quét nháy **MÀU CAM/ĐỎ** báo "VÉ ĐÃ SỬ DỤNG!".
- **Thuyết minh/Phụ đề:** *"Tốc độ nhận diện cực nhanh dưới 0.5 giây. Hơn thế nữa, hệ thống khóa vé ngay lập tức trên Database cục bộ của Máy Trưởng, giúp chống trùng vé tuyệt đối dù đang mất mạng, loại bỏ hoàn toàn nạn xé lẻ 1 vé cho nhiều người."*

### Scene 4: Tính Năng Khách Mời VIP (2:00 - 2:30)
- **Hình ảnh:** Zoom nhẹ vào màn hình Máy Quét.
- **Hành động:**
  1. Chuyển sang soi **mã QR của Khách Mời VIP**.
  2. Màn hình Máy Quét thay đổi UI toàn diện: Chuyển sang màu **VÀNG ĐỒNG sang trọng**, hiện icon **Ngôi Sao** và dòng chữ lớn **"⭐ VÉ KHÁCH MỜI VIP"**.
- **Thuyết minh/Phụ đề:** *"Ứng dụng được thiết kế một luồng riêng dành cho khách VIP. Giao diện Máy Quét thay đổi màu sắc rực rỡ để nhân viên lập tức nhận diện và có chế độ tiếp đón đặc biệt."*

### Scene 5: Đồng Bộ Dữ Liệu Lên Cloud (2:30 - 3:15)
- **Hình ảnh:** Quay lại màn hình Điện thoại trái (Máy Trưởng).
- **Hành động:**
  1. Vuốt thanh công cụ, **BẬT WIFI** trở lại.
  2. Bấm nút **"Đồng bộ lên Server"**. 
  3. Màn hình hiện thông báo "Đã đồng bộ thành công X vé".
- **Thuyết minh/Phụ đề:** *"Sau khi sự kiện kết thúc hoặc khi có mạng 4G trở lại, Tổ trưởng chỉ cần bấm Đồng Bộ. Toàn bộ lịch sử quét vé sẽ được đẩy lên Cloud, hoàn tất một vòng đời soát vé cực kỳ an toàn và khép kín."*

---

## 💡 Mẹo nhỏ để Video chuyên nghiệp hơn:
1. **Âm thanh:** Nếu có thể, hãy đảm bảo âm thanh "Bíp" khi quét vé lọt vào video (tiếng Bíp báo hợp lệ và Bíp lỗi khác nhau sẽ rất ấn tượng).
2. **Log Screen:** Lúc quay Máy Quét soát vé, hãy cố gắng lấy luôn hình ảnh màn hình Máy Trưởng đang nhảy các dòng text Logs (ví dụ: *Đang kiểm tra mã 123... Cập nhật DB nội bộ... Trả kết quả VALID...*) để minh chứng rằng công nghệ truyền tải đang thực sự chạy ngầm phía sau thay vì chỉ là Fake UI.
