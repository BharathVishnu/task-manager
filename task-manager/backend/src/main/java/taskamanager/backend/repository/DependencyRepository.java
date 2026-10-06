package taskamanager.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import taskamanager.backend.model.DependencyId;
import taskamanager.backend.model.Status;
import taskamanager.backend.model.TaskDependency;

public interface DependencyRepository extends JpaRepository<TaskDependency, DependencyId> {

    List<TaskDependency> findByTaskId(Long taskId);
    List<TaskDependency> findByProjectId(Long projectId);

    /** How many of this task's dependencies are NOT done. > 0 means blocked. */
    @Query("select count(d) from TaskDependency d, Task dep " +
           "where d.taskId = :taskId and dep.id = d.dependsOnId and dep.status <> :done")
    long countUnfinished(@Param("taskId") Long taskId, @Param("done") Status done);

    /** Ids of all blocked tasks in a project, in ONE query (avoids N+1). */
    @Query("select distinct d.taskId from TaskDependency d, Task dep " +
           "where d.projectId = :projectId and dep.id = d.dependsOnId and dep.status <> :done")
    List<Long> findBlockedTaskIds(@Param("projectId") Long projectId, @Param("done") Status done);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TaskDependency d where d.taskId = :taskId and d.dependsOnId = :dependsOnId")
    int deleteEdge(@Param("taskId") Long taskId, @Param("dependsOnId") Long dependsOnId);
}