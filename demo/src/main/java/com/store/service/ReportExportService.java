package com.store.service;

import com.store.dto.request.order.OrderFilterRequest;

import java.time.LocalDate;

public interface ReportExportService {

    byte[] exportRevenueReport(LocalDate startDate, LocalDate endDate, String period);

    byte[] exportOrdersReport(OrderFilterRequest filter);

    byte[] exportInventoryReport(Integer warehouseId, String keyword, String stockStatus);
}
