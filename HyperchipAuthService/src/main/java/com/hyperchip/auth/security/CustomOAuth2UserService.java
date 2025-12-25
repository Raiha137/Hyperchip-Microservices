package com.hyperchip.auth.security;

import com.hyperchip.auth.model.User;
import com.hyperchip.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * CustomOAuth2UserService
 * - Handles login/signup using OAuth2 (Google, etc.).
 * - Checks if user already exists in DB by email.
 * - Creates new user if email not found.
 * - Assigns ROLE_USER by default.
 * - Returns DefaultOAuth2User for Spring Security context.
 */
@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // Load user info from OAuth2 provider
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Get email from OAuth provider
        String email = oAuth2User.getAttribute("email");
        if (email == null) throw new RuntimeException("Email is required for registration");

        // Find user by email or create new
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setRole("ROLE_USER"); // default role
            newUser.setEnabled(true);     // enable account
            userRepository.save(newUser);
            log.info("New user created: {}", email);
            return newUser;
        });

        // Return OAuth2 user with granted authority for Spring Security
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole())),
                oAuth2User.getAttributes(),
                "email"
        );
    }
}
