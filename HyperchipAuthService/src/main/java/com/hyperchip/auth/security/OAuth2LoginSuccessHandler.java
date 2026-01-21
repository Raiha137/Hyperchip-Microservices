package com.hyperchip.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2LoginSuccessHandler
 *
 * Developer notes (medium-simple):
 * - Handles OAuth2 login success (Google, etc.).
 * - Extracts user info (email, name) from OAuth provider.
 * - Fallbacks to username/password login if OAuth2 not used.
 * - Redirects to user-service SSO endpoint with email, name, and role.
 * - In production, this URL could carry a signed token instead of query params.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    // SSO finish URL of the user-service, can be configured in application.properties
    @Value("${user.service.sso.finish:http://localhost:8080/sso/finish}")


    private String userServiceSsoFinish;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String email = null;
        String name  = null;

        Object principal = authentication.getPrincipal();

        // If OAuth2 login, extract attributes
        if (principal instanceof OAuth2User) {
            OAuth2User oauth = (OAuth2User) principal;
            Map<String,Object> attrs = oauth.getAttributes();

            email = firstNonNull(attrs.get("email"));
            name  = firstNonNull(attrs.get("name"));

            // fallback for providers like Gmail that use "verified_email"
            if (email == null && attrs.containsKey("verified_email")) {
                email = firstNonNull(attrs.get("verified_email"));
            }

        } else {
            // fallback for username/password login
            email = authentication.getName();
        }

        // Build redirect URL to user-service SSO endpoint
        StringBuilder redirect = new StringBuilder(userServiceSsoFinish).append("?");
        if (email != null) redirect.append("email=").append(URLEncoder.encode(email, StandardCharsets.UTF_8));
        if (name  != null) redirect.append("&name=").append(URLEncoder.encode(name, StandardCharsets.UTF_8));

        // Send user role as query param (optional)
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        if (roles.contains("ROLE_ADMIN") || roles.contains("ADMIN")) {
            redirect = new StringBuilder("http://localhost:8080/admin/home");
        }
        else if (roles.contains("ROLE_USER")) {
            redirect.append("&role=USER");
        }

        // Redirect user to user-service SSO finish URL
        response.sendRedirect(redirect.toString());
    }

    // Helper: returns first non-null, non-empty string from object
    private String firstNonNull(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }
}
