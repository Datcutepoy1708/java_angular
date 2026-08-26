package com.store.service.impl;

import com.store.audit.annotation.Auditable;
import com.store.dto.returnrefund.*;
import com.store.entity.order.Order;
import com.store.entity.order.OrderItem;
import com.store.entity.order.OrderStatus;
import com.store.entity.returnrefund.*;
import com.store.entity.user.User;
import com.store.exception.BadRequestException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.*;
import com.store.service.InventoryService;
import com.store.service.ReturnService;
import com.store.service.SettingService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestItemRepository returnRequestItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final InventoryService inventoryService;
    private final SettingService settingService;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    @Auditable(module = "RETURN_REFUND", actionType = "CREATE", description = "Khách hàng tạo yêu cầu đổi trả")
    public ReturnDetailResponse createReturnRequest(Long userId, ReturnCreateRequest request) {
        log.info("Creating return request for user ID {} and order ID {}", userId, request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền yêu cầu đổi trả cho đơn hàng của người khác");
        }

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể yêu cầu đổi trả đối với đơn hàng đã hoàn tất giao hàng (COMPLETED)");
        }

        // Return Window Check from System Settings (default 14 days)
        int allowedDays = settingService.getReturnWindowDays();
        LocalDateTime deliveredTime = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
        long daysPassed = ChronoUnit.DAYS.between(deliveredTime, LocalDateTime.now());
        if (daysPassed > allowedDays) {
            throw new BadRequestException("Đơn hàng đã vượt quá thời hạn cho phép đổi trả (" + allowedDays + " ngày)");
        }

        // Check if there is an active return request already
        boolean activeExists = returnRequestRepository.existsByOrderOrderIdAndStatusNotIn(
                order.getOrderId(),
                List.of(ReturnStatus.REJECTED, ReturnStatus.CANCELLED)
        );
        if (activeExists) {
            throw new BadRequestException("Đơn hàng này đã có một yêu cầu đổi trả đang được xử lý");
        }

        ReturnReason reason = ReturnReason.fromValue(request.getReturnReason());
        String returnCode = generateReturnCode();

        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnCode(returnCode)
                .order(order)
                .user(order.getUser())
                .status(ReturnStatus.REQUESTED)
                .returnReason(reason)
                .customerNote(request.getCustomerNote())
                .bankName(request.getBankName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankAccountName(request.getBankAccountName())
                .build();

        BigDecimal totalRefund = BigDecimal.ZERO;
        List<ReturnRequestItem> items = new ArrayList<>();

        Map<Long, OrderItem> orderItemMap = new HashMap<>();
        for (OrderItem oi : order.getItems()) {
            orderItemMap.put(oi.getOrderItemId(), oi);
        }

        for (ReturnCreateRequest.ReturnItemRequest itemReq : request.getItems()) {
            OrderItem oi = orderItemMap.get(itemReq.getOrderItemId());
            if (oi == null) {
                throw new BadRequestException("Mặt hàng không thuộc đơn hàng này: ID " + itemReq.getOrderItemId());
            }

            if (itemReq.getQuantity() <= 0 || itemReq.getQuantity() > oi.getQuantity()) {
                throw new BadRequestException("Số lượng đổi trả không hợp lệ cho sản phẩm: " + oi.getProductNameSnapshot());
            }

            BigDecimal itemPrice = oi.getPriceSnapshot() != null ? oi.getPriceSnapshot() : BigDecimal.ZERO;
            BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalRefund = totalRefund.add(lineTotal);

            ItemCondition condition = ItemCondition.fromValue(itemReq.getItemCondition());

            ReturnRequestItem returnItem = ReturnRequestItem.builder()
                    .returnRequest(returnRequest)
                    .orderItem(oi)
                    .variant(oi.getVariant())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemPrice)
                    .itemCondition(condition)
                    .build();

            items.add(returnItem);
        }

        returnRequest.setRefundAmount(totalRefund);
        returnRequest.setItems(items);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<ReturnRequestImage> images = new ArrayList<>();
            for (String url : request.getImageUrls()) {
                images.add(ReturnRequestImage.builder()
                        .returnRequest(returnRequest)
                        .imageUrl(url.trim())
                        .build());
            }
            returnRequest.setImages(images);
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        log.info("Return request created successfully: {} with amount {}", saved.getReturnCode(), totalRefund);
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnDetailResponse> getAdminReturnRequests(ReturnFilterRequest request) {
        log.info("Fetching admin return requests with filter: status={}, reason={}, keyword={}, page={}",
                request.getStatus(), request.getReason(), request.getKeyword(), request.getPage());

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "requestedAt";
        Pageable pageable = PageRequest.of(Math.max(0, request.getPage()), Math.max(1, request.getSize()), Sort.by(direction, sortBy));

        Specification<ReturnRequest> spec = buildAdminSpecification(request);
        Page<ReturnRequest> page = returnRequestRepository.findAll(spec, pageable);

        return page.map(this::mapToDetailResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnDetailResponse> getCustomerReturnRequests(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "requestedAt"));
        return returnRequestRepository.findByUserUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDetailResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnDetailResponse getReturnRequestById(Long returnId, Long currentUserId, boolean isAdmin) {
        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đổi trả ID: " + returnId));

        if (!isAdmin && !req.getUser().getUserId().equals(currentUserId)) {
            throw new BadRequestException("Bạn không có quyền xem yêu cầu đổi trả này");
        }

        return mapToDetailResponse(req);
    }

    @Override
    @Transactional
    @Auditable(module = "RETURN_REFUND", actionType = "STATUS_CHANGE", description = "Admin duyệt hoặc từ chối yêu cầu đổi trả")
    public ReturnDetailResponse reviewReturnRequest(Long returnId, Long adminId, ReturnReviewRequest request) {
        log.info("Admin ID {} reviewing return request ID {}: approved={}", adminId, returnId, request.getApproved());

        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đổi trả ID: " + returnId));

        if (req.getStatus() != ReturnStatus.REQUESTED) {
            throw new BadRequestException("Chỉ có thể phê duyệt yêu cầu đang ở trạng thái Chờ xử lý (REQUESTED)");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            req.setStatus(ReturnStatus.APPROVED);
            req.setApprovedAt(LocalDateTime.now());
            req.setAdminNote(request.getAdminNote());
        } else {
            req.setStatus(ReturnStatus.REJECTED);
            req.setAdminNote(request.getAdminNote());
        }

        ReturnRequest saved = returnRequestRepository.save(req);
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(module = "RETURN_REFUND", actionType = "STATUS_CHANGE", description = "Tiếp nhận hàng hoàn tại kho")
    public ReturnDetailResponse receiveReturnedItems(Long returnId, Long adminId, ReturnReceiveItemRequest request) {
        log.info("Receiving returned items for return ID {} at warehouse ID {}", returnId, request.getWarehouseId());

        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đổi trả ID: " + returnId));

        if (req.getStatus() != ReturnStatus.APPROVED) {
            throw new BadRequestException("Yêu cầu đổi trả phải ở trạng thái Đã duyệt (APPROVED) trước khi tiếp nhận hàng");
        }

        if (!warehouseRepository.existsById(request.getWarehouseId())) {
            throw new ResourceNotFoundException("Kho tiếp nhận không tồn tại: ID " + request.getWarehouseId());
        }

        req.setStatus(ReturnStatus.ITEM_RECEIVED);
        req.setReceivedAt(LocalDateTime.now());
        req.setRestockWarehouseId(request.getWarehouseId());
        if (request.getAdminNote() != null && !request.getAdminNote().isBlank()) {
            req.setAdminNote((req.getAdminNote() != null ? req.getAdminNote() + " | " : "") + request.getAdminNote().trim());
        }

        if (request.getItemConditions() != null) {
            Map<Long, String> conditionMap = new HashMap<>();
            for (ReturnReceiveItemRequest.ItemConditionUpdate ic : request.getItemConditions()) {
                conditionMap.put(ic.getReturnItemId(), ic.getCondition());
            }

            for (ReturnRequestItem item : req.getItems()) {
                if (conditionMap.containsKey(item.getId())) {
                    item.setItemCondition(ItemCondition.fromValue(conditionMap.get(item.getId())));
                }
            }
        }

        ReturnRequest saved = returnRequestRepository.save(req);
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(module = "RETURN_REFUND", actionType = "REFUND", description = "Hoàn tiền và hoàn nhập kho atomic")
    public ReturnDetailResponse processRefund(Long returnId, Long adminId, ReturnProcessRefundRequest request) {
        log.info("Processing refund for return ID {} by admin ID {}", returnId, adminId);

        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đổi trả ID: " + returnId));

        if (req.getStatus() != ReturnStatus.ITEM_RECEIVED && req.getStatus() != ReturnStatus.APPROVED) {
            throw new BadRequestException("Yêu cầu đổi trả phải được duyệt hoặc đã nhận hàng tại kho trước khi hoàn tiền");
        }

        Integer warehouseId = req.getRestockWarehouseId();
        if (warehouseId == null) {
            // Default to warehouse 1 if not specified
            warehouseId = 1;
            req.setRestockWarehouseId(warehouseId);
        }

        // 1. Atomic Restock per returned item based on item_condition
        for (ReturnRequestItem item : req.getItems()) {
            inventoryService.restockReturnedItemAtomic(
                    item.getVariant().getVariantId(),
                    warehouseId,
                    item.getQuantity(),
                    item.getItemCondition().name(),
                    returnId,
                    req.getReturnCode(),
                    adminId
            );
        }

        // 2. Discount Usage Count Handling (Full Return vs Partial Return)
        Order order = req.getOrder();
        boolean isFullReturn = isFullOrderReturn(order, req);
        if (isFullReturn) {
            log.info("Full order return detected for RMA {}. Restoring discount usage and cancelling order #{}",
                    req.getReturnCode(), order.getOrderId());
            if (order.getDiscountId() != null) {
                discountCodeRepository.decrementUsedCountAtomic(order.getDiscountId());
                log.info("Restored 1 discount usage count for discount code ID {}", order.getDiscountId());
            }
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }

        // 3. Update return request status
        req.setStatus(ReturnStatus.REFUNDED);
        req.setRefundedAt(LocalDateTime.now());
        req.setRefundTransactionCode(request.getRefundTransactionCode().trim());
        if (request.getAdminNote() != null && !request.getAdminNote().isBlank()) {
            req.setAdminNote((req.getAdminNote() != null ? req.getAdminNote() + " | " : "") + request.getAdminNote().trim());
        }

        ReturnRequest saved = returnRequestRepository.save(req);
        log.info("Refund successfully processed for RMA {}. Amount: {}, Tx: {}",
                saved.getReturnCode(), saved.getRefundAmount(), saved.getRefundTransactionCode());

        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(module = "RETURN_REFUND", actionType = "STATUS_CHANGE", description = "Khách hàng hủy yêu cầu đổi trả")
    public ReturnDetailResponse cancelReturnRequest(Long returnId, Long userId) {
        ReturnRequest req = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đổi trả ID: " + returnId));

        if (!req.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy yêu cầu đổi trả này");
        }

        if (req.getStatus() != ReturnStatus.REQUESTED) {
            throw new BadRequestException("Chỉ có thể hủy yêu cầu đang ở trạng thái Chờ xử lý (REQUESTED)");
        }

        req.setStatus(ReturnStatus.CANCELLED);
        ReturnRequest saved = returnRequestRepository.save(req);
        return mapToDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnMetricsResponse getReturnMetrics() {
        long total = returnRequestRepository.count();
        long pending = returnRequestRepository.countByStatus(ReturnStatus.REQUESTED);
        long awaiting = returnRequestRepository.countByStatus(ReturnStatus.APPROVED);
        long refunded = returnRequestRepository.countByStatus(ReturnStatus.REFUNDED);
        long rejected = returnRequestRepository.countByStatus(ReturnStatus.REJECTED);
        BigDecimal totalRefundAmount = returnRequestRepository.sumRefundAmountByStatus(ReturnStatus.REFUNDED);

        return ReturnMetricsResponse.builder()
                .totalRequests(total)
                .pendingReviewCount(pending)
                .awaitingItemCount(awaiting)
                .refundedCount(refunded)
                .rejectedCount(rejected)
                .totalRefundedAmount(totalRefundAmount != null ? totalRefundAmount : BigDecimal.ZERO)
                .build();
    }

    private boolean isFullOrderReturn(Order order, ReturnRequest returnRequest) {
        int totalOrderQuantity = 0;
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                totalOrderQuantity += oi.getQuantity();
            }
        }

        int totalReturnedQuantity = 0;
        if (returnRequest.getItems() != null) {
            for (ReturnRequestItem rri : returnRequest.getItems()) {
                totalReturnedQuantity += rri.getQuantity();
            }
        }

        return totalOrderQuantity > 0 && totalReturnedQuantity >= totalOrderQuantity;
    }

    private String generateReturnCode() {
        String datePrefix = "RET-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long countToday = returnRequestRepository.countByReturnCodePrefix(datePrefix);
        return datePrefix + String.format("%04d", countToday + 1);
    }

    private Specification<ReturnRequest> buildAdminSpecification(ReturnFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String term = "%" + request.getKeyword().trim().toLowerCase() + "%";
                var userJoin = root.join("user");
                var orderJoin = root.join("order");

                Predicate pCode = cb.like(cb.lower(root.get("returnCode")), term);
                Predicate pEmail = cb.like(cb.lower(userJoin.get("email")), term);
                Predicate pName = cb.like(cb.lower(userJoin.get("fullName")), term);
                Predicate pOrderCode = cb.like(cb.lower(orderJoin.get("orderCode")), term);

                predicates.add(cb.or(pCode, pEmail, pName, pOrderCode));
            }

            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                ReturnStatus status = ReturnStatus.fromValue(request.getStatus());
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
                ReturnReason reason = ReturnReason.fromValue(request.getReason());
                predicates.add(cb.equal(root.get("returnReason"), reason));
            }

            if (request.getUserId() != null) {
                predicates.add(cb.equal(root.get("user").get("userId"), request.getUserId()));
            }

            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), request.getFromDate().atStartOfDay()));
            }

            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestedAt"), request.getToDate().atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ReturnDetailResponse mapToDetailResponse(ReturnRequest req) {
        String warehouseName = null;
        if (req.getRestockWarehouseId() != null) {
            warehouseName = warehouseRepository.findById(req.getRestockWarehouseId())
                    .map(w -> w.getName())
                    .orElse("Kho #" + req.getRestockWarehouseId());
        }

        List<ReturnDetailResponse.ReturnItemDetail> itemDetails = new ArrayList<>();
        if (req.getItems() != null) {
            for (ReturnRequestItem item : req.getItems()) {
                String imgUrl = null;
                if (item.getVariant() != null) {
                    if (item.getVariant().getImages() != null && !item.getVariant().getImages().isEmpty()) {
                        imgUrl = item.getVariant().getImages().get(0).getImageUrl();
                    } else if (item.getVariant().getProduct() != null && item.getVariant().getProduct().getImages() != null && !item.getVariant().getProduct().getImages().isEmpty()) {
                        imgUrl = item.getVariant().getProduct().getImages().get(0).getImageUrl();
                    }
                }
                String productName = item.getOrderItem().getProductNameSnapshot();
                String variantName = item.getVariant() != null ? item.getVariant().getVariantName() : "";
                String sku = item.getVariant() != null ? item.getVariant().getSkuVariant() : "";

                BigDecimal totalLine = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

                itemDetails.add(ReturnDetailResponse.ReturnItemDetail.builder()
                        .id(item.getId())
                        .orderItemId(item.getOrderItem().getOrderItemId())
                        .variantId(item.getVariant() != null ? item.getVariant().getVariantId() : null)
                        .productName(productName)
                        .variantName(variantName)
                        .skuVariant(sku)
                        .imageUrl(imgUrl)
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(totalLine)
                        .itemCondition(item.getItemCondition() != null ? item.getItemCondition().name() : "OPENED")
                        .build());
            }
        }

        List<String> images = new ArrayList<>();
        if (req.getImages() != null) {
            for (ReturnRequestImage img : req.getImages()) {
                images.add(img.getImageUrl());
            }
        }

        return ReturnDetailResponse.builder()
                .returnId(req.getReturnId())
                .returnCode(req.getReturnCode())
                .orderId(req.getOrder().getOrderId())
                .orderTrackingNumber(req.getOrder().getOrderCode())
                .userId(req.getUser().getUserId())
                .customerName(req.getUser().getFullName())
                .customerEmail(req.getUser().getEmail())
                .customerPhone(req.getUser().getPhone())
                .status(req.getStatus().name())
                .returnReason(req.getReturnReason().name())
                .customerNote(req.getCustomerNote())
                .adminNote(req.getAdminNote())
                .refundAmount(req.getRefundAmount())
                .bankName(req.getBankName())
                .bankAccountNumber(req.getBankAccountNumber())
                .bankAccountName(req.getBankAccountName())
                .refundTransactionCode(req.getRefundTransactionCode())
                .restockWarehouseId(req.getRestockWarehouseId())
                .restockWarehouseName(warehouseName)
                .requestedAt(req.getRequestedAt())
                .approvedAt(req.getApprovedAt())
                .receivedAt(req.getReceivedAt())
                .refundedAt(req.getRefundedAt())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .items(itemDetails)
                .imageUrls(images)
                .build();
    }
}
