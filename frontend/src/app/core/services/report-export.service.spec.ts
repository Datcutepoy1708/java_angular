import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ReportExportService } from './report-export.service';
import { environment } from '../../../environments/environment';

describe('ReportExportService', () => {
  let service: ReportExportService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/admin/reports`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ReportExportService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ReportExportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request revenue excel export with correct params and blob type', () => {
    const mockBlob = new Blob(['mock-excel-binary'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });

    service.exportRevenueExcel('2026-10-01', '2026-10-10', 'day').subscribe((blob) => {
      expect(blob).toBeTruthy();
      expect(blob.size).toBe(mockBlob.size);
    });

    const req = httpMock.expectOne((r) =>
      r.url === `${baseUrl}/revenue/excel` &&
      r.params.get('startDate') === '2026-10-01' &&
      r.params.get('endDate') === '2026-10-10' &&
      r.params.get('period') === 'day'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(mockBlob);
  });

  it('should request orders excel export with filter params', () => {
    const mockBlob = new Blob(['orders-data']);

    service.exportOrdersExcel({ keyword: 'ORD-123', status: 'CONFIRMED' }).subscribe((blob) => {
      expect(blob).toBeTruthy();
    });

    const req = httpMock.expectOne((r) =>
      r.url === `${baseUrl}/orders/excel` &&
      r.params.get('keyword') === 'ORD-123' &&
      r.params.get('status') === 'CONFIRMED'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockBlob);
  });

  it('should request inventory excel export with warehouseId and stockStatus', () => {
    const mockBlob = new Blob(['inventory-data']);

    service.exportInventoryExcel(1, 'CPU', 'LOW_STOCK').subscribe((blob) => {
      expect(blob).toBeTruthy();
    });

    const req = httpMock.expectOne((r) =>
      r.url === `${baseUrl}/inventory/excel` &&
      r.params.get('warehouseId') === '1' &&
      r.params.get('keyword') === 'CPU' &&
      r.params.get('stockStatus') === 'LOW_STOCK'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockBlob);
  });
});
