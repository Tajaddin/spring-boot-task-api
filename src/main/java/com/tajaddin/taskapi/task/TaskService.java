package com.tajaddin.taskapi.task;

import com.tajaddin.taskapi.common.NotFoundException;
import com.tajaddin.taskapi.task.dto.CreateTaskRequest;
import com.tajaddin.taskapi.task.dto.TaskResponse;
import com.tajaddin.taskapi.task.dto.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository tasks;

    public TaskService(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(Long ownerId, TaskStatus status, Pageable pageable) {
        Page<Task> page = (status == null)
                ? tasks.findByOwnerId(ownerId, pageable)
                : tasks.findByOwnerIdAndStatus(ownerId, status, pageable);
        return page.map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long ownerId, Long id) {
        return TaskResponse.from(requireOwned(ownerId, id));
    }

    @Transactional
    public TaskResponse create(Long ownerId, CreateTaskRequest request) {
        Task task = new Task(
                ownerId,
                request.title(),
                request.description(),
                request.priority(),
                request.dueDate());
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional
    public TaskResponse update(Long ownerId, Long id, UpdateTaskRequest request) {
        Task task = requireOwned(ownerId, id);
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional
    public void delete(Long ownerId, Long id) {
        Task task = requireOwned(ownerId, id);
        tasks.delete(task);
    }

    private Task requireOwned(Long ownerId, Long id) {
        return tasks.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("task " + id + " not found"));
    }
}
