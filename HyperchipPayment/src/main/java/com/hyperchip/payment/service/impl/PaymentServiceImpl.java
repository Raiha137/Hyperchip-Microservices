package com.hyperchip.payment.service.impl;

import com.hyperchip.common.dto.PaymentResponse;
import com.hyperchip.payment.model.Payment;
import com.hyperchip.payment.repository.PaymentRepository;
import com.hyperchip.payment.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * PaymentServiceImpl
 *
 * Purpose:
 * - Creates payment orders with Razorpay
 * - Verifies completed payments
 * - Marks payments failed when needed
 * - Persists and updates Payment records in the DB
 * - Notifies the Order Service about payment state changes
 *
 * Developer notes:
 * - This class talks to two external systems:
 *      1) Razorpay (payment provider)
 *      2) Order Service (internal microservice)
 * - The methods are focused on business logic only; controller or request parsing
 *   should remain outside this service.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;

    // Razorpay credentials (injected from properties or environment)
    @Value("${razorpay.key:${razorpay.key-id:}}")
    private String razorpayKey;

    @Value("${razorpay.secret:${razorpay.key-secret:}}")
    private String razorpaySecret;

    // Order service base URL (used to fetch order details and notify status)
    @Value("${order.service.url:http://localhost:8092/api/orders}")
    private String orderServiceUrl;

    /**
     * Constructor
     *
     * @param restTemplate     used for HTTP calls to Order Service
     * @param paymentRepository persistence for Payment entities
     */
    public PaymentServiceImpl(RestTemplate restTemplate,
                              PaymentRepository paymentRepository) {
        this.restTemplate = restTemplate;
        this.paymentRepository = paymentRepository;
    }

    /**
     * CREATE PAYMENT ORDER
     *
     * Flow (high level):
     * 1. Get order details from the Order Service
     * 2. Read the total amount and currency
     * 3. Convert to minor currency unit (e.g., paise for INR)
     * 4. Create an order at Razorpay
     * 5. Save a PENDING Payment record in our DB
     * 6. Return the payment details required by the frontend
     *
     * Important:
     * - This method throws Exception because Razorpay SDK methods may throw checked exceptions.
     *
     * @param orderId the internal order id for which to create a payment
     * @return PaymentResponse contains Razorpay order id and prefill data
     * @throws Exception if any external call fails or amount conversion fails
     */
    @Override
    public PaymentResponse createOrder(Long orderId) throws Exception {

        if (orderId == null) {
            throw new IllegalArgumentException("orderId required");
        }

        // === 1) Fetch order details from Order Service ===
        String url = orderServiceUrl + "/" + orderId;
        Map<String, Object> order = restTemplate.getForObject(url, Map.class);

        if (order == null) {
            // Order service returned nothing — cannot proceed
            throw new RuntimeException("Order service returned null for order " + orderId);
        }

        // === 2) Extract total amount from a few possible keys ===
        Object totalObj = order.get("totalAmount");
        if (totalObj == null) totalObj = order.get("totalPrice");
        if (totalObj == null) totalObj = order.get("grandTotal");
        if (totalObj == null) totalObj = order.get("total");

        if (totalObj == null) {
            // Order did not provide any total amount field
            throw new RuntimeException("Order missing total amount");
        }

        BigDecimal amount = new BigDecimal(totalObj.toString()); // major currency unit (e.g., INR)
        String currency = order.getOrDefault("currency", "INR").toString();

        // === Convert amount to minor unit (e.g., paise) ===
        int fractionDigits = 2;
        try {
            // Use Currency.getInstance to determine fraction digits for the currency
            fractionDigits = java.util.Currency
                    .getInstance(currency)
                    .getDefaultFractionDigits();
        } catch (Exception ignore) {
            // If currency unknown, default to 2 fraction digits
        }

        long amountMinor = amount
                .movePointRight(fractionDigits) // shift decimal to get minor units
                .longValueExact();              // exact value (throws if fraction remains)

        // === 3) Create Razorpay order ===
        // RazorpayClient initialization uses injected key + secret
        RazorpayClient razorpay = new RazorpayClient(razorpayKey, razorpaySecret);

        JSONObject req = new JSONObject();
        req.put("amount", amountMinor);
        req.put("currency", currency);

        // Use orderNumber as receipt if present, otherwise generate a simple receipt
        Object receiptValue = order.getOrDefault("orderNumber", "ORDER-" + orderId);
        req.put("receipt", receiptValue.toString());

        // Create the order at Razorpay
        Order rpOrder = razorpay.orders.create(req);
        String rpOrderId = Objects.toString(rpOrder.get("id"), null);

        // === 4) Save payment in DB with status PENDING ===
        Payment payment = Payment.builder()
                .orderId(orderId)
                .razorpayOrderId(rpOrderId)
                .amount(amount)
                .currency(currency)
                .status("PENDING")
                .build();

        paymentRepository.save(payment);

        // === 5) Build and return response for frontend ===
        return PaymentResponse.builder()
                .success(true)
                .message("Razorpay order created")
                .provider("RAZORPAY")
                .orderId(orderId)
                .orderNumber(Objects.toString(order.get("orderNumber"), null))
                .razorpayKey(razorpayKey)
                .razorpayOrderId(rpOrderId)
                .amount(amount.doubleValue())
                .amountMinor(amountMinor)
                .currency(currency)
                .fake(false)
                .receipt(receiptValue.toString())
                .prefill(
                        // prefill helps frontend populate checkout fields (email here)
                        Map.of("email",
                                order.getOrDefault("userEmail", "test@example.com"))
                )
                .build();
    }

    /**
     * MARK PAYMENT FAILED
     *
     * Used when:
     * - User cancels the checkout
     * - External payment gateway fails to complete the transaction
     *
     * Responsibilities:
     * 1. Notify Order Service that payment failed (best-effort)
     * 2. Update the latest Payment record status to FAILED
     *
     * Note: Notification to Order Service is wrapped in try/catch to avoid blocking status update.
     *
     * @param orderId the internal order id
     * @param reason  optional reason for failure (can be null)
     */
    @Override
    public void markPaymentFailed(Long orderId, String reason) {

        // --- 1) Notify order service (best-effort) ---
        try {
            String url = orderServiceUrl + "/" + orderId + "/mark-payment-failed";
            restTemplate.postForEntity(
                    url,
                    Map.of("reason", reason),
                    Void.class
            );
        } catch (Exception ex) {
            // Log but continue — we still want to update our DB record
            log.warn("Failed to notify order service: {}", ex.getMessage());
        }

        // --- 2) Update payment status in DB ---
        Optional<Payment> paymentOpt =
                paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId);

        if (paymentOpt.isEmpty()) {
            // If no payment record exists, this is an exceptional situation
            throw new RuntimeException("Payment record not found for order " + orderId);
        }

        Payment payment = paymentOpt.get();
        payment.setStatus("FAILED");
        paymentRepository.save(payment);
    }

    /**
     * VERIFY PAYMENT
     *
     * Called after a successful payment completion (frontend will provide paymentId + signature).
     *
     * Flow:
     * 1. Validate inputs
     * 2. Load latest Payment record for the order
     * 3. Prevent double-processing if already marked PAID
     * 4. Notify Order Service that payment is completed
     * 5. Update local Payment record with payment id and mark PAID
     *
     * Note:
     * - Signature verification with Razorpay SDK is not implemented here — if needed,
     *   add signature verification before marking as PAID.
     *
     * @param orderId   internal order id
     * @param paymentId provider payment id (e.g., Razorpay payment id)
     * @param signature provider signature (for verification)
     * @throws Exception if validation fails or DB operations fail
     */
    @Override
    public void verifyPayment(Long orderId,
                              String paymentId,
                              String signature) throws Exception {

        if (orderId == null || paymentId == null || signature == null) {
            throw new IllegalArgumentException("orderId, paymentId and signature are required");
        }

        // Load the latest payment record for this order
        Optional<Payment> paymentOpt =
                paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId);

        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment record not found for order " + orderId);
        }

        Payment payment = paymentOpt.get();

        // Avoid double processing if already marked PAID
        if ("PAID".equalsIgnoreCase(payment.getStatus())) {
            log.info("Payment already PAID for order {}", orderId);
            return;
        }

        // Notify Order Service that payment is successful (best-effort)
        try {
            String notifyUrl = orderServiceUrl + "/" + orderId + "/mark-paid";

            Map<String, Object> req = Map.of(
                    "paymentReference", paymentId,
                    "paymentMethod", "RAZORPAY",
                    // send amount to order service if available (could be null)
                    "amount", payment.getAmount() != null ? payment.getAmount() : 0.0
            );

            restTemplate.postForEntity(notifyUrl, req, Void.class);

        } catch (Exception ex) {
            // Log and continue updating local DB — order service failure shouldn't block local state update
            log.warn("Failed to notify order service: {}", ex.getMessage());
        }

        // Update local payment record as PAID
        payment.setRazorpayPaymentId(paymentId);
        payment.setStatus("PAID");
        paymentRepository.save(payment);

        log.info("Payment verified and marked PAID for order {}", orderId);
    }
}
