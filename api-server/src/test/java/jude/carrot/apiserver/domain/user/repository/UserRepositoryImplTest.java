package jude.carrot.apiserver.domain.user.repository;

import jude.carrot.apiserver.domain.user.User;
import jude.carrot.apiserver.domain.user.repository.jpa.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.context.annotation.Bean;

import java.util.Optional;
import java.util.UUID;

import static jude.carrot.apiserver.domain.user.repository.UserRepositoryImplTest.*;
import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@Testcontainers
@Transactional
@Import(UserRepositoryConfig.class)
class UserRepositoryImplTest {

    @TestConfiguration
    static class UserRepositoryConfig{
        @Bean
        public UserRepositoryImpl userRepository(UserJpaRepository userJpaRepository){
            return new UserRepositoryImpl(userJpaRepository);
        }
    }

    static String DATABASE_NAME = "test";
    static String DATABASE_USERNAME = "test";
    static String DATABASE_PASSWORD = UUID.randomUUID().toString();
    static String TEST_EMAIL = "test@gmail.com";
    static String FAKE_EMAIL = "fake@gmail.com";
    static String ENCODED_PASSWORD = "encodedPassword";

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD);


    @Autowired
    UserRepositoryImpl userRepository;


    @Test
    @DisplayName("findUser Test using TestEmail")
    void findUserTestEmail(){
        Optional<User> optionalUser = userRepository.findUser(TEST_EMAIL);

        assertThat(optionalUser).isPresent()
                .hasValueSatisfying(user -> assertThat(user.getEmail()).isEqualTo(TEST_EMAIL));
    }

    @Test
    @DisplayName("findUser Test using FakeEmail")
    void findUserFakeEmail(){
        Optional<User> optionalUser = userRepository.findUser(FAKE_EMAIL);

        assertThat(optionalUser.isPresent()).isFalse();
    }

    @Test
    @DisplayName("saveUser")
    void save() {
        userRepository.save(TEST_EMAIL,ENCODED_PASSWORD);
        Optional<User> optionalUser = userRepository.findUser(TEST_EMAIL);

        assertThat(optionalUser).isPresent()
                .hasValueSatisfying(user -> assertThat(user)
                        .extracting(User::getEmail, User::getPassword)
                        .containsExactly(TEST_EMAIL, ENCODED_PASSWORD));
    }
}