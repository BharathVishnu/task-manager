package taskamanager.backend.repository;

import jakarta.persistence.LockModeType;
import taskamanager.backend.model.Project;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** SELECT ... FOR UPDATE. Serializes dependency edits and member changes per project. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id")
    Optional<Project> lockById(@Param("id") Long id);
}