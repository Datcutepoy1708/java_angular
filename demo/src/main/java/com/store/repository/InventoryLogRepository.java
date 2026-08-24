package com.store.repository;

import com.store.entity.inventory.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long>, JpaSpecificationExecutor<InventoryLog> {
    List<InventoryLog> findTop50ByOrderByCreatedAtDesc();
}
