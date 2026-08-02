package jude.carrot.web.advice;


import jude.carrot.service.exception.CustomException;
import jude.carrot.web.dto.ValidFailDto;
import jude.carrot.web.response.ApiResponse;
import jude.carrot.web.mapper.ValidFailDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static jude.carrot.service.status.Status.VALID_FAIL;


@RestControllerAdvice
@Slf4j
public class CommonControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidFailDto>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception){
        List<ValidFailDto> data = ValidFailDtoMapper.from(exception.getBindingResult());
        String code = VALID_FAIL.getCode();
        String detailMessage = VALID_FAIL.getDetailMessage();
        HttpStatus httpStatus = VALID_FAIL.getHttpStatus();
        ApiResponse<List<ValidFailDto>> apiResponse = ApiResponse.failFrom(data,code,detailMessage);
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException exception){
        ApiResponse<Void> apiResponse = ApiResponse.failFrom(exception.getCode(),exception.getDetailMessage());
        HttpStatus httpStatus = exception.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }

}
