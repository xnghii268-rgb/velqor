# VELQOR — TIẾN ĐỘ PHÁT TRIỂN

## Trạng thái hiện tại
- **Đang thực hiện**: M0 — Skeleton & Architecture Foundation
- **Thư mục dự án**: `/sdcard/AntiGravity/velqor`
- **Repo GitHub**: `xnghii268-rgb/velqor`
- **Phong cách UI**: Apple-grade Minimalist (Cupertino / Frosted Glass / OLED Black)

## Tiến độ Milestone
- [x] Phase 0: Khảo sát môi trường, thiết lập multi-module Gradle, version catalog.
- [ ] M0: Skeleton (AppContainer, Theme tokens, Navigation, Base UI, CI build).
- [ ] M1: File Browser MVP (Duyệt, sort, search không dấu, CRUD file/folder, multi-select).
- [ ] M2: Task Engine (Hàng đợi tác vụ nền, Foreground Service, Flow tiến trình, hủy).
- [ ] M3: ZIP Engine (Zip4j + Anti-ZipSlip, nén/giải nén, mật khẩu, progress stream).
- [ ] M4: 7z & RAR Engine (Commons-Compress 7z + Junrar trích xuất, báo lỗi chuẩn).
- [ ] M5: SHA-256 Streaming Hasher + Monospace Text Editor (Atomic save).
- [ ] M6: Security Vault (AES-256-GCM chunked stream, Android Keystore, FLAG_SECURE).
- [ ] M7: Polish & Release APK (Animation, Haptics, Proguard, CI Build & Release).

## Quyết định kiến trúc
- **DI**: AppContainer + ViewModelFactory thuần Kotlin (0 annotation processor, siêu nhanh, an toàn).
- **Crypto**: AES-256-GCM chunked streaming (12-byte IV + sequence counter + 128-bit tag).
- **Safety**: Chống Zip Slip bằng chuẩn hóa canonical path, kiểm tra zip bomb.

## Lệnh kiểm chứng
- JVM Test: `bash tools/test_runner.sh`
- CI Build: `gh run list --repo xnghii268-rgb/velqor`
