package jude.carrot.apiserver.domain.image.controller;

import jude.carrot.apiserver.domain.image.response.ImageResponse.PresignedUrlResponse;
import jude.carrot.apiserver.domain.image.service.ImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImageController.class)
class ImageControllerTest {

    private static final String PRESIGNED_URL = "https://carrot-bucket.s3.amazonaws.com/profile.png?X-Amz-Signature=abc";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageService imageService;

    @Test
    @DisplayName("presigned url 발급을 요청하면 200과 함께 발급된 url을 반환한다")
    void fetchPresignedUrl_success() throws Exception {
        when(imageService.fetchPresignedUrl()).thenReturn(PresignedUrlResponse.from(PRESIGNED_URL));

        mockMvc.perform(post("/image/presigned-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.url").value(PRESIGNED_URL));

        verify(imageService).fetchPresignedUrl();
    }
}
