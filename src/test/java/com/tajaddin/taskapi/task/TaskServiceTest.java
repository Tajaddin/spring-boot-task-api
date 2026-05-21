package com.tajaddin.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tajaddin.taskapi.common.NotFoundException;
import com.tajaddin.taskapi.task.dto.CreateTaskRequest;
import com.tajaddin.taskapi.task.dto.TaskResponse;
import com.tajaddin.taskapi.task.dto.UpdateTaskRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TaskServiceTest {

    private TaskRepository repo;
    private TaskService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(TaskRepository.class);
        service = new TaskService(repo);
    }

    @Test
    void createReturnsTaskWithTodoStatus() {
        when(repo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        var request = new CreateTaskRequest("Write tests", "for the service", TaskPriority.HIGH, null);

        TaskResponse out = service.create(99L, request);

        assertThat(out.title()).isEqualTo("Write tests");
        assertThat(out.status()).isEqualTo(TaskStatus.TODO);
        assertThat(out.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void getThrowsWhenTaskNotOwned() {
        when(repo.findByIdAndOwnerId(1L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateAppliesOnlyNonNullFields() {
        Task existing = new Task(99L, "old title", "old desc", TaskPriority.LOW, null);
        when(repo.findByIdAndOwnerId(5L, 99L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var patch = new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null);
        TaskResponse out = service.update(99L, 5L, patch);

        // Title and description unchanged, status updated.
        assertThat(out.title()).isEqualTo("old title");
        assertThat(out.description()).isEqualTo("old desc");
        assertThat(out.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void deleteThrowsWhenNotOwned() {
        when(repo.findByIdAndOwnerId(7L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(99L, 7L)).isInstanceOf(NotFoundException.class);
    }
}
