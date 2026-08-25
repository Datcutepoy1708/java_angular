package com.store.dto.response.banner;

import com.store.entity.banner.Banner;
import com.store.entity.banner.BannerPosition;
import com.store.entity.banner.BannerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerResponse {

    private Long bannerId;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private BannerPosition position;
    private Integer sortOrder;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BannerStatus status;
    private LocalDateTime createdAt;

    public static BannerResponse fromEntity(Banner banner) {
        if (banner == null) {
            return null;
        }
        return BannerResponse.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .position(banner.getPosition())
                .sortOrder(banner.getSortOrder())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .status(banner.getStatus())
                .createdAt(banner.getCreatedAt())
                .build();
    }
}
