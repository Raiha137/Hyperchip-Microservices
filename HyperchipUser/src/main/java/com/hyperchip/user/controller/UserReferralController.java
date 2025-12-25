package com.hyperchip.user.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * UserReferralController
 * ----------------------
 * Handles the referral page for users.
 * Retrieves the current user's ID from the session and passes it to the referral page.
 *
 * This controller does not directly depend on a User class; it uses reflection to get the user ID.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserReferralController {

    /**
     * GET /user/referral
     *
     * Displays the referral page for the currently logged-in user.
     *
     * 1. Reads the "currentUser" object from the session.
     * 2. Uses reflection to safely retrieve the "id" property of the user.
     * 3. Sends the userId to the referral page (Thymeleaf template: user/referral.html).
     *
     * @param model   Spring Model to pass attributes to the view
     * @param session HttpSession object to access current user
     * @return the referral page template
     */
    @GetMapping("/referral")
    public String referralPage(Model model, HttpSession session) {

        Long userId = null;

        // Retrieve the current user from session
        Object current = session.getAttribute("currentUser");
        if (current != null) {
            try {
                // Reflection: call getId() method of the current user object
                java.lang.reflect.Method getIdMethod = current.getClass().getMethod("getId");
                Object value = getIdMethod.invoke(current);

                // Convert the returned value to Long safely
                if (value instanceof Long) {
                    userId = (Long) value;
                } else if (value instanceof Integer) {
                    userId = Long.valueOf((Integer) value);
                } else if (value != null) {
                    userId = Long.valueOf(value.toString());
                }
            } catch (Exception e) {
                // Optional: log the exception for debugging
                // e.printStackTrace();
            }
        }

        // Add userId to the model to make it available in the Thymeleaf template
        model.addAttribute("userId", userId);

        // Return the referral page template (templates/user/referral.html)
        return "user/referral";
    }
}
