package jude.carrot.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KaKaoAddressDto(
        Meta meta,
        List<DocumentAddress> documents
) {

    public String getSido() {
        if(documents.isEmpty()) return null;
        Address address = documents.get(0).address();
        if(address == null) return null;
        return address.region1depthName();
    }

    public String getGu() {
        if(documents.isEmpty()) return null;
        Address address = documents.get(0).address();
        if(address == null) return null;
        return address.region2depthName();
    }

    public String getDong() {
        if(documents.isEmpty()) return null;
        Address address = documents.get(0).address();
        if(address == null) return null;
        return address.region3depthName();
    }

    private record Meta(
            Integer totalCount,
            Integer pageableCount,
            Boolean isEnd
    ) {
    }

    private record DocumentAddress(
            String addressName,
            Double y,
            Double x,
            String addressType,
            Address address,
            RoadAddress roadAddress
    ) {
    }

    private record Address(
            String addressName,
            @JsonProperty("region_1depth_name") String region1depthName,
            @JsonProperty("region_2depth_name") String region2depthName,
            @JsonProperty("region_3depth_name") String region3depthName,
            @JsonProperty("region_3depth_h_name") String region3depthHName,
            String hCode,
            String bCode,
            String mountainYn,
            String mainAddressNo,
            String subAddressNo,
            String x,
            String y
    ) {
    }

    private record RoadAddress(
            String addressName,
            @JsonProperty("region_1depth_name") String region1depthName,
            @JsonProperty("region_2depth_name") String region2depthName,
            @JsonProperty("region_3depth_name") String region3depthName,
            String roadName,
            String undergroundYn,
            String mainBuildingNo,
            String subBuildingNo,
            String buildingName,
            String zoneNo,
            String y,
            String x
    ) {
    }
}
