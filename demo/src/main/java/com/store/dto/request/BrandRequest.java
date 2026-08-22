package com.store.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class BrandRequest implements Serializable {

    @NotBlank(message = "Brand name is required")
    @Size(max = 150, message = "Brand name cannot exceed 150 characters")
    private String name;

    @Size(max = 180, message = "Slug cannot exceed 180 characters")
    private String slug;

    @Size(max = 500, message = "Logo URL cannot exceed 500 characters")
    private String logoUrl;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
