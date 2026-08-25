package com.store.repository;

import com.store.entity.news.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsCategoryRepository extends JpaRepository<NewsCategory, Integer> {

    List<NewsCategory> findByStatusOrderBySortOrderAsc(String status);

    Optional<NewsCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndNewsCatIdNot(String slug, Integer newsCatId);
}
