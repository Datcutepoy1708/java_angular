package com.store.dto.request.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeCartRequest {

    @NotNull(message = "Danh sách mặt hàng cần gộp không được null")
    @Valid
    @Builder.Default
    private List<CartItemSyncDto> items = new ArrayList<>();
}
