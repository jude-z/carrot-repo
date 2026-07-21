package jude.carrot.apiserver.domain.user.repository.jpa;

import jude.carrot.apiserver.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
}
