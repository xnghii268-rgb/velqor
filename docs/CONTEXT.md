# CONTEXT — VELQOR PRO (v2.1.2)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, xem ảnh, soạn thảo code/text, nén/giải nén chuẩn Apple iOS Liquid Glass tối giản, mượt mà và đẳng cấp trên Android.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Android View Native (0 comment, 0 mock data, AOSP CLI tools).
- **Thư viện tích hợp**: Shizuku Client v13.1.5 (AAR trích xuất: api, provider, aidl, shared).
- **Công cụ build**: AOSP CLI (`aapt2`, `javac`, `d8`, `apksigner`).
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **Cấu trúc phân tầng**:
  - `tokens/ThemeTokens.java`: Token màu Apple (Dark/Light/Auto, 7 màu nhấn), kích thước, stroke, scrim.
  - `archive/ZipManager.java`: Nén, giải nén, duyệt entry ZIP không giải nén (`listZipEntries`).
  - `files/`: `FileRepository`, `DirectoryWatcher` (refresh ngầm 3s + FileObserver), `WirelessDebugManager` (Shizuku Binder IPC, quét thật `/Android/data` & `/Android/obb` qua UID 2000 shell, `ShizukuFile`), `TextFileIO`, `FileCreator`, `ThumbnailCache`, `TrashManager`, `StorageScanner`.
  - `ui/`: `FloatingTabBar` (giọt nước Liquid Glass vật lý chất lỏng), `IosPullScrollView`, `IosSwitchView`, `SettingsViewHelper`, `BlurBackdropView`, `ImageViewerActivity`, `TextEditorActivity`, `DonutChartView`, `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, đăng ký Shizuku binder/permission listener, quản lý sheet và tab.
- **Quyền & Provider**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES`, `VelqorFileProvider`, `rikka.shizuku.ShizukuProvider`.

## 3. Cách chạy & Đóng gói
- Build toàn bộ: `bash tools/build_apk.sh` (tự động aapt2, javac, d8 đa thư viện và ký apksigner).
- Test suite: `bash tools/test_runner.sh` (80/80 tests passed - 100%).
- Đích bàn giao: `/sdcard/AntiGravity/velqor-v2.1.2.apk` (549 KB).

## 4. Trạng thái hiện tại (v2.1.2 - Build 17)
- **Phương án 1 — Shizuku Binder IPC (Truy cập thật 100% Android/data & obb)**:
  - Khởi tạo `rikka.shizuku.ShizukuProvider` kết nối trực tiếp với Shizuku Daemon.
  - Chạy `ls -1pA` qua Shizuku process UID 2000 shell trả về dữ liệu thật của thiết bị, xóa 100% dữ liệu mock.
  - Lớp `ShizukuFile extends File` nhận biết chính xác thư mục package từ dấu `/` của `ls -1p`.
  - Quản lý trạng thái và quyền Shizuku trực tiếp từ sheet Cài đặt; hỗ trợ nút mở Shizuku, Cài đặt nhà phát triển và cấp quyền Shizuku.
- **Thanh điều hướng nổi Giọt nước chuẩn Apple Liquid Glass**:
  - Viên thuốc giọt nước 58x42dp bo tròn 21dp căn giữa icon tab.
  - Động lực học chất lỏng `ValueAnimator`: dãn dài `scaleX` đạt đỉnh 1.25x khi bay và nén squashing khi tiếp đất.
  - Chất liệu kính lỏng cao cấp: gradient màu nhấn mờ, vệt sáng phản quang vòm trên, khúc xạ ánh sáng đáy và viền specular rim stroke 0.75dp.
  - Phản hồi xúc giác biến dạng giọt nước khi nhấn và nhấc ngón tay.
- **Tuân thủ quy tắc**: 100% không chứa bất kỳ comment nào trong toàn bộ mã nguồn Java.
