package com.store.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest implements Serializable {

    @NotBlank(message = "Variant name is required")
    @Size(max = 200, message = "Variant name cannot exceed 200 characters")
    private String variantName;

    @Size(max = 100, message = "SKU variant cannot exceed 100 characters")
    private String skuVariant;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Sale price must be greater than or equal to 0")
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price must be greater than or equal to 0")
    private BigDecimal costPrice;

    @Pattern(regexp = "(?i)^(active|inactive)$", message = "Status must be either 'active' or 'inactive'")
    private String status;
}
