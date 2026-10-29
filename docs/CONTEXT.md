# CONTEXT — VELQOR PRO (v2.0.5)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, nén/giải nén và bảo mật thay thế ZArchiver, phong cách Apple iOS tối giản, mượt mà và đẳng cấp.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Kotlin 1.9.22.
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **UI Architecture**:
  - Tầng hiển thị: Custom iOS ModalBottomSheet (Full-width, Gravity.BOTTOM, Scrim 45%, giới hạn max-height 75% màn hình, cuộn mượt), Inset Grouped List (0.5dp inset dividers, semantic folder colors), Floating Liquid Glass Tab Bar với water-drop indicator co giãn xuân mượt (scaleX 1.22, scaleY 0.92, không clip góc).
  - Tầng token: `:core:designsystem` (Liquid Glass tokens, fallback `COLOR_SURFACE_GLASS` 0.96 alpha, viền specular 1dp gradient, elevation nhẹ 6dp).
  - Tầng Edge-to-Edge: `SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | HIDE_NAVIGATION`, status bar trong suốt 100%, Large Title 34sp SemiBold thu nhỏ inline 17sp khi cuộn.
- **Động cơ**: Pure Android SDK, ZipInputStream/ZipOutputStream chống Zip Slip, AES-256-GCM Vault streaming PBKDF2, SHA-256 Hasher.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE` với FileProvider bảo mật (`VelqorFileProvider`).

## 3. Cách chạy & Đóng gói
- Biên dịch tài nguyên: `aapt2 compile` + `aapt2 link --version-code 7 --version-name "2.0.5"`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (19/19 test passed).

## 4. Trạng thái hiện tại (v2.0.5 - Build 7)
- **Hoàn thiện Liquid Glass & chi tiết iOS (MILESTONE UI-REFIT-4)**:
  - Đường blur: Fallback `surfaceElevated` alpha 0.96 (`0xF41C1C1E`) kết hợp viền specular 1dp phản quang và elevation 6dp. Lý do: App thuần Java View Android không có RenderScript/Haze runtime, tránh lỗi "trong suốt nhưng không mờ".
  - Giọt nước & Tab bar: 5 tab chia đều 100%, giọt nước cao 56dp (chiều cao thanh - 8dp) căn giữa tab, icon 24dp, nhãn 10sp cách icon 2dp, co giãn mượt mà không bị clip góc.
  - Danh sách tệp: Inset separator 0.5dp (`COLOR_SEPARATOR_INSET`), thư mục hệ thống có màu nhận diện nhẹ, padding đáy 112dp + navBar không che mục cuối.
  - Sheet Menu: Chiều cao tối đa 75% màn hình, icon Chi tiết outline info chuẩn, nhãn "Mã hóa vào Vault" ngắn gọn.
  - Output APK: `/sdcard/AntiGravity/velqor-v2.0.5.apk` (440 KB).
