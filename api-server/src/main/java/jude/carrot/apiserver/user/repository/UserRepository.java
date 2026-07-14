package jude.carrot.apiserver.user.repository;

import jude.carrot.apiserver.user.domain.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findUser(String email);

    void save(String email, String encodePassword);
}
