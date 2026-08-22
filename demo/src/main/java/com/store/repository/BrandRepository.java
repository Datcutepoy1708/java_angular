package com.store.repository;

import com.store.entity.brand.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {

    Optional<Brand> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsByNameAndBrandIdNot(String name, Integer brandId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndBrandIdNot(String slug, Integer brandId);
}
