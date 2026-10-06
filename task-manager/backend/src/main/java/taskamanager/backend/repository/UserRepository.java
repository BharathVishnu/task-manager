package taskamanager.backend.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import taskamanager.backend.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
