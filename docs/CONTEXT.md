# CONTEXT — VELQOR PRO (v2.1.1)

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
  - `tokens/ThemeTokens.java`: Hệ thống token màu Apple (Dark/Light/Auto, 7 màu nhấn), kích thước, alpha, radius, specular rim.
  - `archive/ZipManager.java`: Động cơ nén, giải nén, duyệt danh mục ZIP không cần giải nén (`listZipEntries`), trích xuất từng tệp.
  - `files/`: `FileRepository`, `DirectoryWatcher` (tự làm mới ngầm 3s + FileObserver), `TextFileIO`, `FileCreator`, `ThumbnailCache`, `TrashManager`, `StorageScanner`.
  - `ui/`: `FloatingTabBar` (3 tab: Tệp, Nén, Cài đặt icon-only), `IosPullScrollView` (kéo làm mới đàn hồi), `IosSwitchView` (công tắc iOS native), `SettingsViewHelper` (Profile Card & Grouped List Cupertino), `BlurBackdropView`, `ImageViewerActivity`, `TextEditorActivity`, `DonutChartView`, `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, 3 tab chính, đệm cuộn thích ứng, cử chỉ lùi hệ thống.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Build toàn bộ: `bash tools/build_apk.sh` (tự động biên dịch tài nguyên AAPT2, javac, d8 và ký apksigner).
- Test suite: `bash tools/test_runner.sh` (80/80 tests passed - 100%).
- Đích bàn giao: `/sdcard/AntiGravity/velqor-v2.1.1.apk` (520 KB).

## 4. Trạng thái hiện tại (v2.1.1 - Build 16)
- **TÍNH NĂNG 1 — Kéo xuống để tải lại (Pull-to-refresh)**:
  - `IosPullScrollView`: Kéo từ đỉnh danh sách có lực cản đàn hồi (damping 0.45), vòng cung loading iOS quay mượt 60fps, rung haptic nhẹ khi vượt ngưỡng kích hoạt 60dp.
  - Hoạt động mượt mà cả khi thư mục rỗng hoặc danh sách ngắn. Tự động thu về mượt khi tải xong.
- **TÍNH NĂNG 2 — Tự làm mới ngầm mỗi 3 giây (Silent Auto-refresh)**:
  - `DirectoryWatcher`: Quét ngầm mỗi 3s trên background thread kết hợp `FileObserver` (CREATE, DELETE, MOVED, CLOSE_WRITE) với debounce 300ms.
  - Diff metadata (`path`, `length`, `lastModified`, `isDir`): Chỉ cập nhật khi có thay đổi thực tế, giữ nguyên vị trí cuộn `scrollY`, không giật lag.
  - Tự động tạm dừng khi app ở nền, màn hình tắt, đang chọn nhiều mục, hoặc đang mở sheet/dialog. Quét ngay 1 lần khi quay lại.
- **Sửa triệt để lỗi đè Header (Phương án 2)**:
  - Tự động điều chỉnh `topPadding` (144dp khi ở tab Tệp có breadcrumbs + shortcuts; 68dp khi ở Nén/Cài đặt).
  - Ẩn/hiện breadcrumbs và shortcuts bar đồng bộ theo tab, không che lấp tệp đầu tiên.
- **Thiết kế lại màn hình Cài đặt chuẩn Apple iOS Liquid Glass**:
  - App Profile Card bo 24dp với logo chữ V gradient, phân nhóm Grouped Card chuẩn iOS (`GIAO DIỆN`, `LƯU TRỮ`, `HIỆU NĂNG`, `THÔNG TIN`), công tắc toggle `IosSwitchView` xanh lá 51x31dp.
- **Tuân thủ quy tắc**: 100% không chứa bất kỳ comment nào trong toàn bộ mã nguồn Java.
