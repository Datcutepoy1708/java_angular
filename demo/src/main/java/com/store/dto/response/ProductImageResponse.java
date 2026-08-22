package com.store.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.store.entity.product.ProductImage;
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
public class ProductImageResponse implements Serializable {

    private Long imageId;
    private Long productId;
    private Long variantId;
    private String variantName;
    private String imageUrl;
    private String imageType;
    private Integer sortOrder;
    private String altText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static ProductImageResponse fromEntity(ProductImage image) {
        if (image == null) {
            return null;
        }

        Long prodId = null;
        if (image.getProduct() != null) {
            prodId = image.getProduct().getProductId();
        }

        Long varId = null;
        String varName = null;
        if (image.getVariant() != null) {
            varId = image.getVariant().getVariantId();
            varName = image.getVariant().getVariantName();
        }

        return ProductImageResponse.builder()
                .imageId(image.getImageId())
                .productId(prodId)
                .variantId(varId)
                .variantName(varName)
                .imageUrl(image.getImageUrl())
                .imageType(image.getImageType() != null ? image.getImageType().getValue() : null)
                .sortOrder(image.getSortOrder())
                .altText(image.getAltText())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
