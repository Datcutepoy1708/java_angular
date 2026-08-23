import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BrandRequest, BrandResponse } from '../models/brand.model';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class BrandService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/brands`;

  getAll(): Observable<ApiResponse<BrandResponse[]>> {
    return this.http.get<ApiResponse<BrandResponse[]>>(this.base);
  }

  getPaginated(page = 0, size = 10, sortBy = 'name', sortDir = 'asc'): Observable<ApiResponse<PageResponse<BrandResponse>>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<ApiResponse<PageResponse<BrandResponse>>>(`${this.base}/page`, { params });
  }

  getById(id: number): Observable<ApiResponse<BrandResponse>> {
    return this.http.get<ApiResponse<BrandResponse>>(`${this.base}/${id}`);
  }

  create(request: BrandRequest): Observable<ApiResponse<BrandResponse>> {
    return this.http.post<ApiResponse<BrandResponse>>(this.base, request);
  }

  update(id: number, request: BrandRequest): Observable<ApiResponse<BrandResponse>> {
    return this.http.put<ApiResponse<BrandResponse>>(`${this.base}/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`);
  }
}
