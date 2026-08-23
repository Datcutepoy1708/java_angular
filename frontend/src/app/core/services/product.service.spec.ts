import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ProductService } from './product.service';
import { environment } from '../../../environments/environment';
import {
  ProductImageRequest,
  ProductRequest,
  ProductResponse,
  ProductVariantRequest,
} from '../models/product.model';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1`;

  const mockProduct: ProductResponse = {
    productId: 1,
    categoryId: 1,
    categoryName: 'CPU',
    categorySlug: 'cpu',
    brandId: 1,
    brandName: 'Intel',
    brandSlug: 'intel',
    supplierId: null,
    supplierName: null,
    name: 'Intel Core i7-14700K',
    slug: 'intel-core-i7-14700k',
    sku: 'BX8071514700K',
    shortDesc: '20 Cores CPU',
    description: 'High performance processor',
    warrantyMonths: 36,
    status: 'active',
    viewCount: 150,
    mainImageUrl: 'https://example.com/i7.jpg',
    images: [],
    variants: [],
    createdAt: '2026-08-23T00:00:00Z',
    updatedAt: '2026-08-23T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProductService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get filtered products', () => {
    service.getProducts({ categoryId: 1, keyword: 'Intel', page: 0, size: 10 }).subscribe((res) => {
      expect(res.data.content.length).toBe(1);
      expect(res.data.content[0].name).toBe('Intel Core i7-14700K');
    });

    const req = httpMock.expectOne((r) => r.url.includes('/api/v1/products') && r.params.has('categoryId'));
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'OK',
      data: {
        content: [mockProduct],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    });
  });

  it('should get product by id', () => {
    service.getById(1).subscribe((res) => {
      expect(res.data.productId).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}/products/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockProduct });
  });

  it('should create product', () => {
    const reqBody: ProductRequest = {
      name: 'Intel Core i7-14700K',
      categoryId: 1,
      status: 'active',
    };

    service.create(reqBody).subscribe((res) => {
      expect(res.data.name).toBe('Intel Core i7-14700K');
    });

    const req = httpMock.expectOne(`${baseUrl}/products`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'Created', data: mockProduct });
  });

  it('should create variant for product', () => {
    const variantReq: ProductVariantRequest = {
      variantName: 'Boxed',
      price: 400,
      status: 'active',
    };

    service.createVariant(1, variantReq).subscribe((res) => {
      expect(res.data.variantName).toBe('Boxed');
    });

    const req = httpMock.expectOne(`${baseUrl}/products/1/variants`);
    expect(req.request.method).toBe('POST');
    req.flush({
      success: true,
      message: 'Created',
      data: {
        variantId: 10,
        productId: 1,
        productName: 'Intel Core i7-14700K',
        variantName: 'Boxed',
        price: 400,
        status: 'active',
      },
    });
  });

  it('should add image for product', () => {
    const imgReq: ProductImageRequest = {
      imageUrl: 'https://example.com/cpu.jpg',
      imageType: 'MAIN',
    };

    service.addImage(1, imgReq).subscribe((res) => {
      expect(res.data.imageUrl).toBe('https://example.com/cpu.jpg');
    });

    const req = httpMock.expectOne(`${baseUrl}/products/1/images`);
    expect(req.request.method).toBe('POST');
    req.flush({
      success: true,
      message: 'Added',
      data: {
        imageId: 20,
        productId: 1,
        imageUrl: 'https://example.com/cpu.jpg',
        imageType: 'MAIN',
      },
    });
  });

  it('should reorder images', () => {
    service.reorderImages(1, [3, 2, 1]).subscribe((res) => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/products/1/images/reorder`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual([3, 2, 1]);
    req.flush({ success: true, message: 'Reordered', data: null });
  });
});
