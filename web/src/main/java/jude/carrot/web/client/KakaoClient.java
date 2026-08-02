package jude.carrot.web.client;

import jude.carrot.infra.entity.user.Address;
import jude.carrot.service.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

import static jude.carrot.service.status.Status.EXTERNAL_SERVER_ERROR;

@Component
@RequiredArgsConstructor
public class KakaoClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private @Value("${jude.kakao-token}") String kakaoToken;
    private @Value("${jude.kakao-url}")String kakaoUrl;

    public Address fetchAddress(String longitude, String latitude){
        ResponseEntity<String> responseEntity = fetchKakao(longitude,latitude);
        if(responseEntity.getStatusCode() != HttpStatus.OK) throw new CustomException(EXTERNAL_SERVER_ERROR);

        String response = responseEntity.getBody();
        JsonNode root = mapper.readTree(response);
        String sido = root.at("/documents/0/region_1depth_name").asText();
        String gu = root.at("/documents/0/region_2depth_name").asText();
        String dong = root.at("/documents/0/region_3depth_name").asText();
        return Address.from(sido,gu,dong);
    }
    private ResponseEntity<String> fetchKakao(String longitude, String latitude){
        URI uri = UriComponentsBuilder
                .fromUriString(kakaoUrl)
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoToken);

        return restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }
}
