package com.hyperchip.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * CustomAuthenticationFailureHandler
 *
 * Developer notes (medium):
 * - Handles login failure scenarios in Spring Security.
 * - Checks the type of authentication exception.
 * - Redirects to /login with a reason code for display on frontend.
 * - Reason codes: "locked", "disabled", "expired", "credentials_expired", "bad_credentials"
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // Default reason
        String reason = "bad_credentials";

        // Determine type of failure
        if (exception instanceof LockedException) {
            reason = "locked";
        } else if (exception instanceof DisabledException) {
            reason = "disabled";
        } else if (exception instanceof AccountExpiredException) {
            reason = "expired";
        } else if (exception instanceof CredentialsExpiredException) {
            reason = "credentials_expired";
        } else if (exception instanceof BadCredentialsException || exception instanceof InsufficientAuthenticationException) {
            reason = "bad_credentials";
        }

        // Encode reason and redirect to login page
        String targetUrl = "/login?error=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + targetUrl);
    }
}
