package jude.carrot.web.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class KaKaoAddressDtoTest {

    private static final String SAMPLE = """
            {
              "meta": {
                "total_count": 1,
                "pageable_count": 1,
                "is_end": true
              },
              "documents": [
                {
                  "road_address": {
                    "address_name": "전북 익산시 망산길 11-17",
                    "region_1depth_name": "전북",
                    "region_2depth_name": "익산시",
                    "region_3depth_name": "부송동",
                    "road_name": "망산길",
                    "underground_yn": "N",
                    "main_building_no": "11",
                    "sub_building_no": "17",
                    "building_name": "",
                    "zone_no": "54547",
                    "y": "35.976749396987046",
                    "x": "126.99599512792346"
                  },
                  "address": {
                    "address_name": "전북 익산시 부송동 100",
                    "region_1depth_name": "전북",
                    "region_2depth_name": "익산시",
                    "region_3depth_name": "부송동",
                    "region_3depth_h_name": "삼성동",
                    "h_code": "4514069000",
                    "b_code": "4514013400",
                    "mountain_yn": "N",
                    "main_address_no": "100",
                    "sub_address_no": "",
                    "x": "126.99597295767953",
                    "y": "35.97664845766847"
                  }
                }
              ]
            }
            """;

    @Test
    void deserializesFromKakaoJsonWithoutSetters() {
        ObjectMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();

        KaKaoAddressDto dto = mapper.readValue(SAMPLE, KaKaoAddressDto.class);

        assertThat(dto.getSido()).isEqualTo("전북");
        assertThat(dto.getGu()).isEqualTo("익산시");
        assertThat(dto.getDong()).isEqualTo("부송동");
    }
}
