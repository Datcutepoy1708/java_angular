import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DiscountService } from './discount.service';
import { environment } from '../../../environments/environment';
import { Discount, DiscountMetrics, DiscountValidationResult } from '../models/discount.model';

describe('DiscountService', () => {
  let service: DiscountService;
  let httpMock: HttpTestingController;
  const customerUrl = `${environment.apiUrl}/api/v1/discounts`;
  const adminUrl = `${environment.apiUrl}/api/v1/admin/discount-codes`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DiscountService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DiscountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should validate discount code for customer', () => {
    const mockResult: DiscountValidationResult = {
      valid: true,
      discountId: 1,
      code: 'SALE10',
      discountType: 'percent',
      discountValue: 10,
      discountAmount: 100000,
      subtotal: 1000000,
      finalTotal: 900000,
      message: 'Áp dụng thành công'
    };

    service.validateDiscount('SALE10').subscribe(res => {
      expect(res.data.valid).toBe(true);
      expect(res.data.discountAmount).toBe(100000);
    });

    const req = httpMock.expectOne(`${customerUrl}/validate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ code: 'SALE10' });
    req.flush({ success: true, message: 'OK', data: mockResult });
  });

  it('should fetch public discounts', () => {
    const mockDiscounts: Discount[] = [{
      discountId: 1,
      code: 'WELCOME',
      discountType: 'fixed',
      discountValue: 50000,
      minOrderValue: 200000,
      usageLimitPerUser: 1,
      usedCount: 10,
      startDate: '2026-01-01T00:00:00',
      endDate: '2026-12-31T23:59:59',
      status: 'active',
      createdAt: '2026-01-01T00:00:00'
    }];

    service.getPublicDiscounts().subscribe(res => {
      expect(res.data.length).toBe(1);
      expect(res.data[0].code).toBe('WELCOME');
    });

    const req = httpMock.expectOne(`${customerUrl}/public`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockDiscounts });
  });

  it('should fetch admin discount metrics', () => {
    const mockMetrics: DiscountMetrics = {
      totalDiscounts: 10,
      activeDiscounts: 8,
      totalUsedCount: 125,
      expiredDiscounts: 2
    };

    service.getMetrics().subscribe(res => {
      expect(res.data.totalDiscounts).toBe(10);
      expect(res.data.activeDiscounts).toBe(8);
    });

    const req = httpMock.expectOne(`${adminUrl}/metrics`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockMetrics });
  });

  it('should create discount via admin endpoint', () => {
    const newDiscount: Partial<Discount> = {
      code: 'NEW50K',
      discountType: 'fixed',
      discountValue: 50000
    };

    service.createDiscount(newDiscount).subscribe(res => {
      expect(res.data.code).toBe('NEW50K');
    });

    const req = httpMock.expectOne(adminUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newDiscount);
    req.flush({ success: true, message: 'Created', data: { ...newDiscount, discountId: 2 } });
  });
});
