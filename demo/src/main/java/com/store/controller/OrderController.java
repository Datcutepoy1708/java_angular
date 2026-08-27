package com.store.controller;

import com.store.dto.request.order.CreateOrderRequest;
import com.store.dto.request.order.OrderFilterRequest;
import com.store.dto.request.order.UpdateOrderStatusRequest;
import com.store.dto.request.order.UpdatePaymentStatusRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.order.OrderMetricsResponse;
import com.store.dto.response.order.OrderResponse;
import com.store.security.CustomUserDetails;
import com.store.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.store.security.GuestOrderRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order Placement & Lifecycle Management APIs")
public class OrderController {

    private final OrderService orderService;
    private final GuestOrderRateLimiter guestOrderRateLimiter;

    @PostMapping
    @Operation(summary = "Submit an order (supports both Member and Guest checkout)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest servletRequest) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        if (userId == null) {
            String clientIp = extractClientIp(servletRequest);
            guestOrderRateLimiter.checkRateLimit(clientIp, request.getReceiverPhone());
            OrderResponse response = orderService.createOrder(null, request);
            guestOrderRateLimiter.recordOrder(clientIp);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Đặt hàng thành công", response));
        } else {
            OrderResponse response = orderService.createOrder(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Đặt hàng thành công", response));
        }
    }

    @GetMapping("/track")
    @Operation(summary = "Public track guest order by order code and receiver phone number")
    public ResponseEntity<ApiResponse<OrderResponse>> trackGuestOrder(
            @RequestParam String code,
            @RequestParam String phone) {
        OrderResponse response = orderService.trackGuestOrder(code, phone);
        return ResponseEntity.ok(ApiResponse.success("Tra cứu thông tin đơn hàng thành công", response));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get list of orders placed by current user")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderResponse> response = orderService.getUserOrders(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", response));
    }

    @GetMapping("/{orderCode}")
    @Operation(summary = "Get order details and timeline by order code")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderCode) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        OrderResponse response = orderService.getOrderByCode(orderCode, userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn hàng thành công", response));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/{orderCode}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Customer cancels a pending order (releases reserved stock)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderCode,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        OrderResponse response = orderService.cancelOrderByCustomer(orderCode, userDetails.getUserId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", response));
    }

    // ==========================================
    // ADMIN ENDPOINTS
    // ==========================================

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Admin: Search and filter all orders with pagination")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAdminOrders(
            @ModelAttribute OrderFilterRequest filter) {
        Page<OrderResponse> response = orderService.getAdminOrders(filter);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng quản trị thành công", response));
    }

    @GetMapping("/admin/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Admin: Get order detail by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getAdminOrderById(
            @PathVariable Long orderId) {
        OrderResponse response = orderService.getAdminOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công", response));
    }

    @PutMapping("/admin/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Admin: Update order status (triggers stock deduction on COMPLETED or release on CANCELLED)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response = orderService.updateOrderStatusByAdmin(orderId, request, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", response));
    }

    @PutMapping("/admin/{orderId}/payment-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Admin: Update payment status (e.g. mark bank transfer as PAID)")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        OrderResponse response = orderService.updatePaymentStatusByAdmin(orderId, request, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thanh toán thành công", response));
    }

    @GetMapping("/admin/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Admin: Get overall order count and status metrics")
    public ResponseEntity<ApiResponse<OrderMetricsResponse>> getAdminMetrics() {
        OrderMetricsResponse response = orderService.getAdminMetrics();
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu thống kê đơn hàng thành công", response));
    }
}
