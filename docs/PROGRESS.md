# VELQOR — TIẾN ĐỘ PHÁT TRIỂN (v2.0.3)

## Trạng thái hiện tại
- **Phiên bản phát hành**: **v2.0.3 (versionCode 5)** — VELQOR PRO
- **File APK cài đặt**: `/sdcard/AntiGravity/velqor-v2.0.3.apk` (440 KB)
- **Thư mục dự án**: `/sdcard/AntiGravity/velqor`
- **Repo GitHub**: `https://github.com/xnghii268-rgb/velqor`

## Các nâng cấp chính v2.0.3

### 1. Milestone UI-REFIT-2 — Menu thao tác ModalBottomSheet kiểu iOS
- **Xóa 100% `AlertDialog`**: Toàn bộ hộp thoại được thay thế bằng hệ thống `ModalBottomSheet` và `ActionSheet` chuẩn Apple (bo góc 24dp, grabber bar 36x5dp màu `separator`, scrim 0.4 alpha, trượt mở/đóng spring mượt mà).
- **Hành động theo 4 nhóm chuyên biệt**:
  - *Nhóm 1*: Mở bằng… (Intent ACTION_VIEW qua FileProvider), Chia sẻ (Intent ACTION_SEND), Chi tiết (sheet hiển thị size, path, timestamp, permissions).
  - *Nhóm 2*: Sao chép (Copy to clipboard), Di chuyển (Move to clipboard), Đổi tên (Input sheet có TextField bo 12dp, kiểm tra trùng tên thời gian thực).
  - *Nhóm 3*: Nén thành ZIP, Giải nén (chỉ hiện khi là archive, có tùy chọn giải nén tại chỗ hoặc vào thư mục riêng), Tính SHA-256 (sheet hiển thị hex code kèm nút sao chép), Mã hóa vào Vault (AES-GCM).
  - *Nhóm 4*: Xóa (khối bo 16dp màu danger red, xác nhận bằng action sheet riêng biệt gồm nút Xóa đỏ và nút Hủy tách rời).
- **Xử lý tệp tin hàng loạt (Multi-Selection)**: Bottom sheet hiển thị "Đã chọn N mục" và chỉ cung cấp các hành động theo lô (Sao chép, Di chuyển, Nén ZIP, Xóa).
- **Thư mục mới**: Sheet nhập liệu kiểu iOS, tự động kiểm tra trùng lặp tên thư mục.

### 2. Milestone UI-REFIT-3 — Thanh điều hướng nổi "Liquid Glass"
- **Floating Tab Bar hình viên thuốc (Pill)**: Cao 64dp, margin ngang 20dp, bo tròn 32dp hoàn toàn, nổi phía trên nội dung và căn giữa màn hình (tối đa rộng 420dp).
- **Hiệu ứng Liquid Glass**: Nền `surfaceElevated` bán trong suốt (`0xE61C1C1E`), viền specular phản quang mảnh 1dp dạng gradient, đổ bóng mềm nhẹ, nội dung cuộn xuyên phía dưới thanh tab.
- **Chỉ báo "giọt nước" (Water-drop indicator)**: Viên pill kính trượt sau tab đang chọn với hiệu ứng co giãn giọt nước (spring stretch & snap), kèm phản hồi rung haptic rất nhẹ.
- **Header Cupertino**: Khoảng cách cách status bar 8dp, Large Title 34sp SemiBold thu nhỏ inline 17sp khi cuộn, nền chuyển sang mờ frosted với đường kẻ mảnh dưới, breadcrumbs dính ngay dưới header.

### 3. Icon App Đẳng Cấp Chuẩn Apple (`img-20261029-184414-1.png`)
- Tích hợp icon mới từ ảnh test người dùng gửi: Folder đen mờ 3D, logo `V` kim loại ánh bạc chém vát góc và tab màu xanh điện tử.
- Thuật toán cắt bỏ toàn bộ 45px khoảng lề rỗng trong suốt thừa, phóng to căng tràn khung hình.
- Xuất đầy đủ bộ mipmap densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) và Adaptive Icon Android 8+ giúp icon trên màn hình chính to rõ, đầy đặn, không còn bị nhỏ hay lọt thỏm.
