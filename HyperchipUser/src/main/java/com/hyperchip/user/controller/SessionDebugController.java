package com.hyperchip.user.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * SessionDebugController
 * ----------------------
 * Provides debugging endpoints for user session information.
 *
 * Purpose:
 * - Inspect the current session and retrieve user information
 * - Useful during development or troubleshooting authentication/session issues
 */
@Controller
@Slf4j
@RequestMapping("/user/api/debug")
public class SessionDebugController {

    /**
     * sessionInfo
     * -----------
     * GET /user/api/debug/session-info
     *
     * Purpose:
     * - Inspect the HttpSession for a "currentUser" attribute
     * - Attempt to resolve the user ID via reflection (getId() or getUserId())
     * - Return detailed session information including raw session object, class, and resolved ID
     */
    @GetMapping("/session-info")
    public ResponseEntity<?> sessionInfo(HttpSession session) {
        Object cur = session == null ? null : session.getAttribute("currentUser");
        Long resolved = null;
        String className = null;

        if (cur != null) {
            className = cur.getClass().getName();
            try {
                // Try getId() method
                Method m = cur.getClass().getMethod("getId");
                Object idv = m.invoke(cur);
                if (idv instanceof Number) resolved = ((Number) idv).longValue();
                else if (idv != null) resolved = Long.parseLong(idv.toString());
            } catch (NoSuchMethodException ignored) {
                try {
                    // fallback to getUserId() method
                    Method m2 = cur.getClass().getMethod("getUserId");
                    Object idv2 = m2.invoke(cur);
                    if (idv2 instanceof Number) resolved = ((Number) idv2).longValue();
                    else if (idv2 != null) resolved = Long.parseLong(idv2.toString());
                } catch (Exception e) {
                    log.debug("session-info reflection failure", e);
                }
            } catch (Exception ex) {
                log.debug("session-info invoke failed", ex);
            }
        }

        return ResponseEntity.ok(Map.of(
                "hasSessionUser", cur != null,
                "sessionUserClass", className,
                "resolvedUserId", resolved,
                "rawSessionUser", cur
        ));
    }
}