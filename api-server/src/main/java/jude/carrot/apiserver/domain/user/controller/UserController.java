package jude.carrot.apiserver.domain.user.controller;

import jakarta.validation.Valid;
import jude.carrot.apiserver.domain.user.response.UserResponse.FetchUserResponse;
import jude.carrot.apiserver.domain.user.service.UserService;
import jude.carrot.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static jude.carrot.apiserver.domain.user.request.UserRequest.*;
import static jude.carrot.service.status.Status.SUCCESS;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<FetchUserResponse>> fetchUser(@PathVariable Long userId, @AuthenticationPrincipal Long requesterId){
        FetchUserResponse data = userService.fetchUser(userId, requesterId);
        return successResponseEntity(data);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable Long userId
            , @AuthenticationPrincipal Long requesterId, @Valid @RequestBody UpdateUserRequest updateUserRequest){
        userService.updateUser(userId, requesterId,updateUserRequest);
        return successResponseEntity();
    }

    @PostMapping("/verify/address")
    public ResponseEntity<ApiResponse<Void>> verifyAddress(@Valid @RequestBody VerifyAddressUserRequest verifyAddressUserRequest,@AuthenticationPrincipal Long requesterId){
        userService.verifyAddress(verifyAddressUserRequest,requesterId);
        return successResponseEntity();
    }

    private <T> ResponseEntity<ApiResponse<T>> successResponseEntity(T data){
        ApiResponse<T> apiResponse = ApiResponse.successFrom(data);
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }

    private ResponseEntity<ApiResponse<Void>> successResponseEntity(){
        ApiResponse<Void> apiResponse = ApiResponse.successFrom();
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }



}
