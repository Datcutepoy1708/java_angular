package com.store.dto.returnrefund;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnCreateRequest {

    @NotNull(message = "Mã đơn hàng không được để trống")
    private Long orderId;

    @NotBlank(message = "Lý do đổi trả không được để trống")
    private String returnReason; // DEFECTIVE, WRONG_ITEM, DAMAGED_IN_TRANSIT, NOT_AS_DESCRIBED, CHANGE_OF_MIND, OTHER

    private String customerNote;

    @NotEmpty(message = "Danh sách sản phẩm đổi trả không được rỗng")
    private List<ReturnItemRequest> items;

    private List<String> imageUrls;

    // Bank refund info
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnItemRequest {
        @NotNull(message = "Mã dòng đơn hàng không được để trống")
        private Long orderItemId;

        @NotNull(message = "Số lượng đổi trả không được để trống")
        private Integer quantity;

        @Builder.Default
        private String itemCondition = "OPENED"; // NEW_SEAL, OPENED, DAMAGED, DEFECTIVE
    }
}
