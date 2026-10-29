# CONTEXT — VELQOR PRO (v2.0.12)

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
  - `files/`: `FileRepository` (Duyệt, copy, move, xóa, đổi tên, `calculateFolderInfo` đệ quy dung lượng/số lượng), `TextFileIO`, `FileCreator`, `ThumbnailCache`.
  - `ui/`: `FloatingTabBar`, `BlurBackdropView`, `ImageViewerActivity`, `TextEditorActivity`, `NewItemSheet`, `HeaderView`, `FileRowView`, `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, cử chỉ lùi hệ thống, chi tiết thư mục đầy đủ.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Link resources: `aapt2 link --version-code 14 --version-name "2.0.12"`
- Biên dịch mã: `javac -cp android.jar` + `d8 --release --min-api 26`
- Ký & Đóng gói: `apksigner sign --ks debug.keystore`
- Test suite: `bash tools/test_runner.sh` (79/79 tests passed - 100%).

## 4. Trạng thái hiện tại (v2.0.12 - Build 14)
- **HOTFIX Kích thước & Nội dung chi tiết thư mục hoàn tất**:
  1. Khắc phục lỗi hiển thị kích thước thư mục chỉ lấy inode (3.4 KB / 4096 bytes) trong hộp thoại "Chi tiết tệp tin":
     - `FileRepository.calculateFolderInfo(File dir)`: Tính toán đệ quy toàn bộ tổng dung lượng thực tế (`totalBytes`), số lượng tệp tin (`fileCount`) và số lượng thư mục con (`dirCount`).
     - Hộp thoại hiển thị ngay lập tức với trạng thái "Đang tính toán...", tính toán ngoài luồng UI và cập nhật kết quả chính xác, mượt mà khi hoàn tất.
     - Bổ sung trường "Nội dung": Thống kê chi tiết số tệp và số thư mục con.
  2. Bổ sung bộ kiểm thử `testFolderInfoCalculation` (79/79 test passed 100%).
  3. **Output APK**: `/sdcard/AntiGravity/velqor-v2.0.12.apk` (497 KB).
