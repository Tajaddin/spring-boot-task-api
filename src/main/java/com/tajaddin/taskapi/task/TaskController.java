package com.tajaddin.taskapi.task;

import com.tajaddin.taskapi.security.CurrentUser;
import com.tajaddin.taskapi.task.dto.CreateTaskRequest;
import com.tajaddin.taskapi.task.dto.TaskResponse;
import com.tajaddin.taskapi.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Page<TaskResponse> list(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int bounded = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt"));
        return taskService.list(CurrentUser.id(), status, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(CurrentUser.id(), id);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(CurrentUser.id(), request));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(CurrentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
