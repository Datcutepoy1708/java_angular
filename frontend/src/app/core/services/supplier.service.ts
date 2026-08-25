import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SupplierPage, SupplierRequest, SupplierResponse } from '../models/supplier.model';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1`;

  getActiveSuppliers(): Observable<ApiResponse<SupplierResponse[]>> {
    return this.http.get<ApiResponse<SupplierResponse[]>>(`${this.base}/suppliers/active`);
  }

  getSuppliersPaginated(
    page = 0,
    size = 10,
    keyword = '',
    status = '',
    sortBy = 'createdAt',
    sortDir = 'desc'
  ): Observable<ApiResponse<SupplierPage>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    if (status.trim() && status !== 'all') {
      params = params.set('status', status.trim());
    }

    return this.http.get<ApiResponse<SupplierPage>>(`${this.base}/admin/suppliers`, { params });
  }

  getSupplierById(id: number): Observable<ApiResponse<SupplierResponse>> {
    return this.http.get<ApiResponse<SupplierResponse>>(`${this.base}/admin/suppliers/${id}`);
  }

  createSupplier(request: SupplierRequest): Observable<ApiResponse<SupplierResponse>> {
    return this.http.post<ApiResponse<SupplierResponse>>(`${this.base}/admin/suppliers`, request);
  }

  updateSupplier(id: number, request: SupplierRequest): Observable<ApiResponse<SupplierResponse>> {
    return this.http.put<ApiResponse<SupplierResponse>>(`${this.base}/admin/suppliers/${id}`, request);
  }

  deleteSupplier(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/admin/suppliers/${id}`);
  }
}
