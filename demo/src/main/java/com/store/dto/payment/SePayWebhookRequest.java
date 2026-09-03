package com.store.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayWebhookRequest {

    @NotNull(message = "Transaction id is required")
    private Long id;

    private String gateway;

    @JsonProperty("transactionDate")
    private String transactionDate;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("subAccount")
    private String subAccount;

    private String code;

    private String content;

    @JsonProperty("transferType")
    private String transferType;

    @NotNull(message = "Transfer amount is required")
    @JsonProperty("transferAmount")
    private BigDecimal transferAmount;

    private BigDecimal accumulated;

    @JsonProperty("referenceCode")
    private String referenceCode;

    private String description;
}
