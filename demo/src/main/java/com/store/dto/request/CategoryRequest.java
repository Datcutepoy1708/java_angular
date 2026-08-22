package com.store.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CategoryRequest implements Serializable {

    @NotBlank(message = "Category name is required")
    @Size(max = 150, message = "Category name cannot exceed 150 characters")
    private String name;

    @Size(max = 180, message = "Slug cannot exceed 180 characters")
    private String slug;

    private Integer parentId;

    @Size(max = 500, message = "Icon URL cannot exceed 500 characters")
    private String iconUrl;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Integer sortOrder;

    @Pattern(regexp = "(?i)^(active|inactive)$", message = "Status must be either 'active' or 'inactive'")
    private String status;
}
