package com.store.entity.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @jakarta.persistence.OneToMany(mappedBy = "variant", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("sortOrder ASC, imageId ASC")
    @Builder.Default
    private java.util.List<ProductImage> images = new java.util.ArrayList<>();

    @Column(name = "variant_name", nullable = false, length = 200)
    private String variantName;

    @Column(name = "sku_variant", unique = true, length = 100)
    private String skuVariant;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "status", columnDefinition = "enum('active','inactive')")
    @Builder.Default
    private ProductVariantStatus status = ProductVariantStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Soft-delete timestamp. NULL = active; NOT NULL = deleted (independently of Product's deleted_at). */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public BigDecimal getEffectivePrice() {
        return (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) ? salePrice : price;
    }
}
