package com.store.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest implements Serializable {

    @NotNull(message = "Category ID is required")
    private Integer categoryId;

    private Integer brandId;

    private Integer supplierId;

    @NotBlank(message = "Product name is required")
    @Size(max = 250, message = "Product name cannot exceed 250 characters")
    private String name;

    @Size(max = 280, message = "Slug cannot exceed 280 characters")
    private String slug;

    @Size(max = 100, message = "SKU cannot exceed 100 characters")
    private String sku;

    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    private String shortDesc;

    private String description;

    @Min(value = 0, message = "Warranty months must be greater than or equal to 0")
    private Integer warrantyMonths;

    @Pattern(regexp = "(?i)^(active|inactive|discontinued)$", message = "Status must be 'active', 'inactive', or 'discontinued'")
    private String status;
}
