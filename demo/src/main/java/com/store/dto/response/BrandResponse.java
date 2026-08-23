package com.store.dto.response;

import com.store.entity.brand.Brand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse implements Serializable {

    private Integer brandId;
    private String name;
    private String slug;
    private String logoUrl;
    private String country;
    private String description;
    private String status;
    private boolean deleted;
    private LocalDateTime deletedAt;

    public static BrandResponse fromEntity(Brand brand) {
        if (brand == null) {
            return null;
        }
        return BrandResponse.builder()
                .brandId(brand.getBrandId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logoUrl(brand.getLogoUrl())
                .country(brand.getCountry())
                .description(brand.getDescription())
                .status(brand.getStatus() != null ? brand.getStatus().getValue() : "active")
                .deleted(brand.getDeletedAt() != null)
                .deletedAt(brand.getDeletedAt())
                .build();
    }
}
