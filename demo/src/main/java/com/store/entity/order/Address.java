package com.store.entity.order;

import com.store.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "receiver_name", nullable = false, length = 150)
    private String receiverName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (detailAddress != null && !detailAddress.isBlank()) {
            sb.append(detailAddress);
        }
        if (ward != null && !ward.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(ward);
        }
        if (district != null && !district.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(district);
        }
        if (province != null && !province.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(province);
        }
        return sb.toString();
    }
}
