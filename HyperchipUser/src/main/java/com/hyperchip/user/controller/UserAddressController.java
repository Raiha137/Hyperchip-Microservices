package com.hyperchip.user.controller;

import com.hyperchip.common.dto.AddressDto;
import lombok.RequiredArgsConstructor;

import com.hyperchip.user.service.UserService;
import com.hyperchip.user.model.Address;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

/**
 * UserAddressController
 * ---------------------
 * Handles the user-facing address management UI.
 * Uses RestTemplate to call the Address Service endpoints.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/user/address")
public class UserAddressController {

    private final UserService userService;

    /**
     * Helper method to resolve the current user ID from session or optional X-User-Id header.
     * @param xUserId Optional header
     * @param session HttpSession
     * @return User ID or null if not found
     */
    private Long getUserId(String xUserId, HttpSession session) {
        if (xUserId != null && !xUserId.isBlank()) {
            try {
                return Long.parseLong(xUserId);
            } catch (NumberFormatException ignored) {}
        }
        Object cur = session.getAttribute("currentUser");
        if (cur != null) {
            try {
                // Assume session object has getId() method
                return (Long) cur.getClass().getMethod("getId").invoke(cur);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * List all addresses for the current user.
     * GET /address
     */
    @GetMapping
    public String listAddresses(@RequestHeader(value = "X-User-Id", required = false) String xUserId,
                                HttpSession session,
                                Model model) {
        Long userId = getUserId(xUserId, session);
        if (userId == null) return "redirect:/login";

        try {
            List<Address> addresses = userService.listAddresses(userId);
            model.addAttribute("addresses", addresses);
        } catch (Exception ex) {
            model.addAttribute("addresses", Collections.emptyList());
        }

        return "user/address-list";
    }

    /**
     * Show empty form for creating a new address.
     * GET /address/new
     */
    @GetMapping("/new")
    public String newAddressForm(Model model) {
        model.addAttribute("address", new AddressDto());
        return "user/address-form";
    }

    /**
     * Create a new address.
     * POST /address/create
     */
    @PostMapping("/create")
    public String createAddress(@RequestHeader(value = "X-User-Id", required = false) String xUserId,
                                HttpSession session,
                                @ModelAttribute AddressDto dto) {
        Long userId = getUserId(xUserId, session);
        if (userId == null) return "redirect:/login";

        try {
            Address address = new Address();

            address.setLabel(dto.getLabel());
            address.setContactName(dto.getContactName());
            address.setContactPhone(dto.getContactPhone());
            address.setAddressLine1(dto.getAddressLine1());
            address.setAddressLine2(dto.getAddressLine2());
            address.setCity(dto.getCity());
            address.setState(dto.getState());
            address.setPincode(dto.getPincode());
            address.setCountry(dto.getCountry());
            address.setIsDefault(dto.getIsDefault());

            userService.addAddress(userId, address);
        } catch (Exception ex) {
            // Optionally log or add error message
        }
        return "redirect:/user/address";
    }

    /**
     * Show edit form for an existing address.
     * GET /address/{id}/edit
     */
    @GetMapping("/{id}/edit")
    public String editAddressForm(@RequestHeader(value = "X-User-Id", required = false) String xUserId,
                                  HttpSession session,
                                  @PathVariable("id") Long id,
                                  Model model) {
        Long userId = getUserId(xUserId, session);
        if (userId == null) return "redirect:/login";

        try {
            Address address = userService.findAddressById(id).orElseThrow();
            model.addAttribute("address", address);
        } catch (Exception ex) {
            model.addAttribute("address", new AddressDto());
        }

        return "user/address-form";
    }

    /**
     * Update an existing address.
     * POST /address/{id}/update
     */
    @PostMapping("/{id}/update")
    public String updateAddress(@RequestHeader(value = "X-User-Id", required = false) String xUserId,
                                HttpSession session,
                                @PathVariable("id") Long id,
                                @ModelAttribute AddressDto dto) {
        Long userId = getUserId(xUserId, session);
        if (userId == null) return "redirect:/login";

        try {
            Address address = new Address();

            address.setLabel(dto.getLabel());
            address.setContactName(dto.getContactName());
            address.setContactPhone(dto.getContactPhone());
            address.setAddressLine1(dto.getAddressLine1());
            address.setAddressLine2(dto.getAddressLine2());
            address.setCity(dto.getCity());
            address.setState(dto.getState());
            address.setPincode(dto.getPincode());
            address.setCountry(dto.getCountry());
            address.setIsDefault(dto.getIsDefault());

            userService.updateAddress(id, address);
        } catch (Exception ex) {
            // Optionally log or handle error
        }
        return "redirect:/user/address";
    }

    /**
     * Delete an existing address.
     * POST /address/{id}/delete
     */
    @PostMapping("/{id}/delete")
    public String deleteAddress(@RequestHeader(value = "X-User-Id", required = false) String xUserId,
                                HttpSession session,
                                @PathVariable("id") Long id) {
        Long userId = getUserId(xUserId, session);
        if (userId == null) return "redirect:/login";

        try {
            userService.deleteAddress(userId, id);
        } catch (Exception ex) {
            // Optionally log or handle
        }
        return "redirect:/user/address";
    }

    /**
     * Set an address as the default.
     * POST /address/{id}/set-default
     */
    @PostMapping("/{id}/set-default")
    public String setDefault(@RequestHeader(value = "X-User-Id", required = false) String xUserId,
                             HttpSession session,
                             @PathVariable("id") Long id) {
        Long userId = getUserId(xUserId, session);
        if (userId == null) return "redirect:/login";

        try {

            userService.setDefaultAddress(id, userId);
        } catch (Exception ex) {
            // ignore
        }

        return "redirect:/user/address";
    }
}
