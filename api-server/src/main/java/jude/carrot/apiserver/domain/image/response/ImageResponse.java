package jude.carrot.apiserver.domain.image.response;

import lombok.Builder;


public class ImageResponse {
    private ImageResponse(){}

    @Builder
    public record PresignedUrlResponse(
            String url
    ) {

        public static PresignedUrlResponse from(String url){
            return PresignedUrlResponse.builder()
                    .url(url)
                    .build();
        }
    }


}
