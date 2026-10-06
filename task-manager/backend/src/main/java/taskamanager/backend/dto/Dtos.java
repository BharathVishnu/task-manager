package taskamanager.backend.dto;

import jakarta.validation.constraints.*;
import taskamanager.backend.model.ActivityAction;
import taskamanager.backend.model.Role;

import java.time.Instant;

import ch.qos.logback.core.status.Status;

public final class Dtos {
    private Dtos() {}

    // ---- auth ----
    public record SignupRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}   // BCrypt ignores bytes past 72
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String accessToken, long expiresInSeconds) {}
    public record UserResponse(Long id, String email) {}

    // ---- projects ----
    public record CreateProjectRequest(@NotBlank @Size(max = 100) String name) {}
    public record ProjectResponse(Long id, String name, Role role, Instant createdAt) {}
    public record AddMemberRequest(@NotBlank @Email String email, @NotNull Role role) {}
    public record UpdateRoleRequest(@NotNull Role role) {}
    public record MemberResponse(Long userId, String email, Role role) {}

    // ---- tasks ----
    public record CreateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String description,
            Long assigneeId) {}
    public record UpdateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String description,
            @NotNull Status status,
            Long assigneeId,
            @NotNull Long version) {}
    public record TaskResponse(Long id, Long projectId, String title, String description,
                               Status status, Long assigneeId, Long version, boolean blocked,
                               Instant createdAt, Instant updatedAt) {}

    // ---- dependencies ----
    public record DependencyRequest(@NotNull Long dependsOnId) {}
    public record DependencyResponse(Long dependsOnId, String title, Status status) {}

    // ---- activity ----
    public record ActivityResponse(Long id, Long userId, String userEmail, ActivityAction action,
                                   String field, String oldValue, String newValue, Instant createdAt) {}
}