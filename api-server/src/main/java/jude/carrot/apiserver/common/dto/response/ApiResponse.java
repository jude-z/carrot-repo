package jude.carrot.apiserver.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import static jude.carrot.apiserver.common.status.Status.SUCCESS;

@Getter
@Builder
public class ApiResponse <T>{

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
    @Builder.Default
    private String code = SUCCESS.getCode();
    @Builder.Default
    private String detailMessage = SUCCESS.getDetailMessage();

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
