package jude.carrot.apiserver.domain.user.repository;

import jude.carrot.apiserver.domain.user.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findUser(String email);

    void save(String email, String encodePassword);
}
