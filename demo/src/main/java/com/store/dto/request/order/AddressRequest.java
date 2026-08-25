package com.store.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 150, message = "Receiver name must not exceed 150 characters")
    private String receiverName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @NotBlank(message = "Province is required")
    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @NotBlank(message = "Ward is required")
    @Size(max = 100, message = "Ward must not exceed 100 characters")
    private String ward;

    @NotBlank(message = "Detail address is required")
    @Size(max = 255, message = "Detail address must not exceed 255 characters")
    private String detailAddress;

    @Builder.Default
    private Boolean isDefault = false;
}
