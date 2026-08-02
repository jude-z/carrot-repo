package jude.carrot.infra.repository.user;

import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.user.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.mapping.UserDefinedArrayType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository{
    private final UserJpaRepository userJpaRepository;

    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public void save(User user) {
        userJpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }
}
