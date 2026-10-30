# Velqor v2.2.0 - Icon Selection & Enhanced 3-Dot Batch Actions

- FileRowView.java:
  - Added onIconClick callback in OnFileRowClickListener.
  - Attached spring tap animations and dedicated click listeners to iconBox and selection check icon, allowing instant selection by tapping the icon without opening the item.

- MainActivity.java:
  - Implemented onIconClick: activates selection mode immediately on icon tap, adds item to selection with haptic feedback, or toggles selection if already in selection mode.
  - Enhanced 3-dot ("3 chấm") options sheet with comprehensive actions:
    - Chọn tất cả trong thư mục (Select all items in folder).
    - Đảo lựa chọn (Invert selection).
    - Nén thành ZIP.
    - Xóa tất cả mục đã chọn.
    - Giữ thư mục, chỉ xóa tệp (Keep folders, delete files only).
    - Giữ tệp, chỉ xóa thư mục (Keep files, delete folders only).
    - Giữ mục đã chọn, xóa các mục còn lại (Keep selected items, delete unselected items in current folder).
    - Xóa sạch nội dung, giữ vỏ thư mục (Clear contents inside selected folders, keep empty directory shells).
  - Enhanced batch delete sheet with folder/file filtering options when selection contains both types.

- IosSheet.java:
  - Added showDangerConfirmDialog for iOS-style destructive action confirmations.

- FileRepository.java:
  - Added clearDirectoryContents(File dir) to wipe child items while preserving the directory.
