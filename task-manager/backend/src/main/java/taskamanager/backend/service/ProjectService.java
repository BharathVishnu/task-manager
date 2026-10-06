package taskamanager.backend.service;


import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import taskamanager.backend.repository.*;
import taskamanager.backend.model.*;
import taskamanager.backend.dto.Dtos.AddMemberRequest;
import taskamanager.backend.dto.Dtos.CreateProjectRequest;
import taskamanager.backend.dto.Dtos.MemberResponse;
import taskamanager.backend.dto.Dtos.ProjectResponse;
import taskamanager.backend.exception.*;


@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final MemberRepository members;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final AccessService access;

    public ProjectService(ProjectRepository projects, MemberRepository members,
                          UserRepository users, TaskRepository tasks, AccessService access) {
        this.projects = projects; this.members = members; this.users = users;
        this.tasks = tasks; this.access = access;
    }

    /** One transaction: a project must never exist without an owner. */
    @Transactional
    public ProjectResponse create(Long userId, CreateProjectRequest req) {
        Project p = projects.save(new Project(req.name().trim(), userId));
        members.save(new Member(userId, p.getId(), Role.OWNER));
        return new ProjectResponse(p.getId(), p.getName(), Role.OWNER, p.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listMine(Long userId) {
        Map<Long, Role> roles = members.findByUserId(userId).stream()
                .collect(Collectors.toMap(Member::getProjectId, Member::getRole));
        return projects.findAllById(roles.keySet()).stream()
                .sorted(Comparator.comparing(Project::getCreatedAt))
                .map(p -> new ProjectResponse(p.getId(), p.getName(), roles.get(p.getId()), p.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long userId, Long projectId) {
        access.requireRole(userId, projectId, Role.VIEWER);
        List<Member> ms = members.findByProjectId(projectId);
        Map<Long, String> emails = users.findAllById(ms.stream().map(Member::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, User::getEmail));
        return ms.stream().map(m -> new MemberResponse(m.getUserId(), emails.get(m.getUserId()), m.getRole())).toList();
    }

    @Transactional
    public MemberResponse addMember(Long userId, Long projectId, AddMemberRequest req) {
        access.requireRole(userId, projectId, Role.OWNER);
        lockProject(projectId);

        User target = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new AppException(404, "USER_NOT_FOUND", "No user with that email"));
        if (members.findByUserIdAndProjectId(target.getId(), projectId).isPresent())
            throw new AppException(409, "ALREADY_MEMBER", "User is already a member");

        members.save(new Member(target.getId(), projectId, req.role()));
        return new MemberResponse(target.getId(), target.getEmail(), req.role());
    }

    @Transactional
    public MemberResponse changeRole(Long userId, Long projectId, Long targetUserId, Role newRole) {
        access.requireRole(userId, projectId, Role.OWNER);
        lockProject(projectId);   // serializes owner changes: two owners can't demote each other at once

        Member target = members.findByUserIdAndProjectId(targetUserId, projectId)
                .orElseThrow(AppException::notFound);
        if (target.getRole() == Role.OWNER && newRole != Role.OWNER)
            requireAnotherOwner(projectId);

        target.setRole(newRole);
        members.save(target);
        String email = users.findById(targetUserId).map(User::getEmail).orElse(null);
        return new MemberResponse(targetUserId, email, newRole);
    }

    @Transactional
    public void removeMember(Long userId, Long projectId, Long targetUserId) {
        access.requireRole(userId, projectId, Role.OWNER);
        lockProject(projectId);

        Member target = members.findByUserIdAndProjectId(targetUserId, projectId)
                .orElseThrow(AppException::notFound);
        if (target.getRole() == Role.OWNER) requireAnotherOwner(projectId);

        members.delete(target);
        tasks.unassignUser(projectId, targetUserId);   // their tasks become unassigned
    }

    private void lockProject(Long projectId) {
        projects.lockById(projectId).orElseThrow(AppException::notFound);
    }

    /** Called under the project lock, so the count can't change underneath us. */
    private void requireAnotherOwner(Long projectId) {
        if (members.countByProjectIdAndRole(projectId, Role.OWNER) <= 1)
            throw new AppException(422, "LAST_OWNER", "A project must keep at least one owner");
    }
}
