package com.hyperchip.order.service.impl;
import com.hyperchip.order.service.client.ProductServiceClient;
import com.hyperchip.common.dto.BestOfferResponse;
import com.hyperchip.order.client.OfferClient;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import com.hyperchip.common.dto.AddressDto;
import com.hyperchip.order.service.DeliveryChargeService;
import com.hyperchip.common.dto.OrderItemResponse;
import com.hyperchip.common.dto.OrderResponse;
import com.hyperchip.common.dto.PlaceOrderRequest;
import com.hyperchip.common.dto.PlaceOrderResponse;
import com.hyperchip.common.dto.WalletBalanceDto;      // WALLET
import com.hyperchip.common.dto.WalletCreditRequest;   // WALLET
import com.hyperchip.common.dto.WalletPaymentRequest;  // WALLET
import com.hyperchip.common.dto.WalletPaymentResponse; // WALLET
import com.hyperchip.common.exception.ResourceNotFoundException;
import com.hyperchip.order.model.Order;
import com.hyperchip.order.model.OrderItem;
import com.hyperchip.order.model.OrderStatus;
import com.hyperchip.order.repository.OrderItemRepository;
import com.hyperchip.order.repository.OrderRepository;
import com.hyperchip.order.service.MailService;
import com.hyperchip.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MailService mailService;
    private final RestTemplate restTemplate;// add to constructor via @RequiredArgsConstructor
    private final ProductServiceClient productServiceClient;
    private final OfferClient offerClient;
    private final DeliveryChargeService deliveryChargeService;
    @Value("${cart.service.url:http://localhost:8091/api/cart}")
    private String cartServiceBase;
    @Value("${uploads.base.url:http://localhost:8086}")
    private String uploadsBaseUrl;
    @Value("${master.service.url:http://localhost:8086}")
    private String masterServiceBase;
    @Value("${product.service.url:http://localhost:8086}")
    private String productServiceBase;
    // WALLET: base URL for wallet-service
    @Value("${wallet.service.url:http://localhost:8095}")
    private String walletServiceBase;
    @Value("${user.service.url:http://localhost:8083}")
    private String userServiceBase;
    //
//    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    /* ------------------- User-side APIs ------------------- */
    @Override
    public PlaceOrderResponse placeOrder(PlaceOrderRequest req) {
        if (req == null || req.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing userId");
        }
        AddressDto addressDto = null;
        if (req.getAddressId() != null) {
            addressDto = fetchAddressById(req.getAddressId());
        }
        List
                <OrderItem>
                items = new ArrayList<>();
        if (req.getItems() != null) {
            for (var dto : req.getItems()) {
                OrderItem item = OrderItem.builder()
                        .productId(dto.getProductId())
                        .productTitle(dto.getProductTitle())
                        .productImage(dto.getProductImage())
                        .quantity(dto.getQuantity() != null ? dto.getQuantity() : 0)
                        .unitPrice(dto.getUnitPrice() != null ? dto.getUnitPrice() : 0.0)
                        .total(dto.getTotal() != null ? dto.getTotal() : 0.0)
                        .build();
                item.setCancelled(false);
                item.setCancelReason(null);
                items.add(item);
            }
        }
        double subtotal = 0.0;
        for (OrderItem item : items) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            double unit = item.getUnitPrice() != null ? item.getUnitPrice() : 0.0;
            BigDecimal linePrice = BigDecimal.valueOf(unit)
                    .multiply(BigDecimal.valueOf(qty));
            BestOfferResponse offerResp = null;
            try {
                offerResp = offerClient.getBestPrice(
                        item.getProductId(),
                        null,
                        linePrice
                );
            } catch (Exception e) {
                log.warn("Offer service failed for product {}: {}", item.getProductId(), e.getMessage());
            }
            BigDecimal finalLine = (offerResp != null && offerResp.getFinalPrice() != null)
                    ? offerResp.getFinalPrice()
                    : linePrice;
            double finalLineDouble = finalLine.doubleValue();
            item.setTotal(finalLineDouble);
            subtotal += finalLineDouble;
        }
        double tax = 0.0;
// 1️⃣ Base shipping from DB rules (Kerala / outside)
        double shipping = deliveryChargeService.getDeliveryChargeForAddress(addressDto);
// 2️⃣ Coupon discount
        double couponDiscount = 0.0;
        if (req.getDiscountAmount() != null) {
            couponDiscount = req.getDiscountAmount();
        }
        if (couponDiscount < 0) {
            couponDiscount = 0.0;
        }
// 3️⃣ Optional: free shipping for high orders (example: ≥ 1500)
//                    if (subtotal >= 1500.0) {
//                        shipping = 0.0;
//                    }
// 4️⃣ Final total
        double totalAmount = subtotal - couponDiscount + tax + shipping;
        if (totalAmount < 0) {
            totalAmount = 0.0;
        }
// 🚫 COD LIMIT: do not allow COD if totalAmount > 1000
        if (req.getPaymentMethod() != null
                && "COD".equalsIgnoreCase(req.getPaymentMethod())
                && totalAmount > 1000.0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cash on Delivery is only available for orders up to 1000. Please choose another payment method."
            );
        }
        Order order = Order.builder()
                .userId(req.getUserId())
                .userEmail(req.getUserEmail())
                .addressId(req.getAddressId())
                .subtotal(subtotal)            // before coupon
                .tax(tax)
                .shipping(shipping)
                .totalAmount(totalAmount)      // after coupon
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "COD")
                .paymentStatus("PENDING")
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        for (OrderItem it : items) order.addItem(it);
        order.setOrderNumber(generateOrderNumber());
        Order saved = orderRepository.save(order);
// ---------- COD FLOW ----------
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
// 🔻 DECREMENT STOCK FOR ALL ORDER ITEMS (COD)
            for (OrderItem it : saved.getOrderItems()) {
                try {
                    productServiceClient.decrementStock(it.getProductId(), it.getQuantity());
                } catch (Exception ex) {
                    log.error("Failed to decrement stock for product {} when placing COD order {}",
                            it.getProductId(), saved.getId(), ex);
                }
            }
            saved.setPaymentStatus("PENDING");
            saved.setPaidAmount(saved.getTotalAmount());
            saved.setPaidAt(Instant.now());
            orderRepository.save(saved);
            mailService.sendOrderConfirmation(saved);
            try {
                String url = cartServiceBase + "/" + saved.getUserId() + "/clear?orderComplete=true";
                restTemplate.delete(url);
            } catch (Exception ex) {
                log.warn("Failed to clear cart for user {}: {}", saved.getUserId(), ex.getMessage());
            }
            PlaceOrderResponse resp = buildPlaceOrderResponse(saved);
            resp.setSuccess(true);
            resp.setMessage("Order placed successfully (COD)");
            return resp;
        }
