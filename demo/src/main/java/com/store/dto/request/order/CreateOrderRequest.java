package com.store.dto.request.order;

import com.store.entity.order.PaymentMethod;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    private Long addressId;

    @Size(max = 150, message = "Receiver name must not exceed 150 characters")
    private String receiverName;

    @Size(max = 20, message = "Receiver phone must not exceed 20 characters")
    private String receiverPhone;

    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;

    private String province;
    private String district;
    private String ward;
    private String detailAddress;

    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
