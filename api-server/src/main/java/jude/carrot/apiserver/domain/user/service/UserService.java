package jude.carrot.apiserver.domain.user.service;

import jakarta.validation.Valid;
import jude.carrot.apiserver.domain.user.exception.UserFoundException;
import jude.carrot.apiserver.common.status.Status;
import jude.carrot.apiserver.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static jude.carrot.apiserver.domain.user.dto.request.UserRequest.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void signUp(SignUpRequest signUpRequest) {
        String email = signUpRequest.getEmail();
        String password = signUpRequest.getPassword();
        userRepository.findUser(email)
                .ifPresent(user -> { throw new UserFoundException(Status.USER_EXIST);});
        String encodePassword = passwordEncoder.encode(password);
        userRepository.save(email,encodePassword);
    }
}
