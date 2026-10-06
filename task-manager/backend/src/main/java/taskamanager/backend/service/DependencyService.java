package taskamanager.backend.service;


import taskamanager.backend.model.*;
import taskamanager.backend.dto.Dtos.DependencyResponse;
import taskamanager.backend.exception.AppException;
import taskamanager.backend.repository.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DependencyService {
    private final DependencyRepository deps;
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final AccessService access;
    private final ActivityService activity;

    public DependencyService(DependencyRepository deps, TaskRepository tasks, ProjectRepository projects,
                             AccessService access, ActivityService activity) {
        this.deps = deps; this.tasks = tasks; this.projects = projects;
        this.access = access; this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<DependencyResponse> list(Long userId, Long taskId) {
        Task task = tasks.findById(taskId).orElseThrow(AppException::notFound);
        access.requireRole(userId, task.getProjectId(), Role.VIEWER);

        List<Long> ids = deps.findByTaskId(taskId).stream().map(TaskDependency::getDependsOnId).toList();
        return tasks.findAllById(ids).stream()
                .map(t -> new DependencyResponse(t.getId(), t.getTitle(), t.getStatus()))
                .toList();
    }

    @Transactional
    public void add(Long userId, Long taskId, Long dependsOnId) {
        Task task = tasks.findById(taskId).orElseThrow(AppException::notFound);
        access.requireRole(userId, task.getProjectId(), Role.EDITOR);

        // LOCK FIRST, then check. Without this lock, two concurrent requests (B->A and A->B)
        // would each see a graph without the other's edge, both pass, and create a cycle (write skew).
        projects.lockById(task.getProjectId()).orElseThrow(AppException::notFound);

        if (taskId.equals(dependsOnId))
            throw new AppException(422, "SELF_DEPENDENCY", "A task cannot depend on itself");

        Task dep = tasks.findById(dependsOnId).orElseThrow(AppException::notFound);
        if (!dep.getProjectId().equals(task.getProjectId()))
            throw new AppException(422, "CROSS_PROJECT", "Tasks must be in the same project");

        if (deps.existsById(new DependencyId(taskId, dependsOnId)))
            throw new AppException(409, "ALREADY_EXISTS", "Dependency already exists");

        if (wouldCreateCycle(task.getProjectId(), taskId, dependsOnId))
            throw new AppException(422, "CYCLE", "This would create a circular dependency");

        deps.save(new TaskDependency(taskId, dependsOnId, task.getProjectId()));
        activity.record(userId, taskId, ActivityAction.DEPENDENCY_ADDED, "dependency",
                null, dep.getId() + ": " + dep.getTitle());
    }

    @Transactional
    public void remove(Long userId, Long taskId, Long dependsOnId) {
        Task task = tasks.findById(taskId).orElseThrow(AppException::notFound);
        access.requireRole(userId, task.getProjectId(), Role.EDITOR);

        if (deps.deleteEdge(taskId, dependsOnId) == 0) throw AppException.notFound();
        activity.record(userId, taskId, ActivityAction.DEPENDENCY_REMOVED, "dependency",
                String.valueOf(dependsOnId), null);
        // Removing an edge can never create a cycle, so no lock needed.
    }

    /**
     * Adding "x waits for y". Walk from y following "waits for" edges.
     * If we can reach x, the new edge would close a loop. DFS, O(V + E).
     */
    private boolean wouldCreateCycle(Long projectId, Long x, Long y) {
        // ONE query for the whole project's edges, then everything happens in memory
        Map<Long, List<Long>> adj = deps.findByProjectId(projectId).stream()
                .collect(Collectors.groupingBy(TaskDependency::getTaskId,
                         Collectors.mapping(TaskDependency::getDependsOnId, Collectors.toList())));

        Deque<Long> stack = new ArrayDeque<>();
        stack.push(y);
        Set<Long> visited = new HashSet<>();
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (cur.equals(x)) return true;      // reached the waiter again: cycle
            if (!visited.add(cur)) continue;     // already explored (handles diamond shapes)
            for (Long next : adj.getOrDefault(cur, List.of())) stack.push(next);
        }
        return false;
    }
}
