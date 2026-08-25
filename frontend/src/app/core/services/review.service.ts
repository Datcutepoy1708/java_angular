import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import { PageResponse } from '../models/discount.model';
import {
  CreateReviewRequest,
  RatingSummary,
  Review,
  ReviewFilterParams,
  ReviewStatus
} from '../models/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  // ==========================================
  // STOREFRONT ENDPOINTS
  // ==========================================

  getProductRatingSummary(productId: number): Observable<ApiResponse<RatingSummary>> {
    return this.http.get<ApiResponse<RatingSummary>>(`${this.baseUrl}/api/v1/products/${productId}/reviews/summary`);
  }

  getProductReviews(
    productId: number,
    rating?: number,
    page = 0,
    size = 10
  ): Observable<ApiResponse<PageResponse<Review>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (rating !== undefined && rating !== null) {
      params = params.set('rating', rating.toString());
    }

    return this.http.get<ApiResponse<PageResponse<Review>>>(
      `${this.baseUrl}/api/v1/products/${productId}/reviews`,
      { params }
    );
  }

  submitReview(productId: number, request: CreateReviewRequest): Observable<ApiResponse<Review>> {
    return this.http.post<ApiResponse<Review>>(
      `${this.baseUrl}/api/v1/products/${productId}/reviews`,
      request
    );
  }

  // ==========================================
  // ADMIN BACKOFFICE ENDPOINTS
  // ==========================================

  getAdminReviews(filter: ReviewFilterParams = {}): Observable<ApiResponse<PageResponse<Review>>> {
    let params = new HttpParams();

    if (filter.productId !== undefined && filter.productId !== null) {
      params = params.set('productId', filter.productId.toString());
    }
    if (filter.rating !== undefined && filter.rating !== null) {
      params = params.set('rating', filter.rating.toString());
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

    return this.http.get<ApiResponse<PageResponse<Review>>>(
      `${this.baseUrl}/api/v1/admin/reviews`,
      { params }
    );
  }

  updateReviewStatus(reviewId: number, status: ReviewStatus): Observable<ApiResponse<Review>> {
    return this.http.put<ApiResponse<Review>>(
      `${this.baseUrl}/api/v1/admin/reviews/${reviewId}/status`,
      null,
      { params: new HttpParams().set('status', status) }
    );
  }

  deleteReview(reviewId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/api/v1/admin/reviews/${reviewId}`);
  }
}
