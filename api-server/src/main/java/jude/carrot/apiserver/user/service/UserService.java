package jude.carrot.apiserver.user.service;

import jakarta.validation.Valid;
import jude.carrot.apiserver.common.exception.UserFoundException;
import jude.carrot.apiserver.common.status.Status;
import jude.carrot.apiserver.user.domain.User;
import jude.carrot.apiserver.user.dto.request.UserRequest;
import jude.carrot.apiserver.user.repository.UserRepository;
import jude.carrot.apiserver.user.repository.UserRepositoryImpl;
import jude.carrot.apiserver.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static jude.carrot.apiserver.user.dto.request.UserRequest.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void signUp(@Valid SignUpRequest signUpRequest) {
        String email = signUpRequest.getEmail();
        String password = signUpRequest.getPassword();
        userRepository.findUser(email)
                .ifPresent(user -> { throw new UserFoundException(Status.USER_EXIST);});
        String encodePassword = passwordEncoder.encode(password);
        userRepository.save(email,encodePassword);
    }
}
