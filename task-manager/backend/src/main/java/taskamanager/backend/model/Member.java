package taskamanager.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "members")
@IdClass(MemberId.class)
public class Member {
    @Id @Column(name = "user_id")
    private Long userId;

    @Id @Column(name = "project_id")
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected Member() {}
    public Member(Long userId, Long projectId, Role role) {
        this.userId = userId; this.projectId = projectId; this.role = role;
    }
    public Long getUserId() { return userId; }
    public Long getProjectId() { return projectId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
