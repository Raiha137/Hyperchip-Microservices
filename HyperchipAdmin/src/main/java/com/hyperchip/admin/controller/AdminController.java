package com.hyperchip.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/home")
    public String adminHome() {
        return "admin/index";
    }

    @PostMapping("/home")
    public String adminHomePost() {
        return "admin/index";
    }

    @GetMapping("/ping")
    public String ping() {
        return "admin/index";
    }
}
