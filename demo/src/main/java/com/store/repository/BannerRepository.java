package com.store.repository;

import com.store.entity.banner.Banner;
import com.store.entity.banner.BannerPosition;
import com.store.entity.banner.BannerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long>, JpaSpecificationExecutor<Banner> {

    @Query("SELECT b FROM Banner b WHERE b.position = :position AND b.status = com.store.entity.banner.BannerStatus.ACTIVE " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.sortOrder ASC, b.createdAt DESC")
    List<Banner> findActiveBannersByPosition(@Param("position") BannerPosition position, @Param("now") LocalDateTime now);

    List<Banner> findByPositionOrderBySortOrderAsc(BannerPosition position);
}