// ---------- WALLET FLOW ----------
        if ("WALLET".equalsIgnoreCase(order.getPaymentMethod())) {
// 1️⃣ First, try to DEDUCT from wallet via wallet-service
            try {
// This will call /api/wallet/pay in wallet-service
                payWithWallet(saved);
            }  catch (Exception ex) {
                log.warn("Wallet payment failed for order {}: {}", saved.getId(), ex.getMessage());
                saved.setPaymentStatus("FAILED");
                saved.setStatus(OrderStatus.PAYMENT_FAILED);
                saved.setPaymentFailureReason("Insufficient amount to buy this product");
                orderRepository.save(saved);
                PlaceOrderResponse resp = buildPlaceOrderResponse(saved);
                resp.setSuccess(false);
                resp.setMessage("Insufficient amount to buy this product");
                return resp;
            }
// 2️⃣ If wallet payment SUCCESS → now decrement stock
            for (OrderItem it : saved.getOrderItems()) {
                try {
                    productServiceClient.decrementStock(it.getProductId(), it.getQuantity());
                } catch (Exception ex) {
                    log.error("Failed to decrement stock for product {} when placing WALLET order {}",
                            it.getProductId(), saved.getId(), ex);
                }
            }
// 3️⃣ Mark order as PAID via wallet
            saved.setPaymentStatus("PAID");
            saved.setPaymentMethod("WALLET");
            saved.setPaidAmount(saved.getTotalAmount());
            saved.setPaidAt(Instant.now());
            saved.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(saved);
// 4️⃣ Clear cart ONLY after successful wallet payment
            try {
                String url = cartServiceBase + "/" + saved.getUserId() + "/clear?orderComplete=true";
                restTemplate.delete(url);
            } catch (Exception ex) {
                log.warn("Failed to clear cart for user {} after wallet payment: {}",
                        saved.getUserId(), ex.getMessage());
            }
            PlaceOrderResponse resp = buildPlaceOrderResponse(saved);
            resp.setSuccess(true);
            resp.setMessage("Order placed successfully (Wallet)");
            return resp;
        }
        PlaceOrderResponse resp = buildPlaceOrderResponse(saved);
        resp.setSuccess(true);
        resp.setMessage("Order placed successfully");
        return resp;
    }
    @Override
    public PlaceOrderResponse cancelOrderByAdmin(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel this order");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
// increment stock using productService
// increment stock for all items – but don't break if product service fails
        order.getOrderItems().forEach(item -> {
            try {
                productServiceClient.incrementStock(item.getProductId(), item.getQuantity());
            } catch (Exception ex) {
                log.error("Failed to increment stock for product {} when cancelling order {}",
                        item.getProductId(), order.getId(), ex);
// do not rethrow – just log and continue
            }
        });
// WALLET: if already paid (online/wallet), refund to wallet
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            double amountToRefund = order.getPaidAmount() != null && order.getPaidAmount() > 0
                    ? order.getPaidAmount()
                    : (order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
            refundToWallet(order, amountToRefund, reason, "ORDER_CANCELLED");
        }
        orderRepository.save(order);
        PlaceOrderResponse response = buildPlaceOrderResponse(order);
        response.setSuccess(true);
        response.setMessage("Order cancelled successfully");
        return response;
    }
    @Override
    public PlaceOrderResponse returnOrderByAdmin(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED && order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return can be processed only after request");
        }
        order.setStatus(OrderStatus.RETURNED);
        order.setCancelReason(reason);
        order.getOrderItems().forEach(item -> {
            try {
                productServiceClient.incrementStock(item.getProductId(), item.getQuantity());
            } catch (Exception ex) {
                log.error("Failed to increment stock for product {} when returning order {}",
                        item.getProductId(), order.getId(), ex);
            }
        });
