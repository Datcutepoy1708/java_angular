package com.store.service.impl;

import com.store.dto.request.order.CreateOrderRequest;
import com.store.dto.request.order.OrderFilterRequest;
import com.store.dto.request.order.UpdateOrderStatusRequest;
import com.store.dto.request.order.UpdatePaymentStatusRequest;
import com.store.dto.response.order.OrderItemResponse;
import com.store.dto.response.order.OrderMetricsResponse;
import com.store.dto.response.order.OrderResponse;
import com.store.dto.response.order.OrderStatusHistoryResponse;
import com.store.entity.cart.CartItem;
import com.store.entity.inventory.Inventory;
import com.store.entity.order.Address;
import com.store.entity.order.Order;
import com.store.entity.order.OrderItem;
import com.store.entity.order.OrderStatus;
import com.store.entity.order.OrderStatusHistory;
import com.store.entity.order.PaymentMethod;
import com.store.entity.order.PaymentStatus;
import com.store.entity.product.Product;
import com.store.entity.product.ProductImage;
import com.store.entity.product.ProductStatus;
import com.store.entity.product.ProductVariant;
import com.store.entity.user.User;
import com.store.dto.response.discount.DiscountValidationResult;
import com.store.entity.discount.DiscountCode;
import com.store.entity.discount.DiscountUsage;
import com.store.exception.InsufficientStockException;
import com.store.exception.InvalidDiscountException;
import com.store.exception.ResourceNotFoundException;
import com.store.dto.request.order.GuestOrderItemRequest;
import com.store.repository.AddressRepository;
import com.store.repository.CartItemRepository;
import com.store.repository.DiscountCodeRepository;
import com.store.repository.DiscountUsageRepository;
import com.store.repository.InventoryRepository;
import com.store.repository.OrderItemRepository;
import com.store.repository.OrderRepository;
import com.store.repository.OrderStatusHistoryRepository;
import com.store.repository.ProductImageRepository;
import com.store.repository.ProductVariantRepository;
import com.store.repository.UserRepository;
import com.store.service.DiscountService;
import com.store.service.OrderService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final DiscountService discountService;
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountUsageRepository discountUsageRepository;
    private final com.store.util.PaymentSecurityUtil paymentSecurityUtil;
    private final com.store.config.PaymentProperties paymentProperties;

    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new SecureRandom();

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("Creating order (userId={})", userId);

        User user = null;
        List<CartItem> cartItems;

        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            cartItems = cartItemRepository.findByUserIdWithDetails(userId);
            if (cartItems == null || cartItems.isEmpty()) {
                throw new IllegalStateException("Giỏ hàng của bạn đang trống, không thể tiến hành đặt hàng");
            }
        } else {
            // Guest Checkout validation
            if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
                throw new InvalidDiscountException("Mã giảm giá chỉ dành riêng cho thành viên đã đăng nhập tài khoản. Vui lòng đăng nhập để áp dụng voucher.");
            }
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new IllegalStateException("Giỏ hàng của bạn đang trống, không thể tiến hành đặt hàng");
            }

            cartItems = new ArrayList<>();
            for (GuestOrderItemRequest itemReq : request.getItems()) {
                if (itemReq.getVariantId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                    continue;
                }
                ProductVariant variant = productVariantRepository.findById(itemReq.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm với id: " + itemReq.getVariantId()));

                CartItem transientItem = CartItem.builder()
                        .variant(variant)
                        .quantity(itemReq.getQuantity())
                        .build();
                cartItems.add(transientItem);
            }

            if (cartItems.isEmpty()) {
                throw new IllegalStateException("Giỏ hàng của bạn đang trống hoặc không hợp lệ");
            }
        }

        // Step 1: Read-only Fail-fast pre-validation for Discount Code (if provided by authenticated user)
        BigDecimal tentativeSubtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            tentativeSubtotal = tentativeSubtotal.add(item.getVariant().getEffectivePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        DiscountCode appliedDiscount = null;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            DiscountValidationResult preVal = discountService.validateAndCalculate(request.getDiscountCode(), userId, tentativeSubtotal, cartItems);
            appliedDiscount = discountCodeRepository.findById(preVal.getDiscountId()).orElse(null);
        }

        // 1. Resolve receiver info
        Address selectedAddress = null;
        String receiverName;
        String receiverPhone;
        String shippingAddress;

        if (userId != null && request.getAddressId() != null) {
            selectedAddress = addressRepository.findByAddressIdAndUserUserId(request.getAddressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));
            receiverName = selectedAddress.getReceiverName();
            receiverPhone = selectedAddress.getPhone();
            shippingAddress = selectedAddress.getFullAddress();
        } else {
            receiverName = request.getReceiverName();
            receiverPhone = request.getReceiverPhone();
            if (request.getShippingAddress() != null && !request.getShippingAddress().isBlank()) {
                shippingAddress = request.getShippingAddress();
            } else {
                StringBuilder sb = new StringBuilder();
                if (request.getDetailAddress() != null) sb.append(request.getDetailAddress());
                if (request.getWard() != null) { if (!sb.isEmpty()) sb.append(", "); sb.append(request.getWard()); }
                if (request.getDistrict() != null) { if (!sb.isEmpty()) sb.append(", "); sb.append(request.getDistrict()); }
                if (request.getProvince() != null) { if (!sb.isEmpty()) sb.append(", "); sb.append(request.getProvince()); }
                shippingAddress = sb.toString();
            }
        }

        if (receiverName == null || receiverName.isBlank()) {
            receiverName = user != null ? user.getFullName() : null;
        }
        if (receiverPhone == null || receiverPhone.isBlank()) {
            receiverPhone = user != null ? user.getPhone() : null;
        }
        if (receiverName == null || receiverName.isBlank()) {
            throw new IllegalArgumentException("Họ tên người nhận không được để trống");
        }
        if (receiverPhone == null || receiverPhone.isBlank()) {
            throw new IllegalArgumentException("Số điện thoại nhận hàng không được để trống");
        }
        if (shippingAddress == null || shippingAddress.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không được để trống");
        }

        PaymentMethod paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.COD;

        // Step 2: Deadlock Prevention: Sort items deterministically by variantId ASC
        List<CartItem> sortedItems = new ArrayList<>(cartItems);
        sortedItems.sort(Comparator.comparing(item -> item.getVariant().getVariantId()));

        // Step 3: Single-Warehouse Allocation & Atomic Stock Reservation Loop
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItemsToSave = new ArrayList<>();

        for (CartItem cartItem : sortedItems) {
            ProductVariant variant = cartItem.getVariant();
            Product product = variant.getProduct();

            if (variant.getDeletedAt() != null || product.getDeletedAt() != null || product.getStatus() != ProductStatus.ACTIVE) {
                throw new IllegalStateException("Sản phẩm '" + product.getName() + "' không còn kinh doanh hoặc đã bị xóa");
            }

            int requestedQty = cartItem.getQuantity();
            if (requestedQty <= 0) {
                continue;
            }

            // Find first warehouse with sufficient available stock (ordered by warehouse_id ASC)
            List<Inventory> availableWarehouses = inventoryRepository.findWarehousesWithAvailableStock(variant.getVariantId(), requestedQty);
            if (availableWarehouses.isEmpty()) {
                throw new InsufficientStockException("Sản phẩm '" + product.getName() + " (" + variant.getVariantName() +
                        ")' không đủ tồn kho tại một chi nhánh duy nhất để đáp ứng số lượng yêu cầu (" + requestedQty + ")");
            }

            Inventory allocatedInventory = availableWarehouses.get(0);
            Integer allocatedWarehouseId = allocatedInventory.getWarehouse().getWarehouseId();

            // Atomic conditional reservation in MySQL
            int reservedRows = inventoryRepository.reserveStockAtomic(variant.getVariantId(), allocatedWarehouseId, requestedQty);
            if (reservedRows == 0) {
                throw new InsufficientStockException("Sản phẩm '" + product.getName() + " (" + variant.getVariantName() +
                        ")' vừa hết tồn kho khả dụng tại chi nhánh");
            }

            BigDecimal itemPrice = variant.getEffectivePrice();
            BigDecimal itemSubtotal = itemPrice.multiply(BigDecimal.valueOf(requestedQty));
            subtotal = subtotal.add(itemSubtotal);

            String productNameSnapshot = product.getName() + (variant.getVariantName() != null && !variant.getVariantName().isBlank() ? " - " + variant.getVariantName() : "");

            OrderItem orderItem = OrderItem.builder()
                    .variant(variant)
                    .warehouse(allocatedInventory.getWarehouse())
                    .productNameSnapshot(productNameSnapshot)
                    .priceSnapshot(itemPrice)
                    .quantity(requestedQty)
                    .subtotal(itemSubtotal)
                    .build();

            orderItemsToSave.add(orderItem);
        }

        // Step 4: Atomic Discount Calculation & Increment
        BigDecimal discountAmount = BigDecimal.ZERO;
        Long discountId = null;

        if (appliedDiscount != null && userId != null) {
            DiscountValidationResult discountResult = discountService.validateAndCalculate(appliedDiscount.getCode(), userId, subtotal, cartItems);
            discountAmount = discountResult.getDiscountAmount();
            discountId = appliedDiscount.getDiscountId();

            LocalDateTime now = LocalDateTime.now();
            int updatedRows = discountService.incrementUsedCountAtomic(discountId, now);
            if (updatedRows == 0) {
                throw new InvalidDiscountException("Mã giảm giá '" + appliedDiscount.getCode() + "' vừa hết lượt sử dụng.");
            }
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);

        // Generate unique order code
        String orderCode = generateUniqueOrderCode();

        // Bank transfer payment reference & polling token generation
        String paymentReference = null;
        String rawPollingToken = null;
        String pollingTokenHash = null;
        LocalDateTime pollingExpiresAt = null;

        if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
            for (int i = 0; i < 10; i++) {
                String candidate = paymentSecurityUtil.generatePaymentReference();
                if (!orderRepository.existsByPaymentReference(candidate)) {
                    paymentReference = candidate;
                    break;
                }
            }
            if (paymentReference == null) {
                paymentReference = paymentSecurityUtil.generatePaymentReference();
            }
            rawPollingToken = paymentSecurityUtil.generateRawPollingToken();
            pollingTokenHash = paymentSecurityUtil.sha256Hex(rawPollingToken);
            pollingExpiresAt = LocalDateTime.now().plusMinutes(30);
        }

        // Step 5: Create and save Order
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .customerEmail(request.getCustomerEmail())
                .address(selectedAddress)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .shippingAddress(shippingAddress)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .discountId(discountId)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentReference(paymentReference)
                .paymentPollingTokenHash(pollingTokenHash)
                .paymentPollingExpiresAt(pollingExpiresAt)
                .paidAmount(BigDecimal.ZERO)
                .reconciliationStatus(com.store.entity.order.ReconciliationStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .note(request.getNote())
                .build();

        for (OrderItem item : orderItemsToSave) {
            order.addItem(item);
        }

        OrderStatusHistory initialHistory = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PENDING.getValue())
                .note(user != null ? "Đơn hàng được khởi tạo thành công" : "Đơn hàng được đặt bởi khách vãng lai (Guest)")
                .changedBy(user)
                .build();
        order.addStatusHistory(initialHistory);

        Order savedOrder = orderRepository.save(order);
        log.info("Successfully created order {} (id: {}) for user {}", orderCode, savedOrder.getOrderId(), userId);

        // Step 6: Save DiscountUsage record (if discount was applied by user)
        if (appliedDiscount != null && user != null) {
            discountUsageRepository.save(DiscountUsage.builder()
                    .discount(appliedDiscount)
                    .user(user)
                    .order(savedOrder)
                    .build());
        }

        // Step 7: Clean up user's cart (only for authenticated user)
        if (userId != null) {
            cartItemRepository.deleteByUserUserId(userId);
        }

        OrderResponse response = mapOrderToResponse(savedOrder);
        if (rawPollingToken != null && response.getPaymentInstruction() != null) {
            response.getPaymentInstruction().setPaymentPollingToken(rawPollingToken);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapOrderToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode, Long userId) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with code: " + orderCode));

        // This endpoint is exclusively for authenticated customers viewing their own orders.
        // Guest orders must be retrieved through trackGuestOrder, which also verifies the phone number.
        if (userId == null || order.getUser() == null || !order.getUser().getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this order");
        }
        return mapOrderToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse trackGuestOrder(String orderCode, String receiverPhone) {
        if (orderCode == null || orderCode.isBlank() || receiverPhone == null || receiverPhone.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp cả Mã đơn hàng và Số điện thoại nhận hàng");
        }
        Order order = orderRepository.findByOrderCodeAndReceiverPhone(orderCode.trim(), receiverPhone.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng khớp với mã đơn và số điện thoại đã cung cấp"));
        return mapOrderToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrderByCustomer(String orderCode, Long userId, String reason) {
        log.info("Customer {} requesting cancellation for order {}", userId, orderCode);
        Order order = orderRepository.findByOrderCodeAndUserUserId(orderCode, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with code: " + orderCode));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể tự hủy đơn hàng khi đang ở trạng thái Chờ xác nhận (PENDING). Vui lòng liên hệ CSKH để được hỗ trợ.");
        }

        // Release reserved stock from each item's allocated warehouse
        releaseOrderStock(order);

        order.setOrderStatus(OrderStatus.CANCELLED);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED.getValue())
                .note("Khách hàng tự hủy đơn" + (reason != null && !reason.isBlank() ? ": " + reason : ""))
                .changedBy(order.getUser())
                .build();
        order.addStatusHistory(history);

        Order updated = orderRepository.save(order);
        log.info("Order {} successfully cancelled by customer {}", orderCode, userId);
        return mapOrderToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAdminOrders(OrderFilterRequest filter) {
        Pageable pageable = createPageable(filter);

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("orderStatus"), filter.getStatus()));
            }
            if (filter.getPaymentStatus() != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), filter.getPaymentStatus()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String pattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                Predicate codePred = cb.like(cb.lower(root.get("orderCode")), pattern);
                Predicate namePred = cb.like(cb.lower(root.get("receiverName")), pattern);
                Predicate phonePred = cb.like(cb.lower(root.get("receiverPhone")), pattern);
                predicates.add(cb.or(codePred, namePred, phonePred));
            }
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate().atStartOfDay()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable).map(this::mapOrderToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getAdminOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return mapOrderToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatusByAdmin(Long orderId, UpdateOrderStatusRequest request, Long adminUserId) {
        log.info("Admin {} updating order {} status to {}", adminUserId, orderId, request.getStatus());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        User adminUser = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;
        OrderStatus oldStatus = order.getOrderStatus();
        OrderStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return mapOrderToResponse(order);
        }

        if (oldStatus == OrderStatus.COMPLETED || oldStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Không thể thay đổi trạng thái của đơn hàng đã Hoàn tất hoặc đã Hủy");
        }

        // Action upon transitioning to COMPLETED
        if (newStatus == OrderStatus.COMPLETED) {
            for (OrderItem item : order.getItems()) {
                if (item.getWarehouse() != null) {
                    inventoryRepository.deductCompletedStockAtomic(
                            item.getVariant().getVariantId(),
                            item.getWarehouse().getWarehouseId(),
                            item.getQuantity()
                    );
                }
            }
            if (order.getPaymentMethod() == PaymentMethod.COD && order.getPaymentStatus() == PaymentStatus.UNPAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        }

        // Action upon transitioning to CANCELLED
        if (newStatus == OrderStatus.CANCELLED) {
            releaseOrderStock(order);
        }

        order.setOrderStatus(newStatus);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus.getValue())
                .note(request.getNote() != null && !request.getNote().isBlank() ? request.getNote() : "Chuyển trạng thái sang " + newStatus.getValue())
                .changedBy(adminUser)
                .build();
        order.addStatusHistory(history);

        Order saved = orderRepository.save(order);
        return mapOrderToResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatusByAdmin(Long orderId, UpdatePaymentStatusRequest request, Long adminUserId) {
        log.info("Admin {} updating order {} payment status to {}", adminUserId, orderId, request.getPaymentStatus());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        User adminUser = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;
        PaymentStatus newPaymentStatus = request.getPaymentStatus();

        order.setPaymentStatus(newPaymentStatus);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(order.getOrderStatus().getValue())
                .note("Cập nhật thanh toán sang " + newPaymentStatus.getValue() + (request.getNote() != null && !request.getNote().isBlank() ? ": " + request.getNote() : ""))
                .changedBy(adminUser)
                .build();
        order.addStatusHistory(history);

        Order saved = orderRepository.save(order);
        return mapOrderToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderMetricsResponse getAdminMetrics() {
        long totalOrders = orderRepository.count();
        long pending = orderRepository.countByOrderStatus(OrderStatus.PENDING);
        long confirmed = orderRepository.countByOrderStatus(OrderStatus.CONFIRMED);
        long processing = orderRepository.countByOrderStatus(OrderStatus.PROCESSING);
        long shipping = orderRepository.countByOrderStatus(OrderStatus.SHIPPING);
        long completed = orderRepository.countByOrderStatus(OrderStatus.COMPLETED);
        long cancelled = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);
        long unpaid = orderRepository.countByPaymentStatus(PaymentStatus.UNPAID);
        long paid = orderRepository.countByPaymentStatus(PaymentStatus.PAID);
        BigDecimal revenue = orderRepository.sumTotalRevenue();

        return OrderMetricsResponse.builder()
                .totalOrders(totalOrders)
                .pendingCount(pending)
                .confirmedCount(confirmed)
                .processingCount(processing)
                .shippingCount(shipping)
                .completedCount(completed)
                .cancelledCount(cancelled)
                .unpaidCount(unpaid)
                .paidCount(paid)
                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .build();
    }

    @Override
    @Transactional
    public int processExpiredPendingOrders() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<Order> expiredOrders = orderRepository.findByOrderStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoffTime);

        if (expiredOrders.isEmpty()) {
            return 0;
        }

        log.info("Found {} expired PENDING orders to auto-cancel", expiredOrders.size());
        int count = 0;

        for (Order order : expiredOrders) {
            try {
                releaseOrderStock(order);
                order.setOrderStatus(OrderStatus.CANCELLED);

                OrderStatusHistory history = OrderStatusHistory.builder()
                        .order(order)
                        .status(OrderStatus.CANCELLED.getValue())
                        .note("Hệ thống tự động hủy đơn do quá hạn thanh toán/xác nhận (24 giờ)")
                        .changedBy(null)
                        .build();
                order.addStatusHistory(history);

                orderRepository.save(order);
                count++;
                log.info("Auto-cancelled expired order {}", order.getOrderCode());
            } catch (Exception e) {
                log.error("Failed to auto-cancel expired order {}: {}", order.getOrderCode(), e.getMessage(), e);
            }
        }

        return count;
    }

    private void releaseOrderStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getWarehouse() != null) {
                inventoryRepository.releaseStockAtomic(
                        item.getVariant().getVariantId(),
                        item.getWarehouse().getWarehouseId(),
                        item.getQuantity()
                );
            }
        }
        if (order.getDiscountId() != null) {
            discountService.rollbackDiscountUsage(order.getDiscountId(), order.getOrderId());
        }
    }

    private String generateUniqueOrderCode() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        for (int i = 0; i < 10; i++) {
            String timestamp = LocalDateTime.now().format(formatter);
            String randomChars = generateRandomAlphanumeric(4);
            String code = "ORD-" + timestamp + "-" + randomChars;
            if (!orderRepository.existsByOrderCode(code)) {
                return code;
            }
        }
        return "ORD-" + System.currentTimeMillis() + "-" + generateRandomAlphanumeric(6);
    }

    private String generateRandomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    private OrderResponse mapOrderToResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String imageUrl = null;
                if (item.getVariant() != null) {
                    List<ProductImage> variantImages = productImageRepository.findByVariant_VariantIdOrderBySortOrderAscImageIdAsc(item.getVariant().getVariantId());
                    if (!variantImages.isEmpty()) {
                        imageUrl = variantImages.get(0).getImageUrl();
                    } else if (item.getVariant().getProduct() != null) {
                        List<ProductImage> prodImages = productImageRepository.findByProduct_ProductIdAndDeletedAtIsNullOrderBySortOrderAscImageIdAsc(item.getVariant().getProduct().getProductId());
                        if (!prodImages.isEmpty()) {
                            imageUrl = prodImages.get(0).getImageUrl();
                        }
                    }
                }
                itemResponses.add(OrderItemResponse.fromEntity(item, imageUrl));
            }
        }

        List<OrderStatusHistoryResponse> historyResponses = new ArrayList<>();
        if (order.getStatusHistory() != null) {
            for (OrderStatusHistory history : order.getStatusHistory()) {
                historyResponses.add(OrderStatusHistoryResponse.fromEntity(history));
            }
        }

        String discountCode = null;
        if (order.getDiscountId() != null) {
            discountCode = discountCodeRepository.findById(order.getDiscountId())
                    .map(DiscountCode::getCode)
                    .orElse(null);
        }

        OrderResponse response = OrderResponse.fromEntity(order, itemResponses, historyResponses, discountCode);
        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && order.getPaymentReference() != null) {
            String bankId = paymentProperties.getBank().getId();
            String accountNo = paymentProperties.getBank().getAccountNo();
            String accountName = paymentProperties.getBank().getAccountName();
            BigDecimal amount = order.getTotalAmount();
            String encodedRef = java.net.URLEncoder.encode(order.getPaymentReference(), java.nio.charset.StandardCharsets.UTF_8);
            String encodedName = java.net.URLEncoder.encode(accountName != null ? accountName : "", java.nio.charset.StandardCharsets.UTF_8);
            String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                    bankId != null ? bankId : "MB",
                    accountNo != null ? accountNo : "",
                    amount != null ? amount.toPlainString() : "0",
                    encodedRef,
                    encodedName);

            com.store.dto.payment.PaymentInstructionResponse instruction = com.store.dto.payment.PaymentInstructionResponse.builder()
                    .paymentReference(order.getPaymentReference())
                    .bankId(bankId)
                    .bankAccountNo(accountNo)
                    .bankAccountName(accountName)
                    .totalAmount(amount)
                    .qrCodeUrl(qrUrl)
                    .build();
            response.setPaymentInstruction(instruction);
        }
        return response;
    }

    private Pageable createPageable(OrderFilterRequest filter) {
        int page = Math.max(0, filter.getPage());
        int size = filter.getSize() > 0 ? Math.min(100, filter.getSize()) : 10;
        String sortBy = filter.getSortBy() != null && !filter.getSortBy().isBlank() ? filter.getSortBy() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
