package com.store.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPollingResponse {

    private String status; // "PENDING" or "PAID"
    private LocalDateTime paidAt;
}
