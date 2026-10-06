package taskamanager.backend.service;


import taskamanager.backend.model.ActivityAction;
import taskamanager.backend.model.Role;
import taskamanager.backend.model.Status;
import taskamanager.backend.dto.Dtos.*;
import taskamanager.backend.model.Task;
import taskamanager.backend.exception.AppException;
import taskamanager.backend.repository.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final DependencyRepository deps;
    private final MemberRepository members;
    private final AccessService access;
    private final ActivityService activity;

    public TaskService(TaskRepository tasks, DependencyRepository deps, MemberRepository members,
                       AccessService access, ActivityService activity) {
        this.tasks = tasks; this.deps = deps; this.members = members;
        this.access = access; this.activity = activity;
    }

    @Transactional
    public TaskResponse create(Long userId, Long projectId, CreateTaskRequest req) {
        access.requireRole(userId, projectId, Role.EDITOR);
        if (req.assigneeId() != null) requireProjectMember(req.assigneeId(), projectId);

        Task t = tasks.saveAndFlush(new Task(projectId, req.title().trim(), req.description(), req.assigneeId()));
        activity.record(userId, t.getId(), ActivityAction.CREATED, "title", null, t.getTitle());
        return toResponse(t, false);
    }

    /** Two queries total for the whole board (no N+1): tasks, then the set of blocked ids. */
    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long userId, Long projectId) {
        access.requireRole(userId, projectId, Role.VIEWER);
        Set<Long> blocked = new HashSet<>(deps.findBlockedTaskIds(projectId, Status.DONE));
        return tasks.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(t -> toResponse(t, blocked.contains(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long userId, Long taskId) {
        Task t = tasks.findById(taskId).orElseThrow(AppException::notFound);
        access.requireRole(userId, t.getProjectId(), Role.VIEWER);
        return toResponse(t, deps.countUnfinished(taskId, Status.DONE) > 0);
    }

    @Transactional
    public TaskResponse update(Long userId, Long taskId, UpdateTaskRequest req) {
        Task task = tasks.findById(taskId).orElseThrow(AppException::notFound);
        access.requireRole(userId, task.getProjectId(), Role.EDITOR);

        // Layer 1 of optimistic locking: the version the USER'S BROWSER held vs. the database now.
        if (!req.version().equals(task.getVersion()))
            throw new AppException(409, "VERSION_CONFLICT", "Task was changed by someone else. Reload and try again.");

        // Rule: cannot start or finish a task with unfinished dependencies
        if (req.status() != task.getStatus() && req.status() != Status.TODO
                && deps.countUnfinished(taskId, Status.DONE) > 0)
            throw new AppException(422, "BLOCKED", "Task has unfinished dependencies");

        if (req.assigneeId() != null && !Objects.equals(req.assigneeId(), task.getAssigneeId()))
            requireProjectMember(req.assigneeId(), task.getProjectId());

        // capture old values for the audit log
        String oldTitle = task.getTitle();
        String oldDesc = task.getDescription();
        Status oldStatus = task.getStatus();
        Long oldAssignee = task.getAssigneeId();

        String newTitle = req.title().trim();
        String newDesc = req.description() == null ? "" : req.description();

        task.setTitle(newTitle);
        task.setDescription(newDesc);
        task.setStatus(req.status());
        task.setAssigneeId(req.assigneeId());

        // Layer 2: Hibernate runs UPDATE ... WHERE id=? AND version=?; flush now so a conflict
        // throws HERE (mapped to 409), not later at commit.
        tasks.saveAndFlush(task);

        logChange(userId, taskId, "title", oldTitle, newTitle);
        logChange(userId, taskId, "description", oldDesc, newDesc);
        logChange(userId, taskId, "status", oldStatus, req.status());
        logChange(userId, taskId, "assignee_id", oldAssignee, req.assigneeId());

        return toResponse(task, deps.countUnfinished(taskId, Status.DONE) > 0);
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        Task t = tasks.findById(taskId).orElseThrow(AppException::notFound);
        access.requireRole(userId, t.getProjectId(), Role.OWNER);
        tasks.delete(t);   // DB cascades remove its dependencies and activity rows
    }

    // ---- helpers ----

    private void logChange(Long userId, Long taskId, String field, Object oldV, Object newV) {
        if (!Objects.equals(oldV, newV))
            activity.record(userId, taskId, ActivityAction.UPDATED, field,
                    oldV == null ? null : oldV.toString(), newV == null ? null : newV.toString());
    }

    private void requireProjectMember(Long assigneeId, Long projectId) {
        if (members.findByUserIdAndProjectId(assigneeId, projectId).isEmpty())
            throw new AppException(422, "ASSIGNEE_NOT_MEMBER", "Assignee must be a member of the project");
    }

    private TaskResponse toResponse(Task t, boolean blocked) {
        return new TaskResponse(t.getId(), t.getProjectId(), t.getTitle(), t.getDescription(),
                t.getStatus(), t.getAssigneeId(), t.getVersion(), blocked,
                t.getCreatedAt(), t.getUpdatedAt());
    }
}