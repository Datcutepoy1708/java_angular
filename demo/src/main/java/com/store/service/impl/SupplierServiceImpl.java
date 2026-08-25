package com.store.service.impl;

import com.store.dto.response.PageResponse;
import com.store.dto.supplier.SupplierRequest;
import com.store.dto.supplier.SupplierResponse;
import com.store.entity.supplier.Supplier;
import com.store.entity.supplier.SupplierStatus;
import com.store.exception.BadRequestException;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ProductRepository;
import com.store.repository.SupplierRepository;
import com.store.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "suppliers", key = "'active'")
    public List<SupplierResponse> getAllActiveSuppliers() {
        log.info("Fetching all active suppliers from database (cache miss)");
        return supplierRepository.findAllActive()
                .stream()
                .map(s -> {
                    long count = productRepository.countBySupplier_SupplierId(s.getSupplierId());
                    return SupplierResponse.fromEntity(s, count);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getSuppliersPaginated(
            int page, int size, String keyword, String status,
            String sortBy, String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        SupplierStatus st = null;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                st = SupplierStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<Supplier> supplierPage = supplierRepository.findAllFiltered(kw, st, pageable);
        List<SupplierResponse> content = supplierPage.getContent()
                .stream()
                .map(s -> {
                    long count = productRepository.countBySupplier_SupplierId(s.getSupplierId());
                    return SupplierResponse.fromEntity(s, count);
                })
                .toList();

        return PageResponse.<SupplierResponse>builder()
                .content(content)
                .pageNumber(supplierPage.getNumber())
                .pageSize(supplierPage.getSize())
                .totalElements(supplierPage.getTotalElements())
                .totalPages(supplierPage.getTotalPages())
                .last(supplierPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Integer supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp không tồn tại với ID: " + supplierId));
        long count = productRepository.countBySupplier_SupplierId(supplierId);
        return SupplierResponse.fromEntity(supplier, count);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "suppliers", allEntries = true)
    public SupplierResponse createSupplier(SupplierRequest request) {
        log.info("Creating new supplier: {}", request.getName());
        String name = request.getName().trim();
        if (supplierRepository.existsByName(name)) {
            throw new DuplicateResourceException("Nhà cung cấp với tên '" + name + "' đã tồn tại");
        }

        Supplier supplier = request.toEntity();
        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created successfully with ID: {}", saved.getSupplierId());
        return SupplierResponse.fromEntity(saved, 0L);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "suppliers", allEntries = true)
    public SupplierResponse updateSupplier(Integer supplierId, SupplierRequest request) {
        log.info("Updating supplier ID: {}", supplierId);
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp không tồn tại với ID: " + supplierId));

        String name = request.getName().trim();
        if (supplierRepository.existsByNameAndSupplierIdNot(name, supplierId)) {
            throw new DuplicateResourceException("Tên nhà cung cấp '" + name + "' đã được sử dụng bởi nhà cung cấp khác");
        }

        SupplierStatus st = "inactive".equalsIgnoreCase(request.getStatus())
                ? SupplierStatus.INACTIVE
                : SupplierStatus.ACTIVE;

        supplier.setName(name);
        supplier.setContactName(request.getContactName() != null ? request.getContactName().trim() : null);
        supplier.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        supplier.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        supplier.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        supplier.setStatus(st);

        Supplier updated = supplierRepository.save(supplier);
        long count = productRepository.countBySupplier_SupplierId(supplierId);
        log.info("Supplier updated successfully with ID: {}", updated.getSupplierId());
        return SupplierResponse.fromEntity(updated, count);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "suppliers", allEntries = true)
    public void deleteSupplier(Integer supplierId) {
        log.info("Deleting supplier ID: {}", supplierId);
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp không tồn tại với ID: " + supplierId));

        long productCount = productRepository.countBySupplier_SupplierId(supplierId);
        if (productCount > 0) {
            throw new BadRequestException("Không thể xóa nhà cung cấp đang có " + productCount + " sản phẩm liên kết. Vui lòng chuyển hoặc xóa sản phẩm liên quan trước.");
        }

        supplierRepository.delete(supplier);
        log.info("Supplier ID: {} deleted successfully", supplierId);
    }
}
