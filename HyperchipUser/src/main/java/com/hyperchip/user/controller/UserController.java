package com.hyperchip.user.controller;

import com.hyperchip.common.dto.CouponDto;
import com.hyperchip.user.model.Address;
import com.hyperchip.user.model.UserDtls;
import com.hyperchip.user.service.UserService;
import com.hyperchip.user.service.other.CurrentUserDetailProvider;
import com.hyperchip.user.session.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.*;

/**
 * UserController
 * ------------------
 * Handles user profile, addresses, orders, and coupons pages.
 * Provides CRUD for addresses and updates profile with optional image upload.
 */
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final CurrentUserDetailProvider currentUserDetailProvider;
    private final RestTemplate restTemplate;

    @Value("${cart.service.url:http://localhost:8091/api/cart}")
    private String cartServiceBase;

    @Value("${master.service.url}")
    private String masterBase;

    // ==================== PROFILE ====================

    /**
     * Display the user's profile page (my-account)
     * Fetches user details and addresses.
     */
    @GetMapping("/profile")
    public String profilePage(Principal principal, Model model, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) return "redirect:/";

        Optional<UserDtls> maybeUser = userService.findById(userId);
        if (maybeUser.isEmpty()) {
            session.removeAttribute("currentUser");
            return "redirect:/";
        }

        UserDtls user = maybeUser.get();
        List<Address> addresses = userService.listAddresses(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        return "user/my-account";
    }

    /**
     * Display form to edit user profile.
     */
    @GetMapping("/updateProfile")
    public String editProfilePage(Principal principal, Model model, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) return "redirect:/user/login";

        UserDtls user = userService.findById(userId).orElse(null);
        model.addAttribute("user", user);
        return "user/edit-profile";
    }

    /**
     * Update user profile including optional profile image upload.
     */
    @PostMapping("/updateProfile")
    public String updateUserProfile(
            Principal principal,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("mobileNumber") String mobileNumber,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            RedirectAttributes attributes,
            HttpSession session,
            Model model) {

        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            attributes.addFlashAttribute("errorMsg", "Please login to update profile.");
            return "redirect:/user/login";
        }

        try {
            UserDtls update = new UserDtls();
            update.setName(name);
            update.setEmail(email);
            update.setMobileNumber(mobileNumber);

            // Handle profile image upload if provided
            if (profileImage != null && !profileImage.isEmpty()) {
                Path uploadDir = Paths.get("uploads/users").toAbsolutePath().normalize();
                if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

                String original = StringUtils.cleanPath(profileImage.getOriginalFilename());
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) ext = original.substring(dot);

                String filename = "user_" + userId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
                Path destinationFile = uploadDir.resolve(Paths.get(filename)).normalize().toAbsolutePath();

                try (var in = profileImage.getInputStream()) {
                    Files.copy(in, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                }

//                String webPath = "/uploads/users/" + filename;
//                update.setProfileImage(webPath);
                update.setProfileImage(filename);

            }

            // Update profile in database
            userService.updateProfile(userId, update);

            // Update session user
            Object cur = session.getAttribute("currentUser");
            if (cur instanceof SessionUser su) {
                su.setName(name);
                su.setEmail(email);
                if (update.getProfileImage() != null) su.setProfileImage(update.getProfileImage());
                session.setAttribute("currentUser", su);
            }

            attributes.addFlashAttribute("successMsg", "Profile updated successfully.");
            return "redirect:/user/profile";

        } catch (IOException ioe) {
            log.error("Failed to save profile image", ioe);
            model.addAttribute("errorMsg", "Failed saving profile image. Please try again.");
            model.addAttribute("user", userService.findById(userId).orElse(null));
            return "user/edit-profile";
        } catch (Exception ex) {
            log.error("Profile update failed", ex);
            attributes.addFlashAttribute("errorMsg", "Profile update failed. Please try again.");
            return "redirect:/user/updateProfile";
        }
    }

    // ==================== ADDRESSES ====================

    /**
     * Display all addresses for current user.
     */
    @GetMapping("/address")
    public String userAddressList(Principal principal, Model model, HttpSession session) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) return "redirect:/";

        List<Address> addresses = userService.listAddresses(userId);
        model.addAttribute("addresses", addresses);
        return "user/address-list";
    }

    /**
     * Display form to create a new address.
     * Supports returning only modal fragment if AJAX request.
     */
    @GetMapping("/address/new")
    public String newAddressForm(Principal principal, Model model, HttpSession session, HttpServletRequest request) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) return "redirect:/";

        model.addAttribute("address", new Address());

        String xrw = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xrw)) {
            return "user/address-form :: addressForm";
        }
        return "user/address-form";
    }

    /**
     * Create new address (AJAX or normal form).
     */
    @PostMapping("/address/create")
    public Object createAddress(Principal principal, @ModelAttribute Address address,
                                RedirectAttributes attrs, HttpSession session, HttpServletRequest request) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return handleAjaxOrRedirect(request, 401, "Please login to add address.", "/user/login");
        }

        try {
            userService.addAddress(userId, address);
            return handleAjaxOrRedirect(request, 200, "Address added successfully.", "/user/address");
        } catch (Exception ex) {
            log.error("Failed to create address for userId={}", userId, ex);
            return handleAjaxOrRedirect(request, 500, "Failed to add address.", "/user/address");
        }
    }

    /**
     * Display form to edit an address.
     */
    @GetMapping("/address/{id}/edit")
    public String editAddressForm(@PathVariable("id") Long id,
                                  Principal principal, Model model,
                                  HttpSession session, HttpServletRequest request) {

        Long userId = resolveUserId(principal, session);
        if (userId == null) return "redirect:/";

        Optional<Address> opt = userService.findAddressById(id);
        if (opt.isEmpty()) return "redirect:/user/address";

        Address a = opt.get();
        if (a.getUser() == null || !userId.equals(a.getUser().getId())) {
            return "redirect:/user/address";
        }

        model.addAttribute("address", a);

        String xrw = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xrw)) {
            return "user/address-form :: addressForm";
        }
        return "user/address-form";
    }

    /**
     * Update address by ID (AJAX or normal form).
     */
    @PostMapping("/address/{id}/update")
    public Object updateAddressById(@PathVariable("id") Long id, @ModelAttribute Address address,
                                    Principal principal, RedirectAttributes attrs,
                                    HttpSession session, HttpServletRequest request) {

        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return handleAjaxOrRedirect(request, 401, "Please login to update address.", "/user/login");
        }

        try {
            userService.updateAddress(id, address);
            return handleAjaxOrRedirect(request, 200, "Address updated successfully.", "/user/address");
        } catch (Exception ex) {
            log.error("Failed to update address id={} for userId={}", id, userId, ex);
            return handleAjaxOrRedirect(request, 500, "Failed to update address.", "/user/address");
        }
    }

    /**
     * Delete address by ID (AJAX or normal form).
     */
    @PostMapping("/address/{id}/delete")
    public Object deleteAddressById(@PathVariable("id") Long id, Principal principal,
                                    RedirectAttributes attrs, HttpSession session, HttpServletRequest request) {

        Long userId = resolveUserId(principal, session);
        if (userId == null) {
            return handleAjaxOrRedirect(request, 401, "Please login to delete address.", "/user/login");
        }

        try {
            userService.deleteAddress(userId, id);
            return handleAjaxOrRedirect(request, 200, "Address deleted", "/user/address");
        } catch (Exception ex) {
            log.error("Failed to delete address id={} for userId={}", id, userId, ex);
            return handleAjaxOrRedirect(request, 500, "Failed to delete address.", "/user/address");
        }
    }

    // ==================== ORDERS ====================

    /**
     * Display a single order detail page for a given orderId.
     * Only numeric orderIds allowed.
     */
    @GetMapping("/order/{orderId:\\d+}")
    public String orderDetail(@PathVariable Long orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "user/order-detail";
    }

    /**
     * Display all orders for the current user.
     */
    @GetMapping("/orders")
    public String ordersPage(Principal principal, HttpSession session, Model model) {
        Long userId = resolveUserId(principal, session);
        if (userId == null) return "redirect:http://localhost:8084/login";

        model.addAttribute("userId", userId);
        return "user/orders";
    }

    // ==================== COUPONS ====================

    /**
     * Display all coupons for the logged-in user.
     */
    @GetMapping("/my-coupons")
    public String myCoupons(Model model, HttpSession session) {
        SessionUser current = (SessionUser) session.getAttribute("currentUser");
        if (current == null) return "redirect:/login";

        Long userId = current.getId();
        String url = masterBase + "/api/coupons/user/" + userId;

        CouponDto[] coupons = restTemplate.getForObject(url, CouponDto[].class);
        model.addAttribute("coupons", coupons != null ? List.of(coupons) : List.of());
        return "user/my-coupons";
    }

    // ==================== HELPERS ====================

    /**
     * Resolve user ID from session or Principal.
     */
    private Long resolveUserId(Principal principal, HttpSession session) {
        Object cur = session.getAttribute("currentUser");
        if (cur instanceof SessionUser su && su.getId() != null) return su.getId();

        if (principal != null) {
            try {
                return currentUserDetailProvider.getUserId(principal);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Helper for handling AJAX vs normal redirect responses.
     */
    private Object handleAjaxOrRedirect(HttpServletRequest request, int status, String message, String redirectUrl) {
        String xrw = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xrw)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("success", status == 200, "message", message));
        }
        return "redirect:" + redirectUrl;
    }
}
