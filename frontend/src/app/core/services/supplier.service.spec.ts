import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { SupplierService } from './supplier.service';
import { SupplierRequest, SupplierResponse } from '../models/supplier.model';
import { environment } from '../../../environments/environment';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('SupplierService', () => {
  let service: SupplierService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SupplierService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SupplierService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getActiveSuppliers should return active suppliers list', () => {
    const mockSuppliers: SupplierResponse[] = [
      {
        supplierId: 1,
        name: 'ASUS',
        contactName: 'A',
        phone: null,
        email: null,
        address: null,
        status: 'active',
        createdAt: null,
        productCount: 10
      }
    ];

    service.getActiveSuppliers().subscribe(res => {
      expect(res.data.length).toBe(1);
      expect(res.data[0].name).toBe('ASUS');
    });

    const req = httpMock.expectOne(`${baseUrl}/suppliers/active`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockSuppliers });
  });

  it('getSuppliersPaginated should send query params correctly', () => {
    service.getSuppliersPaginated(0, 10, 'asus', 'active').subscribe(res => {
      expect(res.data.content.length).toBe(0);
    });

    const req = httpMock.expectOne(r =>
      r.url === `${baseUrl}/admin/suppliers` &&
      r.params.get('keyword') === 'asus' &&
      r.params.get('status') === 'active'
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'OK',
      data: { content: [], pageNumber: 0, pageSize: 10, totalElements: 0, totalPages: 0, last: true }
    });
  });

  it('createSupplier should post data', () => {
    const request: SupplierRequest = { name: 'MSI', status: 'active' };

    service.createSupplier(request).subscribe(res => {
      expect(res.data.name).toBe('MSI');
    });

    const req = httpMock.expectOne(`${baseUrl}/admin/suppliers`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({
      success: true,
      message: 'Created',
      data: {
        supplierId: 2,
        ...request,
        contactName: null,
        phone: null,
        email: null,
        address: null,
        createdAt: null,
        productCount: 0
      }
    });
  });

  it('deleteSupplier should send DELETE request', () => {
    service.deleteSupplier(1).subscribe(res => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/admin/suppliers/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, message: 'Deleted', data: null });
  });
});
