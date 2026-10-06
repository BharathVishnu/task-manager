package taskamanager.backend.repository;


import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import taskamanager.backend.model.Member;
import taskamanager.backend.model.MemberId;
import taskamanager.backend.model.Role;

public interface MemberRepository extends JpaRepository<Member, MemberId> {
    Optional<Member> findByUserIdAndProjectId(Long userId, Long projectId);
    List<Member> findByUserId(Long userId);
    List<Member> findByProjectId(Long projectId);
    long countByProjectIdAndRole(Long projectId, Role role);
}
