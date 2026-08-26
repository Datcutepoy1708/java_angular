import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { AuditLogService } from './audit-log.service';
import { environment } from '../../../environments/environment';

describe('AuditLogService', () => {
  let service: AuditLogService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/admin/audit-logs`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuditLogService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AuditLogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch audit logs with filter params', () => {
    const mockResponse = {
      success: true,
      message: 'Success',
      data: {
        content: [{ logId: 1, actionType: 'CREATE', module: 'ROLE', description: 'Tạo vai trò mới', status: 'SUCCESS', createdAt: '2026-08-26T10:00:00' }],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
        first: true,
        last: true,
        empty: false
      },
      timestamp: '2026-08-26T10:00:00'
    };

    service.getAuditLogs({ module: 'ROLE', actionType: 'CREATE', page: 0, size: 10 }).subscribe(res => {
      expect(res.success).toBe(true);
      expect(res.data.content.length).toBe(1);
      expect(res.data.content[0].module).toBe('ROLE');
    });

    const req = httpMock.expectOne(req => req.url === baseUrl && req.params.get('module') === 'ROLE');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should fetch audit log by ID', () => {
    const mockDetail = {
      success: true,
      message: 'Success',
      data: { logId: 1, actionType: 'UPDATE', module: 'STAFF', description: 'Cập nhật', status: 'SUCCESS', createdAt: '2026-08-26T10:00:00' },
      timestamp: '2026-08-26T10:00:00'
    };

    service.getAuditLogById(1).subscribe(res => {
      expect(res.data.logId).toBe(1);
      expect(res.data.module).toBe('STAFF');
    });

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDetail);
  });

  it('should request CSV export with blob response', () => {
    const mockBlob = new Blob(['csv content'], { type: 'text/csv' });

    service.exportAuditLogsToCsv({ module: 'ORDER' }).subscribe(res => {
      expect(res).toBeTruthy();
    });

    const req = httpMock.expectOne(req => req.url === `${baseUrl}/export` && req.params.get('module') === 'ORDER');
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(mockBlob);
  });
});
