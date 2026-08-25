package com.store.repository;

import com.store.entity.news.News;
import com.store.entity.news.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long>, JpaSpecificationExecutor<News> {

    Optional<News> findBySlugAndStatus(String slug, NewsStatus status);

    Optional<News> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndNewsIdNot(String slug, Long newsId);

    Page<News> findByStatusOrderByPublishedAtDesc(NewsStatus status, Pageable pageable);

    Page<News> findByCategory_NewsCatIdAndStatusOrderByPublishedAtDesc(Integer newsCatId, NewsStatus status, Pageable pageable);

    List<News> findTop4ByCategory_NewsCatIdAndNewsIdNotAndStatusOrderByPublishedAtDesc(Integer newsCatId, Long newsId, NewsStatus status);

    List<News> findTop4ByNewsIdNotAndStatusOrderByPublishedAtDesc(Long newsId, NewsStatus status);

    @Modifying
    @Query("UPDATE News n SET n.viewCount = n.viewCount + 1 WHERE n.newsId = :newsId")
    void incrementViewCount(@Param("newsId") Long newsId);
}
