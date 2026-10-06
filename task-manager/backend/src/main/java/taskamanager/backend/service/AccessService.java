package taskamanager.backend.service;

import org.springframework.stereotype.Service;

import taskamanager.backend.exception.AppException;
import taskamanager.backend.model.Member;
import taskamanager.backend.model.Role;
import taskamanager.backend.repository.MemberRepository;

/** The ONE place that answers "may this user do this in this project?" */
@Service
public class AccessService {
    private final MemberRepository members;

    public AccessService(MemberRepository members) { this.members = members; }

    public Role requireRole(Long userId, Long projectId, Role min) {
        Member m = members.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(AppException::notFound);                  // non-member: 404 hides existence
        if (!m.getRole().atLeast(min))
            throw new AppException(403, "FORBIDDEN", "Insufficient role");
        return m.getRole();
    }
}
