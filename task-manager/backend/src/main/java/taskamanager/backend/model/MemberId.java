package taskamanager.backend.model;

import java.io.Serializable;
import java.util.Objects;

public class MemberId implements Serializable {
    private Long userId;
    private Long projectId;

    public MemberId() {}
    public MemberId(Long userId, Long projectId) { this.userId = userId; this.projectId = projectId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberId m)) return false;
        return Objects.equals(userId, m.userId) && Objects.equals(projectId, m.projectId);
    }
    @Override public int hashCode() { return Objects.hash(userId, projectId); }
}
