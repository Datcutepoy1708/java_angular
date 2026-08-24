import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AttributeService } from './attribute.service';
import { AttributeRequest, BatchSaveProductAttributesRequest } from '../models/attribute.model';
import { environment } from '../../../environments/environment';

describe('AttributeService', () => {
  let service: AttributeService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/attributes`;
  const productAttributesUrl = `${environment.apiUrl}/api/v1/products`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AttributeService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AttributeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get attributes by category', () => {
    const mockResponse = {
      success: true,
      message: 'OK',
      data: [
        { attributeId: 1, categoryId: 10, name: 'Socket', dataType: 'text' as const, sortOrder: 1 }
      ]
    };

    service.getByCategory(10).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.length).toBe(1);
      expect(res.data[0].name).toBe('Socket');
    });

    const req = httpMock.expectOne(`${baseUrl}/category/10`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should create an attribute', () => {
    const request: AttributeRequest = {
      categoryId: 10,
      name: 'Số nhân',
      dataType: 'number',
      unit: 'Nhân',
      sortOrder: 2
    };

    const mockResponse = {
      success: true,
      message: 'Created',
      data: { attributeId: 2, ...request, sortOrder: 2 }
    };

    service.create(request).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.attributeId).toBe(2);
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should update an attribute', () => {
    const request: AttributeRequest = {
      categoryId: 10,
      name: 'Số nhân cập nhật',
      dataType: 'number',
      unit: 'Cores',
      sortOrder: 2
    };

    const mockResponse = {
      success: true,
      message: 'Updated',
      data: { attributeId: 2, ...request, sortOrder: 2 }
    };

    service.update(2, request).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.name).toBe('Số nhân cập nhật');
    });

    const req = httpMock.expectOne(`${baseUrl}/2`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockResponse);
  });

  it('should delete an attribute', () => {
    service.delete(1).subscribe((res) => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, message: 'Deleted', data: null });
  });

  it('should get product attributes', () => {
    const mockResponse = {
      success: true,
      message: 'OK',
      data: [
        { id: 1, attributeId: 1, attributeName: 'Socket', value: 'LGA1700' }
      ]
    };

    service.getProductAttributes(100).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data[0].value).toBe('LGA1700');
    });

    const req = httpMock.expectOne(`${productAttributesUrl}/100/attributes`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should save product attributes batch', () => {
    const request: BatchSaveProductAttributesRequest = {
      attributes: [{ attributeId: 1, value: 'LGA1700' }]
    };

    const mockResponse = {
      success: true,
      message: 'Saved',
      data: [
        { id: 1, attributeId: 1, attributeName: 'Socket', value: 'LGA1700' }
      ]
    };

    service.saveProductAttributes(100, request).subscribe((res) => {
      expect(res.success).toBe(true);
      expect(res.data.length).toBe(1);
    });

    const req = httpMock.expectOne(`${productAttributesUrl}/100/attributes`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockResponse);
  });
});