// 💰 refund to wallet after admin approval
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            double amountToRefund = order.getPaidAmount() != null && order.getPaidAmount() > 0
                    ? order.getPaidAmount()
                    : (order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
            refundToWallet(order, amountToRefund, "Order return refund", "RETURN_APPROVED");
        }
        orderRepository.save(order);
        PlaceOrderResponse response = buildPlaceOrderResponse(order);
        response.setSuccess(true);
        response.setMessage("Return approved and refunded to wallet");
        return response;
    }
    @Override
    public Optional
            <Order>
    getOrder(Long orderId) {
        return orderRepository.findById(orderId);
    }
    @Override
    public OrderResponse getOrderById(Long orderId) {
        return getOrder(orderId).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
    @Override
    public OrderResponse getOrderByIdentifier(String idOrOrderNumber) {
        try {
            Long id = Long.parseLong(idOrOrderNumber);
            return getOrderById(id);
        } catch (NumberFormatException ex) {
            return orderRepository.findAll().stream()
                    .filter(o -> o.getOrderNumber().equalsIgnoreCase(idOrOrderNumber))
                    .findFirst().map(this::toResponse)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        }
    }
    @Override
    public List
            <OrderResponse>
    getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
    @Override
    public Page
            <Order>
    listOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
    @Override
    public Page
            <Order>
    listOrdersForUser(Long userId, Pageable pageable) {
        if (userId == null) return Page.empty();
        Pageable effectivePageable = pageable != null ? pageable : PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByUserId(userId, effectivePageable);
    }
    @Override
    public PlaceOrderResponse changeOrderStatus(Long orderId, OrderStatus status, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
        PlaceOrderResponse resp = buildPlaceOrderResponse(order);
        resp.setSuccess(true);
        resp.setMessage("Order status updated successfully");
        return resp;
    }
    @Override
    public byte[] generateInvoicePdf(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return generateInvoice(order);
    }
    @Override
    public List
            <OrderResponse>
    searchOrders(String keyword) {
        String q = keyword != null ? keyword.toLowerCase() : "";
        return orderRepository.findAll().stream()
                .filter(o -> o.getOrderNumber().toLowerCase().contains(q))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    @Override
    public List
            <OrderResponse>
    getOrdersForUser(Long userId) {
        return getOrdersByUserId(userId);
    }
    /* ------------------- Admin methods ------------------- */
    @Override
    public List
            <OrderResponse>
    listForAdmin(int page, int size, String status, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page
                <Order>
                pageResult;
        if (status != null && !status.isBlank()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            String search = (q != null) ? q : "";
            pageResult = orderRepository.findByStatusAndOrderNumberContainingIgnoreCase(orderStatus, search, pageable);
        } else {
            String search = (q != null) ? q : "";
            pageResult = orderRepository.findByOrderNumberContainingIgnoreCase(search, pageable);
        }
        return pageResult.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    @Override
    public Optional
            <OrderResponse>
    findOrderResponseById(Long id) {
        return orderRepository.findById(id).map(this::toResponse);
    }
    @Override
    public void updateStatusAdmin(Long id, String status, String note) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status required");
        }
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status");
        }
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        OrderStatus currentStatus = order.getStatus();
// ✅ SPECIAL CASE: admin is approving a return
        if (newStatus == OrderStatus.RETURNED &&
                (currentStatus == OrderStatus.RETURN_REQUESTED || currentStatus == OrderStatus.DELIVERED)) {
// Use existing business logic: stock increment + wallet refund
            returnOrderByAdmin(id, note != null ? note : "Return approved by admin");
            log.info("Admin approved return for order {} (note={})", id, note);
            return;
        }
// ✅ All other status changes: normal simple update
        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        if (note != null && !note.isBlank()) {
            try {
                order.getClass()
                        .getMethod("setAdminNote", String.class)
                        .invoke(order, note);
            } catch (Exception ignored) {}
        }
        orderRepository.save(order);
        log.info("Admin updated order {} status to {} (note={})", id, newStatus, note);
    }
    @Override
    public byte[] getInvoicePdf(Long id) throws Exception { return generateInvoicePdf(id); }
    private PlaceOrderResponse buildPlaceOrderResponse(Order o) {
        if (o == null) return null;
        PlaceOrderResponse r = new PlaceOrderResponse();
        r.setSuccess(true);
        r.setOrderNumber(o.getOrderNumber());
        r.setOrderId(o.getId());
        r.setCreatedAt(o.getCreatedAt());
        r.setMessage("Order placed successfully");
// status
        r.setStatus(o.getStatus() != null ? o.getStatus().name() : null);
// totals - prefer totalAmount then total then compute from items
        double total = safeDouble(o.getTotalAmount());
        if (total <= 0) {
            total = safeDouble(o.getTotal());
        }
        if (total <= 0 && o.getOrderItems() != null && !o.getOrderItems().isEmpty()) {
            total = o.getOrderItems().stream()
                    .mapToDouble(it -> (it.getTotal() != null && it.getTotal() > 0) ? it.getTotal() : safeDouble(it.getUnitPrice()) * safeInt(it.getQuantity()))
                    .sum();
        }
        r.setTotal(total);
        r.setSubtotal(safeDouble(o.getSubtotal()));
// items
        if (o.getOrderItems() != null && !o.getOrderItems().isEmpty()) {
            List
                    <OrderItemResponse>
                    itemResponses = o.getOrderItems().stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
            r.setItems(itemResponses);
            r.setOrderItems(itemResponses);
            r.setTotalItems(itemResponses.size());
        } else {
            r.setItems(Collections.emptyList());
            r.setOrderItems(Collections.emptyList());
            r.setTotalItems(0);
        }
        return r;
    }
    /* ------------------- Helper Methods ------------------- */
    private OrderItemResponse toItemResponse(OrderItem it) {
        if (it == null) return null;
        Double storedUnitPrice = it.getUnitPrice();
        Integer storedQty = it.getQuantity() != null ? it.getQuantity() : 0;
        Double storedTotal = it.getTotal();
        String storedImage = it.getProductImage();
        String storedTitle = it.getProductTitle();
        boolean needPrice = (storedUnitPrice == null || storedUnitPrice <= 0.0);
        boolean needImage = (storedImage == null || storedImage.isBlank());
        boolean needTitle = (storedTitle == null || storedTitle.isBlank());
        if (needPrice || needImage || needTitle) {
            try {
                Map<String, Object> prod = fetchProductPublicOrAdmin(it.getProductId());
                if (prod != null) {
                    Object priceObj = prod.get("price");
                    Double price = null;
                    if (priceObj instanceof Number) price = ((Number) priceObj).doubleValue();
                    else if (priceObj instanceof String) {
                        try { price = Double.parseDouble((String) priceObj); } catch (Exception ignored) {}
                    }
                    if (needPrice && price != null) {
                        storedUnitPrice = price;
                        storedTotal = price * storedQty;
                    }
                    Object imageName = prod.get("imageName");
                    Object imageNames = prod.get("imageNames");
                    String first = null;
                    if (imageName != null) first = imageName.toString();
                    else if (imageNames instanceof java.util.List && !((java.util.List<?>) imageNames).isEmpty())
                        first = ((java.util.List<?>) imageNames).get(0).toString();
                    if (needImage && first != null && !first.isBlank()) {
                        String imgUrl;
                        if (first.startsWith("http://") || first.startsWith("https://")) {
                            imgUrl = first;
                        } else {
                            String base = productServiceBase != null && !productServiceBase.isBlank() ? productServiceBase : masterServiceBase;
                            if (base == null || base.isBlank()) base = uploadsBaseUrl;
                            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
                            try { imgUrl = base + "/public/products/images/" + java.net.URLEncoder.encode(first, java.nio.charset.StandardCharsets.UTF_8); }
                            catch (Exception ex) { imgUrl = base + "/public/products/images/" + first; }
                        }
                        storedImage = imgUrl;
                    }
                    if (needTitle && prod.get("title") != null) storedTitle = String.valueOf(prod.get("title"));
                }
            } catch (Exception ex) {
                log.debug("toItemResponse: enrichment failed for product {} : {}", it.getProductId(), ex.getMessage());
            }
        }
        double totalValue = (storedTotal != null && storedTotal > 0) ? storedTotal : safeDouble(storedUnitPrice) * safeInt(storedQty);
        return OrderItemResponse.builder()
                .id(it.getId())
                .productId(it.getProductId())
                .productTitle(storedTitle)
                .productName(storedTitle)
                .productImage(storedImage)
                .quantity(storedQty)
                .unitPrice(storedUnitPrice)
                .total(totalValue)
                .subtotal(totalValue)
                .cancelled(Boolean.TRUE.equals(it.getCancelled()))
                .cancelReason(it.getCancelReason())
                .build();
    }
    private OrderResponse toResponse(Order o) {
        if (o == null) return null;
        OrderResponse r = new OrderResponse();
        r.setId(o.getId());
        r.setOrderId(o.getId());
        r.setOrderNumber(o.getOrderNumber());
        r.setOrderDate(LocalDateTime.ofInstant(o.getCreatedAt(), ZoneId.systemDefault()));
        r.setUserId(o.getUserId());
        r.setStatus(o.getStatus() != null ? o.getStatus().name() : null);
// 🔹 totals
        double orderTotal = safeDouble(o.getTotalAmount());
        if (orderTotal <= 0 && o.getOrderItems() != null && !o.getOrderItems().isEmpty()) {
            orderTotal = o.getOrderItems().stream()
                    .mapToDouble(it -> (it.getTotal() != null && it.getTotal() > 0)
                            ? it.getTotal()
                            : safeDouble(it.getUnitPrice()) * safeInt(it.getQuantity()))
                    .sum();
        }
        r.setTotal(orderTotal);
        double subtotal = safeDouble(o.getSubtotal());
        if (subtotal <= 0 && o.getOrderItems() != null && !o.getOrderItems().isEmpty()) {
            subtotal = o.getOrderItems().stream()
                    .mapToDouble(it -> (it.getTotal() != null && it.getTotal() > 0)
                            ? it.getTotal()
                            : safeDouble(it.getUnitPrice()) * safeInt(it.getQuantity()))
                    .sum();
        }
        r.setSubtotal(subtotal);
// ✅ ADD THESE LINES
        r.setTax(o.getTax());
        r.setShipping(o.getShipping());                // now order.shipping is NOT null
        r.setPaymentMethod(o.getPaymentMethod());      // sends RAZORPAY / WALLET / COD
        r.setPaymentStatus(o.getPaymentStatus());
        r.setPaidAmount(o.getPaidAmount());
// items
        r.setTotalItems(o.getOrderItems() != null ? o.getOrderItems().size() : 0);
        List
                <OrderItemResponse>
                itemResponses = o.getOrderItems() != null
                ? o.getOrderItems().stream().map(this::toItemResponse).collect(Collectors.toList())
                : Collections.emptyList();
        r.setOrderItems(itemResponses);
        r.setItems(itemResponses);
        r.setCreatedAt(o.getCreatedAt());
        r.setUpdatedAt(o.getUpdatedAt());
        r.setUserEmail(o.getUserEmail());
        return r;
    }
    private byte[] generateInvoice(Order o) throws Exception {
// ---------- 0. Basic calculations ----------
        double subtotal = safeDouble(o.getSubtotal());
        double tax = safeDouble(o.getTax());
        double shipping = safeDouble(o.getShipping());
// grand total: prefer totalAmount, else total
        double grandTotal = (o.getTotalAmount() != null && o.getTotalAmount() > 0)
                ? o.getTotalAmount()
                : safeDouble(o.getTotal());
        if (grandTotal <= 0) {
// fallback from items if needed
            if (o.getOrderItems() != null) {
                grandTotal = o.getOrderItems().stream()
                        .mapToDouble(it -> (it.getTotal() != null && it.getTotal() > 0)
                                ? it.getTotal()
                                : safeDouble(it.getUnitPrice()) * safeInt(it.getQuantity()))
                        .sum();
            }
        }
// discount = (subtotal + tax + shipping) - grandTotal (mainly coupon)
        double baseAmount = subtotal + tax + shipping;
        double discount = baseAmount - grandTotal;
        if (discount < 0.01) discount = 0.0; // ignore tiny rounding
        double paid = safeDouble(o.getPaidAmount());
        double balanceDue = grandTotal - paid;
        if (balanceDue < 0.0) balanceDue = 0.0;
// ---------- 1. Fetch address (if available) ----------
        AddressDto address = null;
        try {
            if (o.getAddressId() != null) {
                address = fetchAddressById(o.getAddressId());
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch address {} for invoice: {}", o.getAddressId(), ex.getMessage());
        }
// ---------- 2. Prepare PDF document ----------
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36); // margins
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, baos);
        doc.open();
