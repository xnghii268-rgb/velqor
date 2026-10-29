# CONTEXT — VELQOR PRO (v2.0.3)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, nén/giải nén và bảo mật thay thế ZArchiver, phong cách Apple iOS tối giản, mượt mà và đẳng cấp.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Kotlin 1.9.22.
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **UI Architecture**:
  - Tầng hiển thị: Custom iOS ModalBottomSheet, Inset Grouped List, Floating Liquid Glass Tab Bar với water-drop indicator.
  - Tầng token: `:core:designsystem` (Color, Shape, Type, Theme, Liquid Glass tokens).
- **Động cơ**: Pure Android SDK, ZipInputStream/ZipOutputStream, AES-256-GCM Vault streaming PBKDF2, SHA-256 Hasher.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE` với FileProvider bảo mật (`VelqorFileProvider`).

## 3. Cách chạy & Đóng gói
- Biên dịch tài nguyên: `aapt2 compile` + `aapt2 link`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (19/19 test passed).

## 4. Trạng thái hiện tại (v2.0.3 - Build 5)
- **UI-REFIT-2**: 100% `AlertDialog` đã được thay thế bằng `ModalBottomSheet` và `ActionSheet`.
  - 4 nhóm hành động (Mở/Chia sẻ/Chi tiết, Sao chép/Di chuyển/Đổi tên, Nén/Giải nén/SHA-256/Vault, Xóa).
  - Sheet nhập liệu có kiểm tra trùng tên thời gian thực cho Đổi tên & Thư mục mới.
  - Action sheet xác nhận xóa tách nút Hủy.
- **UI-REFIT-3**: Thanh điều hướng nổi "Liquid Glass" (Pill 64dp, margin 20dp, bo 32dp) với chỉ báo giọt nước trượt spring.
- **Icon App**: Cập nhật icon 3D folder + chữ V kim loại căng nét từ ảnh test người dùng.
- **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.3.apk` (440 KB).
