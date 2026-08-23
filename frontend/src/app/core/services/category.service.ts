import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategoryChildrenCount, CategoryRequest, CategoryResponse } from '../models/category.model';

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
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/categories`;

  getAll(): Observable<ApiResponse<CategoryResponse[]>> {
    return this.http.get<ApiResponse<CategoryResponse[]>>(this.base);
  }

  getTree(): Observable<ApiResponse<CategoryResponse[]>> {
    return this.http.get<ApiResponse<CategoryResponse[]>>(`${this.base}/tree`);
  }

  getRoots(): Observable<ApiResponse<CategoryResponse[]>> {
    return this.http.get<ApiResponse<CategoryResponse[]>>(`${this.base}/roots`);
  }

  getChildren(parentId: number): Observable<ApiResponse<CategoryResponse[]>> {
    return this.http.get<ApiResponse<CategoryResponse[]>>(`${this.base}/${parentId}/children`);
  }

  countChildren(id: number): Observable<ApiResponse<CategoryChildrenCount>> {
    return this.http.get<ApiResponse<CategoryChildrenCount>>(`${this.base}/${id}/children/count`);
  }

  getPaginated(page = 0, size = 10, sortBy = 'sortOrder', sortDir = 'asc'): Observable<ApiResponse<PageResponse<CategoryResponse>>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<ApiResponse<PageResponse<CategoryResponse>>>(`${this.base}/page`, { params });
  }

  getById(id: number): Observable<ApiResponse<CategoryResponse>> {
    return this.http.get<ApiResponse<CategoryResponse>>(`${this.base}/${id}`);
  }

  create(request: CategoryRequest): Observable<ApiResponse<CategoryResponse>> {
    return this.http.post<ApiResponse<CategoryResponse>>(this.base, request);
  }

  update(id: number, request: CategoryRequest): Observable<ApiResponse<CategoryResponse>> {
    return this.http.put<ApiResponse<CategoryResponse>>(`${this.base}/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`);
  }
}