// Fonts
        Font shopNameFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Font invoiceTitle   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
        Font sectionHeader  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Font normalFont     = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font boldSmallFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
// ---------- 3. Header: Shop info ----------
        Paragraph shop = new Paragraph("Hyperchip Electronics Store", shopNameFont);
        shop.setSpacingAfter(4f);
        doc.add(shop);
        Paragraph shopAddr = new Paragraph(
                "Online Electronics Store\n"
                        + "Email: support@hyperchip.com\n"
                        + "Website: www.hyperchip.com",
                normalFont
        );
        shopAddr.setSpacingAfter(10f);
        doc.add(shopAddr);
// Invoice title
        Paragraph invTitle = new Paragraph("INVOICE", invoiceTitle);
        invTitle.setSpacingAfter(10f);
        doc.add(invTitle);
// ---------- 4. Top info (Bill To + Order meta) ----------
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new int[]{55, 45});
// left cell: Bill To / Shipping Address
        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        Paragraph billToHeader = new Paragraph("Bill To / Shipping Address", sectionHeader);
        billToHeader.setSpacingAfter(3f);
        left.addElement(billToHeader);
        if (address != null) {
// We only use fields we are sure exist: city/state/country.
// Email we take from order.
            left.addElement(new Paragraph("Customer: " + (o.getUserEmail() != null ? o.getUserEmail() : "N/A"), normalFont));
            StringBuilder addrLine = new StringBuilder();
            if (address.getCity() != null && !address.getCity().isBlank()) {
                addrLine.append(address.getCity());
            }
            if (address.getState() != null && !address.getState().isBlank()) {
                if (!addrLine.isEmpty()) addrLine.append(", ");
                addrLine.append(address.getState());
            }
            if (address.getCountry() != null && !address.getCountry().isBlank()) {
                if (!addrLine.isEmpty()) addrLine.append(", ");
                addrLine.append(address.getCountry());
            }
            if (!addrLine.isEmpty()) {
                left.addElement(new Paragraph(addrLine.toString(), normalFont));
            } else {
                left.addElement(new Paragraph("Address: (details not available)", normalFont));
            }
        } else {
            left.addElement(new Paragraph("Customer: " + (o.getUserEmail() != null ? o.getUserEmail() : "N/A"), normalFont));
            left.addElement(new Paragraph("Address: (not available)", normalFont));
        }
        infoTable.addCell(left);
// right cell: Order details
        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime orderDate = LocalDateTime.ofInstant(
                o.getCreatedAt(),
                ZoneId.systemDefault()
        );
        right.addElement(new Paragraph("Order No: " + o.getOrderNumber(), boldSmallFont));
        right.addElement(new Paragraph("Order Date: " + orderDate.format(fmt), normalFont));
        String pm = o.getPaymentMethod() != null ? o.getPaymentMethod() : "N/A";
        String prettyPm;
        switch (pm.toUpperCase()) {
            case "COD"      -> prettyPm = "Cash on Delivery";
            case "WALLET"   -> prettyPm = "Wallet Payment";
            case "RAZORPAY" -> prettyPm = "Online Payment (Razorpay)";
            default         -> prettyPm = pm;
        }
        right.addElement(new Paragraph("Payment Method: " + prettyPm, normalFont));
        String pStatus = o.getPaymentStatus() != null ? o.getPaymentStatus() : "PENDING";
        right.addElement(new Paragraph("Payment Status: " + pStatus, normalFont));
        OrderStatus status = o.getStatus();
        if (status != null) {
            right.addElement(new Paragraph("Order Status: " + status.name(), normalFont));
        }
        infoTable.addCell(right);
        infoTable.setSpacingAfter(12f);
        doc.add(infoTable);
// ---------- 5. Items table ----------
        PdfPTable itemsTable = new PdfPTable(5);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new int[]{8, 42, 10, 20, 20});
// header row
        Stream.of("No", "Item", "Qty", "Unit Price (AED)", "Line Total (AED)")
                .forEach(col -> {
                    PdfPCell c = new PdfPCell(new Phrase(col, sectionHeader));
                    c.setBackgroundColor(Color.LIGHT_GRAY);
                    c.setPadding(5f);
                    itemsTable.addCell(c);
                });
        int index = 1;
        if (o.getOrderItems() != null) {
            for (OrderItem it : o.getOrderItems()) {
// No.
                PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(index++), normalFont));
                c1.setPadding(4f);
                itemsTable.addCell(c1);
// Item title
                String title = it.getProductTitle() != null ? it.getProductTitle() : ("Product " + it.getProductId());
                PdfPCell c2 = new PdfPCell(new Phrase(title, normalFont));
                c2.setPadding(4f);
                itemsTable.addCell(c2);
// Qty
                PdfPCell c3 = new PdfPCell(new Phrase(String.valueOf(safeInt(it.getQuantity())), normalFont));
                c3.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                c3.setPadding(4f);
                itemsTable.addCell(c3);
// Unit price
                double unit = safeDouble(it.getUnitPrice());
                PdfPCell c4 = new PdfPCell(new Phrase(String.format("%.2f", unit), normalFont));
                c4.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                c4.setPadding(4f);
                itemsTable.addCell(c4);
// Line total
                double lineTotal = (it.getTotal() != null && it.getTotal() > 0)
                        ? it.getTotal()
                        : unit * safeInt(it.getQuantity());
                PdfPCell c5 = new PdfPCell(new Phrase(String.format("%.2f", lineTotal), normalFont));
                c5.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                c5.setPadding(4f);
                itemsTable.addCell(c5);
            }
        }
        itemsTable.setSpacingAfter(12f);
        doc.add(itemsTable);
// ---------- 6. Summary (totals/discounts) ----------
        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(40);      // small box
        summary.setHorizontalAlignment(PdfPTable.ALIGN_RIGHT);
        summary.setWidths(new int[]{50, 50});
