package com.hyperchip.user.controller;

import com.hyperchip.user.model.UserDtls;
import com.hyperchip.user.session.SessionUser;
import com.hyperchip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/**
 * SsoController
 * -------------
 * Handles the final step of Single Sign-On (SSO) login.
 *
 * Purpose:
 * - Receives SSO callback with user email (and optionally name & role)
 * - Creates the user if they do not exist
 * - Stores user info in session
 * - Redirects based on role (ADMIN -> admin app, else -> user home)
 */
@Controller
@RequiredArgsConstructor
public class SsoController {

    private final UserService userService;

    /**
     * finishSso
     * ----------
     * Endpoint: GET /sso/finish
     *
     * @param email User's email from SSO provider (required)
     * @param name  User's display name (optional)
     * @param role  User role (optional, used for redirect)
     * @param session HttpSession to store user info
     *
     * @return Redirect URL (admin home or user home)
     */
    @GetMapping("/sso/finish")
    public String finishSso(@RequestParam("email") String email,
                            @RequestParam(value = "name", required = false) String name,
                            @RequestParam(value = "role", required = false) String role,
                            HttpSession session) {

        // 1️⃣ If email is missing, redirect to root
        if (email == null || email.isBlank()) {
            return "redirect:/";
        }

        // 2️⃣ Normalize email for consistency
        String normalizedEmail = email.trim().toLowerCase();

        // 3️⃣ Check if user already exists
        Optional<UserDtls> opt = userService.findByEmail(normalizedEmail);
        UserDtls user;
        if (opt.isPresent()) {
            user = opt.get();
        } else {
            // 4️⃣ Create new user if not found
            UserDtls u = new UserDtls();
            u.setName(name != null && !name.isBlank() ? name : normalizedEmail);
            u.setEmail(normalizedEmail);
            user = userService.createUser(u);
        }

        // 5️⃣ Convert to session-safe object and store in session
        SessionUser su = userService.toSessionUser(user);
        session.setAttribute("currentUser", su);

        // 6️⃣ Role-based redirect
        if (role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("ROLE_ADMIN"))) {
            // Admin users redirected to admin app
            return "redirect:http://localhost:8085/admin/home";
        }

        // Default: redirect normal users to user home
        return "redirect:/user/home";
    }
}
