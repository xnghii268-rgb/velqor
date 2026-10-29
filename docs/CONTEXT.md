# CONTEXT — VELQOR PRO (v2.0.11)

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
  - `files/`: `FileRepository` (Duyệt, copy, move, xóa, đổi tên, sắp xếp), `TextFileIO`, `FileCreator`, `ThumbnailCache`.
  - `ui/`: `FloatingTabBar`, `BlurBackdropView`, `ImageViewerActivity`, `TextEditorActivity`, `NewItemSheet`, `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, cử chỉ lùi hệ thống, ẩn/hiện thanh nổi khi cuộn, lớp mờ gradient đáy.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Link resources: `aapt2 link --version-code 13 --version-name "2.0.11"`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (70/70 tests passed - 100%).

## 4. Trạng thái hiện tại (v2.0.11 - Build 13)
- **MILESTONE NAVBAR-POLISH hoàn tất**:
  1. **Sửa viền ám đỏ & Shadow**:
     - Loại bỏ mọi nguy cơ ám màu ở góc trên-trái. Viền kính dùng `LinearGradient` trắng alpha thuần khiết: 0.35 ở trên-trái → 0.05 ở dưới-phải.
     - Đặt bóng đổ trung tính chuẩn đen `0x20000000`/`0x26000000` qua `setOutlineAmbientShadowColor` / `setOutlineSpotShadowColor` (API 28+).
  2. **Giọt nước mỏng, nhẹ (WaterDropView)**:
     - Chiều cao 48dp (căn giữa dọc thanh 64dp), rộng bằng tab - 8dp, bo tròn hoàn toàn 24dp.
     - Nền accent alpha 0.14, viền 0.5dp trắng alpha 0.25 (bỏ viền xanh dày), highlight nửa trên gradient trắng 0.10 → 0.
     - Hiệu ứng co giãn khi trượt (`scaleX 1.20, scaleY 0.94`) và phồng nhẹ khi nhấn giữ.
  3. **Icon & Nhãn chuẩn Apple**:
     - Tab chưa chọn: icon OUTLINE màu `secondaryLabel` (alpha ~0.60), nhãn cùng màu.
     - Tab đang chọn: icon FILLED màu accent, nhãn accent. Chuyển trạng thái crossfade 150ms mượt mà.
     - Bổ sung đủ 5 cặp vector outline/filled: Tệp, Nén, Vault, Hash, Cài đặt.
  4. **Lớp mờ dần đáy màn hình (Bottom Gradient Scrim)**:
     - Gradient trong suốt → 90% background, phủ dưới thanh nổi và trên danh sách, không chặn tương tác chạm vào list.
  5. **Kính thật (RenderEffect API 31+)**:
     - RenderNode blur 24dp, tint tối 0.75 + tint trắng 0.06, fallback 0.94-0.96 cho API < 31.
  6. **Tự động ẩn/hiện khi cuộn**:
     - Cuộn xuống nhanh: trượt xuống ẩn kèm giảm alpha 250ms. Cuộn lên, chạm đáy hoặc về đỉnh: hiện lại. Tôn trọng Reduce Motion.
  7. **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.11.apk` (497 KB).
