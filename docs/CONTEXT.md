# CONTEXT — VELQOR PRO (v2.0.4)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, nén/giải nén và bảo mật thay thế ZArchiver, phong cách Apple iOS tối giản, mượt mà và đẳng cấp.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Kotlin 1.9.22.
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **UI Architecture**:
  - Tầng hiển thị: Custom iOS ModalBottomSheet (Full-width, Gravity.BOTTOM, Scrim 45%, ScrollView), Inset Grouped List, Floating Liquid Glass Tab Bar với water-drop indicator.
  - Tầng token: `:core:designsystem` (Color, Shape, Type, Theme, Liquid Glass tokens).
- **Động cơ**: Pure Android SDK, ZipInputStream/ZipOutputStream, AES-256-GCM Vault streaming PBKDF2, SHA-256 Hasher.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE` với FileProvider bảo mật (`VelqorFileProvider`).

## 3. Cách chạy & Đóng gói
- Biên dịch tài nguyên: `aapt2 compile` + `aapt2 link`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (19/19 test passed).

## 4. Trạng thái hiện tại (v2.0.4 - Build 6)
- **Fix Hotfix ModalBottomSheet**: Khắc phục triệt để lỗi sheet bị co hẹp thành cột dọc ~50dp ở giữa màn hình (do window wrap_content trên Dialog mặc định). Sheet giờ đây mở tràn 100% chiều ngang màn hình, ghim sát đáy (Gravity.BOTTOM), cuộn mượt và trượt spring chuẩn Apple.
- **Floating Liquid Glass Tab Bar**: 5 tab rõ ràng, không bị che bởi dialog.
- **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.4.apk` (440 KB).
