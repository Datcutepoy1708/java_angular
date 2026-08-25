import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import { PageResponse } from '../models/discount.model';
import {
  CreateNewsRequest,
  News,
  NewsCategory,
  NewsCategoryRequest,
  NewsFilterParams,
  UpdateNewsRequest
} from '../models/news.model';

@Injectable({
  providedIn: 'root'
})
export class NewsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  // ==========================================
  // STOREFRONT PUBLIC ENDPOINTS
  // ==========================================

  getPublicNews(categoryId?: number, page = 0, size = 9): Observable<ApiResponse<PageResponse<News>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (categoryId !== undefined && categoryId !== null && categoryId > 0) {
      params = params.set('categoryId', categoryId.toString());
    }

    return this.http.get<ApiResponse<PageResponse<News>>>(`${this.baseUrl}/api/v1/news`, { params });
  }

  getPublicNewsBySlug(slug: string): Observable<ApiResponse<News>> {
    return this.http.get<ApiResponse<News>>(`${this.baseUrl}/api/v1/news/${slug}`);
  }

  getRelatedNews(newsId: number, categoryId?: number): Observable<ApiResponse<News[]>> {
    let params = new HttpParams();
    if (categoryId) {
      params = params.set('categoryId', categoryId.toString());
    }
    return this.http.get<ApiResponse<News[]>>(`${this.baseUrl}/api/v1/news/${newsId}/related`, { params });
  }

  getPublicCategories(): Observable<ApiResponse<NewsCategory[]>> {
    return this.http.get<ApiResponse<NewsCategory[]>>(`${this.baseUrl}/api/v1/news/categories`);
  }

  // ==========================================
  // ADMIN CMS ENDPOINTS
  // ==========================================

  getAdminNews(filter: NewsFilterParams = {}): Observable<ApiResponse<PageResponse<News>>> {
    let params = new HttpParams();

    if (filter.categoryId !== undefined && filter.categoryId !== null) {
      params = params.set('categoryId', filter.categoryId.toString());
    }
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.keyword) {
      params = params.set('keyword', filter.keyword);
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

    return this.http.get<ApiResponse<PageResponse<News>>>(`${this.baseUrl}/api/v1/admin/news`, { params });
  }

  getNewsById(newsId: number): Observable<ApiResponse<News>> {
    return this.http.get<ApiResponse<News>>(`${this.baseUrl}/api/v1/admin/news/${newsId}`);
  }

  createNews(request: CreateNewsRequest): Observable<ApiResponse<News>> {
    return this.http.post<ApiResponse<News>>(`${this.baseUrl}/api/v1/admin/news`, request);
  }

  updateNews(newsId: number, request: UpdateNewsRequest): Observable<ApiResponse<News>> {
    return this.http.put<ApiResponse<News>>(`${this.baseUrl}/api/v1/admin/news/${newsId}`, request);
  }

  deleteNews(newsId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/api/v1/admin/news/${newsId}`);
  }

  // Admin Categories
  getAdminCategories(): Observable<ApiResponse<NewsCategory[]>> {
    return this.http.get<ApiResponse<NewsCategory[]>>(`${this.baseUrl}/api/v1/admin/news/categories`);
  }

  createCategory(request: NewsCategoryRequest): Observable<ApiResponse<NewsCategory>> {
    return this.http.post<ApiResponse<NewsCategory>>(`${this.baseUrl}/api/v1/admin/news/categories`, request);
  }

  updateCategory(catId: number, request: NewsCategoryRequest): Observable<ApiResponse<NewsCategory>> {
    return this.http.put<ApiResponse<NewsCategory>>(`${this.baseUrl}/api/v1/admin/news/categories/${catId}`, request);
  }

  deleteCategory(catId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/api/v1/admin/news/categories/${catId}`);
  }
}
