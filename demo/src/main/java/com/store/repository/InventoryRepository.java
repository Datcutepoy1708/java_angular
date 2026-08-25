package com.store.repository;

import com.store.entity.inventory.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByVariantVariantIdAndWarehouseWarehouseId(Long variantId, Integer warehouseId);

    List<Inventory> findByVariantVariantId(Long variantId);

    List<Inventory> findByVariantProductProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.variantId = :variantId AND i.warehouse.warehouseId = :warehouseId")
    Optional<Inventory> findByVariantIdAndWarehouseIdWithLock(@Param("variantId") Long variantId, @Param("warehouseId") Integer warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.variantId = :variantId ORDER BY i.warehouse.warehouseId ASC")
    List<Inventory> findAllByVariantIdWithLock(@Param("variantId") Long variantId);

    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQty = i.reservedQty + :qty, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.variant.variantId = :variantId AND i.warehouse.warehouseId = :warehouseId " +
           "AND (i.quantity - i.reservedQty) >= :qty")
    int reserveStockAtomic(@Param("variantId") Long variantId, @Param("warehouseId") Integer warehouseId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQty = i.reservedQty - :qty, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.variant.variantId = :variantId AND i.warehouse.warehouseId = :warehouseId " +
           "AND i.reservedQty >= :qty")
    int releaseStockAtomic(@Param("variantId") Long variantId, @Param("warehouseId") Integer warehouseId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :qty, i.reservedQty = i.reservedQty - :qty, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.variant.variantId = :variantId AND i.warehouse.warehouseId = :warehouseId " +
           "AND i.quantity >= :qty AND i.reservedQty >= :qty")
    int deductCompletedStockAtomic(@Param("variantId") Long variantId, @Param("warehouseId") Integer warehouseId, @Param("qty") int qty);

    @Query("SELECT COALESCE(SUM(i.quantity - i.reservedQty), 0) FROM Inventory i WHERE i.variant.variantId = :variantId")
    Long sumAvailableStockByVariantId(@Param("variantId") Long variantId);

    @Query("SELECT i.variant.variantId AS variantId, COALESCE(SUM(i.quantity - i.reservedQty), 0L) AS availableQty " +
           "FROM Inventory i WHERE i.variant.variantId IN :variantIds " +
           "GROUP BY i.variant.variantId")
    List<VariantStockSummaryProjection> findAvailableStockByVariantIds(@Param("variantIds") java.util.Collection<Long> variantIds);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.variant.variantId = :variantId")
    Long sumTotalQuantityByVariantId(@Param("variantId") Long variantId);

    @Query("SELECT COALESCE(SUM(i.reservedQty), 0) FROM Inventory i WHERE i.variant.variantId = :variantId")
    Long sumTotalReservedByVariantId(@Param("variantId") Long variantId);

    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQty) <= :threshold ORDER BY (i.quantity - i.reservedQty) ASC")
    Page<Inventory> findLowStockInventory(@Param("threshold") int threshold, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE (i.quantity - i.reservedQty) <= 10 AND (i.quantity - i.reservedQty) > 0")
    long countLowStockItems();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE (i.quantity - i.reservedQty) <= 0")
    long countOutOfStockItems();

    @Query("SELECT i FROM Inventory i WHERE i.variant.variantId = :variantId AND (i.quantity - i.reservedQty) >= :qty ORDER BY i.warehouse.warehouseId ASC")
    List<Inventory> findWarehousesWithAvailableStock(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i")
    long sumAllPhysicalQuantity();
}
