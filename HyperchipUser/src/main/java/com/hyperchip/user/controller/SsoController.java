package com.hyperchip.user.controller;

import com.hyperchip.user.model.UserDtls;
import com.hyperchip.user.session.SessionUser;
import com.hyperchip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/**
 * SsoController
 */

@RequiredArgsConstructor
@Controller
public class SsoController {

    private final UserService userService;

    @Value("${gateway.base.url:http://localhost:8080}")
    private String gatewayBaseUrl;

    @GetMapping("/sso/finish")
    public String finishSso(@RequestParam("email") String email,
                            @RequestParam(value = "name", required = false) String name,
                            @RequestParam(value = "role", required = false) String role,
                            HttpSession session) {

        if (email == null || email.isBlank()) {
            return "redirect:/";
        }

        String normalizedEmail = email.trim().toLowerCase();

        Optional<UserDtls> opt = userService.findByEmail(normalizedEmail);
        UserDtls user;
        if (opt.isPresent()) {
            user = opt.get();
        } else {
            UserDtls u = new UserDtls();
            u.setName(name != null && !name.isBlank() ? name : normalizedEmail);
            u.setEmail(normalizedEmail);
            user = userService.createUser(u);
        }

        SessionUser su = userService.toSessionUser(user);
        session.setAttribute("currentUser", su);

        if (role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("ROLE_ADMIN"))) {
            return "redirect:" + gatewayBaseUrl + "/admin/home";
        }

        return "redirect:" + gatewayBaseUrl + "/user/home";
    }
}