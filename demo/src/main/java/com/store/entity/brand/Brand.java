package com.store.entity.brand;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 180)
    private String slug;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "description", length = 500)
    private String description;

    @Convert(converter = BrandStatusConverter.class)
    @Column(name = "status", columnDefinition = "enum('active','inactive')")
    @Builder.Default
    private BrandStatus status = BrandStatus.ACTIVE;

    /**
     * Soft-delete timestamp. NULL = active; NOT NULL = in trash.
     * Never changes the `status` field — preserves original business state on restore.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
