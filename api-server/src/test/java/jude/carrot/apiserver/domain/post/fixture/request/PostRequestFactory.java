package jude.carrot.apiserver.domain.post.fixture.request;

import java.util.List;

import static jude.carrot.apiserver.domain.post.request.PostRequest.PostCreateRequest;
import static jude.carrot.apiserver.domain.post.request.PostRequest.PostUpdateRequest;

public class PostRequestFactory {

    private PostRequestFactory() {
    }

    public static PostCreateRequest createPostCreateRequest(String title, Integer price, String longitude, String latitude,
                                                              List<String> contentImageUrls) {
        return PostCreateRequest.builder()
                .title(title)
                .content("content")
                .price(price)
                .longitude(longitude)
                .latitude(latitude)
                .contentImageUrls(contentImageUrls)
                .build();
    }

    public static PostUpdateRequest createPostUpdateRequest(String title, Integer price, String thumbNailImageUrl,
                                                              List<String> contentImageUrls) {
        return PostUpdateRequest.builder()
                .title(title)
                .content("content")
                .price(price)
                .thumbNailImageUrl(thumbNailImageUrl)
                .contentImageUrls(contentImageUrls)
                .build();
    }
}
