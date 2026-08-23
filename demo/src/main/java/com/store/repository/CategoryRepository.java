package com.store.repository;

import com.store.entity.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Active only (deleted_at IS NULL)
    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findAllActive();

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL " +
           "AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Category> findAllActiveFiltered(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL AND c.parent IS NULL ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findActiveRoots();

    // Trash (deleted_at IS NOT NULL)
    Page<Category> findByDeletedAtIsNotNull(Pageable pageable);

    // All (including deleted) — used for cascade operations
    @Query("SELECT c FROM Category c ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findAllIncludingDeleted();

    Optional<Category> findBySlug(String slug);

    @Query("SELECT c FROM Category c WHERE c.slug = :slug AND c.deletedAt IS NULL")
    Optional<Category> findBySlugActive(@Param("slug") String slug);

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
