package com.store.service.impl;

import com.store.dto.request.order.AddressRequest;
import com.store.dto.response.order.AddressResponse;
import com.store.entity.order.Address;
import com.store.entity.user.User;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AddressRepository;
import com.store.repository.UserRepository;
import com.store.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserUserIdOrderByIsDefaultDescAddressIdDesc(userId)
                .stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long addressId, Long userId) {
        Address address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        return AddressResponse.fromEntity(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Address> existing = addressRepository.findByUserUserIdOrderByIsDefaultDescAddressIdDesc(userId);
        boolean isFirst = existing.isEmpty();
        boolean setAsDefault = isFirst || Boolean.TRUE.equals(request.getIsDefault());

        if (setAsDefault) {
            addressRepository.resetDefaultAddressForUser(userId);
        }

        Address address = Address.builder()
                .user(user)
                .receiverName(request.getReceiverName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .detailAddress(request.getDetailAddress())
                .isDefault(setAsDefault)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Created address {} for user {}", saved.getAddressId(), userId);
        return AddressResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long addressId, Long userId, AddressRequest request) {
        Address address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.resetDefaultAddressForUser(userId);
            address.setIsDefault(true);
        }

        address.setReceiverName(request.getReceiverName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setDetailAddress(request.getDetailAddress());

        Address updated = addressRepository.save(address);
        log.info("Updated address {} for user {}", updated.getAddressId(), userId);
        return AddressResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        log.info("Deleted address {} for user {}", addressId, userId);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserUserIdOrderByIsDefaultDescAddressIdDesc(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        addressRepository.resetDefaultAddressForUser(userId);
        address.setIsDefault(true);
        Address updated = addressRepository.save(address);
        log.info("Set address {} as default for user {}", addressId, userId);
        return AddressResponse.fromEntity(updated);
    }
}
