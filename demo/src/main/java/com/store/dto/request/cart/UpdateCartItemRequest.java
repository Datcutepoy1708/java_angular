package com.store.dto.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class UpdateCartItemRequest {

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity;
}
