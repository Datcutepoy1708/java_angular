package com.store.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.store.entity.product.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantResponse implements Serializable {

    private Long variantId;
    private Long productId;
    private String productName;
    private String variantName;
    private String skuVariant;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private String status;
    private String mainImageUrl;
    private java.util.List<ProductImageResponse> images;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static ProductVariantResponse fromEntity(ProductVariant variant) {
        if (variant == null) {
            return null;
        }

        Long prodId = null;
        String prodName = null;
        if (variant.getProduct() != null) {
            prodId = variant.getProduct().getProductId();
            prodName = variant.getProduct().getName();
        }

        java.util.List<ProductImageResponse> imageResponses = null;
        String mainImg = null;
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            imageResponses = variant.getImages().stream()
                    .map(ProductImageResponse::fromEntity)
                    .toList();
            mainImg = variant.getImages().stream()
                    .filter(img -> img.getImageType() == com.store.entity.product.ImageType.MAIN)
                    .map(com.store.entity.product.ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(variant.getImages().get(0).getImageUrl());
        }

        return ProductVariantResponse.builder()
                .variantId(variant.getVariantId())
                .productId(prodId)
                .productName(prodName)
                .variantName(variant.getVariantName())
                .skuVariant(variant.getSkuVariant())
                .price(variant.getPrice())
                .salePrice(variant.getSalePrice())
                .costPrice(variant.getCostPrice())
                .status(variant.getStatus() != null ? variant.getStatus().getValue() : null)
                .mainImageUrl(mainImg)
                .images(imageResponses)
                .createdAt(variant.getCreatedAt())
                .build();
    }
}
