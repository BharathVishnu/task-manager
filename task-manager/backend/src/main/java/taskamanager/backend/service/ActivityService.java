package taskamanager.backend.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import taskamanager.backend.repository.*;
import taskamanager.backend.dto.Dtos.ActivityResponse;
import taskamanager.backend.exception.AppException;
import taskamanager.backend.model.*;

@Service
public class ActivityService {
    private final ActivityRepository repo;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final AccessService access;

    public ActivityService(ActivityRepository repo, TaskRepository tasks,
                           UserRepository users, AccessService access) {
        this.repo = repo; this.tasks = tasks; this.users = users; this.access = access;
    }

    /** Joins the caller's transaction, so the log and the change commit or roll back together. */
    @Transactional
    public void record(Long userId, Long taskId, ActivityAction action,
                       String field, String oldValue, String newValue) {
        repo.save(new Activity(taskId, userId, action, field, oldValue, newValue));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> timeline(Long userId, Long taskId, int page, int size) {
        Task task = tasks.findById(taskId).orElseThrow(AppException::notFound);
        access.requireRole(userId, task.getProjectId(), Role.VIEWER);

        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Activity> rows = repo.findByTaskIdOrderByCreatedAtDescIdDesc(
                taskId, PageRequest.of(Math.max(page, 0), safeSize));

        Set<Long> ids = rows.stream().map(Activity::getUserId).collect(Collectors.toSet());
        Map<Long, String> emails = users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        return rows.stream().map(a -> new ActivityResponse(
                a.getId(), a.getUserId(), emails.get(a.getUserId()), a.getAction(),
                a.getField(), a.getOldValue(), a.getNewValue(), a.getCreatedAt())).toList();
    }
}
