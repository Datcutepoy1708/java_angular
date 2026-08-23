import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { CategoryService } from './category.service';
import { environment } from '../../../environments/environment';
import { CategoryRequest, CategoryResponse } from '../models/category.model';
import { BulkActionRequest } from '../models/bulk.model';

describe('CategoryService', () => {
  let service: CategoryService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/categories`;

  const mockCategory: CategoryResponse = {
    categoryId: 1,
    parentId: null,
    parentName: null,
    name: 'Components',
    slug: 'components',
    iconUrl: 'https://example.com/icon.svg',
    description: 'PC Components',
    sortOrder: 0,
    status: 'active',
    deleted: false,
    deletedAt: null,
    children: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CategoryService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(CategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all categories', () => {
    service.getAll().subscribe((res) => {
      expect(res.data.length).toBe(1);
      expect(res.data[0].name).toBe('Components');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: [mockCategory] });
  });

  it('should get category tree', () => {
    service.getTree().subscribe((res) => {
      expect(res.data.length).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}/tree`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: [mockCategory] });
  });

  it('should count children of category', () => {
    service.countChildren(1).subscribe((res) => {
      expect(res.data.childrenCount).toBe(3);
    });

    const req = httpMock.expectOne(`${baseUrl}/1/children/count`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: { categoryId: 1, childrenCount: 3 } });
  });

  it('should create category with parentId', () => {
    const catReq: CategoryRequest = {
      name: 'CPU',
      parentId: 1,
      status: 'active',
    };

    service.create(catReq).subscribe((res) => {
      expect(res.data.name).toBe('CPU');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(catReq);
    req.flush({ success: true, message: 'Created', data: { ...mockCategory, categoryId: 2, name: 'CPU', parentId: 1 } });
  });

  it('should update category', () => {
    const catReq: CategoryRequest = {
      name: 'PC Components & Parts',
      status: 'active',
    };

    service.update(1, catReq).subscribe((res) => {
      expect(res.data.name).toBe('PC Components & Parts');
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('PUT');
    req.flush({ success: true, message: 'Updated', data: { ...mockCategory, name: 'PC Components & Parts' } });
  });

  it('should soft-delete category', () => {
    service.softDelete(1).subscribe((res) => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, message: 'Deleted', data: null });
  });

  it('should restore category', () => {
    service.restore(1).subscribe((res) => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/1/restore`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ success: true, message: 'Restored', data: null });
  });

  it('should execute bulk action', () => {
    const bulkReq: BulkActionRequest = { ids: [1, 2], action: 'delete' };
    service.bulkAction(bulkReq).subscribe((res) => {
      expect(res.data.successCount).toBe(2);
    });

    const req = httpMock.expectOne(`${baseUrl}/bulk`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(bulkReq);
    req.flush({ success: true, message: 'Bulk done', data: { successCount: 2, failCount: 0, results: [] } });
  });
});
