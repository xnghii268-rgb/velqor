package com.velqor.core.common.result

sealed interface AppError {
    val message: String
    val cause: Throwable?

    data class StoragePermissionDenied(
        override val message: String = "Quyền truy cập bộ nhớ bị từ chối",
        override val cause: Throwable? = null
    ) : AppError

    data class FileNotFound(
        override val message: String = "Không tìm thấy tệp hoặc thư mục",
        override val cause: Throwable? = null
    ) : AppError

    data class AlreadyExists(
        override val message: String = "Tệp hoặc thư mục đã tồn tại",
        override val cause: Throwable? = null
    ) : AppError

    data class InsufficientStorage(
        override val message: String = "Dung lượng bộ nhớ không đủ",
        override val cause: Throwable? = null
    ) : AppError

    data class ZipSlipAttackDetected(
        override val message: String = "Cảnh báo bảo mật: Phát hiện đường dẫn nén bất hợp pháp (Zip Slip)",
        override val cause: Throwable? = null
    ) : AppError

    data class CorruptedArchive(
        override val message: String = "Tệp nén bị hỏng hoặc định dạng không hợp lệ",
        override val cause: Throwable? = null
    ) : AppError

    data class InvalidPassword(
        override val message: String = "Mật khẩu không chính xác hoặc tệp được bảo vệ",
        override val cause: Throwable? = null
    ) : AppError

    data class UnsupportedFormat(
        override val message: String = "Định dạng tệp chưa được hỗ trợ",
        override val cause: Throwable? = null
    ) : AppError

    data class CryptoFailed(
        override val message: String = "Thao tác mã hóa hoặc giải mã thất bại",
        override val cause: Throwable? = null
    ) : AppError

    data class TaskCancelled(
        override val message: String = "Tác vụ đã bị hủy bỏ bởi người dùng",
        override val cause: Throwable? = null
    ) : AppError

    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError
}
