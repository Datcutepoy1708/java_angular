package com.store.service;

import com.store.dto.returnrefund.*;
import org.springframework.data.domain.Page;

public interface ReturnService {

    ReturnDetailResponse createReturnRequest(Long userId, ReturnCreateRequest request);

    Page<ReturnDetailResponse> getAdminReturnRequests(ReturnFilterRequest request);

    Page<ReturnDetailResponse> getCustomerReturnRequests(Long userId, int page, int size);

    ReturnDetailResponse getReturnRequestById(Long returnId, Long currentUserId, boolean isAdmin);

    ReturnDetailResponse reviewReturnRequest(Long returnId, Long adminId, ReturnReviewRequest request);

    ReturnDetailResponse receiveReturnedItems(Long returnId, Long adminId, ReturnReceiveItemRequest request);

    ReturnDetailResponse processRefund(Long returnId, Long adminId, ReturnProcessRefundRequest request);

    ReturnDetailResponse cancelReturnRequest(Long returnId, Long userId);

    ReturnMetricsResponse getReturnMetrics();
}
