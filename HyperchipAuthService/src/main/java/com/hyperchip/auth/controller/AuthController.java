package com.hyperchip.auth.controller;

import com.hyperchip.auth.model.User;
import com.hyperchip.auth.repository.UserRepository;
import com.hyperchip.auth.service.MailService;
import com.hyperchip.auth.service.UserService;
import com.hyperchip.common.dto.ReferralRegistrationRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController
 *
 * Medium-simple developer comments:
 * - Handles user-facing authentication pages and flows:
 *   signup (with OTP), verify OTP, login page, forgot/reset password, and OAuth2 success redirect.
 * - Uses MailService to send/verify OTPs.
 * - Uses UserService / UserRepository to save and read users.
 * - Uses PasswordEncoder to hash passwords before saving.
 * - Calls a referral endpoint (optional) after successful signup; this call is non-blocking.
 *
 * Keep logic here minimal: prepare data for views and call services for work.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    // Base URL for master service (used for referral registration)
    @Value("${master.service.url:http://localhost:8086}")
    private String masterBase;

    // ================= LOGIN PAGE =================
    /**
     * Show the login page.
     * View: user/login.html
     */
    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }

    // ================= SIGNUP PAGE =================
    /**
     * Show the signup form.
     * Adds an empty User object for form binding.
     */
    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("tempUser", new User());
        return "user/signup";
    }

    // ================= SIGNUP + SEND OTP =================
    /**
     * Start signup process:
     * - normalize email
     * - check duplicate email
     * - send signup OTP via MailService
     * - store temporary user and referral code in session
     *
     * After this step the user must verify OTP to complete registration.
     */
    @PostMapping("/signup")
    public String saveUser(@ModelAttribute("tempUser") User user,
                           @RequestParam(value = "referralCode", required = false) String referralCode,
                           HttpSession session,
                           Model model) {

        // Normalize email to lower-case and trimmed
        String email = user.getEmail().trim().toLowerCase();

        // If already registered, show error on signup page
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("errorMsg", "This email is already registered. Please log in instead.");
            return "user/signup";
        }

        // Send OTP for signup - if sending fails, show error
        if (!mailService.sendSignupOtp(email)) {
            model.addAttribute("errorMsg", "Could not send OTP. Please try again.");
            return "user/signup";
        }

        // Save temp user in session until OTP is verified
        user.setEmail(email);
        session.setAttribute("tempUser", user);

        // Save referral code in session (optional)
        if (referralCode != null && !referralCode.trim().isEmpty()) {
            session.setAttribute("referralCode", referralCode.trim());
        } else {
            session.removeAttribute("referralCode");
        }

        // Redirect to OTP verify page
        return "redirect:/verify-otp";
    }

    // ================= OTP PAGE =================
    /**
     * Show OTP verification page.
     * Prefill form values from session.tempUser when present.
     */
    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(HttpSession session, Model model) {

        User tempUser = (User) session.getAttribute("tempUser");

        // If temp user exists in session, prefill values for convenience
        if (tempUser != null) {
            model.addAttribute("email", tempUser.getEmail());
            model.addAttribute("fullName", tempUser.getFullName());
            model.addAttribute("phone", tempUser.getPhone());
        } else {
            // otherwise show empty fields
            model.addAttribute("email", "");
            model.addAttribute("fullName", "");
            model.addAttribute("phone", "");
        }

        // Also pass referral code if stored in session
        String referral = (String) session.getAttribute("referralCode");
        model.addAttribute("referralCode", referral == null ? "" : referral);

        return "user/verify-otp";
    }

    // ================= VERIFY OTP & REGISTER =================
    /**
     * Verify OTP and register user.
     *
     * Steps:
     * 1. Rebuild tempUser from form if session expired
     * 2. Verify OTP using MailService
     * 3. Check password match
     * 4. Hash password and save user via UserService
     * 5. Optionally call referral service (non-fatal)
     * 6. Clear session and OTP, redirect to login on success
     *
     * This method aims to be resilient: if session expires, it reconstructs the data from form.
     */
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String otp,
                            @RequestParam("password") String password,
                            @RequestParam("confirmPassword") String confirmPassword,
                            @RequestParam(value = "email", required = false) String emailParam,
                            @RequestParam(value = "fullName", required = false) String fullNameParam,
                            @RequestParam(value = "phone", required = false) String phoneParam,
                            @RequestParam(value = "referralCode", required = false) String referralParam,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        User tempUser = (User) session.getAttribute("tempUser");

        // If session expired, rebuild a tempUser from submitted form fields
        if (tempUser == null) {
            if (emailParam == null || emailParam.trim().isEmpty()) {
                // nothing to do - go back to signup
                return "redirect:/signup";
            }

            tempUser = new User();
            tempUser.setEmail(emailParam.trim().toLowerCase());
            tempUser.setFullName(fullNameParam == null ? "" : fullNameParam.trim());
            tempUser.setPhone(phoneParam == null ? "" : phoneParam.trim());
            session.setAttribute("tempUser", tempUser);

            if (referralParam != null && !referralParam.trim().isEmpty()) {
                session.setAttribute("referralCode", referralParam.trim());
            }

            log.warn("tempUser missing in session, rebuilt from form");
        }

        String email = tempUser.getEmail().trim().toLowerCase();

        // Verify OTP; if invalid, show error and keep on OTP page
        if (!mailService.verifyOtp(email, otp.trim())) {
            model.addAttribute("errorMsg", "Invalid OTP. Please try again.");
            model.addAttribute("email", email);
            return "user/verify-otp";
        }

        // Ensure password and confirmPassword match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            model.addAttribute("email", email);
            return "user/verify-otp";
        }

        try {
            // Hash password and set default role if not set
            tempUser.setPassword(passwordEncoder.encode(password));
            if (tempUser.getRole() == null) tempUser.setRole("ROLE_USER");

            // Save user (service may handle extra checks)
            User savedUser = userService.registerUser(tempUser);

            if (savedUser == null || savedUser.getId() == null) {
                model.addAttribute("errorMsg", "Could not register user.");
                return "user/verify-otp";
            }

            // Optional: register referral with master service (non-blocking)
            String referralCode = (String) session.getAttribute("referralCode");
            if (referralCode != null && !referralCode.trim().isEmpty()) {
                try {
                    ReferralRegistrationRequest req = new ReferralRegistrationRequest();
                    req.setReferralCode(referralCode.trim());
                    req.setNewUserId(savedUser.getId());
                    req.setNewUserEmail(savedUser.getEmail());

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<ReferralRegistrationRequest> entity = new HttpEntity<>(req, headers);

                    // Fire-and-forget style: failure here does not block user signup
                    restTemplate.postForEntity(
                            masterBase + "/api/referrals/register",
                            entity,
                            Void.class
                    );
                } catch (Exception ex) {
                    // Log and ignore referral failures
                    log.warn("Referral registration failed (ignored): {}", ex.getMessage());
                }
            }

            // Cleanup: remove temp data and OTP
            session.removeAttribute("tempUser");
            session.removeAttribute("referralCode");
            mailService.clearOtp(email);

            // Show success message and redirect to login
            redirectAttributes.addFlashAttribute(
                    "successMsg",
                    "Registered successfully! Please login."
            );
            return "redirect:/login";

        } catch (Exception e) {
            // Log full error for debugging and show friendly message to user
            log.error("Registration failed", e);
            model.addAttribute("errorMsg", "Registration failed due to server error.");
            return "user/verify-otp";
        }
    }

    // ================= FORGOT PASSWORD =================
    /**
     * Show forgot password page (asks for email).
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "user/forgot-password";
    }

    /**
     * Start password reset:
     * - Check if email exists
     * - Send reset OTP
     * - Show reset-password page so user can enter OTP + new password
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {

        email = email.trim().toLowerCase();

        if (!userRepository.existsByEmail(email)) {
            model.addAttribute("errorMsg", "No account found with this email.");
            return "user/forgot-password";
        }

        if (!mailService.sendResetPasswordOtp(email)) {
            model.addAttribute("errorMsg", "Could not send OTP. Please try again.");
            return "user/forgot-password";
        }

        model.addAttribute("email", email);
        return "user/reset-password";
    }

    // ================= RESET PASSWORD =================
    /**
     * Complete password reset:
     * - Verify OTP
     * - Validate passwords match
     * - Update user password (hashed) and clear OTP
     */
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("otp") String otp,
                                @RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        email = email.trim().toLowerCase();

        // Verify OTP first
        if (!mailService.verifyOtp(email, otp)) {
            model.addAttribute("errorMsg", "Invalid OTP.");
            model.addAttribute("email", email);
            return "user/reset-password";
        }

        // Check passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "user/reset-password";
        }

        // Find user and update password
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            model.addAttribute("errorMsg", "User not found.");
            return "user/forgot-password";
        }

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        mailService.clearOtp(email);

        // Success - redirect to login with flash message
        redirectAttributes.addFlashAttribute(
                "successMsg",
                "Password reset successful. Please login."
        );
        return "redirect:/login";
    }

    // ================= OAUTH2 SUCCESS =================
    /**
     * After successful OAuth2 login, redirect user to home.
     * (Actual user creation/merge handled elsewhere by OAuth2 handlers)
     */
    @GetMapping("/oauth2LoginSuccess")
    public String oauth2LoginSuccess() {
        return "redirect:/";
    }
}
