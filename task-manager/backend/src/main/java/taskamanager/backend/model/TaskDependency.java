package taskamanager.backend.model;

import jakarta.persistence.*;

/** Row (taskId, dependsOnId) means: taskId WAITS FOR dependsOnId. */
@Entity
@Table(name = "task_dependencies")
@IdClass(DependencyId.class)
public class TaskDependency {
    @Id @Column(name = "task_id")
    private Long taskId;

    @Id @Column(name = "depends_on_id")
    private Long dependsOnId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    protected TaskDependency() {}
    public TaskDependency(Long taskId, Long dependsOnId, Long projectId) {
        this.taskId = taskId; this.dependsOnId = dependsOnId; this.projectId = projectId;
    }
    public Long getTaskId() { return taskId; }
    public Long getDependsOnId() { return dependsOnId; }
    public Long getProjectId() { return projectId; }
}