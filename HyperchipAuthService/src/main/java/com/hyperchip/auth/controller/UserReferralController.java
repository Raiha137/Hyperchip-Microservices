
// src/main/java/com/hyperchip/auth/controller/UserReferralController.java
package com.hyperchip.auth.controller;

import com.hyperchip.auth.model.User;
import com.hyperchip.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * UserReferralController
 * - Handles referral page for logged-in users.
 * - Fetches the current user's data from database.
 * - Sends user ID and email to the referral Thymeleaf page.
 * - Redirects to login if user is not authenticated or not found.
 */
@Controller
@RequiredArgsConstructor
public class UserReferralController {

    private final UserRepository userRepository;

    /**
     * Show referral page for the logged-in user
     *
     * @param principal authenticated user details from Spring Security
     * @param model     Thymeleaf model to pass data to view
     * @return referral page view or redirect to login
     */
    @GetMapping("/user/referral")
    public String referralPage(@AuthenticationPrincipal UserDetails principal,
                               Model model) {

        // Check if user is logged in
        if (principal == null) {
            return "redirect:/login";
        }

        // Get user's email from principal
        String email = principal.getUsername();

        // Fetch full user info from DB
        User user = userRepository.findByEmail(email).orElse(null);

        // If user not found, redirect to login
        if (user == null) {
            return "redirect:/login";
        }

        // Pass user info to Thymeleaf template
        model.addAttribute("userId", user.getId());
        model.addAttribute("userEmail", user.getEmail());

        // Return referral page
        return "user/referral";
    }
}
