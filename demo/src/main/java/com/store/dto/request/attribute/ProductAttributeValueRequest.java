package com.store.dto.request.attribute;

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
public class ProductAttributeValueRequest implements Serializable {

    @NotNull(message = "Mã thuộc tính không được để trống")
    private Integer attributeId;

    @NotBlank(message = "Giá trị thuộc tính không được để trống")
    @Size(max = 255, message = "Giá trị thuộc tính không được vượt quá 255 ký tự")
    private String value;
}
