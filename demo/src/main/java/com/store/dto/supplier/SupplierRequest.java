package com.store.dto.supplier;

import com.store.entity.supplier.Supplier;
import com.store.entity.supplier.SupplierStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequest {

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(max = 200, message = "Tên nhà cung cấp tối đa 200 ký tự")
    private String name;

    @Size(max = 150, message = "Tên người liên hệ tối đa 150 ký tự")
    private String contactName;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
    private String address;

    @Builder.Default
    private String status = "active";

    public Supplier toEntity() {
        SupplierStatus st = "inactive".equalsIgnoreCase(this.status) ? SupplierStatus.INACTIVE : SupplierStatus.ACTIVE;
        return Supplier.builder()
                .name(this.name.trim())
                .contactName(this.contactName != null ? this.contactName.trim() : null)
                .phone(this.phone != null ? this.phone.trim() : null)
                .email(this.email != null ? this.email.trim() : null)
                .address(this.address != null ? this.address.trim() : null)
                .status(st)
                .build();
    }
}
