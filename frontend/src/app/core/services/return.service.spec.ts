import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ReturnService } from './return.service';
import { environment } from '../../../environments/environment';

describe('ReturnService', () => {
  let service: ReturnService;
  let httpMock: HttpTestingController;
  const customerUrl = `${environment.apiUrl}/api/v1/returns`;
  const adminUrl = `${environment.apiUrl}/api/v1/admin/returns`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ReturnService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ReturnService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should create return request', () => {
    const mockRequest = {
      orderId: 100,
      returnReason: 'DEFECTIVE',
      items: [{ orderItemId: 1, quantity: 1, itemCondition: 'OPENED' }]
    };

    const mockResponse = {
      success: true,
      message: 'Created',
      data: {
        returnId: 1,
        returnCode: 'RET-20260826-0001',
        orderId: 100,
        status: 'REQUESTED' as const,
        returnReason: 'DEFECTIVE' as const,
        refundAmount: 25000000,
        requestedAt: '2026-08-26T10:00:00',
        createdAt: '2026-08-26T10:00:00',
        updatedAt: '2026-08-26T10:00:00',
        items: [],
        imageUrls: [],
        orderTrackingNumber: 'ORD-100',
        userId: 10,
        customerName: 'Nguyen Van A',
        customerEmail: 'a@store.com'
      },
      timestamp: '2026-08-26T10:00:00'
    };

    service.createReturnRequest(mockRequest).subscribe(res => {
      expect(res.success).toBe(true);
      expect(res.data.returnCode).toBe('RET-20260826-0001');
    });

    const req = httpMock.expectOne(customerUrl);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should fetch admin return requests with filter', () => {
    const mockResponse = {
      success: true,
      message: 'Success',
      data: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 15,
        number: 0,
        first: true,
        last: true,
        empty: true
      },
      timestamp: '2026-08-26T10:00:00'
    };

    service.getAdminReturnRequests({ status: 'REQUESTED', page: 0, size: 15 }).subscribe(res => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(req => req.url === adminUrl && req.params.get('status') === 'REQUESTED');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should review return request', () => {
    const mockResponse = {
      success: true,
      message: 'Approved',
      data: { returnId: 1, returnCode: 'RET-1', status: 'APPROVED' } as any,
      timestamp: '2026-08-26T10:00:00'
    };

    service.reviewReturnRequest(1, { approved: true, adminNote: 'Ok' }).subscribe(res => {
      expect(res.data.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne(`${adminUrl}/1/review`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockResponse);
  });

  it('should process refund', () => {
    const mockResponse = {
      success: true,
      message: 'Refunded',
      data: { returnId: 1, returnCode: 'RET-1', status: 'REFUNDED' } as any,
      timestamp: '2026-08-26T10:00:00'
    };

    service.processRefund(1, { refundTransactionCode: 'FT123456' }).subscribe(res => {
      expect(res.data.status).toBe('REFUNDED');
    });

    const req = httpMock.expectOne(`${adminUrl}/1/refund`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockResponse);
  });
});
