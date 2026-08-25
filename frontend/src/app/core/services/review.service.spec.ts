import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ReviewService } from './review.service';
import { environment } from '../../../environments/environment';
import { CreateReviewRequest, RatingSummary, Review } from '../models/review.model';
import { PageResponse } from '../models/discount.model';

describe('ReviewService', () => {
  let service: ReviewService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ReviewService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ReviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get product rating summary', () => {
    const mockSummary: RatingSummary = {
      productId: 100,
      averageRating: 4.8,
      totalReviews: 25,
      ratingCounts: { 5: 20, 4: 5, 3: 0, 2: 0, 1: 0 },
      starPercentages: { 5: 80.0, 4: 20.0, 3: 0, 2: 0, 1: 0 }
    };

    service.getProductRatingSummary(100).subscribe(res => {
      expect(res.data.averageRating).toBe(4.8);
      expect(res.data.totalReviews).toBe(25);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/products/100/reviews/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockSummary });
  });

  it('should get product reviews with pagination and rating filter', () => {
    const mockPage: PageResponse<Review> = {
      content: [{
        reviewId: 1,
        productId: 100,
        userId: 2,
        userFullName: 'Nguyen Van B',
        rating: 5,
        title: 'Rất ưng ý',
        comment: 'Máy chạy êm, mát mẻ',
        isVerifiedPurchase: true,
        status: 'APPROVED',
        createdAt: '2026-08-25T10:00:00'
      }],
      number: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false
    };

    service.getProductReviews(100, 5, 0, 10).subscribe(res => {
      expect(res.data.content.length).toBe(1);
      expect(res.data.content[0].rating).toBe(5);
      expect(res.data.content[0].isVerifiedPurchase).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/products/100/reviews?page=0&size=10&rating=5`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockPage });
  });

  it('should submit review', () => {
    const request: CreateReviewRequest = {
      rating: 5,
      title: 'Tuyệt vời',
      comment: 'Sản phẩm chính hãng đóng gói kỹ lưỡng'
    };

    service.submitReview(100, request).subscribe(res => {
      expect(res.data.reviewId).toBe(10);
      expect(res.data.rating).toBe(5);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/products/100/reviews`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({
      success: true,
      message: 'OK',
      data: { reviewId: 10, productId: 100, userId: 1, ...request, isVerifiedPurchase: true, status: 'APPROVED', createdAt: '2026-08-25' }
    });
  });

  it('should update review status for admin', () => {
    service.updateReviewStatus(10, 'HIDDEN').subscribe(res => {
      expect(res.data.status).toBe('HIDDEN');
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/admin/reviews/10/status?status=HIDDEN`);
    expect(req.request.method).toBe('PUT');
    req.flush({ success: true, message: 'OK', data: { reviewId: 10, status: 'HIDDEN' } });
  });
});
