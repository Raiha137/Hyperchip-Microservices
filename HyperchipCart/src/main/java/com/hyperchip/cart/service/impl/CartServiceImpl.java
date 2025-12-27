package com.hyperchip.cart.service.impl;

// =========================
// Imports
// =========================
import com.hyperchip.common.dto.AddToCartRequest;
import com.hyperchip.common.dto.CartProductDto;
import com.hyperchip.common.dto.CartResponse;
import com.hyperchip.common.dto.CartItemResponse;
import com.hyperchip.common.exception.BadRequestException;
import com.hyperchip.common.exception.ResourceNotFoundException;
import com.hyperchip.common.exception.OutOfStockException;
import com.hyperchip.cart.model.Cart;
import com.hyperchip.cart.model.CartItem;
import com.hyperchip.cart.repository.CartItemRepository;
import com.hyperchip.cart.repository.CartRepository;
import com.hyperchip.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CartServiceImpl
 *
 * Main intention:
 * This service handles all operations related to the shopping cart, including:
 * - Adding products to cart
 * - Updating product quantities
 * - Removing products
 * - Clearing the cart
 * - Fetching the current cart for a user
 * It communicates with the Product Service for stock and pricing info via RestTemplate.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartServiceImpl implements CartService {

    // =========================
    // Repositories & RestTemplate
    // =========================
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RestTemplate restTemplate;

    // =========================
    // Configuration values
    // =========================
    @Value("${uploads.base.url:http://localhost:8086}")
    private String uploadsBaseUrl;

    @Value("${product.service.url:http://localhost:8086}")
    private String productServiceBase;

    @Value("${master.service.url:http://localhost:8086}")
    private String masterServiceBase;

    @Value("${product.service.url:http://localhost:8086}")
    private String productServiceUrl;

    // Maximum quantity allowed per cart item
    private static final int MAX_QUANTITY_PER_ITEM = 10;

    // =========================
    // Get the cart for a user
    // =========================
    @Override
    public CartResponse getCartForUser(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder().userId(userId).build());
        return toResponse(cart);
    }

    // =========================
    // Add a product to the cart
    // =========================
    @Override
    public CartResponse addToCart(AddToCartRequest req) {
        CartProductDto product = fetchProduct(req.getProductId());

        // Check if product exists
        if (product == null)
            throw new ResourceNotFoundException("Product not found: " + req.getProductId());

        // Check if product or its category is blocked
        if (Boolean.TRUE.equals(product.getBlocked()) || Boolean.TRUE.equals(product.getCategoryBlocked())) {
            throw new BadRequestException("Product or category is blocked");
        }

        // Check stock availability
        if (product.getStock() == null || product.getStock() < 1) {
            throw new OutOfStockException("Product is out of stock");
        }

        // Determine requested quantity
        Integer requestedQty = req.getQuantity() == null ? 1 : req.getQuantity();
        requestedQty = Math.max(1, Math.min(requestedQty, MAX_QUANTITY_PER_ITEM));

        // Fetch or create user's cart
        Cart cart = cartRepository.findByUserId(req.getUserId()).orElseGet(() -> {
            Cart c = Cart.builder().userId(req.getUserId()).build();
            return cartRepository.save(c);
        });

        // Check if product already exists in cart
        var maybeItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), req.getProductId());
        if (maybeItem.isPresent()) {
            CartItem item = maybeItem.get();
            int newQty = Math.min(MAX_QUANTITY_PER_ITEM, item.getQuantity() + requestedQty);

            // Check if additional quantity can be fulfilled
            int extraNeeded = newQty - item.getQuantity();
            if (extraNeeded > 0 && (product.getStock() == null || product.getStock() < extraNeeded)) {
                int avail = product.getStock() == null ? 0 : product.getStock();
                throw new OutOfStockException("Not enough stock. Available: " + avail);
            }

            int delta = newQty - item.getQuantity();
            item.setQuantity(newQty);
            cartItemRepository.save(item);
            if (delta > 0) {
                decrementProductStock(req.getProductId(), delta);
            }
        } else {
            int stock = product.getStock() == null ? 0 : product.getStock();
            int qtyToAdd = Math.min(requestedQty, stock);
            if (qtyToAdd < 1) throw new OutOfStockException("Product is out of stock");

            Double effectivePrice =
                    product.getFinalPrice() != null && product.getFinalPrice() > 0
                            ? product.getFinalPrice()
                            : (product.getDiscountPrice() != null && product.getDiscountPrice() > 0
                            ? product.getDiscountPrice()
                            : product.getPrice());

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(req.getProductId())
                    .quantity(qtyToAdd)
                    .unitPrice(effectivePrice)
                    .build();

            cart.addItem(newItem);
            cartRepository.save(cart);
            if (qtyToAdd > 0) {
                decrementProductStock(req.getProductId(), qtyToAdd);
            }
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(cartRepository.findById(cart.getId()).orElse(cart));
    }

    // =========================
    // Update quantity of a cart item
    // =========================
    @Override
    public CartResponse updateQuantity(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 0)
            throw new BadRequestException("Quantity must be >= 0");

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        int currentQty = item.getQuantity();

        // Remove item if quantity is 0
        if (quantity == 0) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
            if (currentQty > 0) incrementProductStock(productId, currentQty);
            return toResponse(cart);
        }

        // Fetch product for validation
        CartProductDto product = fetchProduct(productId);
        if (product == null)
            throw new ResourceNotFoundException("Product not found: " + productId);
        if (Boolean.TRUE.equals(product.getBlocked()) || Boolean.TRUE.equals(product.getCategoryBlocked()))
            throw new BadRequestException("Product or category is blocked");
        if (quantity > MAX_QUANTITY_PER_ITEM)
            quantity = MAX_QUANTITY_PER_ITEM;

        int delta = quantity - currentQty;
        if (delta > 0) {
            if (product.getStock() == null || product.getStock() < delta) {
                throw new BadRequestException("Requested qty more than stock. Available: "
                        + (product.getStock() == null ? 0 : product.getStock()));
            }
            decrementProductStock(productId, delta);
        } else if (delta < 0) {
            incrementProductStock(productId, -delta);
        }

        item.setQuantity(quantity);

        Double effectivePrice =
                product.getFinalPrice() != null && product.getFinalPrice() > 0
                        ? product.getFinalPrice()
                        : (product.getDiscountPrice() != null && product.getDiscountPrice() > 0
                        ? product.getDiscountPrice()
                        : product.getPrice());

        item.setUnitPrice(effectivePrice);

        cartItemRepository.save(item);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(cart);
    }

    // =========================
    // Remove a single item from cart
    // =========================
    @Override
    public void removeItem(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        int qty = item.getQuantity();
        cart.removeItem(item);
        cartItemRepository.delete(item);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        if (qty > 0) incrementProductStock(productId, qty);
    }

    // =========================
    // Clear all items from cart
    // =========================
    @Override
    public void clearCart(Long userId, boolean orderComplete) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) return;

        List<CartItem> items = cart.getItems() == null
                ? Collections.emptyList()
                : new ArrayList<>((List<CartItem>)(List<?>) cart.getItems());

        for (CartItem i : items) {
            int qty = i.getQuantity() == null ? 0 : i.getQuantity();
            if (!orderComplete && qty > 0) incrementProductStock(i.getProductId(), qty);

            cart.removeItem(i);
            cartItemRepository.delete(i);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    public void clearCart(Long userId) {
        clearCart(userId, false);
    }

    // =========================
    // Remove multiple items from cart
    // =========================
    @Override
    public CartResponse removeItems(Long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return getCartForUser(userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        boolean changed = false;

        for (Long pid : productIds) {
            var maybeItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), pid);
            if (maybeItem.isPresent()) {
                CartItem item = maybeItem.get();
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                cart.removeItem(item);
                cartItemRepository.delete(item);
                changed = true;
                if (qty > 0) incrementProductStock(pid, qty);
            }
        }

        if (changed) {
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
        }

        return toResponse(cart);
    }

    // =========================
    // Helper methods for stock adjustment
    // =========================
    private void decrementProductStock(Long productId, int qty) {
        if (qty <= 0) return;
        try {
            String url = productServiceUrl.endsWith("/")
                    ? productServiceUrl + "api/products/" + productId + "/decrementStock?qty=" + qty
                    : productServiceUrl + "/api/products/" + productId + "/decrementStock?qty=" + qty;
            restTemplate.put(url, null);
        } catch (Exception ex) {
            log.error("decrementProductStock failed for product {}: {}", productId, ex.getMessage());
        }
    }

    private void incrementProductStock(Long productId, int qty) {
        if (qty <= 0) return;
        try {
            String url = productServiceUrl.endsWith("/")
                    ? productServiceUrl + "api/products/" + productId + "/incrementStock?qty=" + qty
                    : productServiceUrl + "/api/products/" + productId + "/incrementStock?qty=" + qty;
            restTemplate.put(url, null);
        } catch (Exception ex) {
            log.error("incrementProductStock failed for product {}: {}", productId, ex.getMessage());
        }
    }

    // =========================
    // Fetch product details from Product Service
    // =========================
    private CartProductDto fetchProduct(Long productId) {
        try {
            String publicUrl = productServiceUrl.endsWith("/")
                    ? productServiceUrl + "products/" + productId
                    : productServiceUrl + "/products/" + productId;
            return restTemplate.getForObject(publicUrl, CartProductDto.class);
        } catch (Exception e) {
            log.debug("public fetch failed for product {} : {}", productId, e.getMessage());
            try {
                String adminUrl = productServiceUrl.endsWith("/")
                        ? productServiceUrl + "api/admin/products/" + productId
                        : productServiceUrl + "/api/admin/products/" + productId;
                return restTemplate.getForObject(adminUrl, CartProductDto.class);
            } catch (Exception ex) {
                log.warn("fetchProduct failed for id {}: {}", productId, ex.getMessage());
                return null;
            }
        }
    }

    // =========================
    // Convert Cart entity to CartResponse DTO
    // =========================
    private CartResponse toResponse(Cart cart) {
        if (cart != null) {
            List<CartItem> items = cart.getItems() == null ? Collections.emptyList() : (List<CartItem>) (List<?>) cart.getItems();
            List<CartItem> toRemove = new ArrayList<>();

            // Remove unavailable items
            for (CartItem item : items) {
                try {
                    CartProductDto prod = fetchProduct(item.getProductId());
                    boolean unavailable = (prod == null)
                            || Boolean.TRUE.equals(prod.getBlocked())
                            || Boolean.TRUE.equals(prod.getCategoryBlocked())
                            || prod.getStock() == null
                            || prod.getStock() < 1;
                    if (unavailable) {
                        int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                        if (qty > 0) incrementProductStock(item.getProductId(), qty);
                        toRemove.add(item);
                    }
                } catch (Exception e) {
                    log.warn("toResponse: fetchProduct failed for {}: {}", item.getProductId(), e.getMessage());
                    toRemove.add(item);
                }
            }

            for (CartItem item : toRemove) {
                cart.removeItem(item);
                cartItemRepository.delete(item);
            }

            if (!toRemove.isEmpty()) {
                cart.setUpdatedAt(LocalDateTime.now());
                cartRepository.save(cart);
            }
        }

        List<CartItem> cartItems = cart == null ? Collections.emptyList()
                : (cart.getItems() == null ? Collections.emptyList() : (List<CartItem>) cart.getItems());

        List<CartItemResponse> items = cartItems.stream().map(i -> {
            String title = null;
            String imageUrl = null;
            Integer stock = null;
            Boolean blocked = null;
            Boolean categoryBlocked = null;
            boolean purchasable = true;
            int availableQty = 0;
            CartProductDto prod = null;

            try {
                prod = fetchProduct(i.getProductId());
                if (prod != null) {
                    title = prod.getTitle();
                    stock = prod.getStock();
                    blocked = prod.getBlocked();
                    categoryBlocked = prod.getCategoryBlocked();
                    if (stock != null) availableQty = Math.min(stock, MAX_QUANTITY_PER_ITEM);
                    purchasable = !(Boolean.TRUE.equals(blocked)
                            || Boolean.TRUE.equals(categoryBlocked)
                            || stock == null
                            || stock < 1);

                    if (prod.getImageNames() != null && !prod.getImageNames().isEmpty()) {
                        String base = productServiceUrl.endsWith("/") ? productServiceUrl.substring(0, productServiceUrl.length() - 1) : productServiceUrl;
                        String filename = prod.getImageNames().get(0);
                        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
                        imageUrl = base + "/public/products/images/" + encoded;
                    }
                }
            } catch (Exception e) {
                log.warn("toResponse: fetchProduct failed for {}: {}", i.getProductId(), e.getMessage());
            }

            if (imageUrl == null || imageUrl.isBlank()) imageUrl = "/img/default-product.png";

            Integer qty = i.getQuantity() == null ? 0 : i.getQuantity();

            Double basePrice = (prod != null && prod.getOriginalPrice() != null) ? prod.getOriginalPrice() : (prod != null ? prod.getPrice() : i.getUnitPrice());
            Double discountedPriceVal = (prod != null && prod.getFinalPrice() != null && prod.getFinalPrice() > 0) ? prod.getFinalPrice() :
                    (prod != null && prod.getDiscountPrice() != null && prod.getDiscountPrice() > 0 ? prod.getDiscountPrice() : basePrice);

            Double unitPrice = discountedPriceVal;
            Double lineTotal = unitPrice * qty;

            return CartItemResponse.builder()
                    .productId(i.getProductId())
                    .productTitle(title)
                    .productImage(imageUrl)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .total(lineTotal)
                    .stock(stock)
                    .blocked(blocked)
                    .categoryBlocked(categoryBlocked)
                    .availableQty(availableQty)
                    .purchasable(purchasable)
                    .originalPrice(basePrice)
                    .discountedPrice(discountedPriceVal)
                    .build();
        }).collect(Collectors.toList());

        double subtotal = items.stream().mapToDouble(it -> it.getTotal() == null ? 0.0 : it.getTotal()).sum();
        int totalItems = items.stream().mapToInt(it -> it.getQuantity() == null ? 0 : it.getQuantity()).sum();

        return CartResponse.builder()
                .userId(cart == null ? null : cart.getUserId())
                .subtotal(subtotal)
                .totalItems(totalItems)
                .items(items)
                .build();
    }

    // =========================
    // Helper: ensure absolute image URL
    // =========================
    private String ensureAbsoluteImageUrl(String image) {
        if (image == null) return null;
        String t = image.trim();
        if (t.isEmpty()) return null;
        if (t.startsWith("http://") || t.startsWith("https://")) return t;

        String[] bases = new String[]{ uploadsBaseUrl, productServiceBase, masterServiceBase };
        for (String base : bases) {
            if (base == null) continue;
            base = base.trim();
            if (base.isEmpty()) continue;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

            if (t.startsWith("/")) {
                return base + "/" + t.replaceAll("^/+", "");
            }
            if (t.toLowerCase().contains("uploads/")) {
                return base + "/" + t.replaceAll("^/+", "");
            }
            try {
                String encoded = URLEncoder.encode(t, StandardCharsets.UTF_8);
                return base + "/public/products/images/" + encoded;
            } catch (Exception e) {
                return base + "/public/products/images/" + t;
            }
        }

        return t;
    }
}
