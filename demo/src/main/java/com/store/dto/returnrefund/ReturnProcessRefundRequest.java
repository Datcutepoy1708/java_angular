package com.store.dto.returnrefund;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnProcessRefundRequest {

    @NotBlank(message = "Mã giao dịch ngân hàng không được để trống")
    private String refundTransactionCode;

    private String adminNote;
}
