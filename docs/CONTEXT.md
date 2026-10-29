# CONTEXT — VELQOR PRO (v2.0.6)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, nén/giải nén và bảo mật thay thế ZArchiver, phong cách Apple iOS tối giản, mượt mà và đẳng cấp.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Android View Native (quyết định stack Termux 2026-10-29).
- **Công cụ build**: AOSP CLI (`aapt2`, `javac`, `d8`, `apksigner`).
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **Cấu trúc phân tầng (MILESTONE ARCH-FIX-1)**:
  - `tokens/ThemeTokens.java`: Tập trung toàn bộ token màu sắc, kích thước, alpha, radius, không hard-code.
  - `security/`: `VaultManager` (AES-256-GCM + PBKDF2), `HashUtils` (SHA-256 stream).
  - `archive/`: `ZipManager` (Nén, giải nén, chống Zip Slip).
  - `files/`: `FileRepository` (Duyệt, copy, move, xóa, đổi tên, chống trùng tên, tìm kiếm bỏ dấu tiếng Việt).
  - `ui/`: `BlurBackdropView` (Hardware RenderNode/RenderEffect blur API 31+ & fallback), `FloatingTabBar`, `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, quyền và kết nối các thành phần.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Link resources: `aapt2 link --version-code 8 --version-name "2.0.6"`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (31/31 tests passed).

## 4. Trạng thái hiện tại (v2.0.6 - Build 8)
- **Tách code hoàn tất**: `MainActivity.java` giảm từ 2,410 dòng xuống 1,303 dòng.
- **Blur thật trên API 31+**: `BlurBackdropView` dùng `RenderNode` + `RenderEffect.createBlurEffect(24f, 24f)` vẽ lại backdrop thời gian thực khi cuộn; API < 31 giữ fallback `COLOR_SURFACE_GLASS` alpha 0.96.
- **Bộ kiểm thử mở rộng**: Tăng từ 19 lên 31 test cases bao phủ logic thật (Copy/Move/Delete/Rename/Duplicate protection, NIST SHA-256 vectors, Zip Slip).
- **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.6.apk` (448 KB).
