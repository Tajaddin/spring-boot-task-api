package com.tajaddin.taskapi.task.dto;

import com.tajaddin.taskapi.task.TaskPriority;
import com.tajaddin.taskapi.task.TaskStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Partial update. Null fields are left unchanged.
 */
public record UpdateTaskRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate) {
}
