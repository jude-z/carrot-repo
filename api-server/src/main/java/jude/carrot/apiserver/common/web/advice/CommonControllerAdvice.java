package jude.carrot.apiserver.common.web.advice;

import jude.carrot.apiserver.common.exception.CustomException;
import jude.carrot.apiserver.common.dto.response.ApiResponse;
import jude.carrot.apiserver.common.status.Status;
import jude.carrot.apiserver.common.web.advice.valid.ValidFailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static jude.carrot.apiserver.common.status.Status.*;

@RestControllerAdvice
@Slf4j
public class CommonControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidFailDto>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception){
        BindingResult bindingResult = exception.getBindingResult();
        List<ValidFailDto> data = fetchValidFails(bindingResult);
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

    private List<ValidFailDto> fetchValidFails(BindingResult bindingResult){
        return   bindingResult
                .getFieldErrors()
                .stream()
                .map(error -> ValidFailDto.from(error.getField(), error.getDefaultMessage()))
                .toList();
    }


}
