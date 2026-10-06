package taskamanager.backend.model;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.TODO;

    @Column(name = "assignee_id")
    private Long assigneeId;

    /** Hibernate adds "WHERE version = ?" to every UPDATE and increments it. */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    protected Task() {}
    public Task(Long projectId, String title, String description, Long assigneeId) {
        this.projectId = projectId;
        this.title = title;
        this.description = description == null ? "" : description;
        this.assigneeId = assigneeId;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long a) { this.assigneeId = a; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
