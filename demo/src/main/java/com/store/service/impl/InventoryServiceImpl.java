package com.store.service.impl;

import com.store.dto.request.inventory.InventoryFilterRequest;
import com.store.dto.request.inventory.InventoryLogFilterRequest;
import com.store.dto.request.inventory.StockAdjustmentRequest;
import com.store.dto.request.inventory.StockImportRequest;
import com.store.dto.request.inventory.StockReleaseRequest;
import com.store.dto.request.inventory.StockReserveRequest;
import com.store.dto.request.inventory.StockTransferRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.inventory.InventoryLogResponse;
import com.store.dto.response.inventory.InventoryMetricsResponse;
import com.store.dto.response.inventory.InventoryResponse;
import com.store.dto.response.inventory.VariantStockSummaryResponse;
import com.store.dto.response.inventory.WarehouseStockDto;
import com.store.entity.inventory.Inventory;
import com.store.entity.inventory.InventoryChangeType;
import com.store.entity.inventory.InventoryLog;
import com.store.entity.inventory.Warehouse;
import com.store.entity.product.ProductVariant;
import com.store.entity.user.User;
import com.store.exception.InsufficientStockException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.InventoryLogRepository;
import com.store.repository.InventoryRepository;
import com.store.repository.ProductVariantRepository;
import com.store.repository.UserRepository;
import com.store.repository.WarehouseRepository;
import com.store.service.InventoryService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventoryPage(InventoryFilterRequest request) {
        log.info("Fetching inventory page with filter: warehouseId={}, keyword={}, status={}, page={}, size={}",
                request.getWarehouseId(), request.getKeyword(), request.getStockStatus(), request.getPage(), request.getSize());

        Sort sort = Sort.by(
                "asc".equalsIgnoreCase(request.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                request.getSortBy() != null ? request.getSortBy() : "updatedAt"
        );
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<Inventory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getWarehouseId() != null) {
                predicates.add(cb.equal(root.get("warehouse").get("warehouseId"), request.getWarehouseId()));
            }

            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String term = "%" + request.getKeyword().trim().toLowerCase() + "%";
                var variantJoin = root.join("variant");
                var productJoin = variantJoin.join("product");

                Predicate pName = cb.like(cb.lower(productJoin.get("name")), term);
                Predicate vName = cb.like(cb.lower(variantJoin.get("variantName")), term);
                Predicate sku = cb.like(cb.lower(variantJoin.get("skuVariant")), term);
                predicates.add(cb.or(pName, vName, sku));
            }

            if (request.getStockStatus() != null && !"ALL".equalsIgnoreCase(request.getStockStatus())) {
                jakarta.persistence.criteria.Expression<Integer> availableExp = cb.diff(root.<Integer>get("quantity"), root.<Integer>get("reservedQty")).as(Integer.class);
                if ("IN_STOCK".equalsIgnoreCase(request.getStockStatus())) {
                    predicates.add(cb.greaterThan(availableExp, 10));
                } else if ("LOW_STOCK".equalsIgnoreCase(request.getStockStatus())) {
                    predicates.add(cb.and(
                            cb.lessThanOrEqualTo(availableExp, 10),
                            cb.greaterThan(availableExp, 0)
                    ));
                } else if ("OUT_OF_STOCK".equalsIgnoreCase(request.getStockStatus())) {
                    predicates.add(cb.lessThanOrEqualTo(availableExp, 0));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Inventory> pageResult = inventoryRepository.findAll(spec, pageable);
        List<InventoryResponse> content = pageResult.getContent().stream()
                .map(InventoryResponse::fromEntity)
                .toList();

        return PageResponse.<InventoryResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VariantStockSummaryResponse getStockSummaryByVariant(Long variantId) {
        log.info("Fetching stock summary for variant id: {}", variantId);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));

        List<Warehouse> warehouses = warehouseRepository.findAllByOrderByNameAsc();
        List<Inventory> inventoryList = inventoryRepository.findByVariantVariantId(variantId);

        int totalQty = 0;
        int totalReserved = 0;
        List<WarehouseStockDto> breakdown = new ArrayList<>();

        for (Warehouse w : warehouses) {
            Inventory inv = inventoryList.stream()
                    .filter(i -> i.getWarehouse().getWarehouseId().equals(w.getWarehouseId()))
                    .findFirst()
                    .orElse(null);

            int q = inv != null ? inv.getQuantity() : 0;
            int r = inv != null ? inv.getReservedQty() : 0;
            int avail = Math.max(0, q - r);

            totalQty += q;
            totalReserved += r;

            breakdown.add(WarehouseStockDto.builder()
                    .warehouseId(w.getWarehouseId())
                    .warehouseName(w.getName())
                    .warehouseAddress(w.getAddress())
                    .quantity(q)
                    .reservedQty(r)
                    .availableQty(avail)
                    .build());
        }

        int totalAvailable = Math.max(0, totalQty - totalReserved);
        String status = totalAvailable <= 0 ? "OUT_OF_STOCK" : (totalAvailable <= 10 ? "LOW_STOCK" : "IN_STOCK");

        var product = variant.getProduct();

        return VariantStockSummaryResponse.builder()
                .variantId(variant.getVariantId())
                .variantName(variant.getVariantName())
                .skuVariant(variant.getSkuVariant())
                .productId(product != null ? product.getProductId() : null)
                .productName(product != null ? product.getName() : null)
                .totalQuantity(totalQty)
                .totalReservedQty(totalReserved)
                .totalAvailableQty(totalAvailable)
                .inStock(totalAvailable > 0)
                .stockStatus(status)
                .warehouseBreakdown(breakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VariantStockSummaryResponse> getStockSummaryByProduct(Long productId) {
        log.info("Fetching stock summary for all variants of product id: {}", productId);
        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(productId);
        return variants.stream()
                .map(v -> getStockSummaryByVariant(v.getVariantId()))
                .toList();
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(StockAdjustmentRequest request, Long currentUserId) {
        log.info("Adjusting inventory: variantId={}, warehouseId={}, change={}, user={}",
                request.getVariantId(), request.getWarehouseId(), request.getQuantityChange(), currentUserId);

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + request.getVariantId()));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.getWarehouseId()));

        User user = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        // Pessimistic lock row to avoid race condition during adjustment
        Inventory inventory = inventoryRepository.findByVariantIdAndWarehouseIdWithLock(request.getVariantId(), request.getWarehouseId())
                .orElseGet(() -> Inventory.builder()
                        .variant(variant)
                        .warehouse(warehouse)
                        .quantity(0)
                        .reservedQty(0)
                        .build());

        int newQuantity = inventory.getQuantity() + request.getQuantityChange();
        if (newQuantity < inventory.getReservedQty()) {
            throw new InsufficientStockException(String.format(
                    "Không thể giảm tồn kho xuống dưới số lượng đang được giữ cho đơn hàng (Tồn mới: %d, Đang giữ: %d)",
                    newQuantity, inventory.getReservedQty()
            ));
        }

        inventory.setQuantity(newQuantity);
        Inventory saved = inventoryRepository.save(inventory);

        // Audit log
        InventoryLog auditLog = InventoryLog.builder()
                .variant(variant)
                .warehouse(warehouse)
                .changeType(InventoryChangeType.ADJUST)
                .quantityChange(request.getQuantityChange())
                .referenceType("manual_adjustment")
                .note(request.getReason())
                .createdBy(user)
                .build();
        inventoryLogRepository.save(auditLog);

        log.info("Stock adjusted successfully. New physical quantity: {}", newQuantity);
        return InventoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public List<InventoryResponse> importStock(StockImportRequest request, Long currentUserId) {
        log.info("Importing stock to warehouse id: {}, items count: {}, user={}",
                request.getWarehouseId(), request.getItems().size(), currentUserId);

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.getWarehouseId()));

        User user = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        List<InventoryResponse> results = new ArrayList<>();

        for (var item : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + item.getVariantId()));

            Inventory inventory = inventoryRepository.findByVariantIdAndWarehouseIdWithLock(item.getVariantId(), request.getWarehouseId())
                    .orElseGet(() -> Inventory.builder()
                            .variant(variant)
                            .warehouse(warehouse)
                            .quantity(0)
                            .reservedQty(0)
                            .build());

            inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
            Inventory saved = inventoryRepository.save(inventory);

            // Audit log
            InventoryLog auditLog = InventoryLog.builder()
                    .variant(variant)
                    .warehouse(warehouse)
                    .changeType(InventoryChangeType.IMPORT)
                    .quantityChange(item.getQuantity())
                    .referenceType("purchase_order")
                    .referenceId(request.getSupplierId())
                    .note(request.getNote())
                    .createdBy(user)
                    .build();
            inventoryLogRepository.save(auditLog);

            results.add(InventoryResponse.fromEntity(saved));
        }

        log.info("Stock import completed. Imported {} items", results.size());
        return results;
    }

    @Override
    @Transactional
    public List<InventoryResponse> transferStock(StockTransferRequest request, Long currentUserId) {
        log.info("Transferring stock: variantId={}, fromWarehouse={}, toWarehouse={}, qty={}, user={}",
                request.getVariantId(), request.getFromWarehouseId(), request.getToWarehouseId(), request.getQuantity(), currentUserId);

        if (Objects.equals(request.getFromWarehouseId(), request.getToWarehouseId())) {
            throw new IllegalArgumentException("Kho nguồn và kho đích không được trùng nhau");
        }

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + request.getVariantId()));

        Warehouse sourceWarehouse = warehouseRepository.findById(request.getFromWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Source warehouse not found: " + request.getFromWarehouseId()));

        Warehouse targetWarehouse = warehouseRepository.findById(request.getToWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Target warehouse not found: " + request.getToWarehouseId()));

        User user = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        // Step 1: Pessimistic Lock on source inventory
        Inventory sourceInv = inventoryRepository.findByVariantIdAndWarehouseIdWithLock(request.getVariantId(), request.getFromWarehouseId())
                .orElseThrow(() -> new InsufficientStockException("Kho nguồn không có bản ghi tồn kho cho sản phẩm này"));

        if (sourceInv.getAvailableQty() < request.getQuantity()) {
            throw new InsufficientStockException(String.format(
                    "Kho nguồn '%s' không đủ tồn khả dụng để chuyển hàng (Khả dụng: %d, Yêu cầu: %d)",
                    sourceWarehouse.getName(), sourceInv.getAvailableQty(), request.getQuantity()
            ));
        }

        // Step 2: Pessimistic Lock on target inventory
        Inventory targetInv = inventoryRepository.findByVariantIdAndWarehouseIdWithLock(request.getVariantId(), request.getToWarehouseId())
                .orElseGet(() -> Inventory.builder()
                        .variant(variant)
                        .warehouse(targetWarehouse)
                        .quantity(0)
                        .reservedQty(0)
                        .build());

        // Deduct source & Credit target
        sourceInv.setQuantity(sourceInv.getQuantity() - request.getQuantity());
        targetInv.setQuantity(targetInv.getQuantity() + request.getQuantity());

        inventoryRepository.save(sourceInv);
        inventoryRepository.save(targetInv);

        // Audit Logs (2 sides of the transfer)
        String noteText = request.getNote() != null && !request.getNote().isBlank() ? " - " + request.getNote() : "";

        InventoryLog outLog = InventoryLog.builder()
                .variant(variant)
                .warehouse(sourceWarehouse)
                .changeType(InventoryChangeType.TRANSFER)
                .quantityChange(-request.getQuantity())
                .referenceType("warehouse_transfer")
                .note("Chuyển xuất tới " + targetWarehouse.getName() + noteText)
                .createdBy(user)
                .build();

        InventoryLog inLog = InventoryLog.builder()
                .variant(variant)
                .warehouse(targetWarehouse)
                .changeType(InventoryChangeType.TRANSFER)
                .quantityChange(request.getQuantity())
                .referenceType("warehouse_transfer")
                .note("Chuyển nhập từ " + sourceWarehouse.getName() + noteText)
                .createdBy(user)
                .build();

        inventoryLogRepository.save(outLog);
        inventoryLogRepository.save(inLog);

        log.info("Stock transfer successful. Source remaining: {}, Target new: {}",
                sourceInv.getQuantity(), targetInv.getQuantity());

        return List.of(InventoryResponse.fromEntity(sourceInv), InventoryResponse.fromEntity(targetInv));
    }

    @Override
    @Transactional
    public InventoryResponse reserveStock(StockReserveRequest request, Long currentUserId) {
        log.info("Reserving stock: variantId={}, warehouseId={}, qty={}, refType={}, refId={}",
                request.getVariantId(), request.getWarehouseId(), request.getQuantity(), request.getReferenceType(), request.getReferenceId());

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + request.getVariantId()));

        Integer targetWarehouseId = request.getWarehouseId();

        // If warehouseId is null, auto-allocate using priority warehouse order (1 -> 2 -> 3)
        // TODO Phase 5: Auto-allocation based on customer shipping address / geocoding.
        // Currently falls back to warehouse priority order (1 -> 2 -> 3) or explicitly specified warehouseId.
        if (targetWarehouseId == null) {
            List<Inventory> allWarehouses = inventoryRepository.findAllByVariantIdWithLock(request.getVariantId());
            Inventory candidate = allWarehouses.stream()
                    .filter(i -> i.getAvailableQty() >= request.getQuantity())
                    .findFirst()
                    .orElse(null);

            if (candidate == null) {
                throw new InsufficientStockException(String.format(
                        "Không có kho hàng nào đủ số lượng khả dụng để giữ hàng cho sản phẩm '%s' (Yêu cầu: %d)",
                        variant.getVariantName(), request.getQuantity()
                ));
            }
            targetWarehouseId = candidate.getWarehouse().getWarehouseId();
        }

        // Atomic DB level update preventing race conditions
        int updatedRows = inventoryRepository.reserveStockAtomic(request.getVariantId(), targetWarehouseId, request.getQuantity());
        if (updatedRows == 0) {
            throw new InsufficientStockException(String.format(
                    "Sản phẩm '%s' không đủ số lượng khả dụng trong kho (Yêu cầu: %d)",
                    variant.getVariantName(), request.getQuantity()
            ));
        }

        Inventory updated = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(request.getVariantId(), targetWarehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found after reservation"));

        log.info("Stock reserved atomically. Target warehouse: {}, Reserved qty now: {}",
                targetWarehouseId, updated.getReservedQty());

        return InventoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public InventoryResponse releaseStock(StockReleaseRequest request, Long currentUserId) {
        log.info("Releasing reserved stock: variantId={}, warehouseId={}, qty={}, reason={}",
                request.getVariantId(), request.getWarehouseId(), request.getQuantity(), request.getReason());

        int updatedRows = inventoryRepository.releaseStockAtomic(request.getVariantId(), request.getWarehouseId(), request.getQuantity());
        if (updatedRows == 0) {
            throw new InsufficientStockException("Số lượng đang giữ trong kho không hợp lệ để hoàn trả");
        }

        Inventory updated = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(request.getVariantId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found after release"));

        log.info("Stock released atomically. Reserved qty now: {}", updated.getReservedQty());
        return InventoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public InventoryResponse deductCompletedStock(com.store.dto.request.inventory.StockDeductRequest request, Long currentUserId) {
        log.info("Deducting completed stock: variantId={}, warehouseId={}, qty={}, refType={}, refId={}",
                request.getVariantId(), request.getWarehouseId(), request.getQuantity(), request.getReferenceType(), request.getReferenceId());

        int updatedRows = inventoryRepository.deductCompletedStockAtomic(request.getVariantId(), request.getWarehouseId(), request.getQuantity());
        if (updatedRows == 0) {
            throw new InsufficientStockException("Số lượng tồn kho hoặc số lượng giữ chỗ không đủ để trừ hoàn tất đơn hàng");
        }

        Inventory updated = inventoryRepository.findByVariantVariantIdAndWarehouseWarehouseId(request.getVariantId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found after stock deduction"));

        User user = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        // Audit log for sale fulfillment
        InventoryLog logEntry = InventoryLog.builder()
                .variant(updated.getVariant())
                .warehouse(updated.getWarehouse())
                .changeType(InventoryChangeType.SALE)
                .quantityChange(-request.getQuantity())
                .referenceType(request.getReferenceType() != null ? request.getReferenceType() : "ORDER")
                .referenceId(request.getReferenceId())
                .note(request.getNote() != null ? request.getNote() : "Xuất kho hoàn tất giao hàng")
                .createdBy(user)
                .build();
        inventoryLogRepository.save(logEntry);

        log.info("Stock deducted atomically on order completion. Remaining qty: {}, Reserved qty now: {}",
                updated.getQuantity(), updated.getReservedQty());
        return InventoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getLowStockAlerts(int threshold, int page, int size) {
        log.info("Fetching low stock alerts: threshold={}, page={}, size={}", threshold, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Inventory> pageResult = inventoryRepository.findLowStockInventory(threshold, pageable);

        List<InventoryResponse> content = pageResult.getContent().stream()
                .map(InventoryResponse::fromEntity)
                .toList();

        return PageResponse.<InventoryResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryLogResponse> getInventoryLogs(InventoryLogFilterRequest request) {
        log.info("Fetching inventory audit logs: variantId={}, warehouseId={}, changeType={}, page={}, size={}",
                request.getVariantId(), request.getWarehouseId(), request.getChangeType(), request.getPage(), request.getSize());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<InventoryLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getVariantId() != null) {
                predicates.add(cb.equal(root.get("variant").get("variantId"), request.getVariantId()));
            }

            if (request.getWarehouseId() != null) {
                predicates.add(cb.equal(root.get("warehouse").get("warehouseId"), request.getWarehouseId()));
            }

            if (request.getChangeType() != null) {
                predicates.add(cb.equal(root.get("changeType"), request.getChangeType()));
            }

            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String term = "%" + request.getKeyword().trim().toLowerCase() + "%";
                var variantJoin = root.join("variant");
                var productJoin = variantJoin.join("product");

                Predicate pName = cb.like(cb.lower(productJoin.get("name")), term);
                Predicate vName = cb.like(cb.lower(variantJoin.get("variantName")), term);
                Predicate sku = cb.like(cb.lower(variantJoin.get("skuVariant")), term);
                Predicate note = cb.like(cb.lower(root.get("note")), term);
                predicates.add(cb.or(pName, vName, sku, note));
            }

            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getFromDate()));
            }

            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InventoryLog> pageResult = inventoryLogRepository.findAll(spec, pageable);
        List<InventoryLogResponse> content = pageResult.getContent().stream()
                .map(InventoryLogResponse::fromEntity)
                .toList();

        return PageResponse.<InventoryLogResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryMetricsResponse getInventoryMetrics() {
        log.info("Calculating real-time inventory metrics");
        long totalItems = inventoryRepository.count();
        long lowStock = inventoryRepository.countLowStockItems();
        long outOfStock = inventoryRepository.countOutOfStockItems();
        long totalPhysical = inventoryRepository.sumAllPhysicalQuantity();

        return InventoryMetricsResponse.builder()
                .totalTrackedItems(totalItems)
                .lowStockItemsCount(lowStock)
                .outOfStockItemsCount(outOfStock)
                .totalPhysicalQuantity(totalPhysical)
                .build();
    }
}
