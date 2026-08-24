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
public class AddToCartRequest {

    @NotNull(message = "Mã biến thể sản phẩm không được để trống")
    private Long variantId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng thêm vào giỏ phải từ 1 trở lên")
    private Integer quantity;
}
