package jude.carrot.apiserver.user.repository;

import jude.carrot.apiserver.user.domain.User;
import jude.carrot.apiserver.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository{
    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findUser(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public void save(String email, String encodePassword) {
        User user = User.builder()
                .email(email)
                .password(encodePassword)
                .build();
        userJpaRepository.save(user);
    }
}
