import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ReturnCreateRequest,
  ReturnDetail,
  ReturnFilter,
  ReturnMetrics,
  ReturnProcessRefundRequest,
  ReturnReceiveItemRequest,
  ReturnReviewRequest
} from '../models/return.model';
import { ApiResponse } from './audit-log.service';

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
export class ReturnService {
  private readonly customerUrl = `${environment.apiUrl}/api/v1/returns`;
  private readonly adminUrl = `${environment.apiUrl}/api/v1/admin/returns`;

  constructor(private readonly http: HttpClient) {}

  // Customer methods
  createReturnRequest(request: ReturnCreateRequest): Observable<ApiResponse<ReturnDetail>> {
    return this.http.post<ApiResponse<ReturnDetail>>(this.customerUrl, request);
  }

  getMyReturnRequests(page: number = 0, size: number = 10): Observable<ApiResponse<PageResponse<ReturnDetail>>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<ReturnDetail>>>(this.customerUrl, { params });
  }

  getMyReturnRequestById(returnId: number): Observable<ApiResponse<ReturnDetail>> {
    return this.http.get<ApiResponse<ReturnDetail>>(`${this.customerUrl}/${returnId}`);
  }

  cancelReturnRequest(returnId: number): Observable<ApiResponse<ReturnDetail>> {
    return this.http.put<ApiResponse<ReturnDetail>>(`${this.customerUrl}/${returnId}/cancel`, {});
  }

  // Admin methods
  getAdminReturnRequests(filter: ReturnFilter = {}): Observable<ApiResponse<PageResponse<ReturnDetail>>> {
    let params = new HttpParams();

    if (filter.keyword) params = params.set('keyword', filter.keyword);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.reason) params = params.set('reason', filter.reason);
    if (filter.userId) params = params.set('userId', filter.userId.toString());
    if (filter.fromDate) params = params.set('fromDate', filter.fromDate);
    if (filter.toDate) params = params.set('toDate', filter.toDate);
    if (filter.page !== undefined) params = params.set('page', filter.page.toString());
    if (filter.size !== undefined) params = params.set('size', filter.size.toString());
    if (filter.sortBy) params = params.set('sortBy', filter.sortBy);
    if (filter.sortDirection) params = params.set('sortDirection', filter.sortDirection);

    return this.http.get<ApiResponse<PageResponse<ReturnDetail>>>(this.adminUrl, { params });
  }

  getReturnMetrics(): Observable<ApiResponse<ReturnMetrics>> {
    return this.http.get<ApiResponse<ReturnMetrics>>(`${this.adminUrl}/metrics`);
  }

  getAdminReturnRequestById(returnId: number): Observable<ApiResponse<ReturnDetail>> {
    return this.http.get<ApiResponse<ReturnDetail>>(`${this.adminUrl}/${returnId}`);
  }

  reviewReturnRequest(returnId: number, request: ReturnReviewRequest): Observable<ApiResponse<ReturnDetail>> {
    return this.http.put<ApiResponse<ReturnDetail>>(`${this.adminUrl}/${returnId}/review`, request);
  }

  receiveReturnedItems(returnId: number, request: ReturnReceiveItemRequest): Observable<ApiResponse<ReturnDetail>> {
    return this.http.put<ApiResponse<ReturnDetail>>(`${this.adminUrl}/${returnId}/receive`, request);
  }

  processRefund(returnId: number, request: ReturnProcessRefundRequest): Observable<ApiResponse<ReturnDetail>> {
    return this.http.put<ApiResponse<ReturnDetail>>(`${this.adminUrl}/${returnId}/refund`, request);
  }
}
