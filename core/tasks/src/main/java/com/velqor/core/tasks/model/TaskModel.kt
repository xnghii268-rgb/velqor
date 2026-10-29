package com.velqor.core.tasks.model

enum class TaskType {
    COPY,
    MOVE,
    DELETE,
    COMPRESS,
    EXTRACT,
    ENCRYPT,
    DECRYPT,
    HASH
}

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class TaskInfo(
    val id: String,
    val title: String,
    val type: TaskType,
    val status: TaskStatus = TaskStatus.PENDING,
    val currentItem: String = "",
    val progressPercent: Float = 0f,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null,
    val startTimeMs: Long = System.currentTimeMillis()
)
