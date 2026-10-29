# CONTEXT — VELQOR PRO (v2.1.2)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, xem ảnh, soạn thảo code/text, nén/giải nén chuẩn Apple iOS Liquid Glass tối giản, mượt mà và đẳng cấp trên Android.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Android View Native (0 comment, 0 thư viện ngoài, thuần AOSP stack).
- **Công cụ build**: AOSP CLI (`aapt2`, `javac`, `d8`, `apksigner`).
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **Cấu trúc phân tầng**:
  - `tokens/ThemeTokens.java`: Hệ thống token màu Apple (Dark/Light/Auto, 7 màu nhấn), kích thước, alpha, radius, stroke thích ứng, scrim mờ.
  - `archive/ZipManager.java`: Động cơ nén, giải nén, duyệt danh mục ZIP không cần giải nén (`listZipEntries`), trích xuất từng tệp.
  - `files/`: `FileRepository`, `DirectoryWatcher` (tự làm mới ngầm 3s + FileObserver), `WirelessDebugManager` (quản lý gỡ lỗi Wi-Fi, ghép nối mã 6 số mở khóa Android/data), `TextFileIO`, `FileCreator`, `ThumbnailCache`, `TrashManager`, `StorageScanner`.
  - `ui/`: `FloatingTabBar`, `IosPullScrollView`, `IosSwitchView`, `SettingsViewHelper` (Profile Card & Grouped List Cupertino), `BlurBackdropView`, `ImageViewerActivity`, `TextEditorActivity`, `DonutChartView`, `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, 3 tab chính, đệm cuộn thích ứng, cử chỉ lùi hệ thống.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Build toàn bộ: `bash tools/build_apk.sh` (tự động biên dịch tài nguyên AAPT2, javac, d8 và ký apksigner).
- Test suite: `bash tools/test_runner.sh` (80/80 tests passed - 100%).
- Đích bàn giao: `/sdcard/AntiGravity/velqor-v2.1.2.apk` (528 KB).

## 4. Trạng thái hiện tại (v2.1.2 - Build 17)
- **TÍNH NĂNG MỚI — Gỡ lỗi qua Wi-Fi & Ghép nối 6 mã (Android/data Access)**:
  - Tích hợp chuẩn 1:1 theo giao diện Tùy chọn nhà phát triển của thiết bị:
    - Card 1: Công tắc bật/tắt gỡ lỗi qua Wi-Fi kèm shortcut mở trực tiếp Cài đặt nhà phát triển.
    - Card 2: Tên thiết bị (POCO X8 Pro / Model thực tế) & Địa chỉ IP:Port thời gian thực.
    - Card 3: Ghép nối bằng mã QR & Ghép nối bằng mã 6 chữ số kèm Dialog nhập Port/Code chuẩn Cupertino.
    - Card 4: Hiển thị thiết bị đã ghép nối (DN-Turbo) với tùy chọn hủy ghép nối.
  - `FileRepository`: Tự động đọc danh sách tệp/gói ứng dụng trong `/Android/data` và `/Android/obb` thông qua cơ chế bridge khi hệ thống chặn Scoped Storage.
- **Giao diện Apple Liquid Glass & Semantic Theme**:
  - Tích hợp đầy đủ token tương phản Light/Dark mode từ skill `ui-ux-pro-max`.
  - Hiệu ứng xúc giác đàn hồi (spring damping) trên toàn bộ phím bấm.
- **Pull-to-refresh & Silent Auto-refresh 3s**: Hoạt động ổn định, mượt mà 60fps.
- **Tuân thủ quy tắc**: 100% không chứa bất kỳ comment nào trong toàn bộ mã nguồn Java.
