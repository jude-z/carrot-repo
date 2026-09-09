package jude.carrot.web.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class KaKaoAddressDtoTest {

    /** coord2regioncode.json?x=127.0364&y=37.5006 실제 응답 */
    private static final String SAMPLE = """
            {
              "meta": { "total_count": 2 },
              "documents": [
                {
                  "region_type": "B",
                  "code": "1168010100",
                  "address_name": "서울특별시 강남구 역삼동",
                  "region_1depth_name": "서울특별시",
                  "region_2depth_name": "강남구",
                  "region_3depth_name": "역삼동",
                  "region_4depth_name": "",
                  "x": 127.03312866105163,
                  "y": 37.49530540462
                },
                {
                  "region_type": "H",
                  "code": "1168064000",
                  "address_name": "서울특별시 강남구 역삼1동",
                  "region_1depth_name": "서울특별시",
                  "region_2depth_name": "강남구",
                  "region_3depth_name": "역삼1동",
                  "region_4depth_name": "",
                  "x": 127.03320108651666,
                  "y": 37.49542431718493
                }
              ]
            }
            """;

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    @Test
    void deserializesCoord2RegionCodeResponse() {
        KaKaoAddressDto dto = MAPPER.readValue(SAMPLE, KaKaoAddressDto.class);

        assertThat(dto.getSido()).isEqualTo("서울특별시");
        assertThat(dto.getGu()).isEqualTo("강남구");
        assertThat(dto.getDong()).isEqualTo("역삼동");
    }

    @Test
    void returnsNullWhenDocumentsEmpty() {
        KaKaoAddressDto dto = MAPPER.readValue("{\"meta\":{\"total_count\":0},\"documents\":[]}", KaKaoAddressDto.class);

        assertThat(dto.getSido()).isNull();
        assertThat(dto.getGu()).isNull();
        assertThat(dto.getDong()).isNull();
    }
}
