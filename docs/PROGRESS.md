# VELQOR — TIẾN ĐỘ PHÁT TRIỂN (v2.0.1)

## Trạng thái hiện tại
- **Phiên bản phát hành**: **v2.0.1 (versionCode 3)** — VELQOR PRO
- **File APK cài đặt**: `/sdcard/AntiGravity/velqor-v2.0.1.apk` (366 KB)
- **Thư mục dự án**: `/sdcard/AntiGravity/velqor`
- **Repo GitHub**: `https://github.com/xnghii268-rgb/velqor`

## Các bản sửa lỗi & nâng cấp v2.0.1
1. **Fix Crash FileProvider**: Tự phát triển `VelqorFileProvider` kế thừa thuần `ContentProvider` của Android SDK, loại bỏ triệt để lỗi `ClassNotFoundException: androidx.core.content.FileProvider`.
2. **Icon To & Đầy Đặn (Adaptive Icon)**:
   - Cắt bỏ 80px khoảng trắng trong suốt thừa của file PNG gốc.
   - Bổ sung cấu hình Adaptive Icon chuẩn Android (`res/mipmap-anydpi-v26/ic_launcher.xml`) với nền đen OLED `#000000` và foreground 108dp.
   - Icon trên màn hình chính giờ đây hiển thị to, tràn viền và sắc nét chuẩn Apple.
3. **Tự động tăng phiên bản**: Đã bump lên `v2.0.1` (`versionCode = 3`).
4. **Vệ sinh**: Xóa sạch bản `v2.0.0` cũ, giữ `/sdcard/Download` sạch sẽ 100%.
