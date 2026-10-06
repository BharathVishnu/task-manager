package taskamanager.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import taskamanager.backend.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    /** Used when a member is removed: their tasks become unassigned. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Task t set t.assigneeId = null, t.version = t.version + 1 " +
           "where t.projectId = :projectId and t.assigneeId = :userId")
    int unassignUser(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
