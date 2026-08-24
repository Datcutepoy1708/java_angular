package com.store.repository;

import com.store.entity.product.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Integer> {

    List<Attribute> findByCategoryCategoryIdOrderBySortOrderAscAttributeIdAsc(Integer categoryId);

    List<Attribute> findByCategoryCategoryIdInOrderBySortOrderAsc(List<Integer> categoryIds);

    boolean existsByCategoryCategoryIdAndNameIgnoreCase(Integer categoryId, String name);
}
