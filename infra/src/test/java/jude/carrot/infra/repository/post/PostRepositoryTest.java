package jude.carrot.infra.repository.post;

import jude.carrot.infra.InfraTestConfig;
import jude.carrot.infra.entity.image.MultipleImage;
import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.post.dto.PostElement;
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

import java.util.ArrayList;
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
        user = userJpaRepository.save(User.builder()
                .email("carrot@carrot.com")
                .password("password")
                .build());
    }

    @Test
    @DisplayName("게시글을 저장하면 id가 채번된다")
    void saveAssignsId() {
        Post post = Post.builder()
                .title("title")
                .content("content")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(user)
                .contentImages(new ArrayList<>())
                .build();

        postRepository.save(post);

        assertThat(post.getId()).isNotNull();
    }

    @Test
    @DisplayName("존재하는 게시글은 findById로 조회된다")
    void findByIdSuccess() {
        Post post = Post.builder()
                .title("title")
                .content("content")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(user)
                .contentImages(new ArrayList<>())
                .build();
        postRepository.save(post);
        entityManager.flush();
        entityManager.clear();

        Optional<Post> found = postRepository.findById(post.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(p -> assertThat(p.getTitle()).isEqualTo("title"));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 findById로 조회하면 비어있다")
    void findByIdFailWhenNotExist() {
        Optional<Post> found = postRepository.findById(-1L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("존재하는 게시글은 작성자/썸네일/이미지가 함께 조회된다")
    void fetchJoinByPostIdSuccess() {
        Post post = Post.builder()
                .title("title")
                .content("content")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(user)
                .thumbnailImage(SingleImage.from("http://image.com/thumb.png"))
                .contentImages(new ArrayList<>())
                .build();
        List<String> contentImageUrls = List.of("http://image.com/1.png", "http://image.com/2.png");
        post.setContentImages(contentImageUrls.stream().map(url -> MultipleImage.from(url, post)).toList());
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
    void fetchJoinByPostIdFailWhenNotExist() {
        Optional<Post> found = postRepository.fetchJoinByPostId(-1L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("게시글 목록을 페이지 단위로 조회한다")
    void fetchJoinListSuccess() {
        Post post1 = Post.builder()
                .title("title")
                .content("content")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(user)
                .thumbnailImage(SingleImage.from("http://image.com/thumb1.png"))
                .contentImages(new ArrayList<>())
                .build();
        post1.setContentImages(List.of("http://image.com/1.png").stream().map(url -> MultipleImage.from(url, post1)).toList());
        Post post2 = Post.builder()
                .title("title")
                .content("content")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(user)
                .thumbnailImage(SingleImage.from("http://image.com/thumb2.png"))
                .contentImages(new ArrayList<>())
                .build();
        post2.setContentImages(List.of("http://image.com/2.png").stream().map(url -> MultipleImage.from(url, post2)).toList());
        postRepository.save(post1);
        postRepository.save(post2);
        entityManager.flush();
        entityManager.clear();

        Page<PostElement> page = postRepository.fetchJoinList(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(PostElement::createdByEmail)
                .containsOnly(user.getEmail());
    }

    @Test
    @DisplayName("게시글을 삭제하면 더 이상 조회되지 않는다")
    void deleteByIdSuccess() {
        Post post = Post.builder()
                .title("title")
                .content("content")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(user)
                .thumbnailImage(SingleImage.from("http://image.com/thumb.png"))
                .contentImages(new ArrayList<>())
                .build();
        post.setContentImages(List.of("http://image.com/1.png").stream().map(url -> MultipleImage.from(url, post)).toList());
        postRepository.save(post);
        entityManager.flush();
        Long postId = post.getId();

        postRepository.deleteById(postId);
        entityManager.flush();
        entityManager.clear();

        assertThat(postRepository.findById(postId)).isEmpty();
    }
}
