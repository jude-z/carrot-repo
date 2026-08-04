package jude.carrot.infra.repository.user;

import jude.carrot.infra.InfraTestConfig;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.fixture.user.UserFactory;
import jude.carrot.infra.repository.user.jpa.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@ContextConfiguration(classes = InfraTestConfig.class)
@Testcontainers
@Import(UserRepositoryTest.UserRepositoryConfig.class)
class UserRepositoryTest {

    @TestConfiguration
    static class UserRepositoryConfig {
        @Bean
        UserRepository userRepository(UserJpaRepository userJpaRepository) {
            return new UserRepositoryImpl(userJpaRepository);
        }
    }

    static final String DATABASE_NAME = "test";
    static final String DATABASE_USERNAME = "test";
    static final String DATABASE_PASSWORD = UUID.randomUUID().toString();
    static final String TEST_EMAIL = "carrot@carrot.com";
    static final String FAKE_EMAIL = "fake@carrot.com";

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD);

    @Autowired
    UserRepository userRepository;
    @Autowired
    TestEntityManager entityManager;

    @Test
    @DisplayName("회원을 저장하면 id가 채번된다")
    void saveAssignsId() {
        User user = UserFactory.create(TEST_EMAIL);

        userRepository.save(user);

        assertThat(user.getId()).isNotNull();
    }

    @Test
    @DisplayName("존재하는 이메일로 조회하면 회원이 반환된다")
    void findByEmailSuccess() {
        User user = UserFactory.create(TEST_EMAIL);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findByEmail(TEST_EMAIL);

        assertThat(found).isPresent()
                .hasValueSatisfying(u -> assertThat(u.getEmail()).isEqualTo(TEST_EMAIL));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 비어있다")
    void findByEmailFailWhenNotExist() {
        Optional<User> found = userRepository.findByEmail(FAKE_EMAIL);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("존재하는 회원은 id로 조회된다")
    void findByIdSuccess() {
        User user = UserFactory.create(TEST_EMAIL);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(user.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(u -> assertThat(u.getEmail()).isEqualTo(TEST_EMAIL));
    }

    @Test
    @DisplayName("존재하지 않는 id로 조회하면 비어있다")
    void findByIdFailWhenNotExist() {
        Optional<User> found = userRepository.findById(-1L);

        assertThat(found).isEmpty();
    }
}
