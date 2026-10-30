# CONTEXT — VELQOR PRO (v2.2.0)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, xem ảnh, soạn thảo code/text, nén/giải nén chuẩn Apple iOS Liquid Glass tối giản, mượt mà và đẳng cấp trên Android.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Android View Native (0 comment, 0 mock data, AOSP CLI tools).
- **Thư viện tích hợp**: Shizuku Client v13.1.5 (AAR trích xuất: api, provider, aidl, shared).
- **Công cụ build**: AOSP CLI (`aapt2`, `javac`, `d8`, `apksigner`).
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **Cấu trúc phân tầng**:
  - `tokens/ThemeTokens.java`: Token màu Apple (Dark/Light/Auto, 7 màu nhấn), kính translucent 50%, stroke, scrim.
  - `archive/ZipManager.java`: Nén, giải nén, duyệt entry ZIP không giải nén (`listZipEntries`), trích xuất file lẻ.
  - `files/`: `FileRepository`, `DirectoryWatcher`, `WirelessDebugManager` (Shizuku Binder IPC, `rm -rf`, `mkdir`, `mv`, `cp` UID 2000 shell), `TextFileIO`, `FileCreator`, `ThumbnailCache`, `TrashManager`, `StorageScanner`.
  - `ui/`: `FloatingTabBar` (giọt nước Liquid Glass vật lý chất lỏng, 2 lớp shadow Canvas, WindowInsets ime/navBar), `BlurBackdropView`, `ImageViewerActivity`, `TextEditorActivity`, `DonutChartView`, `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, đăng ký Shizuku binder/permission, quản lý đa chọn, đổi tên hàng loạt, dọn dẹp bộ nhớ.

## 3. Cách chạy & Đóng gói
- Build toàn bộ: `bash tools/build_apk.sh` (aapt2, javac, d8 đa thư viện và ký apksigner).
- Test suite: `bash tools/test_runner.sh` (93/93 tests passed - 100%).
- Đích bàn giao: `/sdcard/AntiGravity/velqor-v2.2.0.apk` (555 KB).
- Patch: `/sdcard/AntiGravity/velqor-patch.zip`.

## 4. Trạng thái hiện tại (v2.2.0 - Build 19)
- **Thao tác Shizuku toàn diện trên Android/data & obb**:
  - Hỗ trợ đầy đủ lệnh xóa (`deleteRecursive`), tạo thư mục, đổi tên, sao chép, di chuyển qua Shizuku UID 2000 shell.
- **Thanh điều hướng nổi & Kính trong suốt**:
  - Tắt contrast-enforced (API 29+), loại bỏ scrim đen, thanh điều hướng Android và tab bar hoàn toàn trong suốt.
- **Chọn nhanh tệp & Thao tác 3 chấm nâng cao**:
  - Chạm trực tiếp icon tệp/thư mục để vào chế độ chọn đa mục ngay lập tức.
  - Menu 3 chấm: Giữ thư mục chỉ xóa tệp, Giữ tệp chỉ xóa thư mục, Giữ mục đã chọn xóa mục còn lại, Xóa sạch ruột giữ vỏ thư mục rỗng, Đổi tên hàng loạt (đánh số tự động, thay thế từ khóa).
- **Duyệt ZIP & Quản lý dung lượng nâng cao**:
  - Bấm vào file ZIP để duyệt và trích xuất từng file lẻ; dọn dẹp tệp lớn >50MB và tệp trùng lặp 1 chạm.
