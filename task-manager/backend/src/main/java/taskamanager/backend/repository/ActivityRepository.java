package taskamanager.backend.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import taskamanager.backend.model.Activity;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByTaskIdOrderByCreatedAtDescIdDesc(Long taskId, Pageable pageable);
}