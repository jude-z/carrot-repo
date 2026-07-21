package jude.carrot.apiserver.domain.user.controller;

import jakarta.validation.Valid;
import jude.carrot.apiserver.common.dto.response.ApiResponse;
import jude.carrot.apiserver.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static jude.carrot.apiserver.common.status.Status.SUCCESS;
import static jude.carrot.apiserver.domain.user.dto.request.UserRequest.*;

@RestController("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest signUpRequest){
        userService.signUp(signUpRequest);
        ApiResponse<Void> apiResponse = ApiResponse.successFrom();
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }

}
