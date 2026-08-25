import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import {
  CreateOrderRequest,
  Order,
  OrderFilter,
  OrderMetrics,
  UpdateOrderStatusRequest,
  UpdatePaymentStatusRequest
} from '../models/order.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/orders`;

  // ==========================================
  // CUSTOMER ENDPOINTS
  // ==========================================

  createOrder(request: CreateOrderRequest): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(this.baseUrl, request);
  }

  getMyOrders(page = 0, size = 10): Observable<ApiResponse<PageResponse<Order>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<Order>>>(`${this.baseUrl}/my-orders`, { params });
  }

  getOrderByCode(orderCode: string): Observable<ApiResponse<Order>> {
    return this.http.get<ApiResponse<Order>>(`${this.baseUrl}/${encodeURIComponent(orderCode)}`);
  }

  cancelMyOrder(orderCode: string, reason?: string): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(`${this.baseUrl}/${encodeURIComponent(orderCode)}/cancel`, { reason });
  }

  // ==========================================
  // ADMIN ENDPOINTS
  // ==========================================

  getAdminOrders(filter: OrderFilter = {}): Observable<ApiResponse<PageResponse<Order>>> {
    let params = new HttpParams();

    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.paymentStatus) {
      params = params.set('paymentStatus', filter.paymentStatus);
    }
    if (filter.keyword && filter.keyword.trim()) {
      params = params.set('keyword', filter.keyword.trim());
    }
    if (filter.startDate) {
      params = params.set('startDate', filter.startDate);
    }
    if (filter.endDate) {
      params = params.set('endDate', filter.endDate);
    }
    if (filter.page !== undefined) {
      params = params.set('page', filter.page.toString());
    }
    if (filter.size !== undefined) {
      params = params.set('size', filter.size.toString());
    }
    if (filter.sortBy) {
      params = params.set('sortBy', filter.sortBy);
    }
    if (filter.sortDir) {
      params = params.set('sortDir', filter.sortDir);
    }

    return this.http.get<ApiResponse<PageResponse<Order>>>(`${this.baseUrl}/admin`, { params });
  }

  getAdminOrderById(orderId: number): Observable<ApiResponse<Order>> {
    return this.http.get<ApiResponse<Order>>(`${this.baseUrl}/admin/${orderId}`);
  }

  updateOrderStatus(orderId: number, request: UpdateOrderStatusRequest): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${this.baseUrl}/admin/${orderId}/status`, request);
  }

  updatePaymentStatus(orderId: number, request: UpdatePaymentStatusRequest): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${this.baseUrl}/admin/${orderId}/payment-status`, request);
  }

  getAdminMetrics(): Observable<ApiResponse<OrderMetrics>> {
    return this.http.get<ApiResponse<OrderMetrics>>(`${this.baseUrl}/admin/metrics`);
  }
}
