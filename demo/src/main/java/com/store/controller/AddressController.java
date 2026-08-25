package com.store.controller;

import com.store.dto.request.order.AddressRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.order.AddressResponse;
import com.store.security.CustomUserDetails;
import com.store.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Address", description = "User Shipping Address Book APIs")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all saved addresses for current user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AddressResponse> response = addressService.getUserAddresses(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa chỉ thành công", response));
    }

    @GetMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a single address by ID")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId) {
        AddressResponse response = addressService.getAddressById(addressId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết địa chỉ thành công", response));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new shipping address")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm địa chỉ giao hàng thành công", response));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an existing shipping address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(addressId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", response));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a shipping address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId) {
        addressService.deleteAddress(addressId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa địa chỉ thành công", null));
    }

    @PutMapping("/{addressId}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Set address as default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId) {
        AddressResponse response = addressService.setDefaultAddress(addressId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Đã đặt làm địa chỉ mặc định", response));
    }
}
