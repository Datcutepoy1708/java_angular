import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ProductFilterRequest,
  ProductImageRequest,
  ProductImageResponse,
  ProductRequest,
  ProductResponse,
  ProductVariantRequest,
  ProductVariantResponse,
} from '../models/product.model';
import { BulkActionRequest, BulkActionResult } from '../models/bulk.model';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1`;

  // ─── Product CRUD ─────────────────────────────────────────────

  getProducts(filter: ProductFilterRequest = {}): Observable<ApiResponse<PageResponse<ProductResponse>>> {
    let params = new HttpParams();
    if (filter.categoryId != null) params = params.set('categoryId', filter.categoryId);
    if (filter.brandId != null) params = params.set('brandId', filter.brandId);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.keyword) params = params.set('keyword', filter.keyword);
    if (filter.attributes) params = params.set('attributes', filter.attributes);
    if (filter.minPrice != null) params = params.set('minPrice', filter.minPrice);
    if (filter.maxPrice != null) params = params.set('maxPrice', filter.maxPrice);
    if (filter.sortBy) params = params.set('sortBy', filter.sortBy);
    if (filter.sortDir) params = params.set('sortDir', filter.sortDir);
    params = params.set('page', filter.page ?? 0);
    params = params.set('size', filter.size ?? 10);
    return this.http.get<ApiResponse<PageResponse<ProductResponse>>>(`${this.base}/products`, { params });
  }

  getTrash(page = 0, size = 10): Observable<ApiResponse<PageResponse<ProductResponse>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<ProductResponse>>>(`${this.base}/products/trash`, { params });
  }

  getById(id: number): Observable<ApiResponse<ProductResponse>> {
    return this.http.get<ApiResponse<ProductResponse>>(`${this.base}/products/${id}`);
  }

  getBySlug(slug: string): Observable<ApiResponse<ProductResponse>> {
    return this.http.get<ApiResponse<ProductResponse>>(`${this.base}/products/slug/${slug}`);
  }

  getProductById(id: number): Observable<ApiResponse<ProductResponse>> {
    return this.getById(id);
  }

  getProductBySlug(slug: string): Observable<ApiResponse<ProductResponse>> {
    return this.getBySlug(slug);
  }

  create(request: ProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.post<ApiResponse<ProductResponse>>(`${this.base}/products`, request);
  }

  update(id: number, request: ProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.put<ApiResponse<ProductResponse>>(`${this.base}/products/${id}`, request);
  }

  softDelete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/products/${id}`);
  }

  restore(id: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.base}/products/${id}/restore`, {});
  }

  bulkAction(request: BulkActionRequest): Observable<ApiResponse<BulkActionResult>> {
    return this.http.patch<ApiResponse<BulkActionResult>>(`${this.base}/products/bulk`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.softDelete(id);
  }

  // ─── Variant CRUD ─────────────────────────────────────────────

  getVariants(productId: number): Observable<ApiResponse<ProductVariantResponse[]>> {
    return this.http.get<ApiResponse<ProductVariantResponse[]>>(`${this.base}/products/${productId}/variants`);
  }

  getDeletedVariants(productId: number): Observable<ApiResponse<ProductVariantResponse[]>> {
    return this.http.get<ApiResponse<ProductVariantResponse[]>>(`${this.base}/products/${productId}/variants/deleted`);
  }

  createVariant(productId: number, request: ProductVariantRequest): Observable<ApiResponse<ProductVariantResponse>> {
    return this.http.post<ApiResponse<ProductVariantResponse>>(`${this.base}/products/${productId}/variants`, request);
  }

  updateVariant(variantId: number, request: ProductVariantRequest): Observable<ApiResponse<ProductVariantResponse>> {
    return this.http.put<ApiResponse<ProductVariantResponse>>(`${this.base}/variants/${variantId}`, request);
  }

  softDeleteVariant(variantId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/variants/${variantId}`);
  }

  restoreVariant(variantId: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.base}/variants/${variantId}/restore`, {});
  }

  deleteVariant(variantId: number): Observable<ApiResponse<void>> {
    return this.softDeleteVariant(variantId);
  }

  // ─── Image CRUD ───────────────────────────────────────────────

  getImages(productId: number): Observable<ApiResponse<ProductImageResponse[]>> {
    return this.http.get<ApiResponse<ProductImageResponse[]>>(`${this.base}/products/${productId}/images`);
  }

  getDeletedImages(productId: number): Observable<ApiResponse<ProductImageResponse[]>> {
    return this.http.get<ApiResponse<ProductImageResponse[]>>(`${this.base}/products/${productId}/images/deleted`);
  }

  addImage(productId: number, request: ProductImageRequest): Observable<ApiResponse<ProductImageResponse>> {
    return this.http.post<ApiResponse<ProductImageResponse>>(`${this.base}/products/${productId}/images`, request);
  }

  updateImage(imageId: number, request: ProductImageRequest): Observable<ApiResponse<ProductImageResponse>> {
    return this.http.put<ApiResponse<ProductImageResponse>>(`${this.base}/images/${imageId}`, request);
  }

  softDeleteImage(imageId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/images/${imageId}`);
  }

  restoreImage(imageId: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.base}/images/${imageId}/restore`, {});
  }

  deleteImage(imageId: number): Observable<ApiResponse<void>> {
    return this.softDeleteImage(imageId);
  }

  setMainImage(imageId: number): Observable<ApiResponse<ProductImageResponse>> {
    return this.http.patch<ApiResponse<ProductImageResponse>>(`${this.base}/images/${imageId}/main`, {});
  }

  reorderImages(productId: number, imageIds: number[]): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/products/${productId}/images/reorder`, imageIds);
  }
}
