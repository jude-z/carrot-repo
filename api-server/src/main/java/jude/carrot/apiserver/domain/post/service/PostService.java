package jude.carrot.apiserver.domain.post.service;

import jude.carrot.apiserver.domain.post.request.PostRequest;
import jude.carrot.apiserver.domain.post.request.PostRequest.PostCreateRequest;
import jude.carrot.infra.entity.image.MultipleImage;
import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.post.PostRepository;
import jude.carrot.infra.repository.post.dto.PostElement;
import jude.carrot.infra.repository.user.UserRepository;
import jude.carrot.service.exception.CustomException;
import jude.carrot.service.status.Status;
import jude.carrot.web.client.KakaoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;


import java.util.List;

import static jude.carrot.apiserver.domain.post.request.PostRequest.*;
import static jude.carrot.apiserver.domain.post.response.PostResponse.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final KakaoClient kakaoClient;

    public FetchPostsResponse fetchPosts(Integer pageNum, Integer pageSize) {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable = PageRequest.of(pageNum-1,pageSize, sort);
        Page<PostElement> page = postRepository.fetchJoinList(pageable);
        return FetchPostsResponse.from(page);
    }

    public FetchPostResponse fetchPost(Long postId) {
        Post post = postRepository.fetchJoinByPostId(postId)
                .orElseThrow(() -> new CustomException(Status.POST_NOT_EXIST));
        return FetchPostResponse.from(post);

    }

    public CreatePostResponse createPost(Long userId, PostCreateRequest postCreateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(Status.USER_NOT_EXIST));
        Address userAddress = user.getAddress();
        String longitude = postCreateRequest.longitude();
        String latitude = postCreateRequest.latitude();
        Address realTimeAddress = kakaoClient.fetchAddress(longitude, latitude);
        if(!validateAddress(userAddress,realTimeAddress)) throw new CustomException(Status.ADDRESS_NOT_ENROLLED);
        Post post = PostRequest.from(postCreateRequest, realTimeAddress, user);
        postRepository.save(post);
        Long id = post.getId();
        return CreatePostResponse.from(id);
    }

    public UpdatePostResponse updatePost(Long userId, Long postId, PostUpdateRequest postUpdateRequest) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(Status.USER_NOT_EXIST));
        Post fetchPost = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(Status.POST_NOT_EXIST));
        if(!isValidPoster(userId, fetchPost)) throw new CustomException(Status.UPDATE_POST_NOT_AUTHORIZE);
        changePost(fetchPost,postUpdateRequest);
        return UpdatePostResponse.from(postId);
    }

    public void deletePost(Long userId, Long postId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(Status.USER_NOT_EXIST));
        Post fetchPost = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(Status.POST_NOT_EXIST));
        if(!isValidPoster(userId, fetchPost)) throw new CustomException(Status.UPDATE_POST_NOT_AUTHORIZE);
        postRepository.deleteById(postId);
    }

    private boolean isValidPoster(Long userId, Post fetchPost) {
        Long posterId = fetchPost.getCreatedBy().getId();
        return userId.equals(posterId);
    }

    private boolean validateAddress(Address userAddress,Address realTimeAddress){
        String userGu = userAddress.getGu();
        String realTimeGu = realTimeAddress.getGu();
        String userDong = userAddress.getDong();
        String realTimeDong = realTimeAddress.getDong();
        return userGu.equals(realTimeGu) || userDong.equals(realTimeDong);
    }

    public void changePost(Post post, PostUpdateRequest postUpdateRequest){
        String thumbnailImageUrl = postUpdateRequest.thumbNailImageUrl();
        List<String> contentImageUrls = postUpdateRequest.contentImageUrls();
        List<MultipleImage> contentImages = contentImageUrls.stream()
                .map(url -> MultipleImage.from(url, post))
                .toList();
        post.setTitle(postUpdateRequest.title());
        post.setContent(postUpdateRequest.content());
        post.setPrice(postUpdateRequest.price());
        if(StringUtils.hasText(thumbnailImageUrl)){
            post.setThumbnailImage(SingleImage.from(thumbnailImageUrl));
        }
        if(!ObjectUtils.isEmpty(contentImages)){
            post.getContentImages().clear();
            post.getContentImages().addAll(contentImages);
        }
    }
}