// helper to add a row
        java.util.function.BiConsumer<String, String> addRow = (label, value) -> {
            PdfPCell l = new PdfPCell(new Phrase(label, normalFont));
            l.setBorder(PdfPCell.NO_BORDER);
            l.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            PdfPCell v = new PdfPCell(new Phrase(value, normalFont));
            v.setBorder(PdfPCell.NO_BORDER);
            v.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
            summary.addCell(l);
            summary.addCell(v);
        };
        addRow.accept("Subtotal", String.format("AED %.2f", subtotal));
        addRow.accept("Tax", String.format("AED %.2f", tax));
        addRow.accept("Shipping", String.format("AED %.2f", shipping));
        if (discount > 0.0) {
            addRow.accept("Discount", String.format("- AED %.2f", discount));
        }
// bold row for grand total
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total", sectionHeader));
        totalLabel.setBorder(PdfPCell.TOP);
        totalLabel.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        summary.addCell(totalLabel);
        PdfPCell totalValue = new PdfPCell(new Phrase(String.format("AED %.2f", grandTotal), sectionHeader));
        totalValue.setBorder(PdfPCell.TOP);
        totalValue.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        summary.addCell(totalValue);
// Paid / Balance
        if (paid > 0.0) {
            addRow.accept("Paid", String.format("AED %.2f", paid));
            addRow.accept("Balance Due", String.format("AED %.2f", balanceDue));
        }
        summary.setSpacingAfter(20f);
        doc.add(summary);
// ---------- 7. Footer ----------
        Paragraph thanks = new Paragraph(
                "Thank you for shopping with Hyperchip!\n"
                        + "This invoice is generated electronically and does not require a signature.",
                normalFont
        );
        thanks.setSpacingBefore(10f);
        doc.add(thanks);
        doc.close();
        return baos.toByteArray();
    }
    // --- safe conversion helpers ---
    private double safeDouble(Double value){ return value != null ? value : 0.0; }
    private int safeInt(Integer value){ return value != null ? value : 0; }
    private String generateOrderNumber(){ return Long.toHexString(System.nanoTime()).toUpperCase(); }
    @SuppressWarnings("unchecked")
    private Map<String,Object> fetchProductPublicOrAdmin(Long productId){
        if (productId == null) return null;
        String base = (productServiceBase != null && !productServiceBase.isBlank()) ? productServiceBase : masterServiceBase;
        if (base == null || base.isBlank()) base = uploadsBaseUrl;
        if (base.endsWith("/")) base = base.substring(0, base.length()-1);
        String publicUrl = base + "/public/products/" + productId;
        try { Map<String,Object> resp = restTemplate.getForObject(publicUrl, Map.class); if (resp != null) return resp; } catch (Exception ex) { log.debug("fetchProductPublicOrAdmin: public fetch failed for {} : {}", productId, ex.getMessage()); }
        String adminUrl = base + "/api/admin/products/" + productId;
        try { Map<String,Object> resp2 = restTemplate.getForObject(adminUrl, Map.class); if (resp2 != null) return resp2; } catch (Exception ex) { log.debug("fetchProductPublicOrAdmin: admin fetch failed for {} : {}", productId, ex.getMessage()); }
        return null;
    }
    @Override
    public void updateOrderStatus(Long orderId, String status, String note) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        try { OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase()); order.setStatus(newStatus); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid status: " + status); }
        if (note != null && !note.isBlank()) { try { order.getClass().getMethod("setAdminNote", String.class).invoke(order, note); } catch (Exception ignored) {} }
        orderRepository.save(order);
    }
    @Override
    public void updatePaymentStatus(String orderId, String status) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId cannot be null or blank");
        }
        Long id;
        try {
            id = Long.parseLong(orderId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid orderId: " + orderId);
        }
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
// ✅ Normalize status: if SUCCESS ⇒ store PAID
        String normalized = (status != null) ? status.toUpperCase() : null;
        if ("SUCCESS".equalsIgnoreCase(normalized)) {
            normalized = "PAID";
        }
        order.setPaymentStatus(normalized);
// For paid status, set paidAt + CONFIRMED
        if ("PAID".equalsIgnoreCase(normalized)) {
            order.setPaidAt(Instant.now());
            order.setStatus(OrderStatus.CONFIRMED);
        }
        orderRepository.save(order);
    }
    @Override
    public void handleRazorpayCallback(Map<String, Object> payload, Map<String, String> params) {
        log.info("Received Razorpay callback: payload={}, params={}", payload, params);
// intentionally left as a stub — server-side verification handled in PaymentService
    }
    @Override
    public void handlePaypalCallback(Map<String, Object> payload, Map<String, String> params) {
        log.info("Received PayPal callback: payload={}, params={}", payload, params);
    }
    // 1) This one MATCHES the interface (returns PlaceOrderResponse)
    @Override
    public PlaceOrderResponse cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
// ✅ Only allow cancel when order is PENDING or CONFIRMED
        if (!(order.getStatus() == OrderStatus.PENDING ||
                order.getStatus() == OrderStatus.CONFIRMED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending or confirmed orders can be cancelled"
            );
        }
        order.setStatus(OrderStatus.CANCELLED);
        try {
            Order.class.getMethod("setCancelReason", String.class).invoke(order, reason);
        } catch (Exception ignored) {}
        order.getOrderItems().forEach(item -> {
            try {
                productServiceClient.incrementStock(item.getProductId(), item.getQuantity());
            } catch (Exception ex) {
                log.error("Failed to increment stock for product {} when cancelling order {}",
                        item.getProductId(), order.getId(), ex);
            }
        });
