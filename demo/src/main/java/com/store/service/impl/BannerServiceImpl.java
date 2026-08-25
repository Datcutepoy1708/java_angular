package com.store.service.impl;

import com.store.dto.request.banner.CreateBannerRequest;
import com.store.dto.request.banner.UpdateBannerRequest;
import com.store.dto.response.banner.BannerResponse;
import com.store.entity.banner.Banner;
import com.store.entity.banner.BannerPosition;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.BannerRepository;
import com.store.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "banners", key = "#position != null ? #position.name() : 'ALL'")
    public List<BannerResponse> getPublicBanners(BannerPosition position) {
        log.info("Fetching public active banners for position: {}", position);
        List<Banner> banners = bannerRepository.findActiveBannersByPosition(position, LocalDateTime.now());
        return banners.stream()
                .map(BannerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getAdminBanners(BannerPosition position) {
        List<Banner> banners;
        if (position != null) {
            banners = bannerRepository.findByPositionOrderBySortOrderAsc(position);
        } else {
            banners = bannerRepository.findAll();
        }
        return banners.stream()
                .map(BannerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BannerResponse getBannerById(Long bannerId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy banner với id: " + bannerId));
        return BannerResponse.fromEntity(banner);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "banners", allEntries = true)
    public BannerResponse createBanner(CreateBannerRequest request) {
        log.info("Creating banner with title: {}", request.getTitle());
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .linkUrl(request.getLinkUrl())
                .position(request.getPosition())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .build();

        Banner saved = bannerRepository.save(banner);
        return BannerResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "banners", allEntries = true)
    public BannerResponse updateBanner(Long bannerId, UpdateBannerRequest request) {
        log.info("Updating banner with id: {}", bannerId);
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy banner với id: " + bannerId));

        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setPosition(request.getPosition());
        banner.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        banner.setStartDate(request.getStartDate());
        banner.setEndDate(request.getEndDate());
        banner.setStatus(request.getStatus());

        Banner saved = bannerRepository.save(banner);
        return BannerResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "banners", allEntries = true)
    public void deleteBanner(Long bannerId) {
        log.info("Deleting banner with id: {}", bannerId);
        if (!bannerRepository.existsById(bannerId)) {
            throw new ResourceNotFoundException("Không tìm thấy banner với id: " + bannerId);
        }
        bannerRepository.deleteById(bannerId);
    }
}
