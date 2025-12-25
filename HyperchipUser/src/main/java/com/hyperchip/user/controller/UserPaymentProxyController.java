package com.hyperchip.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

/**
 * UserPaymentProxyController
 * ---------------------------
 * Acts as a robust proxy for all payment-related endpoints used by the frontend (e.g., checkout.js)
 * Supports flexible GET/POST forwarding to the payment microservice, including:
 * - Creating payments
 * - Verifying payments
 * - Handling redirects
 *
 * This controller handles multiple endpoint variants, retries, and header propagation.
 */
@RestController
@RequestMapping({"/user/api/proxy", "/api/proxy"})
@RequiredArgsConstructor
@Slf4j
public class UserPaymentProxyController {

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Base URL of the payment microservice.
     * Can include "/api/payments" or just host:port; the controller normalizes it.
     */
    @Value("${payment.service.url:http://localhost:8094}")
    private String paymentServiceUrl;


    // ======================== CREATE PAYMENT ========================

    /**
     * Handles POST request to create payment for a given order.
     * Flexible: forwards request to multiple candidate endpoints in payment service.
     *
     * @param orderId         Order ID for which payment is being created
     * @param incoming        Optional request body containing additional payment info
     * @param incomingHeaders Headers from the incoming request (e.g., auth, cookies)
     * @return Response from the first working payment service endpoint
     */
    @PostMapping("/payment/create/{orderId}")
    public ResponseEntity<?> createPaymentFlexible(@PathVariable("orderId") Long orderId,
                                                   @RequestBody(required = false) Map<String, Object> incoming,
                                                   @RequestHeader HttpHeaders incomingHeaders) {
        List<String> candidates = buildCreateCandidates(orderId);
        Map<String, Object> payload = (incoming == null) ? Map.of() : incoming;

        return forwardPostToFirstWorking(candidates, payload, incomingHeaders);
    }

    /**
     * Bridge GET request to POST create payment.
     * Useful for browser redirects or GET-based callbacks that need to create a payment.
     *
     * @param orderId         Order ID
     * @param incomingHeaders Request headers from the GET call
     * @return Response from payment service
     */
    @GetMapping("/payment/create/{orderId}")
    public ResponseEntity<?> createPaymentFlexibleGet(@PathVariable("orderId") Long orderId,
                                                      @RequestHeader HttpHeaders incomingHeaders) {
        List<String> candidates = buildCreateCandidates(orderId);
        Map<String, Object> payload = Map.of(); // no body for GET
        return forwardPostToFirstWorking(candidates, payload, incomingHeaders);
    }


    // ======================== VERIFY PAYMENT ========================

    /**
     * Handles POST request to verify payment for a given order.
     * Flexible: forwards request to multiple candidate endpoints in payment service.
     *
     * @param orderId         Order ID
     * @param incoming        Optional request body containing payment verification info
     * @param incomingHeaders Headers from the incoming request
     * @return Response from first working payment service endpoint
     */
    @PostMapping("/payment/verify/{orderId}")
    public ResponseEntity<?> verifyPaymentFlexible(@PathVariable("orderId") Long orderId,
                                                   @RequestBody(required = false) Map<String, Object> incoming,
                                                   @RequestHeader HttpHeaders incomingHeaders) {
        List<String> candidates = buildVerifyCandidates(orderId);
        Map<String, Object> payload = (incoming == null) ? Map.of() : incoming;

        return forwardPostToFirstWorking(candidates, payload, incomingHeaders);
    }

    /**
     * Bridge GET request to POST verify payment.
     * Converts query params from GET callback into a POST body to verify payment.
     *
     * @param orderId         Order ID
     * @param queryParams     Query parameters (e.g., razorpay_payment_id, razorpay_order_id)
     * @param incomingHeaders Request headers
     * @return Response from first working payment service endpoint
     */
    @GetMapping("/payment/verify/{orderId}")
    public ResponseEntity<?> verifyPaymentFlexibleGet(@PathVariable("orderId") Long orderId,
                                                      @RequestParam Map<String, String> queryParams,
                                                      @RequestHeader HttpHeaders incomingHeaders) {
        Map<String, Object> payload = new HashMap<>();

        // Map GET query params to POST body for verification
        if (queryParams.containsKey("razorpay_payment_id")) payload.put("razorpay_payment_id", queryParams.get("razorpay_payment_id"));
        if (queryParams.containsKey("razorpay_order_id")) payload.put("razorpay_order_id", queryParams.get("razorpay_order_id"));
        if (queryParams.containsKey("razorpay_signature")) payload.put("razorpay_signature", queryParams.get("razorpay_signature"));
        payload.put("orderId", orderId);

        List<String> candidates = buildVerifyCandidates(orderId);
        return forwardPostToFirstWorking(candidates, payload, incomingHeaders);
    }


