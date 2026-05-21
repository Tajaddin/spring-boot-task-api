package com.tajaddin.taskapi.task;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}
