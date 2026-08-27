package jude.carrot.web.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import static jude.carrot.service.status.Status.SUCCESS;

@Builder
public record ApiResponse<T>(
        @JsonInclude(JsonInclude.Include.NON_NULL) T data,
        String code,
        String detailMessage
) {

    public ApiResponse {
        if (code == null) code = SUCCESS.getCode();
        if (detailMessage == null) detailMessage = SUCCESS.getDetailMessage();
    }

    public static <T> ApiResponse<T> successFrom(T data){
        return ApiResponse.<T>builder()
                .data(data)
                .build();
    }

    public static ApiResponse<Void> successFrom(){
        return ApiResponse.<Void>builder()
                .build();
    }

    public static <T> ApiResponse<T> failFrom(T data, String code, String detailMessage){
        return ApiResponse.<T>builder()
                .data(data)
                .code(code)
                .detailMessage(detailMessage)
                .build();
    }

    public static ApiResponse<Void> failFrom(String code, String detailMessage){
        return ApiResponse.<Void>builder()
                .code(code)
                .detailMessage(detailMessage)
                .build();
    }
}
