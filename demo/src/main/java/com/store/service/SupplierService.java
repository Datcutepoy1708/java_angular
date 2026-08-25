package com.store.service;

import com.store.dto.response.PageResponse;
import com.store.dto.supplier.SupplierRequest;
import com.store.dto.supplier.SupplierResponse;

import java.util.List;

public interface SupplierService {

    List<SupplierResponse> getAllActiveSuppliers();

    PageResponse<SupplierResponse> getSuppliersPaginated(
            int page, int size, String keyword, String status,
            String sortBy, String sortDir
    );

    SupplierResponse getSupplierById(Integer supplierId);

    SupplierResponse createSupplier(SupplierRequest request);

    SupplierResponse updateSupplier(Integer supplierId, SupplierRequest request);

    void deleteSupplier(Integer supplierId);
}
