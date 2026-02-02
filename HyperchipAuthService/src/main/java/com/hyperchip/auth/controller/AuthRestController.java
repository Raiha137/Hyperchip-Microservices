package com.hyperchip.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
public class AuthRestController {

    @GetMapping("/test")
    public String test() {
        return "Auth service is working";
    }
}
