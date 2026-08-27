package jude.carrot.apiserver.domain.post.controller;

import jakarta.validation.Valid;
import jude.carrot.apiserver.domain.post.response.PostResponse.FetchPostResponse;
import jude.carrot.apiserver.domain.post.service.PostService;
import jude.carrot.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static jude.carrot.apiserver.domain.post.request.PostRequest.*;
import static jude.carrot.apiserver.domain.post.response.PostResponse.*;
import static jude.carrot.service.status.Status.SUCCESS;

@RestController
@RequestMapping("api/v1/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    ResponseEntity<ApiResponse<FetchPostsResponse>> fetchPosts(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize){
        FetchPostsResponse fetchPostsResponse = postService.fetchPosts(pageNum, pageSize);
        return successResponseEntity(fetchPostsResponse);
    }

    @GetMapping("/{postId}")
    ResponseEntity<ApiResponse<FetchPostResponse>> fetchPost(@PathVariable Long postId){
        FetchPostResponse fetchPostResponse = postService.fetchPost(postId);
        return successResponseEntity(fetchPostResponse);
    }

    @PostMapping
    ResponseEntity<ApiResponse<CreatePostResponse>> createPost(@AuthenticationPrincipal Long userId,
                                              @Valid @RequestBody PostCreateRequest postCreateRequest){
        CreatePostResponse createPostResponse = postService.createPost(userId, postCreateRequest);
        return successResponseEntity(createPostResponse);
    }

    @PutMapping("/{postId}")
    ResponseEntity<ApiResponse<UpdatePostResponse>> updatePost(@AuthenticationPrincipal Long userId,
                                                               @PathVariable Long postId,
                                                               @Valid @RequestBody PostUpdateRequest postUpdateRequest){
        UpdatePostResponse updatePostResponse = postService.updatePost(userId, postId, postUpdateRequest);
        return successResponseEntity(updatePostResponse);
    }

    @DeleteMapping("/{postId}")
    ResponseEntity<ApiResponse<Void>> deletePost(@AuthenticationPrincipal Long userId,
                                                 @PathVariable Long postId){
        postService.deletePost(userId,postId);
        return successResponseEntity();
    }

    private <T> ResponseEntity<ApiResponse<T>> successResponseEntity(T data){
        ApiResponse<T> apiResponse = ApiResponse.successFrom(data);
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }

    private ResponseEntity<ApiResponse<Void>> successResponseEntity(){
        ApiResponse<Void> apiResponse = ApiResponse.successFrom();
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }


}
