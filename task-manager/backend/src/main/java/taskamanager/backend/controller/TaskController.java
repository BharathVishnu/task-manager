package taskamanager.backend.controller;


import jakarta.validation.Valid;
import taskamanager.backend.dto.Dtos.ActivityResponse;
import taskamanager.backend.dto.Dtos.CreateTaskRequest;
import taskamanager.backend.dto.Dtos.DependencyRequest;
import taskamanager.backend.dto.Dtos.DependencyResponse;
import taskamanager.backend.dto.Dtos.TaskResponse;
import taskamanager.backend.dto.Dtos.UpdateTaskRequest;
import taskamanager.backend.service.*;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskController {
    private final TaskService tasks;
    private final DependencyService deps;
    private final ActivityService activity;

    public TaskController(TaskService tasks, DependencyService deps, ActivityService activity) {
        this.tasks = tasks; this.deps = deps; this.activity = activity;
    }

    // ----- tasks -----
    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskResponse> list(@AuthenticationPrincipal Long userId, @PathVariable Long projectId) {
        return tasks.list(userId, projectId);
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@AuthenticationPrincipal Long userId, @PathVariable Long projectId,
                               @Valid @RequestBody CreateTaskRequest req) {
        return tasks.create(userId, projectId, req);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse get(@AuthenticationPrincipal Long userId, @PathVariable Long taskId) {
        return tasks.get(userId, taskId);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long taskId,
                               @Valid @RequestBody UpdateTaskRequest req) {
        return tasks.update(userId, taskId, req);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long taskId) {
        tasks.delete(userId, taskId);
    }

    // ----- dependencies -----
    @GetMapping("/tasks/{taskId}/dependencies")
    public List<DependencyResponse> dependencies(@AuthenticationPrincipal Long userId, @PathVariable Long taskId) {
        return deps.list(userId, taskId);
    }

    @PostMapping("/tasks/{taskId}/dependencies")
    @ResponseStatus(HttpStatus.CREATED)
    public void addDependency(@AuthenticationPrincipal Long userId, @PathVariable Long taskId,
                              @Valid @RequestBody DependencyRequest req) {
        deps.add(userId, taskId, req.dependsOnId());
    }

    @DeleteMapping("/tasks/{taskId}/dependencies/{dependsOnId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDependency(@AuthenticationPrincipal Long userId, @PathVariable Long taskId,
                                 @PathVariable Long dependsOnId) {
        deps.remove(userId, taskId, dependsOnId);
    }

    // ----- activity -----
    @GetMapping("/tasks/{taskId}/activity")
    public List<ActivityResponse> timeline(@AuthenticationPrincipal Long userId, @PathVariable Long taskId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return activity.timeline(userId, taskId, page, size);
    }
}
