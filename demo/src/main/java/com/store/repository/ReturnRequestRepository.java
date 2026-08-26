package com.store.repository;

import com.store.entity.returnrefund.ReturnRequest;
import com.store.entity.returnrefund.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long>, JpaSpecificationExecutor<ReturnRequest> {

    Optional<ReturnRequest> findByReturnCode(String returnCode);

    List<ReturnRequest> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    Page<ReturnRequest> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByOrderOrderIdAndStatusNotIn(Long orderId, List<ReturnStatus> statuses);

    long countByStatus(ReturnStatus status);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM ReturnRequest r WHERE r.status = :status")
    BigDecimal sumRefundAmountByStatus(@Param("status") ReturnStatus status);

    @Query("SELECT COUNT(r) FROM ReturnRequest r WHERE r.returnCode LIKE CONCAT(:prefix, '%')")
    long countByReturnCodePrefix(@Param("prefix") String prefix);
}
