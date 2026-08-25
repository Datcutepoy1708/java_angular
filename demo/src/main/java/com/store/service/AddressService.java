package com.store.service;

import com.store.dto.request.order.AddressRequest;
import com.store.dto.response.order.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getUserAddresses(Long userId);

    AddressResponse getAddressById(Long addressId, Long userId);

    AddressResponse createAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long addressId, Long userId, AddressRequest request);

    void deleteAddress(Long addressId, Long userId);

    AddressResponse setDefaultAddress(Long addressId, Long userId);
}
