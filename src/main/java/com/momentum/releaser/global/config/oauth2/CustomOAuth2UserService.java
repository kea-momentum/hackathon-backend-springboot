package com.momentum.releaser.global.config.oauth2;

import com.momentum.releaser.domain.user.dao.AuthPasswordRepository;
import com.momentum.releaser.domain.user.dao.AuthSocialRepository;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.AuthPassword;
import com.momentum.releaser.domain.user.domain.AuthSocial;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.config.oauth2.user.OAuth2UserInfo;
import com.momentum.releaser.global.config.oauth2.user.OAuth2UserInfoFactory;
import com.momentum.releaser.global.jwt.AuthProvider;
import com.momentum.releaser.global.jwt.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;

import java.util.Optional;


@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AuthSocialRepository authSocialRepository;
    private final AuthPasswordRepository authPasswordRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest){
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        final String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        if(StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("OAuth2 provider에 이메일이 없습니다.");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        AuthSocial authSocial;
        if(userOptional.isPresent()) {
            user = userOptional.get();
            authSocial = userOptional.get().getAuthSocial();

            // 가져온 유저의 Provider와 넘어온 Provider가 다른 경우
            if(!authSocial.getProvider().equals(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()))) {
                throw new OAuth2AuthenticationProcessingException("이미 등록된 멤버입니다.");
            }
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }
        return UserPrincipal.create(user, user.getAuthPassword());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        User user = new User(
                oAuth2UserInfo.getName(),
                oAuth2UserInfo.getEmail(),
                oAuth2UserInfo.getImageUrl(),
                'Y');
        User saveUser = userRepository.save(user);
        AuthSocial authSocial = new AuthSocial(
                saveUser,
                AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()),
                'Y'
        );
        AuthPassword authPassword = new AuthPassword(
                saveUser,
                "Momentum2023!",
                'Y'
        );
        saveUser.updateAuth(authSocial, authPassword);
        authSocialRepository.save(authSocial);
        authPasswordRepository.save(authPassword);
        userRepository.save(saveUser);
        return saveUser;
    }
}