# VELQOR — TIẾN ĐỘ & QUYẾT ĐỊNH KIẾN TRÚC

## Quyết định stack: giữ Java + View vì build trên Termux; được chủ dự án xác nhận ngày 2026-10-29
- **Ngôn ngữ**: Java 17 OpenJDK (chạy trực tiếp trên Termux).
- **UI Toolkit**: Android View Native (không dùng Jetpack Compose runtime vì quá nặng và dễ bị OOM trên Termux).
- **Công cụ build**: AOSP CLI tools (`aapt2`, `javac`, `d8`, `apksigner`) thay cho Gradle Daemon để build offline 100% chỉ trong 3 giây.
- **SDK Target**: `minSdkVersion = 26`, `targetSdkVersion = 34`, `compileSdkVersion = 34`.
- **Thư viện ngoài**: 0 thư viện bên ngoài (dùng 100% Android Platform SDK + Java Standard Libraries).
- **Độ lệch với master prompt ban đầu**: Lệch ở chỗ không dùng Kotlin/Compose đa module qua Gradle mà dùng Java + Android View CLI vì lý do tài nguyên phần cứng Termux hạn chế, tránh OOM kill và phụ thuộc mạng. Cam kết giữ nguyên stack này, không tự ý thay đổi.

## Trạng thái hiện tại (v2.0.5 - Build 7)
- **APK hiện tại**: `/sdcard/AntiGravity/velqor-v2.0.5.apk` (440 KB, versionCode 7).
- **Số dòng nguồn**:
  - `MainActivity.java`: 2,410 dòng (đang chuẩn bị phân tách theo MILESTONE ARCH-FIX-1).
  - `VelqorFileProvider.java`: 76 dòng.
  - `VelqorApplication.java`: 17 dòng.
  - `TestRunner.java`: 291 dòng (19 test cases pass 100%).
- **UI Milestone hoàn thành**:
  - `UI-REFIT-2`: Thay 100% AlertDialog bằng ModalBottomSheet kiểu iOS.
  - `UI-REFIT-3`: Floating Liquid Glass Tab Bar với giọt nước trượt xuân.
  - `UI-REFIT-4`: Edge-to-edge trong suốt, phân cách 0.5dp, màu thư mục hệ thống.

## Lộ trình ARCH-FIX-1 & FEATURE-PACK-1
1. **ARCH-FIX-1**: Tách `MainActivity.java` thành các module `ui/`, `files/`, `security/`, `archive/`, `tokens/`, làm blur RenderEffect thật trên API 31+, bổ sung test cases thật.
2. **FEATURE-PACK-1**:
   - `F1`: Text Editor cao cấp (UTF-8/16/BOM/CRLF, undo/redo, regex, phím phụ, sửa file trong ZIP).
   - `F2`: Duyệt & sửa ZIP như thư mục (kiểu ZArchiver).
   - `F3`: Quick Look xem trước ảnh, text, ZIP, thumbnail.
   - `F4`: Thùng rác trong app, yêu thích, tìm kiếm nội dung text.
   - `F5`: Chia nhỏ/ghép file, tìm trùng lặp SHA-256, phân tích dung lượng.
   - `F6`: Cài đặt nâng cao & đánh bóng TalkBack.
