# CONTEXT — VELQOR PRO (v2.0.8)

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
  - `tokens/ThemeTokens.java`: Tập trung toàn bộ token màu sắc, kích thước, alpha, radius, không hard-code.
  - `security/`: `VaultManager` (AES-256-GCM + PBKDF2), `HashUtils` (SHA-256 stream).
  - `archive/`: `ZipManager` (Nén, giải nén, chống Zip Slip).
  - `files/`: `FileRepository` (Duyệt, copy, move, xóa, đổi tên, chống trùng tên, tìm kiếm bỏ dấu tiếng Việt), `TextFileIO` (đọc/ghi UTF-8/BOM/CRLF/UTF-16/Windows-1252, paging 64KB cho file >10MB, atomic save, update ZIP entry text trực tiếp), `ThumbnailCache` (RAM LruCache + Disk cache 50MB LRU, key prefix `v2_`).
  - `ui/`: `ImageViewerActivity` (TouchImageView pinch zoom/pan/double-tap/swipe-down/swipe-sibling/EXIF), `TextEditorActivity` (header kính, gutter số dòng, undo/redo, regex search/replace không crash, thanh phím phụ, auto-indent khi Enter, atomic save), `ThumbnailLoader` (ThreadPoolExecutor 3 luồng, icon thật cho APK, thumbnail ảnh/video/nhạc/PDF, clearColorFilter tránh ám màu toàn bộ ảnh), `BlurBackdropView` (RenderNode blur API 31+ & fallback), `FloatingTabBar`, `HeaderView`, `FileRowView` (placeholder 22dp căn giữa, không kéo giãn icon gây khối màu đặc), `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, quyền, cài đặt APK trực tiếp (`installApk`) và phân luồng tệp tin.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider` (trả đúng MIME type cho APK và các định dạng).

## 3. Cách chạy & Đóng gói
- Link resources: `aapt2 link --version-code 10 --version-name "2.0.8"`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (53/53 tests passed).

## 4. Trạng thái hiện tại (v2.0.8 - Build 10)
- **HOTFIX APK Installer & Real Color Thumbnails hoàn tất**:
  - Khai báo quyền `REQUEST_INSTALL_PACKAGES` trong `AndroidManifest.xml` và xử lý quyền ứng dụng không rõ nguồn trên Android 8.0+.
  - `VelqorFileProvider.getType()` trả về đúng `application/vnd.android.package-archive` cho file APK (thay vì cố định `octet-stream`).
  - `installApk`: Cài đặt APK trực tiếp khi chạm tệp trong danh sách và thêm tùy chọn "Cài đặt ứng dụng" ở đầu Action Sheet.
  - Sửa lỗi hiển thị Thumbnail: Gọi `clearColorFilter()` khi gán ảnh thumbnail và chuyển placeholder về 22dp căn giữa, loại bỏ hoàn toàn hiện tượng khối màu cam/xanh/đỏ đơn sắc.
- **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.8.apk` (468 KB).
- **Vệ sinh thư mục**: Đã xóa `velqor-v2.0.7.apk`, trong `/sdcard/AntiGravity/` chỉ còn `velqor-v2.0.8.apk` và `termuxpro-v0.120.14.apk`.