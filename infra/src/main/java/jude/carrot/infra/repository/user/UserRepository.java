package jude.carrot.infra.repository.user;


import jude.carrot.infra.entity.user.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    void save(User user);

    Optional<User> findById(Long id);
}
