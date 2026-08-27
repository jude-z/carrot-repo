package jude.carrot.web.client;

import jude.carrot.infra.entity.user.Address;
import jude.carrot.service.exception.CustomException;
import jude.carrot.web.dto.KaKaoAddressDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

import static jude.carrot.service.status.Status.EXTERNAL_SERVER_ERROR;

@Component
public class KakaoClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public KakaoClient(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
        this.mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
    }
    private @Value("${jude.kakao-token}") String kakaoToken;
    private @Value("${jude.kakao-url}")String kakaoUrl;

    public Address fetchAddress(String longitude, String latitude){
        ResponseEntity<String> responseEntity = fetchKakao(longitude,latitude);
        if(responseEntity.getStatusCode() != HttpStatus.OK) throw new CustomException(EXTERNAL_SERVER_ERROR);

        String response = responseEntity.getBody();
        KaKaoAddressDto kaKaoAddressDto = mapper.readValue(response, KaKaoAddressDto.class);
        String sido = kaKaoAddressDto.getSido();
        String gu = kaKaoAddressDto.getGu();
        String dong = kaKaoAddressDto.getDong();
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
