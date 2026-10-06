package taskamanager.backend.controller;

import taskamanager.backend.dto.Dtos.*;
import taskamanager.backend.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService service;
    public ProjectController(ProjectService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody CreateProjectRequest req) {
        return service.create(userId, req);
    }

    @GetMapping
    public List<ProjectResponse> mine(@AuthenticationPrincipal Long userId) {
        return service.listMine(userId);
    }

    @GetMapping("/{projectId}/members")
    public List<MemberResponse> members(@AuthenticationPrincipal Long userId, @PathVariable Long projectId) {
        return service.listMembers(userId, projectId);
    }

    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@AuthenticationPrincipal Long userId, @PathVariable Long projectId,
                                    @Valid @RequestBody AddMemberRequest req) {
        return service.addMember(userId, projectId, req);
    }

    @PutMapping("/{projectId}/members/{targetUserId}")
    public MemberResponse changeRole(@AuthenticationPrincipal Long userId, @PathVariable Long projectId,
                                     @PathVariable Long targetUserId,
                                     @Valid @RequestBody UpdateRoleRequest req) {
        return service.changeRole(userId, projectId, targetUserId, req.role());
    }

    @DeleteMapping("/{projectId}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal Long userId, @PathVariable Long projectId,
                             @PathVariable Long targetUserId) {
        service.removeMember(userId, projectId, targetUserId);
    }
}