// 💰 if already paid, refund directly to wallet
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            double amountToRefund = order.getPaidAmount() != null && order.getPaidAmount() > 0
                    ? order.getPaidAmount()
                    : (order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
            refundToWallet(order, amountToRefund, "Order cancellation", "ORDER_CANCELLED");
        }
        orderRepository.save(order);
        PlaceOrderResponse resp = buildPlaceOrderResponse(order);
        resp.setSuccess(true);
        resp.setMessage("Order cancelled successfully");
        return resp;
    }
    // ✅ Keep only one helper (no @Override)
    public void cancelOrder(Long orderId) {
        cancelOrder(orderId, null);
    }
    @Override
    public PlaceOrderResponse cancelOrderItem(Long orderId, Long itemId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        OrderItem item = order.getOrderItems().stream()
                .filter(it -> Objects.equals(it.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));
// Already cancelled?
        if (Boolean.TRUE.equals(item.isCancelled())) {
            PlaceOrderResponse already = buildPlaceOrderResponse(order);
            already.setSuccess(true);
            already.setMessage("Order item already cancelled");
            return already;
        }
// ✅ 1) Mark item cancelled
        item.setCancelled(true);
        try {
            OrderItem.class.getMethod("setCancelReason", String.class).invoke(item, reason);
        } catch (Exception ignored) {}
        orderItemRepository.save(item);
// ✅ 2) Restock this item
        try {
            productServiceClient.incrementStock(item.getProductId(), item.getQuantity());
        } catch (Exception ex) {
            log.warn("Failed to restock productId={} qty={}: {}",
                    item.getProductId(), item.getQuantity(), ex.getMessage());
        }
// ✅ 3) Recalculate order subtotal / totalAmount from NON-cancelled items only
        double tax = safeDouble(order.getTax());
        double shipping = safeDouble(order.getShipping());
        double oldSubtotal = safeDouble(order.getSubtotal());
        double oldTotal = safeDouble(order.getTotalAmount());
        double newSubtotal = order.getOrderItems().stream()
                .filter(it -> !Boolean.TRUE.equals(it.getCancelled()))
                .mapToDouble(it -> {
                    double unit = safeDouble(it.getUnitPrice());
                    int qty = safeInt(it.getQuantity());
                    double lineTotal = (it.getTotal() != null && it.getTotal() > 0)
                            ? it.getTotal()
                            : unit * qty;
                    return lineTotal;
                })
                .sum();
// Preserve same coupon discount, if any
        double baseOld = oldSubtotal + tax + shipping;
        double couponDiscount = baseOld - oldTotal;
        if (couponDiscount < 0) couponDiscount = 0.0;
        double newTotalAmount = newSubtotal + tax + shipping - couponDiscount;
        if (newTotalAmount < 0) newTotalAmount = 0.0;
// store back
        order.setSubtotal(newSubtotal);
        order.setTotalAmount(newTotalAmount);
// ✅ 4) If all items cancelled → cancel whole order
        boolean allCancelled = order.getOrderItems().stream()
                .allMatch(OrderItem::isCancelled);
        if (allCancelled) {
            order.setStatus(OrderStatus.CANCELLED);
            try {
                Order.class.getMethod("setCancelReason", String.class).invoke(order, reason);
            } catch (Exception ignored) {}
        }
// (Optional: if paymentStatus == PAID, refund difference oldTotal - newTotalAmount back to wallet)
        orderRepository.save(order);
        PlaceOrderResponse resp = buildPlaceOrderResponse(order);
        resp.setSuccess(true);
        resp.setMessage(allCancelled
                ? "Order cancelled (all items cancelled)"
                : "Order item cancelled");
        return resp;
    }
    @Override
    public Double getPayableAmount(Long orderId) {
        Optional
                <Order>
                opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) return null;
        Order order = opt.get();
        return order.getTotalAmount() != null ? order.getTotalAmount() : order.getTotal();
    }
    @Override
    public boolean markOrderPaid(Long orderId, String paymentReference, String paymentMethod, Double amount) {
        Optional
                <Order>
                optional = orderRepository.findById(orderId);
        if (optional.isEmpty()) return false;
        Order order = optional.get();
// 🔻 DECREMENT STOCK FOR ALL ORDER ITEMS (ONLINE PAYMENT SUCCESS)
        for (OrderItem it : order.getOrderItems()) {
            try {
                productServiceClient.decrementStock(it.getProductId(), it.getQuantity());
            } catch (Exception ex) {
                log.error("Failed to decrement stock for product {} when marking order {} as PAID",
                        it.getProductId(), order.getId(), ex);
            }
        }
        order.setPaymentStatus("PAID");
        order.setPaymentReference(paymentReference);
        order.setPaymentMethod(paymentMethod);
        order.setPaidAmount(amount);
        order.setPaidAt(Instant.now());
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        try {
            String url = cartServiceBase + "/" + order.getUserId() + "/clear?orderComplete=true";
            restTemplate.delete(url);
        } catch (Exception ex) {
            log.warn("Failed to clear cart for user {}: {}", order.getUserId(), ex.getMessage());
        }
        try {
            mailService.sendOrderConfirmation(order);
        } catch (Exception ex) {
            log.warn("sendOrderPaidNotification failed for order {}: {}", order.getId(), ex.getMessage());
        }
        return true;
    }
    @Override
    public boolean markOrderPaymentFailed(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setPaymentStatus("FAILED");
        order.setPaymentFailureReason(reason);
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
        try {
            mailService.sendPaymentFailedNotification(order);
        } catch (Exception ex) {
            log.warn("Failed to send payment failure email: {}", ex.getMessage());
        }
        return true;
    }
    @Override
    public void incrementStock(Long productId, Integer quantity) {
        productServiceClient.incrementStock(productId, quantity);
    }
    @Override
    public Page
            <OrderResponse>
    listOrdersForUserResponses(Long userId, Pageable pageable) {
        if (userId == null) return Page.empty(pageable);
        Page
                <Order>
                po = orderRepository.findByUserId(userId,
                pageable != null ? pageable : PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        return po.map(this::toResponse);
    }
    // =================== WALLET HELPERS ===================
    private void refundToWallet(Order order, double amount, String reason, String source) {
        if (order == null || order.getUserId() == null) return;
        if (amount <= 0) return;
        if (walletServiceBase == null || walletServiceBase.isBlank()) {
            log.warn("walletServiceBase not configured; skipping wallet refund");
            return;
        }
        try {
            String base = walletServiceBase;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            String url = base + "/api/wallet/credit";
            WalletCreditRequest request = WalletCreditRequest.builder()
                    .userId(order.getUserId())
                    .orderId(order.getId())
                    .amount(amount)
                    .reason(reason != null ? reason : "Refund for order " + order.getOrderNumber())
                    .source(source)
                    .build();
            restTemplate.postForEntity(url, request, WalletBalanceDto.class);
            order.setPaymentStatus("REFUNDED");
        } catch (Exception ex) {
            log.warn("Failed to refund wallet for order {}: {}", order.getId(), ex.getMessage());
        }
    }
// =================== RETURN REQUEST (USER) ===================
    @Override
    public PlaceOrderResponse requestReturn(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));
// ✅ Only allow return request if it is currently DELIVERED
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only delivered orders can be returned"
            );
        }
        order.setStatus(OrderStatus.RETURN_REQUESTED);
// store reason (re-using cancelReason field)
        try {
            Order.class.getMethod("setCancelReason", String.class)
                    .invoke(order, reason);
        } catch (Exception ignored) {}
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        PlaceOrderResponse resp = buildPlaceOrderResponse(order);
        resp.setSuccess(true);
        resp.setMessage("Return request submitted");
        return resp;
    }
    @Override
    public void cancelReturnRequest(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));
// only allow cancel if it is currently RETURN_REQUESTED
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No pending return request for this order"
            );
        }
// go back to DELIVERED
        order.setStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }
    // =================== REPLACEMENT REQUEST (USER) ===================
    @Override
    public PlaceOrderResponse requestReplacement(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));
// ✅ Only allow replacement if order is DELIVERED
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only delivered orders can be replaced"
            );
        }
        order.setStatus(OrderStatus.REPLACEMENT_REQUESTED);
// store reason (reuse cancelReason field)
        try {
            Order.class.getMethod("setCancelReason", String.class)
                    .invoke(order, reason);
        } catch (Exception ignored) {}
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        PlaceOrderResponse resp = buildPlaceOrderResponse(order);
        resp.setSuccess(true);
        resp.setMessage("Replacement request submitted");
        return resp;
    }
    @Override
    public void cancelReplacementRequest(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));
// only allow cancel if it is currently REPLACEMENT_REQUESTED
        if (order.getStatus() != OrderStatus.REPLACEMENT_REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No pending replacement request for this order"
            );
        }
