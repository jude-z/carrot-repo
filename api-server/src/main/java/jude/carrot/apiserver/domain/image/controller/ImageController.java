package jude.carrot.apiserver.domain.image.controller;

import jude.carrot.apiserver.domain.image.response.ImageResponse.PresignedUrlResponse;
import jude.carrot.apiserver.domain.image.service.ImageService;
import jude.carrot.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static jude.carrot.service.status.Status.SUCCESS;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/image/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> fetchPresignedUrl(){
        PresignedUrlResponse presignedUrlResponse = imageService.fetchPresignedUrl();
        return successResponseEntity(presignedUrlResponse);
    }

    private <T> ResponseEntity<ApiResponse<T>> successResponseEntity(T data){
        ApiResponse<T> apiResponse = ApiResponse.successFrom(data);
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }
}
