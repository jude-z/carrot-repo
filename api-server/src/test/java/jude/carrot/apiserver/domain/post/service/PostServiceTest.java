package jude.carrot.apiserver.domain.post.service;

import jude.carrot.infra.entity.image.MultipleImage;
import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.post.PostRepository;
import jude.carrot.infra.repository.post.dto.PostElement;
import jude.carrot.infra.repository.user.UserRepository;
import jude.carrot.service.exception.CustomException;
import jude.carrot.web.client.KakaoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static jude.carrot.apiserver.domain.post.fixture.request.PostRequestFactory.createPostCreateRequest;
import static jude.carrot.apiserver.domain.post.fixture.request.PostRequestFactory.createPostUpdateRequest;
import static jude.carrot.apiserver.domain.post.request.PostRequest.PostCreateRequest;
import static jude.carrot.apiserver.domain.post.request.PostRequest.PostUpdateRequest;
import static jude.carrot.apiserver.domain.post.response.PostResponse.*;
import static jude.carrot.service.status.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final String EMAIL = "carrot@carrot.com";
    private static final String NICKNAME = "carrot";
    private static final String TITLE = "title";
    private static final Integer PRICE = 10000;

    @InjectMocks
    PostService postService;

    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private KakaoClient kakaoClient;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.from(EMAIL, "encoded-password", NICKNAME, null);
        user.setId(USER_ID);
    }

    private Post postOf(Long creatorId, List<MultipleImage> contentImages) {
        User creator = User.from(EMAIL, "encoded-password", NICKNAME, null);
        creator.setId(creatorId);
        return Post.builder()
                .title(TITLE)
                .content("content")
                .price(PRICE)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(creator)
                .contentImages(contentImages)
                .build();
    }

    @Test
    @DisplayName("게시글 목록을 페이지 단위로 조회한다")
    void fetchPosts_success() {
        PostElement element = PostElement.builder()
                .id(POST_ID)
                .title(TITLE)
                .price(PRICE)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .createdById(USER_ID)
                .createdByEmail(EMAIL)
                .build();
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        Page<PostElement> page = new PageImpl<>(List.of(element), pageable, 1);
        when(postRepository.fetchJoinList(any())).thenReturn(page);

        FetchPostsResponse response = postService.fetchPosts(1, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).fetchJoinList(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);

        assertThat(response.page()).containsExactly(element);
        assertThat(response.pageNum()).isZero();
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.elementCount()).isEqualTo(1);
        assertThat(response.totalPage()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 단건 조회하면 CustomException(POST_NOT_EXIST)을 던진다")
    void fetchPost_fail_whenPostNotExist() {
        when(postRepository.fetchJoinByPostId(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.fetchPost(POST_ID))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(POST_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("존재하는 게시글을 단건 조회하면 작성자 정보를 포함한 응답을 반환한다")
    void fetchPost_success() {
        Post post = postOf(USER_ID, List.of(MultipleImage.from("http://image.com/1.png")));
        when(postRepository.fetchJoinByPostId(POST_ID)).thenReturn(Optional.of(post));

        FetchPostResponse response = postService.fetchPost(POST_ID);

        assertThat(response.title()).isEqualTo(TITLE);
        assertThat(response.price()).isEqualTo(PRICE);
        assertThat(response.createdById()).isEqualTo(USER_ID);
        assertThat(response.createdByEmail()).isEqualTo(EMAIL);
        assertThat(response.contentImageUrls()).containsExactly("http://image.com/1.png");
    }

    @Test
    @DisplayName("존재하지 않는 회원이 게시글을 작성하면 CustomException(USER_NOT_EXIST)을 던진다")
    void createPost_fail_whenUserNotExist() {
        PostCreateRequest request = createPostCreateRequest(TITLE, PRICE, "127.0", "37.5", List.of("http://image.com/1.png"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(USER_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_NOT_EXIST.getHttpStatus());

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("실시간 주소가 회원의 인증된 동네와 일치하지 않으면 CustomException(ADDRESS_NOT_ENROLLED)을 던진다")
    void createPost_fail_whenAddressNotEnrolled() {
        user.setAddress(Address.from("서울시", "강남구", "역삼동"));
        PostCreateRequest request = createPostCreateRequest(TITLE, PRICE, "127.0", "37.5", List.of("http://image.com/1.png"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(kakaoClient.fetchAddress("127.0", "37.5")).thenReturn(Address.from("경기도", "분당구", "정자동"));

        assertThatThrownBy(() -> postService.createPost(USER_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(ADDRESS_NOT_ENROLLED.getHttpStatus());

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("실시간 주소가 회원의 인증된 동네와 일치하면 게시글을 저장한다")
    void createPost_success() {
        Address verifiedAddress = Address.from("서울시", "강남구", "역삼동");
        user.setAddress(verifiedAddress);
        Address realTimeAddress = Address.from("서울시", "강남구", "삼성동");
        List<String> contentImageUrls = List.of("http://image.com/1.png", "http://image.com/2.png");
        PostCreateRequest request = createPostCreateRequest(TITLE, PRICE, "127.0", "37.5", contentImageUrls);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(kakaoClient.fetchAddress("127.0", "37.5")).thenReturn(realTimeAddress);

        postService.createPost(USER_ID, request);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post savedPost = captor.getValue();
        assertThat(savedPost.getTitle()).isEqualTo(TITLE);
        assertThat(savedPost.getPrice()).isEqualTo(PRICE);
        assertThat(savedPost.getAddress()).isEqualTo(realTimeAddress);
        assertThat(savedPost.getCreatedBy()).isEqualTo(user);
        assertThat(savedPost.getContentImages()).extracting("url").containsExactlyElementsOf(contentImageUrls);
        assertThat(savedPost.getContentImages()).allSatisfy(image -> assertThat(image.getPost()).isEqualTo(savedPost));
    }

    @Test
    @DisplayName("존재하지 않는 회원이 게시글을 수정하면 CustomException(USER_NOT_EXIST)을 던진다")
    void updatePost_fail_whenUserNotExist() {
        PostUpdateRequest request = createPostUpdateRequest(TITLE, PRICE, null, List.of("http://image.com/1.png"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(USER_ID, POST_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 수정하면 CustomException(POST_NOT_EXIST)을 던진다")
    void updatePost_fail_whenPostNotExist() {
        PostUpdateRequest request = createPostUpdateRequest(TITLE, PRICE, null, List.of("http://image.com/1.png"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(USER_ID, POST_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(POST_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("작성자가 아닌 회원이 게시글을 수정하면 CustomException(UPDATE_POST_NOT_AUTHORIZE)을 던진다")
    void updatePost_fail_whenRequesterIsNotWriter() {
        Post post = postOf(OTHER_USER_ID, List.of());
        PostUpdateRequest request = createPostUpdateRequest(TITLE, PRICE, null, List.of("http://image.com/1.png"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(USER_ID, POST_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(UPDATE_POST_NOT_AUTHORIZE.getHttpStatus());
    }

    @Test
    @DisplayName("작성자 본인이 게시글을 수정하면 제목/내용/가격/썸네일/이미지가 갱신된다")
    void updatePost_success() {
        Post post = postOf(USER_ID, List.of(MultipleImage.from("http://image.com/old.png")));
        String newTitle = "new title";
        Integer newPrice = 20000;
        String newThumbnailUrl = "http://image.com/new-thumb.png";
        List<String> newContentImageUrls = List.of("http://image.com/new1.png", "http://image.com/new2.png");
        PostUpdateRequest request = createPostUpdateRequest(newTitle, newPrice, newThumbnailUrl, newContentImageUrls);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        postService.updatePost(USER_ID, POST_ID, request);

        assertThat(post.getTitle()).isEqualTo(newTitle);
        assertThat(post.getContent()).isEqualTo("content");
        assertThat(post.getPrice()).isEqualTo(newPrice);
        assertThat(post.getThumbnailImage().getUrl()).isEqualTo(newThumbnailUrl);
        assertThat(post.getContentImages()).extracting("url").containsExactlyElementsOf(newContentImageUrls);
    }

    @Test
    @DisplayName("썸네일 URL이 비어있으면 기존 썸네일 이미지를 그대로 유지한다")
    void updatePost_success_keepsThumbnailWhenBlank() {
        Post post = postOf(USER_ID, List.of());
        PostUpdateRequest request = createPostUpdateRequest(TITLE, PRICE, "", List.of("http://image.com/1.png"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        postService.updatePost(USER_ID, POST_ID, request);

        assertThat(post.getThumbnailImage()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 회원이 게시글을 삭제하려하면 CustomException(USER_NOT_EXIST)을 던진다")
    void deletePost_fail_whenUserNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(USER_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 삭제하려하면 CustomException(POST_NOT_EXIST)을 던진다")
    void deletePost_fail_whenPostNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(USER_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(POST_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("작성자가 아닌 회원이 게시글을 삭제하려하면 CustomException(UPDATE_POST_NOT_AUTHORIZE)을 던진다")
    void deletePost_fail_whenRequesterIsNotWriter() {
        Post post = postOf(OTHER_USER_ID, List.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(USER_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(UPDATE_POST_NOT_AUTHORIZE.getHttpStatus());
    }

    @Test
    @DisplayName("작성자 본인이 게시글 삭제를 요청하면 게시글이 삭제된다")
    void deletePost_success_whenRequesterIsWriter() {
        Post post = postOf(USER_ID, List.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        postService.deletePost(USER_ID, POST_ID);

        verify(postRepository).deleteById(POST_ID);
    }

    @Test
    @DisplayName("작성자가 아니면 게시글 삭제 요청 시 실제 삭제는 일어나지 않는다")
    void deletePost_fail_whenRequesterIsNotWriter_doesNotDelete() {
        Post post = postOf(OTHER_USER_ID, List.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(USER_ID, POST_ID))
                .isInstanceOf(CustomException.class);

        verify(postRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("썸네일 URL이 있으면 게시글 생성 시 썸네일 이미지가 설정된다")
    void createPost_success_setsThumbnailImageWhenUrlProvided() {
        Address verifiedAddress = Address.from("서울시", "강남구", "역삼동");
        user.setAddress(verifiedAddress);
        String thumbnailUrl = "http://image.com/thumb.png";
        PostCreateRequest request = PostCreateRequest.builder()
                .title(TITLE)
                .content("content")
                .price(PRICE)
                .longitude("127.0")
                .latitude("37.5")
                .thumbNailImageUrl(thumbnailUrl)
                .contentImageUrls(List.of("http://image.com/1.png"))
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(kakaoClient.fetchAddress("127.0", "37.5")).thenReturn(verifiedAddress);

        postService.createPost(USER_ID, request);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getThumbnailImage().getUrl()).isEqualTo(thumbnailUrl);
    }
}
