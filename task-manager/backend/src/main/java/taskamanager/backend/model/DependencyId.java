package taskamanager.backend.model;

import java.io.Serializable;
import java.util.Objects;

public class DependencyId implements Serializable {
    private Long taskId;
    private Long dependsOnId;

    public DependencyId() {}
    public DependencyId(Long taskId, Long dependsOnId) { this.taskId = taskId; this.dependsOnId = dependsOnId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependencyId d)) return false;
        return Objects.equals(taskId, d.taskId) && Objects.equals(dependsOnId, d.dependsOnId);
    }
    @Override public int hashCode() { return Objects.hash(taskId, dependsOnId); }
}