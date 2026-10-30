# Velqor v2.3.0 (Build 20) - Comprehensive Logic Audit, Full Shizuku Integration & Version Bump

- Version Bump:
  - Upgraded AndroidManifest.xml: versionCode="20", versionName="2.3.0".
  - Updated MainActivity.java: Settings Profile card, Settings "Phiên bản" row, and "Giới thiệu VELQOR" about card to "v2.3.0 (Build 20)".
  - Updated tools/build_apk.sh to package and sign /sdcard/AntiGravity/velqor-v2.3.0.apk.

- WirelessDebugManager.java:
  - Added runShizukuShellCommand, deleteViaShizuku, createDirectoryViaShizuku, renameViaShizuku, copyViaShizuku, and moveViaShizuku for UID 2000 shell operations.

- FileRepository.java:
  - Delegated deleteRecursive, clearDirectoryContents, createDirectory, rename, copy, and move in Android/data and Android/obb to Shizuku shell.

- MainActivity.java:
  - Instant icon click multi-selection in FileRowView.
  - Advanced 3-dot operations: Keep folders/delete files, Keep files/delete folders, Keep selected/delete remaining, Clear contents inside folders.
  - Direct Archive Browser: tapping any .zip archive opens showArchiveInspectorSheet to inspect and extract single items.
  - Added "Đổi tên hàng loạt" in 3-dot menu with sequential numbering (Mẫu_01, Mẫu_02...) and find-and-replace support.
  - Replaced stub toasts in Storage Analysis with interactive sheets: showLargeFilesSheet (>50MB files) and showDuplicateFilesSheet (1-tap duplicate cleanup).

- TestRunner.java:
  - Added regression tests for clearDirectoryContents, expanding test suite to 93/93 tests (100% passed).
