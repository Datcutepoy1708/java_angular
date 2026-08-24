package com.store.dto.request.attribute;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSaveProductAttributesRequest implements Serializable {

    @NotNull(message = "Danh sách thuộc tính không được null")
    @Valid
    @Builder.Default
    private List<ProductAttributeValueRequest> attributes = new ArrayList<>();
}
