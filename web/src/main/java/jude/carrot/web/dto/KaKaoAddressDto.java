package jude.carrot.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Kakao Local API coord2regioncode.json (좌표 -> 행정구역) 응답.
 * documents 는 region_type B(법정동), H(행정동) 순으로 오며, 시/구/동은 첫 번째(법정동) 항목을 사용한다.
 */
public record KaKaoAddressDto(
        Meta meta,
        List<Region> documents
) {

    public String getSido() {
        Region region = first();
        return region == null ? null : region.region1depthName();
    }

    public String getGu() {
        Region region = first();
        return region == null ? null : region.region2depthName();
    }

    public String getDong() {
        Region region = first();
        return region == null ? null : region.region3depthName();
    }

    private Region first() {
        if (documents == null || documents.isEmpty()) return null;
        return documents.get(0);
    }

    private record Meta(
            Integer totalCount
    ) {
    }

    private record Region(
            String regionType,
            String code,
            String addressName,
            @JsonProperty("region_1depth_name") String region1depthName,
            @JsonProperty("region_2depth_name") String region2depthName,
            @JsonProperty("region_3depth_name") String region3depthName,
            @JsonProperty("region_4depth_name") String region4depthName,
            Double x,
            Double y
    ) {
    }
}
