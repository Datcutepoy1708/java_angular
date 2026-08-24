package com.store.dto.request.attribute;

import com.store.entity.product.AttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeRequest implements Serializable {

    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    @NotBlank(message = "Tên thuộc tính không được để trống")
    @Size(max = 150, message = "Tên thuộc tính không được vượt quá 150 ký tự")
    private String name;

    @NotNull(message = "Kiểu dữ liệu thuộc tính không được để trống")
    private AttributeDataType dataType;

    @Size(max = 50, message = "Đơn vị không được vượt quá 50 ký tự")
    private String unit;

    @Builder.Default
    private Integer sortOrder = 0;
}
