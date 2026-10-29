# VELQOR — TIẾN ĐỘ PHÁT TRIỂN (v2.0.0)

## Trạng thái hiện tại
- **Phiên bản**: **v2.0.0 (versionCode 2)** — VELQOR PRO
- **Thư mục dự án**: `/sdcard/AntiGravity/velqor` (định vị 100% trong AntiGravity)
- **Repo GitHub**: `https://github.com/xnghii268-rgb/velqor`
- **Phong cách UI**: Apple-grade Minimalist Pro (Cupertino, Frosted Glass, OLED Black)

## Nâng cấp v2.0.0 (VELQOR Pro Features)
- [x] **Phiên bản v2**: Bump `versionCode = 2`, `versionName = "2.0.0"`.
- [x] **TAR/GZ/BZ2/XZ Engine**: Hỗ trợ đầy đủ định dạng Linux/Unix archives (`.tar`, `.tar.gz`, `.tgz`, `.tar.bz2`, `.tar.xz`).
- [x] **Interactive Create Archive Dialog**: Tùy chọn tên tệp, định dạng (ZIP, TAR, TAR.GZ), thanh trượt mức độ nén (0-9), mật khẩu AES-256.
- [x] **File Details & Hash Inspector**: Hiển thị metadata đầy đủ và tính toán mã băm SHA-256 theo luồng, 1-tap Copy.
- [x] **Anti-Zip Slip Shield**: Bảo vệ toàn diện chống path traversal trên toàn bộ các engine (ZIP, 7Z, RAR, TAR).
- [x] **Kiểm thử kiến trúc**: Chạy `bash tools/test_runner.sh` đạt **19/19 bài test (100% pass)**.

## Lệnh kiểm chứng
- JVM Test: `bash /sdcard/AntiGravity/velqor/tools/test_runner.sh` (19/19 tests passed).
- Git status: `git status`