    // ======================== BUILD CANDIDATE ENDPOINTS ========================

    /**
     * Build list of candidate create payment endpoints for flexibility
     * Handles path variable, request param, and generic body-based endpoints.
     *
     * @param orderId Order ID
     * @return List of candidate URLs
     */
    private List<String> buildCreateCandidates(Long orderId) {
        List<String> list = new ArrayList<>();
        String base = normalizeBase(paymentServiceUrl);

        list.add(base + "/api/payments/create/" + orderId);          // path variable
        list.add(base + "/api/payments/create?orderId=" + orderId);  // request param style
        list.add(base + "/api/payments/create");                     // generic endpoint

        return list;
    }

    /**
     * Build list of candidate verify payment endpoints.
     * Handles path variable or body-based verification.
     *
     * @param orderId Order ID
     * @return List of candidate URLs
     */
    private List<String> buildVerifyCandidates(Long orderId) {
        List<String> list = new ArrayList<>();
        String base = normalizeBase(paymentServiceUrl);

        list.add(base + "/api/payments/verify/" + orderId);  // path variant
        list.add(base + "/api/payments/verify");             // body variant

        return list;
    }


    // ======================== HELPER METHODS ========================

    /**
     * Normalize the base URL:
     * - Removes trailing slash
     * - Removes internal "/api/payments" if present
     *
     * @param base Input base URL
     * @return Normalized URL base
     */
    private String normalizeBase(String base) {
        if (base == null) base = "";
        base = base.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        String suffix = "/api/payments";
        if (base.endsWith(suffix)) {
            base = base.substring(0, base.length() - suffix.length());
        }

        return base;
    }

    /**
     * Forward POST request to the first working target endpoint.
     * Tries all candidates until one returns 2xx success or meaningful response.
     * Handles 3xx redirects by reposting to Location header.
     *
     * @param targets         List of target URLs
     * @param payload         Request body to send
     * @param incomingHeaders Optional incoming headers to propagate
     * @return ResponseEntity from the first successful target or error response
     */
    private ResponseEntity<?> forwardPostToFirstWorking(List<String> targets, Map<String,Object> payload, HttpHeaders incomingHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Merge incoming headers (auth, cookies) but don't override Content-Type
        if (incomingHeaders != null) {
            incomingHeaders.forEach((k, v) -> {
                if (!"content-type".equalsIgnoreCase(k)) {
                    headers.put(k, new ArrayList<>(v));
                }
            });
        }

        for (String target : targets) {
            try {
                log.info("Attempting POST to payment service target: {}", target);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> resp = restTemplate.exchange(target, HttpMethod.POST, entity, String.class);

                // 2xx → parse body and return
                if (resp.getStatusCode().is2xxSuccessful()) {
                    String body = resp.getBody();
                    if (body == null || body.isBlank()) {
                        return ResponseEntity.status(resp.getStatusCode()).build();
                    }
                    try {
                        Map parsed = objectMapper.readValue(body, Map.class);
                        return ResponseEntity.status(resp.getStatusCode()).body(parsed);
                    } catch (Exception e) {
                        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.TEXT_PLAIN).body(body);
                    }
                }

                // 3xx → follow redirect with POST
                if (resp.getStatusCode().is3xxRedirection()) {
                    URI loc = resp.getHeaders().getLocation();
                    log.warn("Received 3xx from target {}, Location={}", target, loc);
                    if (loc != null) {
                        try {
                            ResponseEntity<Map> follow = restTemplate.postForEntity(loc, new HttpEntity<>(payload, headers), Map.class);
                            return ResponseEntity.status(follow.getStatusCode()).body(follow.getBody());
                        } catch (Exception ex) {
                            log.warn("Re-POST to Location failed: {}", ex.getMessage(), ex);
                        }
                    }
                }

                log.warn("Non-2xx from target {} status={} body={}", target, resp.getStatusCode(), resp.getBody());

            } catch (HttpClientErrorException | HttpServerErrorException rex) {
                String body = rex.getResponseBodyAsString();
                log.warn("HTTP error calling target {} status={} body={}", target, rex.getStatusCode(), body);
            } catch (Exception ex) {
                log.warn("Unexpected error calling target {} : {}", target, ex.getMessage());
            }
        }

        // No endpoint succeeded
        String msg = "No payment endpoint responded with success. Tried: " + String.join(", ", targets);
        log.error(msg);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "success", false,
                        "message", "Failed to create/verify payment on payment service",
                        "tried", targets
                ));
    }
}
