package jude.carrot.apiserver.domain.user.controller;

import jakarta.validation.Valid;
import jude.carrot.apiserver.domain.user.service.UserService;
import jude.carrot.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static jude.carrot.apiserver.domain.user.request.UserRequest.*;
import static jude.carrot.service.status.Status.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest signUpRequest){
        userService.signUp(signUpRequest);
        return successResponseEntity();
    }

    private ResponseEntity<ApiResponse<Void>> successResponseEntity(){
        ApiResponse<Void> apiResponse = ApiResponse.successFrom();
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }
}
