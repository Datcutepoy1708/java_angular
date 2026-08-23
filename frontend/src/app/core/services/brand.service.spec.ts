import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { BrandService } from './brand.service';
import { environment } from '../../../environments/environment';
import { BrandRequest, BrandResponse } from '../models/brand.model';

describe('BrandService', () => {
  let service: BrandService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/brands`;

  const mockBrand: BrandResponse = {
    brandId: 1,
    name: 'ASUS',
    slug: 'asus',
    logoUrl: 'https://example.com/asus.png',
    country: 'Taiwan',
    description: 'PC Hardware manufacturer',
    createdAt: '2026-08-23T00:00:00Z',
    updatedAt: '2026-08-23T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BrandService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(BrandService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all brands', () => {
    service.getAll().subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.length).toBe(1);
      expect(res.data[0].name).toBe('ASUS');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: [mockBrand] });
  });

  it('should get paginated brands with query parameters', () => {
    service.getPaginated(1, 5, 'name', 'desc').subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.content.length).toBe(1);
      expect(res.data.totalElements).toBe(10);
    });

    const req = httpMock.expectOne(`${baseUrl}/page?page=1&size=5&sortBy=name&sortDir=desc`);
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'OK',
      data: {
        content: [mockBrand],
        totalElements: 10,
        totalPages: 2,
        size: 5,
        number: 1,
      },
    });
  });

  it('should get brand by id', () => {
    service.getById(1).subscribe((res) => {
      expect(res.data.brandId).toBe(1);
      expect(res.data.name).toBe('ASUS');
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockBrand });
  });

  it('should create a new brand', () => {
    const newBrandReq: BrandRequest = {
      name: 'MSI',
      country: 'Taiwan',
    };

    service.create(newBrandReq).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.name).toBe('MSI');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newBrandReq);
    req.flush({ success: true, message: 'Created', data: { ...mockBrand, brandId: 2, name: 'MSI' } });
  });

  it('should update an existing brand', () => {
    const updateReq: BrandRequest = {
      name: 'ASUS ROG',
      country: 'Taiwan',
    };

    service.update(1, updateReq).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.name).toBe('ASUS ROG');
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updateReq);
    req.flush({ success: true, message: 'Updated', data: { ...mockBrand, name: 'ASUS ROG' } });
  });

  it('should delete a brand', () => {
    service.delete(1).subscribe((res) => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, message: 'Deleted', data: null });
  });
});
