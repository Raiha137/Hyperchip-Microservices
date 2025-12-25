package com.hyperchip.user.controller;

import com.hyperchip.common.dto.ContactDto;
import com.hyperchip.user.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ContactController
 * ----------------
 * Handles both public and authenticated user contact forms.
 *
 * Purpose:
 * - Provide endpoints to submit contact messages
 * - Different views for anonymous users and logged-in users
 * - Validate input and save via ContactService
 */
@Controller
@RequiredArgsConstructor
public class ContactController {

    /**
     * contactService
     * --------------
     * Service layer to handle saving contact messages to the database.
     * Keeps controller thin by delegating business logic.
     */
    private final ContactService contactService;

    // -------------------- Public Contact (before login) --------------------

    /**
     * showPublicContactForm
     * ---------------------
     * GET /contact
     *
     * Purpose:
     * - Display the contact form to anonymous users
     * - Adds an empty ContactDto to the model for form binding
     * - Uses public header and layout
     */
    @GetMapping("/contact")
    public String showPublicContactForm(Model model) {
        model.addAttribute("contactDto", new ContactDto());
        return "user/contact"; // view with public header
    }

    /**
     * submitPublicContact
     * -------------------
     * POST /contact
     *
     * Purpose:
     * - Handles form submission from anonymous users
     * - Validates ContactDto using JSR-303 annotations
     * - If valid, saves contact message via ContactService
     * - Adds success flash message for display after redirect
     */
    @PostMapping("/contact")
    public String submitPublicContact(
            @Valid @ModelAttribute("contactDto") ContactDto contactDto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            // return form view with validation errors
            return "user/contact";
        }

        // save message to database
        contactService.saveContact(contactDto);

        // set success message for redirect
        redirectAttributes.addFlashAttribute("successMsg", "Your message has been sent successfully!");

        return "redirect:/contact";
    }

    // -------------------- User Contact (after login) -----------------------

    /**
     * showUserContactForm
     * ------------------
     * GET /user/contact
     *
     * Purpose:
     * - Display the contact form for authenticated users
     * - Adds an empty ContactDto for form binding
     * - Uses user-specific header and layout
     */
    @GetMapping("/user/contact")
    public String showUserContactForm(Model model) {
        model.addAttribute("contactDto", new ContactDto());
        return "user/contact-user"; // view with user header
    }

    /**
     * submitUserContact
     * -----------------
     * POST /user/contact
     *
     * Purpose:
     * - Handles form submission from logged-in users
     * - Validates ContactDto input
     * - Saves the contact message via ContactService
     * - Adds flash attribute to show success message after redirect
     */
    @PostMapping("/user/contact")
    public String submitUserContact(
            @Valid @ModelAttribute("contactDto") ContactDto contactDto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            // return form view with validation errors
            return "user/contact-user";
        }

        // save message to database
        contactService.saveContact(contactDto);

        // set success message for redirect
        redirectAttributes.addFlashAttribute("successMsg", "Your message has been sent successfully!");

        return "redirect:/user/contact-user";
    }
}