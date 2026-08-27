package jude.carrot.apiserver.domain.image.service;

import jude.carrot.apiserver.domain.image.response.ImageResponse.PresignedUrlResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageServiceTest {

    private static final String BUCKET_NAME = "carrot-bucket";
    private static final String KEY_NAME = "profile/image.png";

    private final ImageService imageService = new ImageService(BUCKET_NAME, KEY_NAME);

    @Test
    @DisplayName("presigned url을 발급받으면 S3Presigner가 생성한 url을 응답으로 반환한다")
    void fetchPresignedUrl_success() throws Exception {
        SdkHttpRequest httpRequest = SdkHttpRequest.builder()
                .method(SdkHttpMethod.PUT)
                .uri(URI.create("https://" + BUCKET_NAME + ".s3.amazonaws.com/" + KEY_NAME + "?X-Amz-Signature=abc"))
                .build();
        PresignedPutObjectRequest presignedRequest = PresignedPutObjectRequest.builder()
                .httpRequest(httpRequest)
                .expiration(Instant.now().plusSeconds(600))
                .isBrowserExecutable(false)
                .signedHeaders(Map.of("host", List.of(BUCKET_NAME + ".s3.amazonaws.com")))
                .build();

        S3Presigner presigner = mock(S3Presigner.class);
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        PresignedUrlResponse response;
        try (MockedStatic<S3Presigner> mockedStatic = mockStatic(S3Presigner.class)) {
            mockedStatic.when(S3Presigner::create).thenReturn(presigner);

            response = imageService.fetchPresignedUrl();
        }

        assertThat(response.url()).isEqualTo(httpRequest.getUri().toURL().toExternalForm());

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(captor.capture());
        PutObjectRequest putObjectRequest = captor.getValue().putObjectRequest();
        assertThat(putObjectRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(putObjectRequest.key()).isEqualTo(KEY_NAME);
        verify(presigner).close();
    }

    @Test
    @DisplayName("S3Presigner가 presigned url 생성에 실패하면 예외를 그대로 전파하고 리소스를 정리한다")
    void fetchPresignedUrl_fail_whenPresignerThrows() {
        S3Presigner presigner = mock(S3Presigner.class);
        RuntimeException presignFailure = new RuntimeException("presign failed");
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenThrow(presignFailure);

        try (MockedStatic<S3Presigner> mockedStatic = mockStatic(S3Presigner.class)) {
            mockedStatic.when(S3Presigner::create).thenReturn(presigner);

            assertThatThrownBy(() -> imageService.fetchPresignedUrl())
                    .isSameAs(presignFailure);
        }

        verify(presigner).close();
    }
}
