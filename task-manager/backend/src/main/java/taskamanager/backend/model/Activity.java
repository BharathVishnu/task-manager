package taskamanager.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Append-only: no setters, never updated. */
@Entity
@Table(name = "activity")
public class Activity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityAction action;

    private String field;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Activity() {}
    public Activity(Long taskId, Long userId, ActivityAction action, String field, String oldValue, String newValue) {
        this.taskId = taskId; this.userId = userId; this.action = action;
        this.field = field; this.oldValue = oldValue; this.newValue = newValue;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public ActivityAction getAction() { return action; }
    public String getField() { return field; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Instant getCreatedAt() { return createdAt; }
}
