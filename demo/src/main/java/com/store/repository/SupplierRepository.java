package com.store.repository;

import com.store.entity.supplier.Supplier;
import com.store.entity.supplier.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    @Query("SELECT s FROM Supplier s WHERE s.status = com.store.entity.supplier.SupplierStatus.ACTIVE ORDER BY s.name ASC")
    List<Supplier> findAllActive();

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:keyword IS NULL OR " +
           " LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR s.status = :status)")
    Page<Supplier> findAllFiltered(
            @Param("keyword") String keyword,
            @Param("status") SupplierStatus status,
            Pageable pageable
    );

    boolean existsByName(String name);

    boolean existsByNameAndSupplierIdNot(String name, Integer supplierId);
}
