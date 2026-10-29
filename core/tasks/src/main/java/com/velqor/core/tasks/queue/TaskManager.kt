package com.velqor.core.tasks.queue

import com.velqor.core.common.dispatcher.DispatcherProvider
import com.velqor.core.tasks.model.TaskInfo
import com.velqor.core.tasks.model.TaskStatus
import com.velqor.core.tasks.model.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TaskManager(
    private val dispatchers: DispatcherProvider
) {
    private val scope = CoroutineScope(dispatchers.default)
    private val _tasks = MutableStateFlow<List<TaskInfo>>(emptyList())
    val tasks: StateFlow<List<TaskInfo>> = _tasks.asStateFlow()

    private val runningJobs = mutableMapOf<String, Job>()

    fun enqueueTask(
        title: String,
        type: TaskType,
        block: suspend (
            updateProgress: (currentItem: String, percent: Float, bytesProcessed: Long, totalBytes: Long) -> Unit,
            isCancelled: () -> Boolean
        ) -> Unit
    ): String {
        val taskId = UUID.randomUUID().toString()
        val newTask = TaskInfo(
            id = taskId,
            title = title,
            type = type,
            status = TaskStatus.PENDING
        )

        _tasks.update { listOf(newTask) + it }

        val job = scope.launch(dispatchers.io) {
            _tasks.update { list ->
                list.map { if (it.id == taskId) it.copy(status = TaskStatus.RUNNING) else it }
            }

            try {
                block(
                    { item, pct, bytes, total ->
                        _tasks.update { list ->
                            list.map {
                                if (it.id == taskId) it.copy(
                                    currentItem = item,
                                    progressPercent = pct,
                                    bytesProcessed = bytes,
                                    totalBytes = total
                                ) else it
                            }
                        }
                    },
                    {
                        val current = _tasks.value.firstOrNull { it.id == taskId }
                        current?.status == TaskStatus.CANCELLED
                    }
                )

                _tasks.update { list ->
                    list.map {
                        if (it.id == taskId && it.status == TaskStatus.RUNNING) {
                            it.copy(status = TaskStatus.COMPLETED, progressPercent = 1f)
                        } else it
                    }
                }
            } catch (e: Exception) {
                _tasks.update { list ->
                    list.map {
                        if (it.id == taskId) {
                            it.copy(
                                status = TaskStatus.FAILED,
                                errorMessage = e.message ?: "Tác vụ gặp sự cố"
                            )
                        } else it
                    }
                }
            } finally {
                runningJobs.remove(taskId)
            }
        }

        runningJobs[taskId] = job
        return taskId
    }

    fun cancelTask(taskId: String) {
        _tasks.update { list ->
            list.map { if (it.id == taskId) it.copy(status = TaskStatus.CANCELLED) else it }
        }
        runningJobs[taskId]?.cancel()
        runningJobs.remove(taskId)
    }

    fun clearCompleted() {
        _tasks.update { list ->
            list.filter { it.status == TaskStatus.RUNNING || it.status == TaskStatus.PENDING }
        }
    }
}
