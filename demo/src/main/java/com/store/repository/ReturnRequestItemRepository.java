package com.store.repository;

import com.store.entity.returnrefund.ReturnRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Long> {

    List<ReturnRequestItem> findByReturnRequestReturnId(Long returnId);
}
