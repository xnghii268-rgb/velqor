# CONTEXT — VELQOR PRO (v2.1.0)

## 1. Dự án là gì
- **Slug**: `velqor`
- **Mục tiêu**: Trình quản lý tệp tin, xem ảnh, soạn thảo code/text, nén/giải nén chuẩn Apple iOS Liquid Glass tối giản, mượt mà và đẳng cấp trên Android.
- **Vị trí**: `/sdcard/AntiGravity/velqor`
- **Repo**: `https://github.com/xnghii268-rgb/velqor`

## 2. Công nghệ & Kiến trúc
- **Ngôn ngữ**: Java 17 + Android View Native (0 comment, 0 thư viện ngoài, thuần AOSP stack).
- **Công cụ build**: AOSP CLI (`aapt2`, `javac`, `d8`, `apksigner`).
- **Android Target**: minSdk 26, targetSdk 34, compileSdk 34.
- **Cấu trúc phân tầng**:
  - `tokens/ThemeTokens.java`: Hệ thống token màu Apple (Dark/Light/Auto, 7 màu nhấn), kích thước, alpha, radius, specular rim.
  - `archive/ZipManager.java`: Động cơ nén, giải nén, duyệt danh mục ZIP không cần giải nén (`listZipEntries`), trích xuất từng tệp.
  - `files/`: `FileRepository`, `TextFileIO`, `FileCreator`, `ThumbnailCache`, `TrashManager` (thùng rác), `StorageScanner` (phân tích dung lượng & tệp trùng).
  - `ui/`: `FloatingTabBar` (3 tab: Tệp, Nén, Cài đặt dạng icon-only), `BlurBackdropView`, `ImageViewerActivity`, `TextEditorActivity` (cú pháp màu & xem trước HTML/MD), `DonutChartView` (biểu đồ tròn bộ nhớ), `HeaderView` (breadcrumbs + phím tắt + chuyển chế độ xem Lưới/Danh sách), `FileRowView` (hiệu ứng co giãn spring 0.97x, thẻ lưới 2 cột), `IosSheet`.
  - `MainActivity.java`: Điều phối vòng đời, 3 tab chính, tìm kiếm với bộ lọc thể loại, cử chỉ lùi hệ thống.
- **Quyền**: `MANAGE_EXTERNAL_STORAGE`, `REQUEST_INSTALL_PACKAGES` với `VelqorFileProvider`.

## 3. Cách chạy & Đóng gói
- Build toàn bộ: `bash tools/build_apk.sh` (tự động biên dịch tài nguyên AAPT2, javac, d8 và ký apksigner).
- Test suite: `bash tools/test_runner.sh` (78/78 tests passed - 100%).
- Đích bàn giao: `/sdcard/AntiGravity/velqor-v2.1.0.apk` (509 KB).

## 4. Trạng thái hiện tại (v2.1.0 - Build 15)
- **Nâng cấp toàn diện Apple Liquid Glass & 3 Tab**:
  1. Loại bỏ hoàn toàn Vault & Hash, tinh gọn thanh điều hướng nổi còn 3 tab: Tệp, Nén, Cài đặt với thiết kế Icon-only (không nhãn chữ).
  2. Bổ sung chế độ xem Lưới (Grid 2 cột) song song với Danh sách (List), có nút chuyển đổi trực tiếp trên thanh tiêu đề.
  3. Thanh tìm kiếm thông minh tích hợp bộ lọc thể loại chip Apple: Tất cả, Ảnh, Video, Tài liệu, Kho nén, Lớn >10MB.
  4. Thanh phím tắt thư mục nhanh ngay dưới breadcrumbs (Tải về, Ảnh/DCIM, Tài liệu, Hình ảnh, Âm nhạc, Phim).
  5. Động cơ duyệt tệp nén trực tiếp trong ứng dụng không cần giải nén (Archive Inspector).
  6. Dashboard dung lượng với biểu đồ Donut trực quan, quét tệp lớn >50MB và nhóm tệp trùng lặp.
  7. Tính năng Thùng rác (Trash Bin) khôi phục tệp 1 chạm.
  8. Trình soạn thảo văn bản bổ sung cú pháp màu (Syntax Highlighting) và Xem trước trực tiếp HTML/Markdown.
  9. Hệ thống chủ đề Light/Dark/Auto và bộ chọn 7 màu nhấn Apple Liquid.
  10. Tuân thủ tuyệt đối quy tắc: 100% không chứa comment trong toàn bộ mã nguồn.