// go back to DELIVERED
        order.setStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }
    // =================== WALLET PAYMENT HELPER ===================
    private WalletPaymentResponse payWithWallet(Order order) {
        if (order == null || order.getUserId() == null) return null;
        if (walletServiceBase == null || walletServiceBase.isBlank()) {
            log.warn("walletServiceBase not configured; skipping wallet payment");
            return null;
        }
        String base = walletServiceBase;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String url = base + "/api/wallet/pay";
        WalletPaymentRequest request = WalletPaymentRequest.builder()
                .userId(order.getUserId())
                .orderId(order.getId())
                .amount(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
                .build();
        return restTemplate.postForObject(url, request, WalletPaymentResponse.class);
    }
    private AddressDto fetchAddressById(Long addressId) {
        if (addressId == null) return null;
        if (userServiceBase == null || userServiceBase.isBlank()) {
            log.warn("userServiceBase not configured; cannot fetch address {}", addressId);
            return null;
        }
        try {
            String base = userServiceBase;
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
// Adjust path if your user-service endpoint is different
            String url = base + "/api/addresses/" + addressId;
            return restTemplate.getForObject(url, AddressDto.class);
        } catch (Exception ex) {
            log.warn("Failed to fetch address {} from user-service: {}", addressId, ex.getMessage());
            return null;
        }
    }
}



