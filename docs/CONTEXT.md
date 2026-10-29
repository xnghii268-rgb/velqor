# CONTEXT — VELQOR PRO (v2.0.7)

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
  - `files/`: `FileRepository` (Duyệt, copy, move, xóa, đổi tên, chống trùng tên, tìm kiếm bỏ dấu tiếng Việt), `TextFileIO` (đọc/ghi UTF-8/BOM/CRLF/UTF-16/Windows-1252, paging 64KB cho file >10MB, atomic save, update ZIP entry text trực tiếp), `ThumbnailCache` (RAM LruCache + Disk cache 50MB LRU).
  - `ui/`: `ImageViewerActivity` (TouchImageView pinch zoom/pan/double-tap/swipe-down/swipe-sibling/EXIF), `TextEditorActivity` (header kính, gutter số dòng, undo/redo, regex search/replace không crash, thanh phím phụ, auto-indent khi Enter, atomic save), `ThumbnailLoader` (ThreadPoolExecutor 3 luồng, icon thật cho APK, thumbnail ảnh/video/nhạc/PDF), `BlurBackdropView` (RenderNode blur API 31+ & fallback), `FloatingTabBar`, `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, quyền và kết nối các thành phần (1,352 dòng).
- **Quyền**: `MANAGE_EXTERNAL_STORAGE` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Link resources: `aapt2 link --version-code 9 --version-name "2.0.7"`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (53/53 tests passed).

## 4. Trạng thái hiện tại (v2.0.7 - Build 9)
- **EDITOR-NOW hoàn tất**: Chạm ảnh mở `ImageViewerActivity` (hỗ trợ đầy đủ cử chỉ); chạm text mở `TextEditorActivity` (giữ BOM/CRLF, atomic temp save, paging >10MB, sửa file text trong ZIP trực tiếp).
- **THUMBNAILS hoàn tất**: Hiển thị icon thật APK qua `PackageArchiveInfo`, thumbnail ảnh/video qua `inSampleSize` + `ThumbnailUtils`, nhạc/PDF; bộ đệm LRU RAM + 50MB Disk.
- **Bộ kiểm thử mở rộng**: Tăng lên 53 bài test trong `TestRunner.java` bao phủ toàn bộ UTF-8 BOM, CRLF, UTF-16, Empty file, tiếng Việt có dấu, Regex an toàn, Zip entry editing và Cache key logic.
- **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.7.apk` (468 KB).
- **Vệ sinh thư mục**: Đã xóa toàn bộ APK cũ trong `/sdcard/AntiGravity/`, chỉ giữ lại `velqor-v2.0.7.apk` và `termuxpro-v0.120.14.apk`.