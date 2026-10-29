# VELQOR — TIẾN ĐỘ PHÁT TRIỂN

## Trạng thái hiện tại
- **Đang thực hiện**: M0 + M1 + M2 + M3 + M4 + M5 + M6 — Core Architecture & Feature Suites Completed
- **Thư mục dự án**: `/sdcard/AntiGravity/velqor` (đã chuyển 100% vào AntiGravity)
- **Repo GitHub**: `https://github.com/xnghii268-rgb/velqor` (Commit `404b2d4`)
- **App Icon**: Đã tích hợp icon 500x500 RGBA từ `/sdcard/AntiGravity/49374-removebg-preview.png` vào đầy đủ các mật độ màn hình `mipmap` (`hdpi` đến `xxxhdpi`).

## Tiến độ Milestone
- [x] Phase 0: Khảo sát môi trường, multi-module Gradle catalog, thiết lập repo.
- [x] M0: Skeleton & Architecture (AppContainer, Theme tokens, Navigation, MainActivity).
- [x] M1: File Browser MVP (Duyệt thư mục, sắp xếp, tìm kiếm tiếng Việt không dấu, CRUD, clipboard).
- [x] M2: Task Engine (TaskManager, hàng đợi tác vụ nền, Foreground Service, Flow tiến trình, hủy).
- [x] M3: ZIP Engine (Zip4j + Anti-ZipSlip defense, nén/giải nén, mật khẩu AES-256).
- [x] M4: 7z & RAR Engine (Commons-Compress 7z + Junrar trích xuất, báo lỗi rõ ràng).
- [x] M5: SHA-256 Streaming Hasher + Monospace Text Editor (Atomic save, đánh số dòng).
- [x] M6: Security Vault (AES-256-GCM chunked stream, sequence AAD, chống giả mạo).
- [x] M7: Kiểm thử kiến trúc JVM & Tích hợp (16/16 tests passed 100%).

## Quyết định kiến trúc
- **DI**: AppContainer + Service Locator thuần Kotlin (0 annotation processor, siêu tốc, an toàn).
- **Crypto**: AES-256-GCM chunked streaming (12-byte IV + sequence counter + 128-bit tag).
- **Zip Slip Defense**: Chuẩn hóa canonical path, chặn đứng mọi tấn công path traversal `../`.

## Lệnh kiểm chứng
- JVM Test: `bash /sdcard/AntiGravity/velqor/tools/test_runner.sh` (16/16 tests passed).
- Git Remote: `git status && git log -1`