/**
 * OrderServiceImpl
 *
 * Overview:
 * ----------
 * This class implements the OrderService for the Hyperchip order microservice.
 * It handles the full order lifecycle for both user and admin flows:
 *   - placing orders (COD, Wallet, and other payment flows),
 *   - marking orders paid / payment failed,
 *   - cancelling orders (user/admin and single item cancellations),
 *   - return and replacement requests,
 *   - admin actions (list/search/update status),
 *   - invoice generation (PDF),
 *   - lightweight enrichment of order item data from product service,
 *   - wallet refunds/payments via wallet-service,
 *   - stock increment/decrement via ProductServiceClient,
 *   - clearing user cart via Cart service,
 *   - and various helpers for building DTO responses.
 *
 * Main responsibilities and behaviour:
 * -----------------------------------
 * 1) placeOrder(PlaceOrderRequest)
 *    - Validates incoming request (userId required).
 *    - Optionally fetches shipping address via user-service.
 *    - Builds OrderItem list from request items and applies "best offer" via OfferClient
 *      to compute per-line final price (fallback to line price if offer service fails).
 *    - Calculates subtotal (sum of item final lines), tax (currently 0.0), shipping using
 *      DeliveryChargeService.getDeliveryChargeForAddress(address), coupon discount,
 *      and final total amount (subtotal - coupon + tax + shipping).
 *    - Enforces business rules: e.g. COD not allowed above configured limit (example: 1000).
 *    - Persist order (Order + OrderItems), create an order number.
 *    - Branches per payment method:
 *         - COD: decrement stock for all items, mark pending/paid appropriately, clear cart.
 *         - WALLET: attempt wallet payment via wallet-service (payWithWallet()). On success:
 *             - decrement stock, mark order PAID/CONFIRMED, clear cart.
 *           On failure: mark payment failed and return failure PlaceOrderResponse.
 *         - Other (online): simply save and send confirmation email (mailService.sendOrderConfirmation).
 *    - Returns PlaceOrderResponse with order info and success/failure message.
 *
 * 2) Payment and status updates
 *    - markOrderPaid(orderId, paymentReference, paymentMethod, amount):
 *        - Decrements stock for items on successful online payment.
 *        - Sets payment details (status=PAID, reference, method, paidAt, paidAmount).
 *        - Marks order as CONFIRMED and clears user cart.
 *        - Sends confirmation email.
 *    - markOrderPaymentFailed(orderId, reason):
 *        - Marks payment status FAILED and order status PAYMENT_FAILED, tries to notify by email.
 *    - updatePaymentStatus(orderIdString, status):
 *        - Normalizes status (e.g., SUCCESS -> PAID) and updates paidAt + status when PAID.
 *
 * 3) Cancellations, returns and replacements
 *    - cancelOrder(orderId, reason):
 *        - Allowed only when order is PENDING or CONFIRMED (business rule).
 *        - Marks order CANCELLED, restocks items via ProductServiceClient.incrementStock.
 *        - If already PAID, attempts to refund to wallet (refundToWallet()).
 *    - cancelOrderItem(orderId, itemId, reason):
 *        - Marks a single order item cancelled, restocks that product, recalculates
 *          order subtotal and total (preserves coupon discount), if all items cancelled
 *          then cancels the whole order. Persists changes.
 *    - requestReturn(orderId, reason) / cancelReturnRequest(orderId):
 *        - requestReturn: allowed only when order status == DELIVERED; sets status RETURN_REQUESTED.
 *        - cancelReturnRequest: allowed only when RETURN_REQUESTED; reverts to DELIVERED.
 *    - requestReplacement(orderId, reason) / cancelReplacementRequest(orderId):
 *        - Similar guard: only for DELIVERED -> sets REPLACEMENT_REQUESTED, or cancels back to DELIVERED.
 *    - returnOrderByAdmin(orderId, reason) / cancelOrderByAdmin(orderId, reason)
 *        - Admin-approved flows that perform stock increment and (if PAID) wallet refunds.
 *
 * 4) Admin list/search/update helpers
 *    - listForAdmin(page,size,status,q): paginated listing supporting status filter and search.
 *    - findOrderResponseById(id): returns DTO wrapped in Optional.
 *    - updateStatusAdmin(id, status, note): supports approving returns specially (calls returnOrderByAdmin),
 *      and generic status update (saves admin note if setter present).
 *
 * 5) Invoice generation
 *    - generateInvoicePdf(Long orderId) -> generateInvoice(Order)
 *    - generateInvoice builds a nicely formatted PDF (iText / lowagie) that includes:
 *        - Shop header and contact details
 *        - Bill To / Shipping address (fetched from user-service if addressId exists)
 *        - Order meta (orderNo, date, payment method, status)
 *        - Items table (No, Item, Qty, Unit Price, Line Total)
 *        - Summary block (subtotal, tax, shipping, discount, total, paid, balance)
 *        - Footer / thank you note
 *    - Uses safe numeric conversions and falls back to item totals where totals are missing.
 *
 * 6) DTO conversion & enrichment
 *    - toItemResponse(OrderItem): builds OrderItemResponse including enrichment from product service
 *      for missing price/image/title information. If product image names are returned, constructs
 *      a public image URL using configured product/master/uploads base URLs.
 *    - toResponse(Order): builds OrderResponse with fields: id, orderNumber, totals, tax, shipping,
 *      payment method/status, paid amount, items, timestamps, userEmail, etc.
 *    - buildPlaceOrderResponse(Order): builds PlaceOrderResponse used after create/cancel flows.
 *
 * 7) External integrations / dependencies
 *    - OrderRepository, OrderItemRepository: persistence.
 *    - ProductServiceClient (feign or custom client): decrementStock/incrementStock calls (critical side-effects).
 *    - OfferClient: getBestPrice(productId, ...) — used during placeOrder to compute final line price.
 *    - DeliveryChargeService: getDeliveryChargeForAddress used to determine shipping.
 *    - RestTemplate: used for calls to cart-service (clear cart), user-service (address fetch),
 *      wallet-service (pay/refund), and fallback product endpoints in fetchProductPublicOrAdmin.
 *    - MailService: email notifications (sendOrderConfirmation, sendPaymentFailedNotification).
 *
 * 8) Configuration properties used (injected via @Value)
 *    - cart.service.url (cartServiceBase) : used to clear user cart after successful order placement.
 *    - uploads.base.url (uploadsBaseUrl) : fallback host for product images.
 *    - master.service.url (masterServiceBase) : fallback service base.
 *    - product.service.url (productServiceBase) : product service base for enrichment.
 *    - wallet.service.url (walletServiceBase) : used for wallet payments/refunds.
 *    - user.service.url (userServiceBase) : used to fetch addresses for invoices/shipping.
 *
 * 9) Important business rules & side effects
 *    - Stock decrement is performed when:
 *        - placing COD (immediately after saving order),
 *        - marking order paid (online/wallet),
 *      and increment is performed on cancellations/returns.
 *    - Wallet payment: payWithWallet() calls wallet-service /api/wallet/pay; refundToWallet()
 *      calls /api/wallet/credit. Both are best-effort and logged on failure (do not throw).
 *    - Cart clearing: attempted after successful placement/payment using cartServiceBase endpoint.
 *    - Email sending: best-effort (exceptions logged but do not break order flow).
 *    - Many operations use "best-effort" external calls: failures are logged and processing continues
 *      where reasonable (to avoid leaving critical flows blocked).
 *
 * 10) Error handling and guards
 *    - Uses ResponseStatusException(HttpStatus.*) for REST-friendly errors (e.g., NOT_FOUND, BAD_REQUEST).
 *    - Validates method inputs (e.g., userId required for placeOrder).
 *    - Uses try/catch around external calls and logs failures rather than failing the entire transaction in many cases.
 *
 * 11) Transactional behaviour
 *    - Class is annotated with @Transactional: operations that mutate DB are enclosed in a transactional boundary.
 *      Note: external calls (productServiceClient, wallet, cart clear, email) are outside DB and are best-effort;
 *      consider compensating actions if strict consistency is required.
 *
 * 12) Helper methods
 *    - safeDouble(Double), safeInt(Integer) : null-safe numeric conversions.
 *    - generateOrderNumber() : generates a simple hex-based order number using System.nanoTime.
 *    - fetchProductPublicOrAdmin(productId) : tries product public endpoint then admin endpoint using RestTemplate.
 *    - payWithWallet(Order) : asks wallet-service to charge (used by WALLET payment flow).
 *    - refundToWallet(Order,amount,reason,source) : credits wallet on cancellations/returns (best-effort).
 *
 * 13) Internal aggregation & calculations
 *    - Subtotal computation prefers stored subtotal, otherwise sums order items.
 *    - Discount calculation for invoice = (subtotal + tax + shipping) - grandTotal (assumes coupons stored this way).
 *    - On item cancellation, recalculation preserves coupon discount (re-applies old coupon delta).
 *
 * 14) Security & validation considerations (notes for maintainers)
 *    - Caller identity / authorization not enforced here — the controller layer should ensure user/admin permissions.
 *    - External services communication is assumed trusted; production should use timeouts/circuit-breakers/retries.
 *    - Consider idempotency keys for payment/checkout endpoints to avoid duplicate side-effects (stock decrement, wallet charge).
 *
 * 15) TODOs / suggestions (non-invasive)
 *    - Add circuit-breaker/resilience for external calls (product, wallet, offer, cart).
 *    - Move monetary calculations to a Money type (BigDecimal) for precision across the class.
 *    - Add metrics/logging around failed external calls for operational visibility.
 *    - Consider making coupon discount calculation explicit and storing coupon usage at order placement time.
 *
 * Method summary:
 * ----------------
 * - placeOrder(PlaceOrderRequest req) : Main checkout flow for COD/WALLET/online. Creates Order, handles payment branch,
 *                                       decrements stock, clears cart, returns PlaceOrderResponse.
 *
 * - cancelOrderByAdmin(Long orderId, String reason)  : Admin cancels an order, increments stock, refunds wallet if paid.
 * - returnOrderByAdmin(Long orderId, String reason)  : Admin accepts return, increments stock, refunds wallet if paid.
 * - getOrder(Long orderId)                           : Returns Optional<Order>.
 * - getOrderById(Long orderId)                       : Returns OrderResponse DTO or throws NOT_FOUND.
 * - getOrderByIdentifier(String idOrOrderNumber)     : Fetch by numeric id or by orderNumber string.
 * - getOrdersByUserId(Long userId)                   : All orders for a user (DTO list).
 * - listOrders(Pageable pageable)                    : Paginated Order entities.
 * - listOrdersForUser(Long userId, Pageable pageable): Paginated orders for a user (entities).
 * - changeOrderStatus(Long orderId, OrderStatus s, String note)
 *                                                    : Change status and return PlaceOrderResponse.
 * - generateInvoicePdf(Long orderId)                 : Builds and returns invoice PDF bytes.
 * - searchOrders(String keyword)                     : Simple search by order number.
 * - getOrdersForUser(Long userId)                    : Alias for getOrdersByUserId.
 * - listForAdmin(int page,int size,String status,String q)
 *                                                    : Admin paginated search/list.
 * - findOrderResponseById(Long id)                   : Optional<OrderResponse>.
 * - updateStatusAdmin(Long id, String status, String note)
 *                                                    : Admin status update with special return approval handling.
 * - getInvoicePdf(Long id)                           : Alias for generateInvoicePdf.
 * - buildPlaceOrderResponse(Order o)                 : Helper that builds PlaceOrderResponse DTO.
 * - toItemResponse(OrderItem it)                     : Enriches OrderItem -> OrderItemResponse with product info if needed.
 * - toResponse(Order o)                              : Converts Order -> OrderResponse (enriched).
 * - generateInvoice(Order o)                         : Builds invoice PDF bytes.
 * - safeDouble(Double), safeInt(Integer)             : Null-safe helpers.
 * - generateOrderNumber()                            : Simple order number generator.
 * - fetchProductPublicOrAdmin(Long productId)        : Tries public then admin product endpoints.
 * - updateOrderStatus(Long orderId, String status, String note)
 *                                                    : Updates status (used by interface).
 * - updatePaymentStatus(String orderId, String status)
 *                                                    : Update payment status, normalize SUCCESS -> PAID.
 * - handleRazorpayCallback(...), handlePaypalCallback(...)
 *                                                    : Stubs for payment gateway callbacks (logged).
 * - cancelOrder(Long orderId)                        : Convenience wrapper that calls cancelOrder(orderId, null).
 * - cancelOrderItem(Long orderId, Long itemId, String reason)
 *                                                    : Cancels a single item, restocks and recalculates totals.
 * - getPayableAmount(Long orderId)                   : Returns order.totalAmount or order.total.
 * - markOrderPaid(...)                                : Mark order PAID, decrement stock, clear cart and notify email.
 * - markOrderPaymentFailed(...)                      : Mark payment failed and send failure email.
 * - incrementStock(productId, quantity)              : Proxy to productServiceClient.incrementStock.
 * - listOrdersForUserResponses(Long userId, Pageable pageable)
 *                                                    : Returns Page<OrderResponse> for a user.
 * - refundToWallet(Order order, double amount, String reason, String source)
 *                                                    : Credits wallet-service (best-effort).
 * - requestReturn, cancelReturnRequest               : User return request flow.
 * - requestReplacement, cancelReplacementRequest     : User replacement request flow.
 * - payWithWallet(Order order)                       : Calls wallet service to charge the user.
 * - fetchAddressById(Long addressId)                 : Fetches address from user-service (for invoice/shipping).
 *
 * Final note:
 * -----------
 * This implementation stitches together domain logic, persistence and multiple external services.
 * It favors availability in the face of external failures by logging errors and continuing where
 * possible. Keep the transactional boundaries and side-effects in mind when modifying or moving logic.
 */
