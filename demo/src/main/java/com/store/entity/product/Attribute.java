package com.store.entity.product;

import com.store.entity.category.Category;
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

@Entity
@Table(name = "attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attribute_id")
    private Integer attributeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "data_type", columnDefinition = "enum('text','number','boolean')")
    @Builder.Default
    private AttributeDataType dataType = AttributeDataType.TEXT;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
