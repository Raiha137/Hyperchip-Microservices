package com.hyperchip.master.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PingController
 *
 * Purpose:
 * - Provides a simple health check endpoint for the service.
 * - Can be used by monitoring tools or load balancers to verify that the service is up.
 */
@RestController
public class PingController {

    /**
     * GET /ping
     *
     * Purpose:
     * - Responds with a simple "pong" string to indicate that the service is running.
     *
     * @return "pong" string
     */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
