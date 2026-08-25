package com.store.dto.supplier;

import com.store.entity.supplier.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponse {

    private Integer supplierId;
    private String name;
    private String contactName;
    private String phone;
    private String email;
    private String address;
    private String status;
    private LocalDateTime createdAt;
    private Long productCount;

    public static SupplierResponse fromEntity(Supplier supplier, Long productCount) {
        if (supplier == null) return null;
        return SupplierResponse.builder()
                .supplierId(supplier.getSupplierId())
                .name(supplier.getName())
                .contactName(supplier.getContactName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .status(supplier.getStatus() != null ? supplier.getStatus().name().toLowerCase() : "active")
                .createdAt(supplier.getCreatedAt())
                .productCount(productCount != null ? productCount : 0L)
                .build();
    }

    public static SupplierResponse fromEntity(Supplier supplier) {
        return fromEntity(supplier, 0L);
    }
}
