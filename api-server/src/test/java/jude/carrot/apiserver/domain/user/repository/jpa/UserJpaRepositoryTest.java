package jude.carrot.apiserver.domain.user.repository.jpa;

import jude.carrot.apiserver.domain.user.User;
import jude.carrot.apiserver.domain.user.fixture.domain.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@Transactional
public class UserJpaRepositoryTest {

    static String DATABASE_NAME = "test";
    static String DATABASE_USERNAME = "test";
    static String DATABASE_PASSWORD = UUID.randomUUID().toString();
    static String TEST_EMAIL = "test@gmail.com";
    static String FAKE_EMAIL = "fake@gmail.com";

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD);

    @Autowired
    UserJpaRepository userJpaRepository;

    @BeforeEach
    void setUp(){
        User user = UserFactory.create(TEST_EMAIL);
        userJpaRepository.save(user);
    }

    @Test
    @DisplayName("findByEmail Test using TestEmail")
    void findByEmailTestEmail(){
        Optional<User> optionalUser = userJpaRepository.findByEmail(TEST_EMAIL);

        assertThat(optionalUser).isPresent()
                .hasValueSatisfying(user -> assertThat(user.getEmail()).isEqualTo(TEST_EMAIL));
    }

    @Test
    @DisplayName("findByEmail Test using FakeEmail")
    void findByEmailFakeEmail(){
        Optional<User> optionalUser = userJpaRepository.findByEmail(FAKE_EMAIL);
        assertThat(optionalUser.isPresent()).isFalse();
    }


}
