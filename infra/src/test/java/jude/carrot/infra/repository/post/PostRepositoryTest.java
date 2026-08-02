package jude.carrot.infra.repository.post;

import jude.carrot.infra.InfraTestConfig;
import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.fixture.post.PostFactory;
import jude.carrot.infra.fixture.user.UserFactory;
import jude.carrot.infra.repository.post.dto.PostDto.PostElement;
import jude.carrot.infra.repository.post.jpa.PostJpaRepository;
import jude.carrot.infra.repository.user.jpa.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@ContextConfiguration(classes = InfraTestConfig.class)
@Testcontainers
@Transactional
@Import(PostRepositoryTest.PostRepositoryConfig.class)
class PostRepositoryTest {

    @TestConfiguration
    static class PostRepositoryConfig {
        @Bean
        PostRepository postRepository(PostJpaRepository postJpaRepository) {
            return new PostRepositoryImpl(postJpaRepository);
        }
    }

    static final String DATABASE_NAME = "test";
    static final String DATABASE_USERNAME = "test";
    static final String DATABASE_PASSWORD = UUID.randomUUID().toString();

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD);

    @Autowired
    PostRepository postRepository;
    @Autowired
    UserJpaRepository userJpaRepository;
    @Autowired
    TestEntityManager entityManager;

    User user;

    @BeforeEach
    void setUp() {
        user = userJpaRepository.save(UserFactory.create("carrot@carrot.com"));
    }

    @Test
    @DisplayName("게시글을 저장하면 id가 채번된다")
    void save_assignsId() {
        Post post = PostFactory.create(user);

        postRepository.save(post);

        assertThat(post.getId()).isNotNull();
    }

    @Test
    @DisplayName("존재하는 게시글은 findById로 조회된다")
    void findById_success() {
        Post post = PostFactory.create(user);
        postRepository.save(post);
        entityManager.flush();
        entityManager.clear();

        Optional<Post> found = postRepository.findById(post.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(p -> assertThat(p.getTitle()).isEqualTo("title"));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 findById로 조회하면 비어있다")
    void findById_fail_whenNotExist() {
        Optional<Post> found = postRepository.findById(-1L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("존재하는 게시글은 작성자/썸네일/이미지가 함께 조회된다")
    void fetchJoinByPostId_success() {
        Post post = PostFactory.createWithImages(user, "http://image.com/thumb.png", List.of("http://image.com/1.png", "http://image.com/2.png"));
        postRepository.save(post);
        entityManager.flush();
        entityManager.clear();

        Optional<Post> found = postRepository.fetchJoinByPostId(post.getId());

        assertThat(found).isPresent();
        Post fetched = found.get();
        assertThat(fetched.getCreatedBy().getEmail()).isEqualTo(user.getEmail());
        assertThat(fetched.getThumbnailImage().getUrl()).isEqualTo("http://image.com/thumb.png");
        assertThat(fetched.getContentImages()).extracting("url")
                .containsExactlyInAnyOrder("http://image.com/1.png", "http://image.com/2.png");
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 fetchJoinByPostId로 조회하면 비어있다")
    void fetchJoinByPostId_fail_whenNotExist() {
        Optional<Post> found = postRepository.fetchJoinByPostId(-1L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("게시글 목록을 페이지 단위로 조회한다")
    void fetchJoinList_success() {
        Post post1 = PostFactory.createWithImages(user, "http://image.com/thumb1.png", List.of("http://image.com/1.png"));
        Post post2 = PostFactory.createWithImages(user, "http://image.com/thumb2.png", List.of("http://image.com/2.png"));
        postRepository.save(post1);
        postRepository.save(post2);
        entityManager.flush();
        entityManager.clear();

        Page<PostElement> page = postRepository.fetchJoinList(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(PostElement::getCreatedByEmail)
                .containsOnly(user.getEmail());
    }

    @Test
    @DisplayName("게시글을 삭제하면 더 이상 조회되지 않는다")
    void deleteById_success() {
        Post post = PostFactory.createWithImages(user, "http://image.com/thumb.png", List.of("http://image.com/1.png"));
        postRepository.save(post);
        entityManager.flush();
        Long postId = post.getId();

        postRepository.deleteById(postId);
        entityManager.flush();
        entityManager.clear();

        assertThat(postRepository.findById(postId)).isEmpty();
    }
}
