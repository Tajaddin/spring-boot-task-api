package com.tajaddin.taskapi.task.dto;

import com.tajaddin.taskapi.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        TaskPriority priority,
        LocalDate dueDate) {
}
