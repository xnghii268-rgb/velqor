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
  - `tokens/ThemeTokens.java`: Hệ thống token màu Apple (Dark/Light/Auto, 7 màu nhấn), kích thước, alpha, radius, stroke thích ứng, scrim mờ.
  - `archive/ZipManager.java`: Động cơ nén, giải nén, duyệt danh mục ZIP không cần giải nén (`listZipEntries`), trích xuất từng tệp.
  - `files/`: `FileRepository`, `DirectoryWatcher` (tự làm mới ngầm 3s + FileObserver), `TextFileIO`, `FileCreator`, `ThumbnailCache`, `TrashManager`, `StorageScanner`.
  - `ui/`: `FloatingTabBar` (3 tab: Tệp, Nén, Cài đặt icon-only, giọt nước mỏng), `IosPullScrollView` (kéo làm mới đàn hồi), `IosSwitchView` (công tắc iOS native), `SettingsViewHelper` (Profile Card & Grouped List Cupertino), `BlurBackdropView` (kính mờ thích ứng), `ImageViewerActivity`, `TextEditorActivity`, `DonutChartView`, `HeaderView` (spring touch + breadcrumbs kính), `FileRowView` (dòng 58dp & thẻ Grid 18dp), `IosSheet` (modal Cupertino chuẩn).
  - `MainActivity.java`: Điều phối vòng đời, 3 tab chính, đệm cuộn thích ứng, cử chỉ lùi hệ thống.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Build toàn bộ: `bash tools/build_apk.sh` (tự động biên dịch tài nguyên AAPT2, javac, d8 và ký apksigner).
- Test suite: `bash tools/test_runner.sh` (80/80 tests passed - 100%).
- Đích bàn giao: `/sdcard/AntiGravity/velqor-v2.1.1.apk` (520 KB).

## 4. Trạng thái hiện tại (v2.1.1 - Build 16)
- **THIẾT KẾ APPLE LIQUID GLASS TOÀN DIỆN**:
  - Tích hợp tri thức từ `ui-ux-pro-max`: Bảng màu semantic thích ứng tuyệt đối giữa Dark và Light Mode (tương phản WCAG 2.2).
  - Vùng chạm tối thiểu đạt chuẩn Apple (58dp cho list row, 44x44dp cho icon buttons, 48dp cho action rows).
  - Phản hồi xúc giác vi mô (micro-interaction spring damping 0.92x - 0.95x) trên header buttons, shortcut chips, file cards và list rows.
  - Tab Nén nâng cấp lên Hero Card với squircle icon nén và hiển thị danh mục tệp nén trực quan.
  - Thẻ Grid Card bo góc 18dp với viền kính thích ứng và squircle 56x56dp bo 16dp.
- **TÍNH NĂNG 1 — Kéo xuống để tải lại (Pull-to-refresh)**:
  - `IosPullScrollView`: Damping 0.45, spinner iOS mượt mà, rung haptic nhẹ khi vượt ngưỡng.
- **TÍNH NĂNG 2 — Tự làm mới ngầm mỗi 3 giây (Silent Auto-refresh)**:
  - `DirectoryWatcher`: Background scan 3s kết hợp FileObserver, diff metadata êm ái giữ nguyên vị trí cuộn.
- **Tuân thủ quy tắc**: 100% không chứa bất kỳ comment nào trong toàn bộ mã nguồn Java.
