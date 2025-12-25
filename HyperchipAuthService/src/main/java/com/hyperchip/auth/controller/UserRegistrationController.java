package com.hyperchip.auth.controller;

import com.hyperchip.auth.model.User;
import com.hyperchip.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * UserRegistrationController
 * - Handles user registration via API
 * - Encodes password before saving
 * - Includes a test endpoint to check if service is running
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserRegistrationController {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Register a new user
     *
     * @param user User object from request body
     * @return saved User object after registration
     */
    @PostMapping("/register")
    @ResponseBody // Ensure it returns JSON
    public User register(@RequestBody User user) {
        // Encode password before saving to DB
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Call service to save user
        return userService.registerUser(user);
    }

    /**
     * Simple test endpoint to check if service is running
     *
     * @return status message
     */
    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "✅ Auth service is running!";
    }
}
