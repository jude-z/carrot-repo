package jude.carrot.apiserver.domain.post.controller;

import jude.carrot.apiserver.domain.post.request.PostRequest.PostCreateRequest;
import jude.carrot.apiserver.domain.post.request.PostRequest.PostUpdateRequest;
import jude.carrot.apiserver.domain.post.service.PostService;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.service.exception.CustomException;
import jude.carrot.web.advice.CommonControllerAdvice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.stream.Collectors;

import static jude.carrot.apiserver.domain.post.response.PostResponse.*;
import static jude.carrot.service.status.Status.*;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostController가 PostService 호출 결과를 ApiResponse로 감싸 반환하고,
 * CustomException 발생 시 CommonControllerAdvice를 거쳐 실패 응답으로,
 * PostCreateRequest/PostUpdateRequest의 Bean Validation(@Valid) 실패 시 VALID_FAIL 응답으로
 * 이어지는지 확인하는 MVC 레이어 테스트.
 */
@WebMvcTest(controllers = PostController.class)
@Import({CommonControllerAdvice.class, PostControllerTest.ArgumentResolverConfig.class})
class PostControllerTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Long POST_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @BeforeEach
    void setUpPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(REQUESTER_ID, null));
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    private String createPostJson(String title, String content, Integer price, String latitude, String longitude, List<String> contentImageUrls) {
        return """
                {"title":%s,"content":%s,"price":%s,"latitude":%s,"longitude":%s,"contentImageUrls":%s}
                """.formatted(
                        jsonString(title), jsonString(content), jsonNumber(price),
                        jsonString(latitude), jsonString(longitude), jsonArray(contentImageUrls));
    }

    private String updatePostJson(String title, String content, Integer price, List<String> contentImageUrls) {
        return """
                {"title":%s,"content":%s,"price":%s,"contentImageUrls":%s}
                """.formatted(jsonString(title), jsonString(content), jsonNumber(price), jsonArray(contentImageUrls));
    }

    private String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private String jsonNumber(Integer value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String jsonArray(List<String> values) {
        return values == null ? "null" : values.stream()
                .map(this::jsonString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Test
    @DisplayName("게시글 목록을 조회하면 200과 함께 데이터를 반환한다")
    void fetchPosts_success() throws Exception {
        FetchPostsResponse dto = FetchPostsResponse.builder()
                .page(List.of())
                .pageNum(0)
                .pageSize(20)
                .totalPage(0)
                .elementCount(0)
                .isLast(true)
                .build();
        when(postService.fetchPosts(1, 20)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.elementCount").value(0));
    }

    @Test
    @DisplayName("게시글을 단건 조회하면 200과 함께 데이터를 반환한다")
    void fetchPost_success() throws Exception {
        FetchPostResponse dto = FetchPostResponse.builder()
                .id(POST_ID)
                .title("title")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .createdById(REQUESTER_ID)
                .createdByEmail("carrot@carrot.com")
                .build();
        when(postService.fetchPost(POST_ID)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/post/{postId}", POST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.createdByEmail").value("carrot@carrot.com"));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회하면 400과 함께 POST_NOT_EXIST 코드를 반환한다")
    void fetchPost_fail_whenPostNotExist() throws Exception {
        when(postService.fetchPost(POST_ID)).thenThrow(new CustomException(POST_NOT_EXIST));

        mockMvc.perform(get("/api/v1/post/{postId}", POST_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(POST_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("올바른 값으로 게시글 생성을 요청하면 200을 반환하고 서비스를 호출한다")
    void createPost_success() throws Exception {
        when(postService.createPost(eq(REQUESTER_ID), any())).thenReturn(CreatePostResponse.from(POST_ID));

        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"title","content":"content","price":10000,"latitude":"37.5","longitude":"127.0","contentImageUrls":["http://image.com/1.png"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.id").value(POST_ID.intValue()));

        verify(postService).createPost(eq(REQUESTER_ID), any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("인증된 동네와 실시간 주소가 다르면 400과 함께 ADDRESS_NOT_ENROLLED 코드를 반환한다")
    void createPost_fail_whenAddressNotEnrolled() throws Exception {
        when(postService.createPost(eq(REQUESTER_ID), any())).thenThrow(new CustomException(ADDRESS_NOT_ENROLLED));

        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"title","content":"content","price":10000,"latitude":"37.5","longitude":"127.0","contentImageUrls":["http://image.com/1.png"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ADDRESS_NOT_ENROLLED.getCode()));
    }

    @ParameterizedTest(name = "title=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("게시글 생성 시 제목이 비어있으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void createPost_fail_whenTitleBlank(String invalidTitle) throws Exception {
        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostJson(invalidTitle, "content", 10000, "37.5", "127.0", List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("title")));

        verify(postService, never()).createPost(any(), any());
    }

    @ParameterizedTest(name = "content=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("게시글 생성 시 내용이 비어있으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void createPost_fail_whenContentBlank(String invalidContent) throws Exception {
        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostJson("title", invalidContent, 10000, "37.5", "127.0", List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("content")));

        verify(postService, never()).createPost(any(), any());
    }

    @Test
    @DisplayName("게시글 생성 시 가격이 비어있으면 검증 실패(400, VF)를 반환한다")
    void createPost_fail_whenPriceNull() throws Exception {
        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostJson("title", "content", null, "37.5", "127.0", List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("price")));

        verify(postService, never()).createPost(any(), any());
    }

    @ParameterizedTest(name = "price={0} 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("게시글 생성 시 가격이 0 이하이면 검증 실패(400, VF)를 반환한다")
    @ValueSource(ints = {0, -1})
    void createPost_fail_whenPriceNotPositive(int invalidPrice) throws Exception {
        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostJson("title", "content", invalidPrice, "37.5", "127.0", List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("price")));

        verify(postService, never()).createPost(any(), any());
    }

    @ParameterizedTest(name = "latitude=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("게시글 생성 시 위도가 비어있으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void createPost_fail_whenLatitudeBlank(String invalidLatitude) throws Exception {
        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostJson("title", "content", 10000, invalidLatitude, "127.0", List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("latitude")));

        verify(postService, never()).createPost(any(), any());
    }

    @ParameterizedTest(name = "longitude=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("게시글 생성 시 경도가 비어있으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void createPost_fail_whenLongitudeBlank(String invalidLongitude) throws Exception {
        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostJson("title", "content", 10000, "37.5", invalidLongitude, List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("longitude")));

        verify(postService, never()).createPost(any(), any());
    }

    @ParameterizedTest
    @DisplayName("게시글 생성 시 게시글 이미지가 없으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void createPost_fail_whenContentImageUrlsEmpty(List<String> invalidContentImageUrls) throws Exception {
        mockMvc.perform(post("/api/v1/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostJson("title", "content", 10000, "37.5", "127.0", invalidContentImageUrls)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("contentImageUrls")));

        verify(postService, never()).createPost(any(), any());
    }

    @Test
    @DisplayName("올바른 값으로 게시글 수정을 요청하면 200을 반환하고 서비스를 호출한다")
    void updatePost_success() throws Exception {
        when(postService.updatePost(eq(REQUESTER_ID), eq(POST_ID), any())).thenReturn(UpdatePostResponse.from(POST_ID));

        mockMvc.perform(put("/api/v1/post/{postId}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"new title","content":"new content","price":20000,"contentImageUrls":["http://image.com/1.png"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.id").value(POST_ID.intValue()));

        verify(postService).updatePost(eq(REQUESTER_ID), eq(POST_ID), any(PostUpdateRequest.class));
    }

    @Test
    @DisplayName("작성자가 아닌 회원이 게시글 수정을 요청하면 400과 함께 UPDATE_POST_NOT_AUTHORIZE 코드를 반환한다")
    void updatePost_fail_whenRequesterIsNotWriter() throws Exception {
        when(postService.updatePost(eq(REQUESTER_ID), eq(POST_ID), any())).thenThrow(new CustomException(UPDATE_POST_NOT_AUTHORIZE));

        mockMvc.perform(put("/api/v1/post/{postId}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"new title","content":"new content","price":20000,"contentImageUrls":["http://image.com/1.png"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(UPDATE_POST_NOT_AUTHORIZE.getCode()));
    }

    @ParameterizedTest(name = "title=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("게시글 수정 시 제목이 비어있으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void updatePost_fail_whenTitleBlank(String invalidTitle) throws Exception {
        mockMvc.perform(put("/api/v1/post/{postId}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePostJson(invalidTitle, "content", 10000, List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("title")));

        verify(postService, never()).updatePost(any(), any(), any());
    }

    @ParameterizedTest(name = "content=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("게시글 수정 시 내용이 비어있으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void updatePost_fail_whenContentBlank(String invalidContent) throws Exception {
        mockMvc.perform(put("/api/v1/post/{postId}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePostJson("title", invalidContent, 10000, List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("content")));

        verify(postService, never()).updatePost(any(), any(), any());
    }

    @Test
    @DisplayName("게시글 수정 시 가격이 비어있으면 검증 실패(400, VF)를 반환한다")
    void updatePost_fail_whenPriceNull() throws Exception {
        mockMvc.perform(put("/api/v1/post/{postId}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePostJson("title", "content", null, List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("price")));

        verify(postService, never()).updatePost(any(), any(), any());
    }

    @ParameterizedTest(name = "price={0} 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("게시글 수정 시 가격이 0 이하이면 검증 실패(400, VF)를 반환한다")
    @ValueSource(ints = {0, -1})
    void updatePost_fail_whenPriceNotPositive(int invalidPrice) throws Exception {
        mockMvc.perform(put("/api/v1/post/{postId}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePostJson("title", "content", invalidPrice, List.of("http://image.com/1.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("price")));

        verify(postService, never()).updatePost(any(), any(), any());
    }

    @ParameterizedTest
    @DisplayName("게시글 수정 시 게시글 이미지가 없으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void updatePost_fail_whenContentImageUrlsEmpty(List<String> invalidContentImageUrls) throws Exception {
        mockMvc.perform(put("/api/v1/post/{postId}", POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePostJson("title", "content", 10000, invalidContentImageUrls)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("contentImageUrls")));

        verify(postService, never()).updatePost(any(), any(), any());
    }

    @Test
    @DisplayName("게시글 삭제를 요청하면 200을 반환하고 서비스를 호출한다")
    void deletePost_success() throws Exception {
        mockMvc.perform(delete("/api/v1/post/{postId}", POST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"));

        verify(postService).deletePost(REQUESTER_ID, POST_ID);
    }

    @Test
    @DisplayName("작성자가 아닌 회원이 게시글 삭제를 요청하면 400과 함께 UPDATE_POST_NOT_AUTHORIZE 코드를 반환한다")
    void deletePost_fail_whenRequesterIsNotWriter() throws Exception {
        doThrow(new CustomException(UPDATE_POST_NOT_AUTHORIZE)).when(postService).deletePost(REQUESTER_ID, POST_ID);

        mockMvc.perform(delete("/api/v1/post/{postId}", POST_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(UPDATE_POST_NOT_AUTHORIZE.getCode()));
    }

    @TestConfiguration
    static class ArgumentResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
