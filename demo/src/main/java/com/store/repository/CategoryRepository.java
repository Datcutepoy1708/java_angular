package com.store.repository;

import com.store.entity.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIsNullOrderBySortOrderAscNameAsc();

    List<Category> findByParent_CategoryIdOrderBySortOrderAscNameAsc(Integer parentId);

    long countByParent_CategoryId(Integer parentId);

    boolean existsByNameAndParentIsNull(String name);

    boolean existsByNameAndParentIsNullAndCategoryIdNot(String name, Integer categoryId);

    boolean existsByNameAndParent_CategoryId(String name, Integer parentId);

    boolean existsByNameAndParent_CategoryIdAndCategoryIdNot(String name, Integer parentId, Integer categoryId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndCategoryIdNot(String slug, Integer categoryId);
}
