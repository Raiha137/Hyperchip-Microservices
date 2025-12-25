//package com.hyperchip.admin.controller;
//
//import com.hyperchip.common.dto.OrderResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.*;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//
//@Controller
//@RequestMapping("/admin")
//@RequiredArgsConstructor
//@Slf4j
//public class OrderAdminWebController {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final String ORDER_ADMIN_BASE = "http://localhost:8092/api/admin/orders";
//
//    @Value("${order.service.url:http://localhost:8086}")
//    private String orderServiceBaseUrl;
//
//    @GetMapping("/orders")
//    public String listOrders(@RequestParam(defaultValue = "0") int page,
//                             @RequestParam(defaultValue = "10") int size,
//                             @RequestParam(required = false) String status,
//                             @RequestParam(required = false) String q,
//                             Model model) {
//
//        String url = ORDER_ADMIN_BASE + "?page=" + page + "&size=" + size;
//        if (status != null && !status.isBlank()) {
//            url += "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);
//        }
//        if (q != null && !q.isBlank()) {
//            url += "&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
//        }
//
//        ResponseEntity<OrderResponse[]> resp = restTemplate.getForEntity(url, OrderResponse[].class);
//        OrderResponse[] orders = resp.getBody();
//
//        model.addAttribute("orders", orders);
//        model.addAttribute("currentPage", page);
//        model.addAttribute("filterStatus", status);
//        model.addAttribute("q", q);
//        model.addAttribute("activePage", "orders");
//        return "admin/orders";
//    }
//
//    @GetMapping("/orders/{id}")
//    public String viewOrder(@PathVariable Long id, Model model) {
//        OrderResponse order = restTemplate.getForObject(ORDER_ADMIN_BASE + "/" + id, OrderResponse.class);
//        model.addAttribute("order", order);
//        model.addAttribute("orderId", id);
//        return "admin/order-detail";
//    }
//
//    @GetMapping("/orders/{id}/status")
//    public String showOrderStatus(@PathVariable Long id, Model model) {
//        OrderResponse order = restTemplate.getForObject(ORDER_ADMIN_BASE + "/" + id, OrderResponse.class);
//        model.addAttribute("order", order);
//        model.addAttribute("orderId", id);
//        model.addAttribute("statuses", new String[]{
//                "PENDING", "CONFIRMED", "PACKED", "SHIPPED",
//                "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED", "RETURNED"
//        });
//        return "admin/order-status"; // your Thymeleaf template name
//    }
//
//    @PostMapping("/orders/{id}/status")
//    public String updateOrderStatus(@PathVariable Long id,
//                                    @RequestParam("status") String status,
//                                    @RequestParam(value = "note", required = false) String note,
//                                    RedirectAttributes redirectAttributes) {
//        try {
//            String url = ORDER_ADMIN_BASE + "/" + id + "/status";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            Map<String, Object> body = new HashMap<>();
//            body.put("status", status);
//            if (note != null && !note.isBlank()) body.put("note", note);
//
//            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
//
//            // Forward update to order service admin API (use PUT as server-side expects update semantics)
//            restTemplate.exchange(url, HttpMethod.PUT, req, Void.class);
//
//            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully.");
//        } catch (Exception e) {
//            log.error("Error updating order status for orderId {}", id, e);
//            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update order status.");
//        }
//        return "redirect:/admin/orders";
//    }
//
//    @PostMapping("/orders/{id}/cancel")
//    public String cancelOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
//        String url = ORDER_ADMIN_BASE + "/" + id + "/cancel";
//        if (reason != null) url += "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
//        restTemplate.postForEntity(url, null, Void.class);
//        return "redirect:/admin/orders";
//    }
//
//    @GetMapping("/orders/{id}/invoice")
//    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
//        ResponseEntity<byte[]> resp = restTemplate.getForEntity(ORDER_ADMIN_BASE + "/" + id + "/invoice", byte[].class);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_PDF);
//        headers.setContentDisposition(ContentDisposition.builder("attachment").filename("invoice-" + id + ".pdf").build());
//        return new ResponseEntity<>(resp.getBody(), headers, HttpStatus.OK);
//    }
//}
