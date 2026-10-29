# CONTEXT — VELQOR PRO (v2.0.9)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, xem ảnh, sửa mã nguồn/text, nén/giải nén và bảo mật phong cách Apple iOS tối giản, mượt mà và đẳng cấp.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Android View Native (quyết định stack Termux 2026-10-29).
- **Công cụ build**: AOSP CLI (`aapt2`, `javac`, `d8`, `apksigner`).
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **Cấu trúc phân tầng**:
  - `tokens/ThemeTokens.java`: Toàn bộ token màu sắc, kích thước, alpha, radius, không hard-code.
  - `security/`: `VaultManager` (AES-256-GCM + PBKDF2), `HashUtils` (SHA-256 stream).
  - `archive/`: `ZipManager` (Nén, giải nén, chống Zip Slip).
  - `files/`: `FileRepository` (Duyệt, copy, move, xóa, đổi tên, chống trùng tên, tìm kiếm bỏ dấu tiếng Việt), `TextFileIO` (UTF-8/BOM/CRLF/UTF-16, paging >10MB, atomic save, update ZIP entry text), `ThumbnailCache` (RAM + 50MB Disk LRU).
  - `ui/`: `ImageViewerActivity`, `TextEditorActivity`, `ThumbnailLoader`, `BlurBackdropView`, `FloatingTabBar` (hỗ trợ `selectTab` đồng bộ giọt nước khi back tab), `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, điều hướng Back cử chỉ hệ thống (Predictive Back, hardware key, virtual back), cài đặt APK trực tiếp (`installApk`).
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Link resources: `aapt2 link --version-code 11 --version-name "2.0.9"`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (53/53 tests passed).

## 4. Trạng thái hiện tại (v2.0.9 - Build 11)
- **HOTFIX System Gesture Back Navigation hoàn tất**:
  - Khắc phục lỗi vuốt cạnh màn hình để Back bị văng thoát app thay vì lùi về thư mục cha:
    1. Đăng ký `OnBackInvokedCallback` (Android 13+ / API 33+) đón trọn cử chỉ vuốt Predictive Back từ cả 2 cạnh viền.
    2. Override `onBackPressed()` và `onKeyDown(KEYCODE_BACK)` cho toàn bộ các đời máy và phím ảo/vật lý.
    3. Trình điều hướng phân tầng: Hủy chọn nhiều -> Đóng tìm kiếm -> Quay lại tab Tệp -> Lùi thư mục cha (`mCurrentDir.getParentFile()`) -> Chạm lần 2 trong 2s mới thoát app.
- **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.9.apk` (472 KB).
- **Vệ sinh thư mục**: Đã xóa `velqor-v2.0.8.apk`, trong `/sdcard/AntiGravity/` chỉ còn `velqor-v2.0.9.apk` và `termuxpro-v0.120.14.apk`.