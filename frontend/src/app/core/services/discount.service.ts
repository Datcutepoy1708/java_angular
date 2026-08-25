import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import {
  Discount,
  DiscountFilterParams,
  DiscountMetrics,
  DiscountUsage,
  DiscountValidationResult,
  PageResponse
} from '../models/discount.model';

@Injectable({
  providedIn: 'root'
})
export class DiscountService {
  private readonly http = inject(HttpClient);
  private readonly customerUrl = `${environment.apiUrl}/api/v1/discounts`;
  private readonly adminUrl = `${environment.apiUrl}/api/v1/admin/discount-codes`;

  // ==========================================
  // CUSTOMER / STOREFRONT ENDPOINTS
  // ==========================================

  validateDiscount(code: string): Observable<ApiResponse<DiscountValidationResult>> {
    return this.http.post<ApiResponse<DiscountValidationResult>>(`${this.customerUrl}/validate`, { code });
  }

  getPublicDiscounts(): Observable<ApiResponse<Discount[]>> {
    return this.http.get<ApiResponse<Discount[]>>(`${this.customerUrl}/public`);
  }

  // ==========================================
  // ADMIN BACKOFFICE ENDPOINTS
  // ==========================================

  getAdminDiscounts(filter: DiscountFilterParams = {}): Observable<ApiResponse<PageResponse<Discount>>> {
    let params = new HttpParams();

    if (filter.keyword) {
      params = params.set('keyword', filter.keyword);
    }
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.discountType) {
      params = params.set('discountType', filter.discountType);
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

    return this.http.get<ApiResponse<PageResponse<Discount>>>(this.adminUrl, { params });
  }

  getMetrics(): Observable<ApiResponse<DiscountMetrics>> {
    return this.http.get<ApiResponse<DiscountMetrics>>(`${this.adminUrl}/metrics`);
  }

  getDiscountById(id: number): Observable<ApiResponse<Discount>> {
    return this.http.get<ApiResponse<Discount>>(`${this.adminUrl}/${id}`);
  }

  createDiscount(data: Partial<Discount>): Observable<ApiResponse<Discount>> {
    return this.http.post<ApiResponse<Discount>>(this.adminUrl, data);
  }

  updateDiscount(id: number, data: Partial<Discount>): Observable<ApiResponse<Discount>> {
    return this.http.put<ApiResponse<Discount>>(`${this.adminUrl}/${id}`, data);
  }

  deleteDiscount(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.adminUrl}/${id}`);
  }

  getDiscountUsages(id: number): Observable<ApiResponse<DiscountUsage[]>> {
    return this.http.get<ApiResponse<DiscountUsage[]>>(`${this.adminUrl}/${id}/usages`);
  }
}
