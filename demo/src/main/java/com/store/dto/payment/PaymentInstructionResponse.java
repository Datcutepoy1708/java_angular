package com.store.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInstructionResponse {

    private String paymentReference;
    private String paymentPollingToken; // Only populated on initial order creation
    private String bankId;
    private String bankAccountNo;
    private String bankAccountName;
    private BigDecimal totalAmount;
    private String qrCodeUrl;
}
