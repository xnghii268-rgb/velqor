# Velqor v2.2.0 - Comprehensive Logic Audit & Advanced Features

- WirelessDebugManager.java:
  - Added runShizukuShellCommand, deleteViaShizuku, createDirectoryViaShizuku, renameViaShizuku, copyViaShizuku, and moveViaShizuku for UID 2000 shell operations.

- FileRepository.java:
  - Delegated deleteRecursive, clearDirectoryContents, createDirectory, rename, copy, and move in Android/data to Shizuku shell.

- MainActivity.java:
  - Direct Archive Browser: tapping any .zip archive opens showArchiveInspectorSheet to inspect and extract single items.
  - Added "Đổi tên hàng loạt" in 3-dot menu with sequential numbering (Mẫu_01, Mẫu_02...) and find-and-replace support.
  - Replaced stub toasts in Storage Analysis with interactive sheets: showLargeFilesSheet (>50MB files) and showDuplicateFilesSheet (1-tap duplicate cleanup).

- TestRunner.java:
  - Added regression tests for clearDirectoryContents, expanding test suite to 93/93 tests (100% passed).
