package jude.carrot.apiserver.domain.user.service;


import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.image.ImageRepository;
import jude.carrot.infra.repository.user.UserRepository;
import jude.carrot.service.exception.CustomException;
import jude.carrot.web.client.KakaoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static jude.carrot.apiserver.domain.user.request.UserRequest.*;
import static jude.carrot.apiserver.domain.user.response.UserResponse.*;
import static jude.carrot.apiserver.domain.user.response.UserResponse.FetchUserResponse.from;
import static jude.carrot.infra.entity.image.SingleImage.from;
import static jude.carrot.service.status.Status.*;
import static jude.carrot.service.status.Status.USER_EXIST;
import static jude.carrot.service.status.Status.USER_NOT_EXIST;
import static org.springframework.util.StringUtils.hasText;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final KakaoClient kakaoClient;



    public void signUp(SignUpRequest signUpRequest) {
        String email = signUpRequest.email();
        String password = signUpRequest.password();
        String confirmPassword = signUpRequest.confirmPassword();
        if(!password.equals(confirmPassword)) throw new CustomException(PASSWORD_CONFIRM_NOT_MATCH);
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new CustomException(USER_EXIST);
                });
        String encodePassword = passwordEncoder.encode(password);
        String profileImageUrl = signUpRequest.profileImageUrl();
        String nickname = signUpRequest.nickname();
        SingleImage profileImage = from(profileImageUrl);
        User user = User.from(email, encodePassword, nickname, profileImage);
        userRepository.save(user);
    }


    public FetchUserResponse fetchUser(Long userId, Long requesterId) {
        User fetchUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_EXIST));

        boolean isMine = fetchUser.getId().equals(requesterId);
        return from(fetchUser,isMine);
    }

    public void updateUser(Long userId, Long requesterId, UpdateUserRequest updateUserRequest) {
        User fetchUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_EXIST));
        if(!fetchUser.getId().equals(requesterId)) throw new CustomException(UPDATE_USER_NOT_AUTHORIZE);
        String rawPassword = updateUserRequest.password();
        String confirmPassword = updateUserRequest.confirmPassword();
        if(!rawPassword.equals(confirmPassword)) throw new CustomException(PASSWORD_CONFIRM_NOT_MATCH);
        String imageUrl = updateUserRequest.profileImageUrl();
        SingleImage preImage = fetchUser.getImage();
        saveImage(imageUrl,preImage,fetchUser);
        String encodedPassword = passwordEncoder.encode(rawPassword);
        updateUserRequest.updateUser(fetchUser,encodedPassword);
    }

    private void saveImage(String imageUrl, SingleImage preImage, User user){
        if(!hasText(imageUrl)) return;
        String preImageUrl = "";
        if(preImage != null){
            preImageUrl = preImage.getUrl();
        }
        if(!imageUrl.equals(preImageUrl)){
            SingleImage newImage = from(imageUrl);
            imageRepository.save(newImage);
            user.setImage(newImage);
        }
    }

    public void verifyAddress(VerifyAddressUserRequest verifyAddressUserRequest, Long userId) {
        User fetchUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_EXIST));

        String longitude = verifyAddressUserRequest.longitude();
        String latitude = verifyAddressUserRequest.latitude();
        if(!isValid(longitude,latitude)) throw new CustomException(NOT_VALID_ADDRESS);
        Address address = kakaoClient.fetchAddress(longitude,latitude);
        fetchUser.setAddress(address);
        fetchUser.setVerified(true);
    }

    private boolean isValid(String longitude, String latitude){
        return hasText(longitude) && hasText(latitude);
    }
}
