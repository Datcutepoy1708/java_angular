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
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1`;

  // ─── Product CRUD ─────────────────────────────────────────────

  getProducts(filter: ProductFilterRequest = {}): Observable<ApiResponse<PageResponse<ProductResponse>>> {
    let params = new HttpParams();
    if (filter.categoryId != null) params = params.set('categoryId', filter.categoryId);
    if (filter.brandId != null) params = params.set('brandId', filter.brandId);
    if (filter.supplierId != null) params = params.set('supplierId', filter.supplierId);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.keyword) params = params.set('keyword', filter.keyword);
    params = params.set('page', filter.page ?? 0);
    params = params.set('size', filter.size ?? 10);
    return this.http.get<ApiResponse<PageResponse<ProductResponse>>>(`${this.base}/products`, { params });
  }

  getById(id: number): Observable<ApiResponse<ProductResponse>> {
    return this.http.get<ApiResponse<ProductResponse>>(`${this.base}/products/${id}`);
  }

  create(request: ProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.post<ApiResponse<ProductResponse>>(`${this.base}/products`, request);
  }

  update(id: number, request: ProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.put<ApiResponse<ProductResponse>>(`${this.base}/products/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/products/${id}`);
  }

  // ─── Variant CRUD ─────────────────────────────────────────────

  getVariants(productId: number): Observable<ApiResponse<ProductVariantResponse[]>> {
    return this.http.get<ApiResponse<ProductVariantResponse[]>>(`${this.base}/products/${productId}/variants`);
  }

  createVariant(productId: number, request: ProductVariantRequest): Observable<ApiResponse<ProductVariantResponse>> {
    return this.http.post<ApiResponse<ProductVariantResponse>>(`${this.base}/products/${productId}/variants`, request);
  }

  updateVariant(variantId: number, request: ProductVariantRequest): Observable<ApiResponse<ProductVariantResponse>> {
    return this.http.put<ApiResponse<ProductVariantResponse>>(`${this.base}/variants/${variantId}`, request);
  }

  deleteVariant(variantId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/variants/${variantId}`);
  }

  // ─── Image CRUD ───────────────────────────────────────────────

  getImages(productId: number): Observable<ApiResponse<ProductImageResponse[]>> {
    return this.http.get<ApiResponse<ProductImageResponse[]>>(`${this.base}/products/${productId}/images`);
  }

  addImage(productId: number, request: ProductImageRequest): Observable<ApiResponse<ProductImageResponse>> {
    return this.http.post<ApiResponse<ProductImageResponse>>(`${this.base}/products/${productId}/images`, request);
  }

  updateImage(imageId: number, request: ProductImageRequest): Observable<ApiResponse<ProductImageResponse>> {
    return this.http.put<ApiResponse<ProductImageResponse>>(`${this.base}/images/${imageId}`, request);
  }

  deleteImage(imageId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/images/${imageId}`);
  }

  setMainImage(imageId: number): Observable<ApiResponse<ProductImageResponse>> {
    return this.http.patch<ApiResponse<ProductImageResponse>>(`${this.base}/images/${imageId}/main`, {});
  }

  /**
   * Reorder images by sending the ordered list of imageIds.
   * Called after drag-and-drop reordering in the product form.
   */
  reorderImages(productId: number, imageIds: number[]): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.base}/products/${productId}/images/reorder`, imageIds);
  }
}
