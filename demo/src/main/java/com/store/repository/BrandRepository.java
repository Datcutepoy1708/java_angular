package com.store.repository;

import com.store.entity.brand.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {

    // Active only (deleted_at IS NULL)
    @Query("SELECT b FROM Brand b WHERE b.deletedAt IS NULL ORDER BY b.name ASC")
    List<Brand> findAllActive();

    @Query("SELECT b FROM Brand b WHERE b.deletedAt IS NULL " +
           "AND (:keyword IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR b.status = com.store.entity.brand.BrandStatus.ACTIVE OR " +
           "     (:status = 'inactive' AND b.status = com.store.entity.brand.BrandStatus.INACTIVE))")
    Page<Brand> findAllActiveFiltered(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    // Trash (deleted_at IS NOT NULL)
    Page<Brand> findByDeletedAtIsNotNull(Pageable pageable);

    Optional<Brand> findBySlugAndDeletedAtIsNull(String slug);

    Optional<Brand> findBySlug(String slug);

    boolean existsByName(String name);
    boolean existsByNameAndDeletedAtIsNull(String name);
    boolean existsByNameAndBrandIdNot(String name, Integer brandId);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndBrandIdNot(String slug, Integer brandId);
}
