package com.store.dto.request.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 150, message = "Tên danh mục tối đa 150 ký tự")
    private String name;

    private String slug;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private String status = "active";
}
