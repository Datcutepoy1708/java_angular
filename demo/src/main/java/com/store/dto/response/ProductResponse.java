package com.store.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.store.entity.product.Product;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse implements Serializable {

    private Long productId;

    private Integer categoryId;
    private String categoryName;
    private String categorySlug;

    private Integer brandId;
    private String brandName;
    private String brandSlug;

    private Integer supplierId;
    private String supplierName;

    private String name;
    private String slug;
    private String sku;
    private String shortDesc;
    private String description;
    private Integer warrantyMonths;
    private String status;
    private Integer viewCount;
    private String mainImageUrl;
    private java.util.List<ProductImageResponse> images;
    private java.util.List<ProductVariantResponse> variants;
    private java.util.List<com.store.dto.response.attribute.ProductAttributeValueResponse> specifications;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        Integer catId = null;
        String catName = null;
        String catSlug = null;
        if (product.getCategory() != null) {
            catId = product.getCategory().getCategoryId();
            catName = product.getCategory().getName();
            catSlug = product.getCategory().getSlug();
        }

        Integer bId = null;
        String bName = null;
        String bSlug = null;
        if (product.getBrand() != null) {
            bId = product.getBrand().getBrandId();
            bName = product.getBrand().getName();
            bSlug = product.getBrand().getSlug();
        }

        Integer sId = null;
        String sName = null;
        if (product.getSupplier() != null) {
            sId = product.getSupplier().getSupplierId();
            sName = product.getSupplier().getName();
        }

        java.util.List<ProductVariantResponse> variantResponses = null;
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            variantResponses = product.getVariants().stream()
                    .map(ProductVariantResponse::fromEntity)
                    .toList();
        }

        java.util.List<ProductImageResponse> imageResponses = null;
        String mainImg = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageResponses = product.getImages().stream()
                    .map(ProductImageResponse::fromEntity)
                    .toList();
            mainImg = product.getImages().stream()
                    .filter(img -> img.getImageType() == com.store.entity.product.ImageType.MAIN)
                    .map(com.store.entity.product.ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(product.getImages().get(0).getImageUrl());
        }

        return ProductResponse.builder()
                .productId(product.getProductId())
                .categoryId(catId)
                .categoryName(catName)
                .categorySlug(catSlug)
                .brandId(bId)
                .brandName(bName)
                .brandSlug(bSlug)
                .supplierId(sId)
                .supplierName(sName)
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .shortDesc(product.getShortDesc())
                .description(product.getDescription())
                .warrantyMonths(product.getWarrantyMonths())
                .status(product.getStatus() != null ? product.getStatus().getValue() : null)
                .viewCount(product.getViewCount())
                .mainImageUrl(mainImg)
                .images(imageResponses)
                .variants(variantResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
