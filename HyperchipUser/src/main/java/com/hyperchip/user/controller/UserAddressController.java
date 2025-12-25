package com.hyperchip.user.controller;

import com.hyperchip.common.dto.AddressDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
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
@RequestMapping("/address")
public class UserAddressController {

    private final RestTemplate restTemplate;

    @Value("${address.service.url:http://localhost:8090}")
    private String addressServiceUrl;

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
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId.toString());
            HttpEntity<Void> ent = new HttpEntity<>(headers);

            ResponseEntity<AddressDto[]> resp = restTemplate.exchange(
                    addressServiceUrl + "/api/addresses",
                    HttpMethod.GET,
                    ent,
                    AddressDto[].class
            );

            List<AddressDto> addresses = resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null
                    ? Arrays.asList(resp.getBody())
                    : Collections.emptyList();
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", userId.toString());
            HttpEntity<AddressDto> ent = new HttpEntity<>(dto, headers);
            restTemplate.postForEntity(addressServiceUrl + "/api/addresses", ent, AddressDto.class);
        } catch (Exception ex) {
            // Optionally log or add error message
        }
        return "redirect:/address";
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
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId.toString());
            HttpEntity<Void> ent = new HttpEntity<>(headers);

            ResponseEntity<AddressDto> resp = restTemplate.exchange(
                    addressServiceUrl + "/api/addresses/" + id,
                    HttpMethod.GET,
                    ent,
                    AddressDto.class
            );
            model.addAttribute("address", resp.getBody());
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", userId.toString());
            HttpEntity<AddressDto> ent = new HttpEntity<>(dto, headers);
            restTemplate.exchange(addressServiceUrl + "/api/addresses/" + id, HttpMethod.PUT, ent, AddressDto.class);
        } catch (Exception ex) {
            // Optionally log or handle error
        }

        return "redirect:/address";
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
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId.toString());
            HttpEntity<Void> ent = new HttpEntity<>(headers);
            restTemplate.exchange(addressServiceUrl + "/api/addresses/" + id, HttpMethod.DELETE, ent, Void.class);
        } catch (Exception ex) {
            // Optionally log or handle
        }

        return "redirect:/address";
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
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId.toString());
            HttpEntity<Void> ent = new HttpEntity<>(headers);
            restTemplate.exchange(addressServiceUrl + "/api/addresses/" + id + "/set-default", HttpMethod.PUT, ent, AddressDto.class);
        } catch (Exception ex) {
            // ignore
        }

        return "redirect:/address";
    }
}
