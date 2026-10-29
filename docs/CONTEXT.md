# CONTEXT — VELQOR PRO (v2.0.10)

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
  - `tokens/ThemeTokens.java`: Toàn bộ token màu sắc, kích thước, alpha, radius.
  - `security/`: `VaultManager` (AES-256-GCM + PBKDF2), `HashUtils` (SHA-256 stream).
  - `archive/`: `ZipManager` (Nén, giải nén, chống Zip Slip).
  - `files/`: `FileRepository` (Duyệt, copy, move, xóa, đổi tên, sắp xếp), `TextFileIO` (Đọc/ghi text, BOM, LF/CRLF, zip edit), `FileCreator` (Tạo tệp/mẫu tệp chuẩn, validate, chống trùng tên), `ThumbnailCache`.
  - `ui/`: `ImageViewerActivity`, `TextEditorActivity`, `NewItemSheet`, `HeaderView`, `FileRowView`, `FloatingTabBar`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, cử chỉ lùi hệ thống, điều hướng tệp/thư mục.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Link resources: `aapt2 link --version-code 12 --version-name "2.0.10"`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (70/70 tests passed - 100%).

## 4. Trạng thái hiện tại (v2.0.10 - Build 12)
- **MILESTONE EDITOR-POLISH-1 & CREATE-FILE hoàn tất**:
  1. **TextEditorActivity**:
     - Xóa triệt để bug phantom line numbers 972–1013: Chỉ đánh số dòng logic thực, dòng ngắt tự động để trống trong gutter, độ rộng gutter co giãn động.
     - Thanh tìm kiếm thu gọn 1 hàng 40dp, hàng 2 mở rộng Thay thế & chip lọc `Aa` / `.*`.
     - Thanh phím phụ dính sát trên bàn phím (WindowInsets IME), tự động đóng cặp ngoặc/nháy khi gõ hoặc bao quanh khối chọn, phím Tab 4 khoảng trắng.
     - Header chuẩn Apple: nút Quay lại, tên tệp + chấm `●` báo chưa lưu, nút Tìm kiếm, menu `⋯` (Đi tới dòng, Tự xuống dòng, Cỡ chữ, Lưu bản sao, Chia sẻ), nút Lưu dạng pill sáng khi có thay đổi.
     - Tự xuống dòng: Bật mặc định cho `.md, .txt, .log`, tắt mặc định cho code, ghi nhớ cài đặt theo đuôi tệp.
  2. **Tạo tệp & thư mục chuyên nghiệp**:
     - Nút `+` trên Header & mục trong Sheet "Tùy chọn": Sheet iOS "Tạo mới" (Thư mục, Text, Markdown, HTML, JSON, Shell, Python, Tệp trống).
     - Hộp thoại nhập tên kèm chip chọn đuôi nhanh, kiểm tra trùng tên gợi ý "Tệp mới (2).txt", cảnh báo tệp ẩn, tạo nguyên tử.
  3. **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.10.apk` (489 KB).
