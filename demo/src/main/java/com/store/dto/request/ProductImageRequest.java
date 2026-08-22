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
public class ProductImageRequest implements Serializable {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    private Long variantId;

    @Pattern(regexp = "(?i)^(main|sub)$", message = "Image type must be 'main' or 'sub'")
    private String imageType;

    private Integer sortOrder;

    @Size(max = 255, message = "Alt text cannot exceed 255 characters")
    private String altText;
}
