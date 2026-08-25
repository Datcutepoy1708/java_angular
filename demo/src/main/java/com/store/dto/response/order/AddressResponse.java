package com.store.dto.response.order;

import com.store.entity.order.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private Long addressId;
    private Long userId;
    private String receiverName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String detailAddress;
    private String fullAddress;
    private Boolean isDefault;

    public static AddressResponse fromEntity(Address address) {
        if (address == null) return null;
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .userId(address.getUser() != null ? address.getUser().getUserId() : null)
                .receiverName(address.getReceiverName())
                .phone(address.getPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .fullAddress(address.getFullAddress())
                .isDefault(address.getIsDefault())
                .build();
    }
}
